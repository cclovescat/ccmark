package com.example.watermarkapp;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {

    public static Bitmap loadBitmapFromUri(Context context, Uri uri, int maxSize) {
        if (context == null || uri == null) {
            return null;
        }

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    return null;
                }
                BitmapFactory.decodeStream(inputStream, null, options);
            }

            int scale = calculateInSampleSize(options.outWidth, options.outHeight, maxSize);

            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    return null;
                }
                return BitmapFactory.decodeStream(inputStream, null, options);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap loadBitmapFromUri(Context context, Uri uri) {
        return loadBitmapFromUri(context, uri, 4096);
    }

    public static int calculateInSampleSize(int width, int height, int maxSize) {
        if (width <= 0 || height <= 0 || maxSize <= 0) {
            return 1;
        }
        int sampleSize = 1;
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    public static String saveImageToGallery(Context context, File sourceFile, String fileName) {
        if (context == null || sourceFile == null || !sourceFile.exists()) {
            return null;
        }

        String displayName = fileName;
        if (displayName == null || displayName.isEmpty()) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            displayName = "WM_" + timeStamp + ".png";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Watermark");

            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return null;
            }

            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    return null;
                }
                copyFileToStream(sourceFile, outputStream);
                return uri.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File watermarkDir = new File(picturesDir, "Watermark");
            if (!watermarkDir.exists() && !watermarkDir.mkdirs()) {
                return null;
            }

            File imageFile = new File(watermarkDir, displayName);
            try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                copyFileToStream(sourceFile, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            return Uri.fromFile(imageFile).toString();
        }
    }

    public static String copyUriToInternal(Context context, Uri uri, String fileName) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            File file = new File(context.getFilesDir(), fileName);
            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void copyFileToStream(File source, OutputStream target) throws IOException {
        try (InputStream inputStream = new java.io.FileInputStream(source)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                target.write(buffer, 0, bytesRead);
            }
            target.flush();
        }
    }

    public static Bitmap loadBitmapFromFile(String path, int maxSize) {
        if (path == null) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            int scale = calculateInSampleSize(options.outWidth, options.outHeight, maxSize);

            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
