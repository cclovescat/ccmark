package com.example.watermarkapp;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class ResultActivity extends AppCompatActivity implements ProcessingCache.Listener {

    private RecyclerView rvResults;
    private ResultAdapter resultAdapter;
    private MaterialButton btnBack;
    private MaterialButton btnSaveAll;

    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView tvProgressText;
    private int progressTitleRes = R.string.processing;

    private ProcessingCache processingCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlassTheme.applyNightMode(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        applyWindowInsets();
        GlassTheme.applyToRoot(this);

        initViews();
        setupToolbar();
        setupRecyclerView();

        processingCache = ProcessingCache.getInstance();
        processingCache.setListener(this);

        switch (processingCache.getState()) {
            case PROCESSING:
                // 旋转恢复：setListener 已同步回调过一次进度，对话框可能已显示
                showProgress(R.string.processing);
                break;
            case DONE:
                resultAdapter.setResultItems(processingCache.getResults());
                break;
            case IDLE:
            default:
                startProcessingIfNeeded();
                break;
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSaveAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllImages();
            }
        });
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    private void startProcessingIfNeeded() {
        List<Uri> imageUris = getIntent().getParcelableArrayListExtra("imageUris");
        if (imageUris == null || imageUris.isEmpty()) {
            Toast.makeText(this, "没有收到待处理的照片", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        WatermarkCache watermarkCache = WatermarkCache.getInstance();
        Bitmap whiteWatermark = watermarkCache.getWhiteWatermark();
        Bitmap blackWatermark = watermarkCache.getBlackWatermark();
        if (whiteWatermark == null && blackWatermark == null) {
            Toast.makeText(this, R.string.no_watermark_selected, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int watermarkAlpha = getIntent().getIntExtra("watermarkAlpha", 255);
        showProgress(R.string.processing);
        processingCache.processImages(getApplicationContext(), imageUris,
                whiteWatermark, blackWatermark, watermarkAlpha);
    }

    private void initViews() {
        rvResults = findViewById(R.id.rvResults);
        btnBack = findViewById(R.id.btnBack);
        btnSaveAll = findViewById(R.id.btnSaveAll);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setupRecyclerView() {
        resultAdapter = new ResultAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        rvResults.setLayoutManager(layoutManager);
        rvResults.setAdapter(resultAdapter);
    }

    private void displayResults(List<ProcessingCache.ResultItem> results) {
        resultAdapter.setResultItems(results);
        if (results.isEmpty()) {
            Toast.makeText(this, "处理失败，无法加载照片", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.processing_complete, Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAllImages() {
        if (processingCache.getState() != ProcessingCache.State.DONE) {
            return;
        }
        List<ProcessingCache.ResultItem> items = resultAdapter.getResultItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        showProgress(R.string.saving);
        processingCache.saveResults(getApplicationContext(), items);
    }

    private void showProgress(int titleRes) {
        progressTitleRes = titleRes;
        if (progressDialog != null && progressDialog.isShowing()) {
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null);
        progressBar = view.findViewById(R.id.pbProgress);
        tvProgressText = view.findViewById(R.id.tvProgressText);
        progressDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setView(view)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    private void updateProgress(int current, int total) {
        if (progressDialog == null || !progressDialog.isShowing()) {
            showProgress(progressTitleRes);
        }
        progressBar.setMax(total);
        progressBar.setProgress(current);
        tvProgressText.setText(String.format(getString(R.string.progress_count), current, total));
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    @Override
    public void onProcessingProgress(int current, int total) {
        updateProgress(current, total);
    }

    @Override
    public void onProcessingComplete(List<ProcessingCache.ResultItem> results) {
        dismissProgress();
        displayResults(results);
    }

    @Override
    public void onSaveProgress(int current, int total) {
        updateProgress(current, total);
    }

    @Override
    public void onSaveComplete(int savedCount) {
        dismissProgress();
        Toast.makeText(this, "成功保存 " + savedCount + " 张照片", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        processingCache.setListener(null);
        dismissProgress();
    }
}
