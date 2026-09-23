package com.example.watermarkapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * 液态玻璃主题引擎：亮/暗模式切换 + 主题色动态着色。
 * 颜色通过代码施加到控件（按钮 tag 区分主次样式），因此预设色和用户自定义色都能即时生效。
 */
public class GlassTheme {

    public static final String PREFS_NAME = "appearance";
    private static final String KEY_NIGHT_MODE = "night_mode";   // system / light / dark
    private static final String KEY_PALETTE = "palette";         // preset_<index> / custom
    private static final String KEY_CUSTOM_COLOR = "custom_color";

    public static final String NIGHT_SYSTEM = "system";
    public static final String NIGHT_LIGHT = "light";
    public static final String NIGHT_DARK = "dark";

    /** 按钮样式 tag，与布局中的 android:tag 对应 */
    public static final String TAG_PRIMARY = "primary";
    public static final String TAG_SECONDARY = "secondary";
    public static final String TAG_OUTLINED_PRIMARY = "outlined-primary";
    public static final String TAG_OUTLINED_SECONDARY = "outlined-secondary";

    public static class Preset {
        public final String name;
        public final int primary;
        public final int secondary;

        Preset(String name, int primary, int secondary) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
        }
    }

    private static final int P(String hex) {
        return Color.parseColor(hex);
    }

    /** 莫兰迪色系 */
    public static final List<Preset> MORANDI = new ArrayList<>();
    /** 马卡龙色系 */
    public static final List<Preset> MACARON = new ArrayList<>();

    static {
        MORANDI.add(new Preset("雾霾蓝", P("#7C96AB"), P("#A9C0D1")));
        MORANDI.add(new Preset("灰豆绿", P("#93A895"), P("#BCCFC0")));
        MORANDI.add(new Preset("藕荷粉", P("#C9A8A6"), P("#E4C9C7")));
        MORANDI.add(new Preset("燕麦奶咖", P("#BFA98C"), P("#DCCEB6")));
        MORANDI.add(new Preset("灰紫", P("#A395B5"), P("#CCC2DA")));

        MACARON.add(new Preset("樱花粉", P("#F5A8BD"), P("#FBD1DB")));
        MACARON.add(new Preset("薄荷绿", P("#8CDCC0"), P("#C2EEDF")));
        MACARON.add(new Preset("薰衣草", P("#B9A8E8"), P("#DCD3F6")));
        MACARON.add(new Preset("柠檬奶油", P("#EFCF6E"), P("#F8E8B5")));
        MACARON.add(new Preset("天空蓝", P("#93C6E7"), P("#C8E2F4")));
    }

    private GlassTheme() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getNightMode(Context context) {
        return prefs(context).getString(KEY_NIGHT_MODE, NIGHT_SYSTEM);
    }

    public static void setNightMode(Context context, String mode) {
        prefs(context).edit().putString(KEY_NIGHT_MODE, mode).apply();
        applyNightMode(context);
    }

    public static void applyNightMode(Context context) {
        String mode = getNightMode(context);
        if (NIGHT_DARK.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (NIGHT_LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    public static String getPaletteId(Context context) {
        return prefs(context).getString(KEY_PALETTE, "preset_0");
    }

    public static void setPalette(Context context, String paletteId, int customColor) {
        SharedPreferences.Editor editor = prefs(context).edit();
        editor.putString(KEY_PALETTE, paletteId);
        if (customColor != -1) {
            editor.putInt(KEY_CUSTOM_COLOR, customColor);
        }
        editor.apply();
    }

    public static int getCustomColor(Context context) {
        return prefs(context).getInt(KEY_CUSTOM_COLOR, P("#7C96AB"));
    }

    /** 当前生效的主色 / 次色 */
    public static int getPrimaryColor(Context context) {
        String id = getPaletteId(context);
        if ("custom".equals(id)) {
            return getCustomColor(context);
        }
        Preset preset = findPreset(id);
        return preset != null ? preset.primary : MORANDI.get(0).primary;
    }

    public static int getSecondaryColor(Context context) {
        String id = getPaletteId(context);
        Preset preset = findPreset(id);
        return preset != null ? preset.secondary : MORANDI.get(0).secondary;
    }

    private static Preset findPreset(String id) {
        if (id == null || !id.startsWith("preset_")) {
            return null;
        }
        try {
            int index = Integer.parseInt(id.substring("preset_".length()));
            if (index < MORANDI.size()) {
                return MORANDI.get(index);
            }
            index -= MORANDI.size();
            if (index < MACARON.size()) {
                return MACARON.get(index);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /** 根据 id 计算预设序号，-1 表示自定义 */
    public static int getSelectedPresetIndex(Context context) {
        String id = getPaletteId(context);
        if (!id.startsWith("preset_")) {
            return -1;
        }
        try {
            return Integer.parseInt(id.substring("preset_".length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 在 setContentView 之后调用：切换夜色模式并把主题色刷到当前视图树上。
     */
    public static void applyToRoot(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root instanceof ViewGroup) {
            applyToViewTree((ViewGroup) root,
                    getPrimaryColor(activity),
                    getSecondaryColor(activity));
        }
    }

    public static void applyToViewTree(ViewGroup root, int primary, int secondary) {
        ArrayList<MaterialButton> buttons = new ArrayList<>();
        collectViews(root, MaterialButton.class, buttons);
        for (MaterialButton button : buttons) {
            Object tag = button.getTag();
            if (TAG_PRIMARY.equals(tag)) {
                button.setBackgroundTintList(ColorStateList.valueOf(primary));
                button.setTextColor(onColor(primary));
                button.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
            } else if (TAG_SECONDARY.equals(tag)) {
                button.setBackgroundTintList(ColorStateList.valueOf(secondary));
                button.setTextColor(onColor(secondary));
                button.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
            } else if (TAG_OUTLINED_PRIMARY.equals(tag)) {
                button.setStrokeColor(ColorStateList.valueOf(primary));
                button.setTextColor(primary);
            } else if (TAG_OUTLINED_SECONDARY.equals(tag)) {
                button.setStrokeColor(ColorStateList.valueOf(secondary));
                button.setTextColor(secondary);
            }
        }

        ArrayList<MaterialToolbar> toolbars = new ArrayList<>();
        collectViews(root, MaterialToolbar.class, toolbars);
        for (MaterialToolbar toolbar : toolbars) {
            int glassPrimary = (primary & 0x00FFFFFF) | 0xB3000000;
            ViewCompat.setBackgroundTintList(toolbar, ColorStateList.valueOf(glassPrimary));
        }

        ArrayList<SwitchMaterial> switches = new ArrayList<>();
        collectViews(root, SwitchMaterial.class, switches);
        for (SwitchMaterial sw : switches) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            };
            sw.setThumbTintList(new ColorStateList(states, new int[]{primary, 0xFFE0E4E8}));
            sw.setTrackTintList(new ColorStateList(states,
                    new int[]{blend(primary, 0x80FFFFFF), 0xFFC8CDD2}));
        }

        ArrayList<SeekBar> seekBars = new ArrayList<>();
        collectViews(root, SeekBar.class, seekBars);
        for (SeekBar seekBar : seekBars) {
            seekBar.setProgressTintList(ColorStateList.valueOf(primary));
            seekBar.setThumbTintList(ColorStateList.valueOf(primary));
        }
    }

    /** 递归收集视图树中指定类型的控件 */
    private static <T extends View> void collectViews(View view, Class<T> clazz, List<T> out) {
        if (clazz.isInstance(view)) {
            out.add(clazz.cast(view));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectViews(group.getChildAt(i), clazz, out);
            }
        }
    }

    /** 浅色底配深字、深色底配白字 */
    public static int onColor(int background) {
        int r = Color.red(background);
        int g = Color.green(background);
        int b = Color.blue(background);
        double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
        return luminance > 165 ? 0xFF37474F : 0xFFFFFFFF;
    }

    private static int blend(int foreground, int background) {
        int fa = Color.alpha(foreground);
        int ba = Color.alpha(background);
        int alpha = fa + ba * (255 - fa) / 255;
        int r = (Color.red(foreground) * fa + Color.red(background) * ba * (255 - fa) / 255) / Math.max(alpha, 1);
        int g = (Color.green(foreground) * fa + Color.green(background) * ba * (255 - fa) / 255) / Math.max(alpha, 1);
        int b = (Color.blue(foreground) * fa + Color.blue(background) * ba * (255 - fa) / 255) / Math.max(alpha, 1);
        return Color.argb(alpha, r, g, b);
    }

    /**
     * 构建 10 个预设色圆点：外圈为次色、内芯为主色，选中的带描边。
     */
    public static void buildSwatches(Context context, android.widget.LinearLayout container,
                                     final SwatchClickListener listener) {
        container.removeAllViews();
        int selected = getSelectedPresetIndex(context);
        boolean nightMode = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int selectedStroke = nightMode ? 0xFFFFFFFF : 0xFF37474F;
        int index = 0;
        int density = (int) (context.getResources().getDisplayMetrics().density * 38);

        List<Preset> all = new ArrayList<>(MORANDI);
        all.addAll(MACARON);
        for (final Preset preset : all) {
            GradientDrawable outer = new GradientDrawable();
            outer.setShape(GradientDrawable.OVAL);
            outer.setColor(preset.secondary);

            GradientDrawable inner = new GradientDrawable();
            inner.setShape(GradientDrawable.OVAL);
            inner.setColor(preset.primary);

            LayerDrawable layered = new LayerDrawable(new GradientDrawable[]{outer, inner});
            int inset = density / 6;
            layered.setLayerInset(1, inset, inset, inset, inset);

            if (index == selected) {
                outer.setStroke(density / 10, selectedStroke);
            }

            final String paletteId = "preset_" + index;
            View dot = new View(context);
            setSquareSize(dot, density);
            dot.setBackground(layered);
            dot.setContentDescription(preset.name);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onSwatchClick(paletteId);
                }
            });
            setHorizontalMargins(dot, density / 5);
            container.addView(dot);
            index++;
        }
    }

    private static void setSquareSize(View view, int sizePx) {
        view.setLayoutParams(new android.widget.LinearLayout.LayoutParams(sizePx, sizePx));
    }

    private static void setHorizontalMargins(View view, int marginPx) {
        android.widget.LinearLayout.LayoutParams lp =
                (android.widget.LinearLayout.LayoutParams) view.getLayoutParams();
        lp.leftMargin = marginPx;
        lp.rightMargin = marginPx;
        view.setLayoutParams(lp);
    }

    public interface SwatchClickListener {
        void onSwatchClick(String presetId);
    }
}
