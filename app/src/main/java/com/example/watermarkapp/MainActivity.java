package com.example.watermarkapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    private RecyclerView rvSelectedImages;
    private ImageAdapter imageAdapter;
    private TextView tvSelectedCount;
    private ImageView ivWhiteWatermark;
    private ImageView ivBlackWatermark;
    private MaterialButton btnSelectImages;
    private MaterialButton btnUploadWhite;
    private MaterialButton btnUploadBlack;
    private MaterialButton btnStartProcessing;
    private SeekBar sbAlpha;
    private TextView tvAlphaValue;
    private SwitchMaterial swDarkMode;
    private android.widget.LinearLayout llSwatches;
    private MaterialButton btnCustomColor;
    private View llAppearanceHeader;
    private android.widget.LinearLayout llAppearanceContent;
    private TextView tvAppearanceArrow;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private Bitmap whiteWatermarkBitmap;
    private Bitmap blackWatermarkBitmap;

    private static final String PREFS_NAME = "WatermarkPrefs";
    private static final String KEY_WHITE_WM_PATH = "white_wm_path";
    private static final String KEY_BLACK_WM_PATH = "black_wm_path";

    private final ActivityResultLauncher<String> pickMultipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(),
                    new ActivityResultCallback<List<Uri>>() {
                        @Override
                        public void onActivityResult(List<Uri> uris) {
                            if (uris != null && !uris.isEmpty()) {
                                for (Uri uri : uris) {
                                    if (!selectedImageUris.contains(uri)) {
                                        selectedImageUris.add(uri);
                                    }
                                }
                                updateSelectedImages();
                            }
                        }
                    });

    private final ActivityResultLauncher<String> pickWhiteWatermarkLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            if (uri != null) {
                                String localPath = ImageUtils.copyUriToInternal(MainActivity.this, uri, "white_wm.png");
                                if (localPath != null) {
                                    whiteWatermarkBitmap = ImageUtils.loadBitmapFromFile(localPath, 2048);
                                    if (whiteWatermarkBitmap != null) {
                                        Glide.with(MainActivity.this)
                                                .load(whiteWatermarkBitmap)
                                                .fitCenter()
                                                .into(ivWhiteWatermark);
                                        WatermarkCache.getInstance().setWhiteWatermark(whiteWatermarkBitmap);
                                        saveWatermarkPath(KEY_WHITE_WM_PATH, localPath);
                                    }
                                }
                            }
                        }
                    });

    private final ActivityResultLauncher<String> pickBlackWatermarkLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    new ActivityResultCallback<Uri>() {
                        @Override
                        public void onActivityResult(Uri uri) {
                            if (uri != null) {
                                String localPath = ImageUtils.copyUriToInternal(MainActivity.this, uri, "black_wm.png");
                                if (localPath != null) {
                                    blackWatermarkBitmap = ImageUtils.loadBitmapFromFile(localPath, 2048);
                                    if (blackWatermarkBitmap != null) {
                                        Glide.with(MainActivity.this)
                                                .load(blackWatermarkBitmap)
                                                .fitCenter()
                                                .into(ivBlackWatermark);
                                        WatermarkCache.getInstance().setBlackWatermark(blackWatermarkBitmap);
                                        saveWatermarkPath(KEY_BLACK_WM_PATH, localPath);
                                    }
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GlassTheme.applyNightMode(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        applyWindowInsets();
        GlassTheme.applyToRoot(this);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        setupAppearance();
        checkStoragePermission();
        loadSavedWatermarks();
        updateSelectedImages();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    private void loadSavedWatermarks() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String whitePath = prefs.getString(KEY_WHITE_WM_PATH, null);
        String blackPath = prefs.getString(KEY_BLACK_WM_PATH, null);

        if (whitePath != null) {
            whiteWatermarkBitmap = ImageUtils.loadBitmapFromFile(whitePath, 2048);
            if (whiteWatermarkBitmap != null) {
                Glide.with(this).load(whiteWatermarkBitmap).fitCenter().into(ivWhiteWatermark);
                WatermarkCache.getInstance().setWhiteWatermark(whiteWatermarkBitmap);
            }
        }

        if (blackPath != null) {
            blackWatermarkBitmap = ImageUtils.loadBitmapFromFile(blackPath, 2048);
            if (blackWatermarkBitmap != null) {
                Glide.with(this).load(blackWatermarkBitmap).fitCenter().into(ivBlackWatermark);
                WatermarkCache.getInstance().setBlackWatermark(blackWatermarkBitmap);
            }
        }
    }

    private void saveWatermarkPath(String key, String path) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(key, path)
                .apply();
    }

    private void initViews() {
        rvSelectedImages = findViewById(R.id.rvSelectedImages);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        ivWhiteWatermark = findViewById(R.id.ivWhiteWatermark);
        ivBlackWatermark = findViewById(R.id.ivBlackWatermark);
        btnSelectImages = findViewById(R.id.btnSelectImages);
        btnUploadWhite = findViewById(R.id.btnUploadWhite);
        btnUploadBlack = findViewById(R.id.btnUploadBlack);
        btnStartProcessing = findViewById(R.id.btnStartProcessing);
        sbAlpha = findViewById(R.id.sbAlpha);
        tvAlphaValue = findViewById(R.id.tvAlphaValue);
        swDarkMode = findViewById(R.id.swDarkMode);
        llSwatches = findViewById(R.id.llSwatches);
        btnCustomColor = findViewById(R.id.btnCustomColor);
        llAppearanceHeader = findViewById(R.id.llAppearanceHeader);
        llAppearanceContent = findViewById(R.id.llAppearanceContent);
        tvAppearanceArrow = findViewById(R.id.tvAppearanceArrow);
    }

    private void setupAppearance() {
        swDarkMode.setChecked(GlassTheme.NIGHT_DARK.equals(GlassTheme.getNightMode(this)));
        swDarkMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                GlassTheme.setNightMode(MainActivity.this,
                        isChecked ? GlassTheme.NIGHT_DARK : GlassTheme.NIGHT_LIGHT);
            }
        });

        GlassTheme.buildSwatches(this, llSwatches, new GlassTheme.SwatchClickListener() {
            @Override
            public void onSwatchClick(String paletteId) {
                GlassTheme.setPalette(MainActivity.this, paletteId, -1);
                recreate();
            }
        });

        btnCustomColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCustomColorDialog();
            }
        });

        llAppearanceHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean expand = llAppearanceContent.getVisibility() != View.VISIBLE;
                llAppearanceContent.setVisibility(expand ? View.VISIBLE : View.GONE);
                tvAppearanceArrow.animate()
                        .rotation(expand ? 180f : 0f)
                        .setDuration(150)
                        .start();
                tvAppearanceArrow.setContentDescription(
                        getString(expand ? R.string.collapse : R.string.expand));
            }
        });
    }

    private void showCustomColorDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_custom_color, null);
        final SeekBar sbRed = view.findViewById(R.id.sbRed);
        final SeekBar sbGreen = view.findViewById(R.id.sbGreen);
        final SeekBar sbBlue = view.findViewById(R.id.sbBlue);
        final TextView tvRed = view.findViewById(R.id.tvRedValue);
        final TextView tvGreen = view.findViewById(R.id.tvGreenValue);
        final TextView tvBlue = view.findViewById(R.id.tvBlueValue);
        final View preview = view.findViewById(R.id.viewColorPreview);

        final GradientDrawable previewDrawable = new GradientDrawable();
        previewDrawable.setShape(GradientDrawable.OVAL);
        preview.setBackground(previewDrawable);

        final int[] color = {GlassTheme.getSelectedPresetIndex(this) == -1
                ? GlassTheme.getCustomColor(this)
                : GlassTheme.getPrimaryColor(this)};
        sbRed.setProgress(Color.red(color[0]));
        sbGreen.setProgress(Color.green(color[0]));
        sbBlue.setProgress(Color.blue(color[0]));

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                color[0] = Color.argb(255, sbRed.getProgress(), sbGreen.getProgress(), sbBlue.getProgress());
                previewDrawable.setColor(color[0]);
                previewDrawable.setStroke(2, GlassTheme.onColor(color[0]));
                tvRed.setText(String.valueOf(sbRed.getProgress()));
                tvGreen.setText(String.valueOf(sbGreen.getProgress()));
                tvBlue.setText(String.valueOf(sbBlue.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        sbRed.setOnSeekBarChangeListener(listener);
        sbGreen.setOnSeekBarChangeListener(listener);
        sbBlue.setOnSeekBarChangeListener(listener);
        listener.onProgressChanged(sbRed, sbRed.getProgress(), false);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.custom_color)
                .setView(view)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        GlassTheme.setPalette(MainActivity.this, "custom", color[0]);
                        recreate();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupRecyclerView() {
        imageAdapter = new ImageAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        rvSelectedImages.setLayoutManager(layoutManager);
        rvSelectedImages.setAdapter(imageAdapter);

        imageAdapter.setOnImageClickListener(new ImageAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(int position) {
                showRemoveImageDialog(position);
            }
        });
    }

    private void setupClickListeners() {
        btnSelectImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickMultipleImagesLauncher.launch("image/*");
            }
        });

        btnUploadWhite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickWhiteWatermarkLauncher.launch("image/*");
            }
        });

        btnUploadBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickBlackWatermarkLauncher.launch("image/*");
            }
        });

        btnStartProcessing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProcessing();
            }
        });

        sbAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int percent = (int) (progress / 255.0 * 100);
                tvAlphaValue.setText(percent + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateSelectedImages() {
        imageAdapter.setImageUris(selectedImageUris);
        tvSelectedCount.setText(String.format(getString(R.string.selected_count), selectedImageUris.size()));
    }

    private void showRemoveImageDialog(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("移除图片")
                .setMessage("确定要移除这张图片吗？")
                .setPositiveButton("移除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedImageUris.remove(position);
                        updateSelectedImages();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_STORAGE_PERMISSION);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startProcessing() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, R.string.no_image_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (whiteWatermarkBitmap == null && blackWatermarkBitmap == null) {
            Toast.makeText(this, R.string.no_watermark_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putParcelableArrayListExtra("imageUris", new ArrayList<>(selectedImageUris));
        intent.putExtra("hasWhiteWatermark", whiteWatermarkBitmap != null);
        intent.putExtra("hasBlackWatermark", blackWatermarkBitmap != null);
        intent.putExtra("watermarkAlpha", sbAlpha.getProgress());

        WatermarkCache.getInstance().setWhiteWatermark(whiteWatermarkBitmap);
        WatermarkCache.getInstance().setBlackWatermark(blackWatermarkBitmap);

        startActivity(intent);

        selectedImageUris.clear();
        updateSelectedImages();
    }
}
