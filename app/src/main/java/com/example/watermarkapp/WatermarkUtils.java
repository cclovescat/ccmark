package com.example.watermarkapp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

public class WatermarkUtils {

    public static Bitmap addWatermark(Bitmap sourceBitmap, Bitmap whiteWatermark, Bitmap blackWatermark, int alpha) {
        return addWatermark(sourceBitmap, pickWatermark(sourceBitmap, whiteWatermark, blackWatermark), alpha);
    }

    public static Bitmap addWatermark(Bitmap sourceBitmap, Bitmap watermarkToUse, int alpha) {
        if (sourceBitmap == null) {
            return null;
        }

        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();

        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(sourceBitmap, 0, 0, null);

        if (watermarkToUse == null) {
            return resultBitmap;
        }

        Bitmap scaledWatermark = scaleWatermarkToCover(watermarkToUse, width, height);

        float left = (width - scaledWatermark.getWidth()) / 2f;
        float top = (height - scaledWatermark.getHeight()) / 2f;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setAlpha(alpha);
        canvas.drawBitmap(scaledWatermark, left, top, paint);

        if (scaledWatermark != watermarkToUse) {
            scaledWatermark.recycle();
        }

        return resultBitmap;
    }

    public static Bitmap addWatermark(Bitmap sourceBitmap, Bitmap whiteWatermark, Bitmap blackWatermark) {
        return addWatermark(sourceBitmap, whiteWatermark, blackWatermark, 255);
    }

    public static Bitmap scaleWatermarkToCover(Bitmap watermark, int targetWidth, int targetHeight) {
        if (watermark == null) {
            return null;
        }

        int wmWidth = watermark.getWidth();
        int wmHeight = watermark.getHeight();

        float scaleWidth = (float) targetWidth / wmWidth;
        float scaleHeight = (float) targetHeight / wmHeight;
        float scaleFactor = Math.max(scaleWidth, scaleHeight);

        int scaledWidth = Math.round(wmWidth * scaleFactor);
        int scaledHeight = Math.round(wmHeight * scaleFactor);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(watermark, scaledWidth, scaledHeight, true);
        return scaledBitmap;
    }

    public static Bitmap pickWatermark(Bitmap sourceBitmap, Bitmap whiteWatermark, Bitmap blackWatermark) {
        if (whiteWatermark == null && blackWatermark == null) {
            return null;
        }
        boolean useWhite;
        if (whiteWatermark != null && blackWatermark != null) {
            useWhite = isWhiteWatermarkVisible(sourceBitmap);
        } else {
            useWhite = whiteWatermark != null;
        }
        return useWhite ? whiteWatermark : blackWatermark;
    }

    public static boolean isWhiteWatermarkVisible(Bitmap sourceBitmap) {
        if (sourceBitmap == null) {
            return true;
        }

        int sampleSize = calculateSampleSize(sourceBitmap.getWidth(), sourceBitmap.getHeight());
        int sampledWidth = sourceBitmap.getWidth() / sampleSize;
        int sampledHeight = sourceBitmap.getHeight() / sampleSize;

        Bitmap sampled = Bitmap.createScaledBitmap(sourceBitmap, sampledWidth, sampledHeight, true);

        int totalPixels = sampledWidth * sampledHeight;
        int darkPixelCount = 0;
        int[] pixels = new int[totalPixels];
        sampled.getPixels(pixels, 0, sampledWidth, 0, 0, sampledWidth, sampledHeight);

        for (int pixel : pixels) {
            double luminance = calculateLuminance(pixel);
            if (luminance < 128) {
                darkPixelCount++;
            }
        }

        if (sampled != sourceBitmap) {
            sampled.recycle();
        }

        double darkRatio = (double) darkPixelCount / totalPixels;
        return darkRatio >= 0.3;
    }

    private static int calculateSampleSize(int width, int height) {
        int maxDimension = 100;
        int sampleSize = 1;
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2;
        }
        return sampleSize;
    }

    private static double calculateLuminance(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        if (source == null) {
            return null;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
