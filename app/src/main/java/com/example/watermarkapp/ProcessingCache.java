package com.example.watermarkapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 后台处理引擎：水印批处理与保存都在与 Activity 生命周期解耦的单例中执行，
 * 屏幕旋转后新 Activity 实例通过 {@link Listener} 重新挂载即可恢复进度和结果。
 */
public class ProcessingCache {

    public enum State {IDLE, PROCESSING, DONE}

    public interface Listener {
        void onProcessingProgress(int current, int total);

        void onProcessingComplete(List<ResultItem> results);

        void onSaveProgress(int current, int total);

        void onSaveComplete(int savedCount);
    }

    public static class ResultItem {
        public final File file;
        public final String info;

        public ResultItem(File file, String info) {
            this.file = file;
            this.info = info;
        }
    }

    private static volatile ProcessingCache instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile State state = State.IDLE;
    private final List<ResultItem> results = new ArrayList<>();
    private volatile int progressCurrent;
    private volatile int progressTotal;
    private Listener listener;

    private ProcessingCache() {
    }

    public static ProcessingCache getInstance() {
        if (instance == null) {
            synchronized (ProcessingCache.class) {
                if (instance == null) {
                    instance = new ProcessingCache();
                }
            }
        }
        return instance;
    }

    public synchronized void setListener(Listener listener) {
        this.listener = listener;
        if (listener != null && state == State.PROCESSING) {
            listener.onProcessingProgress(progressCurrent, progressTotal);
        }
    }

    public State getState() {
        return state;
    }

    public int getProgressCurrent() {
        return progressCurrent;
    }

    public int getProgressTotal() {
        return progressTotal;
    }

    public List<ResultItem> getResults() {
        return results;
    }

    public void processImages(final Context appContext, final List<Uri> uris,
                              final Bitmap whiteWatermark, final Bitmap blackWatermark,
                              final int watermarkAlpha) {
        state = State.PROCESSING;
        results.clear();
        progressCurrent = 0;
        progressTotal = uris.size();

        File outputDir = new File(appContext.getCacheDir(), "processed");
        deleteDirectory(outputDir);
        outputDir.mkdirs();

        notifyProcessingProgress();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < uris.size(); i++) {
                    Uri uri = uris.get(i);
                    try {
                        Bitmap sourceBitmap = ImageUtils.loadBitmapFromUri(appContext, uri);
                        if (sourceBitmap != null) {
                            Bitmap watermark = WatermarkUtils.pickWatermark(
                                    sourceBitmap, whiteWatermark, blackWatermark);
                            Bitmap resultBitmap = WatermarkUtils.addWatermark(
                                    sourceBitmap, watermark, watermarkAlpha);

                            String watermarkType = watermark == whiteWatermark ? "白字水印" : "黑字水印";
                            String info = String.format(java.util.Locale.getDefault(),
                                    "第%d张 | %s | %dx%d",
                                    i + 1, watermarkType,
                                    resultBitmap.getWidth(), resultBitmap.getHeight());

                            File outputFile = new File(outputDir, "result_" + (i + 1) + ".png");
                            if (compressToPng(resultBitmap, outputFile)) {
                                synchronized (results) {
                                    results.add(new ResultItem(outputFile, info));
                                }
                            }

                            resultBitmap.recycle();
                            sourceBitmap.recycle();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    progressCurrent = i + 1;
                    notifyProcessingProgress();
                }

                state = State.DONE;
                notifyProcessingComplete();
            }
        });
    }

    public void saveResults(final Context appContext, final List<ResultItem> items) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int savedCount = 0;
                for (int i = 0; i < items.size(); i++) {
                    String timeStamp = new java.text.SimpleDateFormat(
                            "yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                    String fileName = "WM_" + timeStamp + "_" + (i + 1) + ".png";
                    if (ImageUtils.saveImageToGallery(appContext, items.get(i).file, fileName) != null) {
                        savedCount++;
                    }
                    mainHandler.post(new SaveProgressNotify(i + 1, items.size()));
                }

                // 缓存文件不在此处删除，允许用户旋转或返回后再次保存；
                // 缓存目录会在下一次批处理开始时统一清理
                final int count = savedCount;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Listener current = listener;
                        if (current != null) {
                            current.onSaveComplete(count);
                        }
                    }
                });
            }
        });
    }

    private boolean compressToPng(Bitmap bitmap, File target) {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(target);
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    private void notifyProcessingProgress() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Listener current = listener;
                if (current != null) {
                    current.onProcessingProgress(progressCurrent, progressTotal);
                }
            }
        });
    }

    private void notifyProcessingComplete() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                List<ResultItem> snapshot;
                synchronized (results) {
                    snapshot = new ArrayList<>(results);
                }
                Listener current = listener;
                if (current != null) {
                    current.onProcessingComplete(snapshot);
                }
            }
        });
    }

    private class SaveProgressNotify implements Runnable {
        private final int current;
        private final int total;

        SaveProgressNotify(int current, int total) {
            this.current = current;
            this.total = total;
        }

        @Override
        public void run() {
            Listener currentListener = listener;
            if (currentListener != null) {
                currentListener.onSaveProgress(current, total);
            }
        }
    }
}
