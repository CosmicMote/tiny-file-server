package com.fowler.tinyfileserver;

import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class DirectoryContent {

    private static final Logger logger = Logger.getLogger(DirectoryContent.class.getName());

    private final File dir;
    private final List<File> files = new ArrayList<>();

    public DirectoryContent() {
        this(null);
    }

    public DirectoryContent(File dir) {

        if(dir != null) {
            this.dir = dir;
        } else {
            this.dir = new File(System.getProperty("user.home"));
        }

        File[] fileArr = this.dir.listFiles();
        if(fileArr != null) {
            Collections.addAll(files, fileArr);

            Collections.sort(this.files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    int compare = Boolean.valueOf(f1.isDirectory()).compareTo(f2.isDirectory());
                    if(compare != 0)
                        return compare;

                    return f1.getName().compareTo(f2.getName());
                }
            });
        } else {
            logger.severe("Could not list files of " + this.dir);
        }
    }

    public String toJSON() throws JSONException {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<JSONObject> entries = new ArrayList<>();
        for(File file : files) {
            JSONObject entry = new JSONObject();
            entry.put("isDir", file.isDirectory());
            entry.put("name", file.getName());
            entry.put("path", file.getAbsolutePath());
            if(file.isFile())
                entry.put("size", file.length());
            entry.put("lastModified", sdf.format(new Date(file.lastModified())));
            MimeType mimeType = MimeType.forFile(file);
            entry.put("isImage", mimeType.isImage());
            if(mimeType.isImage()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), options);
                int width = options.outWidth;
                int height = options.outHeight;
//                String mediaType = options.outMimeType;
                entry.put("imageWidth", width);
                entry.put("imageHeight", height);
            }
            entries.add(entry);
        }

        JSONObject json = new JSONObject();
        json.put("dir", dir.getPath());
        json.put("writable", dir.canWrite());
        json.put("entries", new JSONArray(entries));

        return json.toString(4);  //human-readable, indent 4 spaces
    }

    public File getDir() {
        return dir;
    }
}
