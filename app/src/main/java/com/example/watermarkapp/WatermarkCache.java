package com.example.watermarkapp;

import android.graphics.Bitmap;

public class WatermarkCache {

    private static WatermarkCache instance;
    private Bitmap whiteWatermark;
    private Bitmap blackWatermark;

    private WatermarkCache() {
    }

    public static synchronized WatermarkCache getInstance() {
        if (instance == null) {
            instance = new WatermarkCache();
        }
        return instance;
    }

    public Bitmap getWhiteWatermark() {
        return whiteWatermark;
    }

    public void setWhiteWatermark(Bitmap whiteWatermark) {
        this.whiteWatermark = whiteWatermark;
    }

    public Bitmap getBlackWatermark() {
        return blackWatermark;
    }

    public void setBlackWatermark(Bitmap blackWatermark) {
        this.blackWatermark = blackWatermark;
    }

    public void clear() {
        if (whiteWatermark != null) {
            whiteWatermark.recycle();
            whiteWatermark = null;
        }
        if (blackWatermark != null) {
            blackWatermark.recycle();
            blackWatermark = null;
        }
    }
}
