package com.fowler.tinyfileserver;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {

    private static final Logger logger = Logger.getLogger(HttpServer.class.getName());

    private static final int DEFAULT_PORT = 8080;

    private static class HttpServerHolder {
        private static final HttpServer instance = new HttpServer();
    }

    public static HttpServer getHttpServer() {
        return HttpServerHolder.instance;
    }

    private HttpServer() {
        super(DEFAULT_PORT);
    }

    private Context context;
    private String user = "admin";
    private String password;
    private Set<String> validSessionIds = Collections.synchronizedSet(new HashSet<String>());
    private Favorites favorites;

    public void start(Context context) throws IOException {
        this.context = context;
        this.favorites = new Favorites(context);
        this.password = UUID.randomUUID().toString().split("-")[0];
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    @Override
    public void stop() {
        super.stop();
        validSessionIds.clear();
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {

            Response response;

            if(isUnprotected(session.getUri()) || authenticate(session)) {
                switch(session.getMethod()) {
                    case GET:
                        response = handleGetRequest(session);
                        break;
                    case POST:
                        response = handlePostRequest(session);
                        break;
                    case DELETE:
                        response = handleDeleteRequest(session);
                        break;
                    default:
                        response = errorResponse(Response.Status.METHOD_NOT_ALLOWED);
                }
            } else {
//                response = errorResponse(Response.Status.FORBIDDEN);
//                response = failedAuthResponse();
                logger.info("Failed or missing authentication; redirecting to login page");
                response = redirectResponse("/login.html");
            }

//        response.addHeader("Access-Control-Allow-Origin", "*");
            return response;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Failed to handle http request", e);
            return errorResponse(Response.Status.INTERNAL_ERROR);
        }
    }

    private boolean isUnprotected(String uri) {
        String resource = uri.replace("/", "");
        return "login".equals(resource) || assetExists("unprotected/" + resource);
    }

    private boolean assetExists(String fileName) {
        try {
            //todo: close this?  better way to handle this?
            context.getAssets().open(fileName);
            return true;
        } catch(IOException e) {
            return false;
        }
    }

    private boolean authenticate(IHTTPSession session) {
        String sessionId = session.getCookies().read("sessionid");
        return sessionId != null && validSessionIds.contains(sessionId);
    }

    private Response failedAuthResponse() {
//        Response response = errorResponse(Response.Status.UNAUTHORIZED);
//        response.addHeader("WWW-Authenticate", "Basic realm=\"basic\"");
//        return response;

        Response response = okResponse();
        response.addHeader("REQUIRES_AUTH", "1");
        return response;
    }

    private Response handleGetRequest(IHTTPSession session) {

        String command = session.getUri().replace("/", "").toLowerCase();
        if("".equals(command))
            command = "index.html";

        Map<String, List<String>> queryParams = session.getParms();

        switch(command) {

            case "logout": {
                String sessionId = session.getCookies().read("sessionid");
                if(sessionId != null) {
                    validSessionIds.remove(sessionId);
                }
//                return okResponse();
                return redirectResponse("/login.html");
            }

            case "list": {
                File dir;
                if (queryParams.containsKey("dir")) {
                    dir = new File(queryParams.get("dir").get(0));
                    if (!dir.exists())
                        return errorResponse(Response.Status.NOT_FOUND);
                    else if (!dir.isDirectory())
                        return errorResponse(Response.Status.BAD_REQUEST);
                } else {
                    dir = Environment.getExternalStorageDirectory();
                }
                DirectoryContent dirContent = new DirectoryContent(dir);
                try {
                    return newFixedLengthResponse(Response.Status.OK, MimeType.JSON.toString(), dirContent.toJSON());
                } catch (JSONException e) {
                    logger.log(Level.SEVERE, "Failed to render JSON for directory content of " + dirContent.getDir(), e);
                    return errorResponse(Response.Status.INTERNAL_ERROR);
                }
            }

            case "download": {
                if (queryParams.containsKey("path")) {

                    List<File> paths = IOUtils.toFiles(queryParams.get("path"));

                    if(paths.size() == 1 && paths.get(0).isFile()) {
                        favorites.addFavorite(paths.get(0).getParentFile());
                        return fileResponse(paths.get(0), true);

                    } else {

                        ZipFile zipFile = new ZipFile();
                        for(File pathFile : paths)
                            zipFile.add(pathFile);

                        try {
                            favorites.addFavorite(paths.get(0).getParentFile());
                            return fileResponse(zipFile.zip(), true);
                        } catch (IOException e) {
                            return errorResponse(Response.Status.INTERNAL_ERROR);
                        }
                    }
                } else {
                    return errorResponse(Response.Status.BAD_REQUEST);
                }
            }

            case "view": {
                if (queryParams.containsKey("file")) {
                    File file = new File(queryParams.get("file").get(0));
                    favorites.addFavorite(file.getParentFile());
                    return fileResponse(file, false);
                } else {
                    return errorResponse(Response.Status.BAD_REQUEST);
                }
            }

            case "thumbnail": {
                if(!queryParams.containsKey("file"))
                    return errorResponse(Response.Status.BAD_REQUEST);
                File file = new File(queryParams.get("file").get(0));
                if(!file.isFile())
                    return errorResponse(Response.Status.BAD_REQUEST);
                MimeType mimeType = MimeType.forFile(file);
                if(!mimeType.isImage())
                    return errorResponse(Response.Status.BAD_REQUEST);

                try {
                    InputStream is = ImageUtils.getThumbmailInputStream(file, 100, Bitmap.CompressFormat.PNG);
                    return newChunkedResponse(Response.Status.OK, MimeType.PNG.toString(), is);
                } catch(IOException e) {
                    return errorResponse(Response.Status.INTERNAL_ERROR);
                }
            }

            //todo test server side favorites
            //todo implement client side favorites
            case "favorites": {
                try {
                    List<Favorite> favorites = this.favorites.getFavorites();
                    String json = Favorite.toJSON(favorites);
                    return newFixedLengthResponse(Response.Status.OK, MimeType.JSON.toString(), json);
                } catch(Exception e) {
                    return errorResponse(Response.Status.INTERNAL_ERROR);
                }
            }

            default: {
                // Default is to assume this is an asset and try to read it from the asset manager.
                return fileResponse(command);
            }
        }
    }

    private Response handlePostRequest(IHTTPSession session) {

        String command = session.getUri().replace("/", "").toLowerCase();

        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
            logger.info("Files map:");
            for(Map.Entry<String, String> entry : files.entrySet()) {
                logger.info(entry.getKey() + " => " + entry.getValue());
            }

            logger.info("Parms map:");
            for(Map.Entry<String, List<String>> entry : session.getParms().entrySet()) {
                logger.info(entry.getKey() + " => " + entry.getValue());
            }

        } catch (IOException e) {
            logger.log(Level.SEVERE, "parseBody failed with IOException", e);
        } catch (ResponseException e) {
            logger.log(Level.SEVERE, "parseBody failed with ResponseException", e);
        }

        Map<String, List<String>> params = session.getParms();

        switch(command) {

            case "login": {
                List<String> user = params.get("user");
                List<String> password = params.get("password");
                if(user != null && user.size() == 1 && password != null && password.size() == 1 &&
                        user.get(0).equals(getUser()) && password.get(0).equals(getPassword())) {
                    String newSessionId = UUID.randomUUID().toString().split("-")[4];
                    validSessionIds.add(newSessionId);
                    session.getCookies().set("sessionid", newSessionId, 30);
                    logger.info("Successful authentication, session id = " + newSessionId);
//                    return okResponse();
                    return redirectResponse("/index.html");
                } else {
                    // Is forbidden more appropriate than unauthorized?
//                    return errorResponse(Response.Status.FORBIDDEN);
                    return redirectResponse("/login.html");
                }
            }

            case "upload": {
                String fileName = params.get("file").get(0);
                String dirPath = params.get("dir").get(0);
                String tempFilePath = files.get("file");

                if(fileName != null && dirPath != null && tempFilePath != null) {
                    File file = new File(dirPath, fileName);
                    File tempFile = new File(tempFilePath);
                    if(file.getParentFile().canWrite()) {
                        try {
                            IOUtils.copy(tempFile, file);
                            favorites.addFavorite(file.getParentFile());
                            return okResponse();
                        } catch(IOException e) {
                            logger.log(Level.SEVERE, "Failed to write file " + file, e);
                            return errorResponse(Response.Status.INTERNAL_ERROR);
                        }
                    } else {
                        return errorResponse(Response.Status.FORBIDDEN);
                    }
                } else {
                    return errorResponse(Response.Status.BAD_REQUEST);
                }
            }

            default:
                return errorResponse(Response.Status.BAD_REQUEST);
        }
    }

    private Response handleDeleteRequest(IHTTPSession session) {

        Map<String, List<String>> queryParams = session.getParms();
        if(queryParams.containsKey("path")) {

            List<File> paths = IOUtils.toFiles(queryParams.get("path"));
            for(File path : paths) {
                if(!path.exists())
                    return errorResponse(Response.Status.NOT_FOUND);
                if(!path.canWrite())
                    return errorResponse(Response.Status.FORBIDDEN);

                try {
                    IOUtils.delete(path);
                } catch(IOException e) {
                    return errorResponse(Response.Status.INTERNAL_ERROR);
                }
            }
            favorites.addFavorite(paths.get(0).getParentFile());
            return okResponse();
        } else if(queryParams.containsKey("favorite")) {
            List<String> dirs = queryParams.get("favorite");
            for(String dir : dirs)
                favorites.deleteFavorite(dir);
            return okResponse();
        } else {
            return errorResponse(Response.Status.BAD_REQUEST);
        }
    }

    private Response errorResponse(Response.Status status) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, status.getDescription());
    }

    private Response fileResponse(File file, boolean attachment) {

        if(!file.exists())
            return errorResponse(Response.Status.NOT_FOUND);
        else if(!file.isFile())
            return errorResponse(Response.Status.BAD_REQUEST);

        try {
            MimeType mimeType = MimeType.forFile(file);
            Response response = newChunkedResponse(Response.Status.OK, mimeType.toString(), new FileInputStream(file));
            if(attachment)
                response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));
            return response;
        } catch(FileNotFoundException e) {
            return errorResponse(Response.Status.NOT_FOUND);
        }
    }

    private Response fileResponse(String fileName) {
        AssetManager assets = context.getAssets();
        InputStream is;
        try {
            try {
                is = assets.open("unprotected/" + fileName);
            } catch(FileNotFoundException e) {
                is = assets.open(fileName);
            }
        } catch(FileNotFoundException e) {
            return errorResponse(Response.Status.NOT_FOUND);
        } catch(IOException e) {
            return errorResponse(Response.Status.INTERNAL_ERROR);
        }

        MimeType mimeType = MimeType.forFile(fileName);
        Response response = newChunkedResponse(Response.Status.OK, mimeType.toString(), is);
        if(mimeType == MimeType.OCTET_STREAM)
            response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
        return response;
    }

    private Response okResponse() {
        return newFixedLengthResponse(Response.Status.OK, MimeType.PLAIN_TEXT.toString(), "");
    }

    private Response redirectResponse(String location) {
        Response response = errorResponse(Response.Status.REDIRECT);
        response.addHeader("Location", location);
        return response;
    }
}
