package com.fowler.tinyfileserver;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum MimeType {

    CSS("text/css", "css"),
    HTML("text/html", "html", "htm"),
    PLAIN_TEXT("text/plain", "conf", "c", "cxx", "c++", "pl", "log", "txt", "text", "cc", "java", "py", "cpp", "h"),
    XML("text/xml", "xml"),

    JAVASCRIPT("application/javascript", "js"),
    OCTET_STREAM("application/octet-stream"),
    ZIP("application/zip", "zip"),

    JPG("image/jpeg", true, "jpg", "jpeg"),
    PNG("image/png", true, "png"),
    SVG("image/svg+xml", true, "svg"),
    BMP("image/bmp", true, "bmp"),
    GIF("image/gif", true, "gif"),
    XPM("image/xpm", true, "xpm"),
    TIFF("image/tiff", true, "tiff", "tif"),
    ICON("image/x-icon", true, "ico"),

    MPEG("video/mpeg", "mpeg", "mpg"),
    AVI("video/avi", "avi"),
    QUICKTIME("video/quicktime", "mov", "qt"),

    MPEG3("audio/mpeg3", "mp3"),
    MIDI("audio/midi", "midi", "mid"),

    JSON("application/json", "org/json");

    private String value;
    private Set<String> extensions = new HashSet<>();
    private boolean image;

    MimeType(String value, String... extensions) {
        this(value, false, extensions);
    }

    MimeType(String value, boolean image, String... extensions) {
        this.value = value;
        this.image = image;
        Collections.addAll(this.extensions, extensions);
    }

    public boolean isImage() {
        return image;
    }

    @Override
    public String toString() {
        return value;
    }

    public static MimeType forFile(File file) {
        return forFile(file.getName());
    }

    public static MimeType forFile(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if(idx > -1 && idx < fileName.length() - 1) {
            String ext = fileName.substring(idx + 1).toLowerCase();
            for(MimeType mimeType : MimeType.values()) {
                if(mimeType.extensions.contains(ext))
                    return mimeType;
            }
        }

        return OCTET_STREAM;
    }
}
