package com.fowler.tinyfileserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageUtils {

    public static int[] getDimensions(File imageFile) {
        validateImage(imageFile);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        return new int[] { options.outWidth, options.outHeight };
    }

    public static InputStream getThumbmailInputStream(File imageFile,
                                                      int maxWidthOrHeight,
                                                      Bitmap.CompressFormat compressFormat)
            throws IOException {
        validateImage(imageFile);
        int[] dimensions = getDimensions(imageFile);
        int imageWidth = dimensions[0];
        int imageHeight = dimensions[1];
        int maxDimension = Math.max(imageWidth, imageHeight);
        if(maxDimension > maxWidthOrHeight) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            float scaleFactor = (float)maxDimension / (float)maxWidthOrHeight;
            options.inSampleSize = Math.round(scaleFactor);
            Bitmap scaledBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaledBitmap.compress(compressFormat, 0, baos); // 0 means compress for low size, but ignored for PNG
            return new ByteArrayInputStream(baos.toByteArray());
        } else {
            return new FileInputStream(imageFile);
        }
    }

    private static void validateImage(File file) {
        if(!file.isFile() || !MimeType.forFile(file).isImage())
            throw new IllegalArgumentException("Invalid image file: " + file);
    }

    private ImageUtils() {}
}
