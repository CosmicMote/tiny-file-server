package com.fowler.tinyfileserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFile {

    private static final Logger logger = Logger.getLogger(ZipFile.class.getName());

    private static final int BUFFER = 2048;

    private String zipFileName;
    private List<File> files;
    private Map<File, List<File>> dirToFiles;

    public ZipFile() {
    }

    public ZipFile(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public ZipFile(String zipFileName, List<String> paths) {
        this.zipFileName = zipFileName;
        for(String path : paths)
            add(new File(path));
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

    public ZipFile(File dir) {
        this.zipFileName = dir.getName() + ".zip";
        add(dir);
    }

    public void add(File fileOrDir) {
        if(fileOrDir.isFile()) {
            if(files == null)
                files = new ArrayList<>();
            files.add(fileOrDir);
        } else if(fileOrDir.isDirectory()) {
            List<File> dirFiles = new ArrayList<>();
            if(dirToFiles == null)
                dirToFiles = new HashMap<>();
            dirToFiles.put(fileOrDir, dirFiles);
            getDescendantFiles(fileOrDir, dirFiles);
        }
    }

    private void getDescendantFiles(File dir, List<File> files) {
        for(File child : dir.listFiles()) {
            if(child.isFile())
                files.add(child);
            else if(child.isDirectory())
                getDescendantFiles(child, files);
        }
    }

    public File zip() throws IOException {

        if(zipFileName == null) {

            // If there's just one file, name the zip after the file
            if(files != null && files.size() == 1 && dirToFiles == null)
                zipFileName = files.get(0).getName() + ".zip";

            // else if there's just one directory, name the zip after that directory
            else if(dirToFiles != null && dirToFiles.size() == 1 && files == null)
                zipFileName = dirToFiles.keySet().iterator().next().getName() + ".zip";

            // else default file name
            else
                zipFileName = "files.zip";
        }

        BufferedInputStream origin;

        TempFileManager tempFileManager = TempFileManager.getTempFileManager();
        File zipFile = tempFileManager.getFile(zipFileName);
        FileOutputStream dest = new FileOutputStream(zipFile);

        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

        if(dirToFiles != null) {
            for(Map.Entry<File, List<File>> mapEntry : dirToFiles.entrySet()) {
                File dir = mapEntry.getKey();
                List<File> files = mapEntry.getValue();
                for(File file : files) {
                    logger.fine("Adding: " + file);
                    FileInputStream fi = new FileInputStream(file);
                    origin = new BufferedInputStream(fi, BUFFER);
                    // If we're zipping from a directory, we'd like to keep the zip file structure
                    // rooted at that directory. Strip out parent directory information.
                    String path = file.getAbsolutePath().replace(dir.getParent(), "");
                    if(path.startsWith("/"))
                        path = path.substring(1);
                    ZipEntry zipEntry = new ZipEntry(path);
                    out.putNextEntry(zipEntry);
                    IOUtils.copy(origin, out);
                    origin.close();
                }
            }
        }

        if(files != null) {
            for(File file : files) {
                logger.fine("Adding: " + file);
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry zipEntry = new ZipEntry(file.getName());
                out.putNextEntry(zipEntry);
                IOUtils.copy(origin, out);
                origin.close();
            }
        }

        out.close();

        return zipFile;
    }
}
