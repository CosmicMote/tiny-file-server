package com.fowler.tinyfileserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TempFileManager {

    private static final Logger logger = Logger.getLogger(TempFileManager.class.getName());

    private static class TempFileManagerHolder {
        private static TempFileManager instance = new TempFileManager();
    }

    public static TempFileManager getTempFileManager() {
        return TempFileManagerHolder.instance;
    }

    private TempFileManager() {  }

    private List<File> files = new ArrayList<>();

    public File getFile(String fileName) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tempDir, fileName);
        files.add(file);
        return file;
    }

    public void clear() {
        for(File file : files) {
            if(!file.delete())
                logger.severe("Failed to delete temp file: " + file);
        }
        files.clear();
    }
}
