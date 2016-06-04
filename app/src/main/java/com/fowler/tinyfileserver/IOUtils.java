package com.fowler.tinyfileserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class IOUtils {

    private static final int BUFFER = 2048;

    public static void copy(InputStream in, OutputStream out) throws IOException {
        int count;
        byte[] data = new byte[BUFFER];
        while ((count = in.read(data, 0, BUFFER)) != -1) {
            out.write(data, 0, count);
        }
    }

    public static void copy(File source, File dest) throws IOException {

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(dest);
            copy(in, out);
        } finally {
            if(in != null)
                in.close();
            if(out != null)
                out.close();
        }
    }

    public static void delete(File path) throws IOException {

        if(path.isDirectory()) {
            for (File child : path.listFiles())
                delete(child);
        }

        if(!path.delete())
            throw new IOException("Failed to delete: " + path);
    }

    public static List<File> toFiles(List<String> paths) {
        List<File> files = new ArrayList<>(paths.size());
        for(String path : paths)
            files.add(new File(path));
        return files;
    }

    private IOUtils() {}
}
