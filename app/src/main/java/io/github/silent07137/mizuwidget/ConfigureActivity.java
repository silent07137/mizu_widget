// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import java.io.IOException;
import java.util.UUID;

public final class ConfigureActivity extends Activity {
    static final String ACTION_CREATE = "io.github.silent07137.mizuwidget.CREATE";
    private static final int PICK_IMAGE = 101;
    private static final int[] BACKGROUNDS = {Color.TRANSPARENT, Color.WHITE, 0xFF192B30, 0xFFDCEDEA};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int widgetId;
    private WidgetConfig config;
    private ConfigStore store;
    private ImageView preview;
    private TextView status;
    private Button save;
    private PreviewPlayback playback;
    private LinearLayout gifOptions;
    private Switch gifSwitch;
    private Spinner gifRate;
    private static final int[] GIF_RATES = {1, 2, 5, 15, 30};
    private volatile int generation;
    private boolean busy;
    private boolean ready;
    private boolean committed;
    private String retainedDraft;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        store = new ConfigStore(this);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !WidgetEngine.owns(this, widgetId)) { finish(); return; }
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !ACTION_CREATE.equals(getIntent().getAction())) { finish(); return; }
        config = widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ? WidgetConfig.defaults() : store.get(widgetId);
        if (config == null) config = WidgetConfig.defaults();
        if (state != null) {
            try { config = WidgetConfig.fromJson(state.getString("config", "")); } catch (org.json.JSONException ignored) { }
            retainedDraft = state.getString("draft");
        }
        LinearLayout page = Ui.page(this);
        Ui.heading(this, page, "", widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ? "添加组件" : "组件设置", "");
        preview = Ui.preview(this, page, 220);
        playback = new PreviewPlayback(preview);
        Ui.gap(this, page, 12);
        status = Ui.text(this, "未选择图片", 13, Ui.MUTED);
        status.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        page.addView(status);
        Ui.gap(this, page, 12);
        Button choose = Ui.button(this, "选择图片", false);
        choose.setOnClickListener(v -> chooseImage());
        page.addView(choose);
        Ui.gap(this, page, 20);
        gifOptions = Ui.card(this, page);
        gifSwitch = new Switch(this);
        gifSwitch.setText("GIF 播放");
        gifSwitch.setTextColor(Ui.INK);
        gifSwitch.setMinimumHeight(Ui.dp(this, 48));
        gifSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (!busy && checked != config.gifPlayback) {
                config = animationConfig(checked, config.gifFps);
                refreshPreview();
            }
        });
        gifOptions.addView(gifSwitch);
        label(gifOptions, "帧率上限");
        gifRate = spinner(gifOptions, new String[]{"1 FPS", "2 FPS", "5 FPS", "15 FPS（高帧率）", "30 FPS（高帧率）"}, rateIndex(), index -> {
            if (config.gifFps != GIF_RATES[index]) {
                config = animationConfig(config.gifPlayback, GIF_RATES[index]);
                refreshPreview();
            }
        });
        Ui.gap(this, page, 14);
        LinearLayout card = Ui.card(this, page);
        label(card, "缩放方式");
        spinner(card, new String[]{"完整显示", "居中裁剪", "拉伸铺满"}, config.scale.ordinal(), index -> {
            config = copy(config.uri, WidgetConfig.Scale.values()[index], config.radiusDp, config.opacity, config.background, config.click, config.gif);
            refreshPreview();
        });
        Ui.gap(this, card, 14);
        slider(card, "圆角", 64, Math.round(config.radiusDp), " dp", value -> {
            config = copy(config.uri, config.scale, value, config.opacity, config.background, config.click, config.gif);
            refreshPreview();
        });
        Ui.gap(this, card, 14);
        slider(card, "不透明度", 100, Math.round(config.opacity * 100), "%", value -> {
            config = copy(config.uri, config.scale, config.radiusDp, value / 100f, config.background, config.click, config.gif);
            refreshPreview();
        });
        Ui.gap(this, card, 14);
        label(card, "背景");
        int selected = 0;
        for (int i = 0; i < BACKGROUNDS.length; i++) if (config.background == BACKGROUNDS[i]) selected = i;
        spinner(card, new String[]{"透明", "白色", "深色", "浅水绿"}, selected, index -> {
            config = copy(config.uri, config.scale, config.radiusDp, config.opacity, BACKGROUNDS[index], config.click, config.gif);
            refreshPreview();
        });
        Ui.gap(this, card, 14);
        label(card, "点击操作");
        spinner(card, new String[]{"打开设置", "查看图片", "无"}, config.click.ordinal(), index ->
            config = copy(config.uri, config.scale, config.radiusDp, config.opacity, config.background, WidgetConfig.Click.values()[index], config.gif));
        Ui.gap(this, page, 22);
        save = Ui.button(this, widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ? "添加到桌面" : "保存", true);
        save.setOnClickListener(v -> saveWidget());
        page.addView(save);
        Ui.gap(this, page, 12);
        Button cancel = Ui.button(this, "取消", false);
        cancel.setOnClickListener(v -> { if (!busy) finish(); });
        page.addView(cancel);
        refreshPreview();
    }
    private WidgetConfig copy(String uri, WidgetConfig.Scale scale, float radius, float opacity, int bg, WidgetConfig.Click click, boolean gif) {
        return new WidgetConfig(uri, scale, radius, opacity, bg, click, gif, config.gifPlayback, config.gifFps);
    }
    private WidgetConfig animationConfig(boolean play, int fps) {
        return new WidgetConfig(config.uri, config.scale, config.radiusDp, config.opacity, config.background,
            config.click, config.gif, play, fps);
    }
    private int rateIndex() {
        for (int i = 0; i < GIF_RATES.length; i++) if (GIF_RATES[i] == config.gifFps) return i;
        return 1;
    }
    private interface Changed { void accept(int value); }
    private void label(LinearLayout card, String value) { card.addView(Ui.text(this, value, 14, Ui.INK)); }
    private Spinner spinner(LinearLayout card, String[] values, int selected, Changed changed) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        spinner.setMinimumHeight(Ui.dp(this, 48));
        spinner.setContentDescription(card.getChildCount() > 0 && card.getChildAt(card.getChildCount() - 1) instanceof TextView
            ? ((TextView) card.getChildAt(card.getChildCount() - 1)).getText() : "选项");
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            int previous = selected;
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position != previous && !busy) { previous = position; changed.accept(position); }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        card.addView(spinner);
        return spinner;
    }
    private void slider(LinearLayout card, String label, int max, int initial, String unit, Changed changed) {
        TextView title = Ui.text(this, label + "  ·  " + initial + unit, 14, Ui.INK);
        card.addView(title);
        SeekBar slider = new SeekBar(this);
        slider.setMax(max);
        slider.setProgress(initial);
        slider.setContentDescription(label);
        slider.setMinimumHeight(Ui.dp(this, 48));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                title.setText(label + "  ·  " + value + unit);
                if (user && !busy) changed.accept(value);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        card.addView(slider, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
    }
    private void chooseImage() {
        if (busy) return;
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(picker, PICK_IMAGE); }
        catch (android.content.ActivityNotFoundException e) { error("此设备没有可用的文件选择器"); }
    }
    @Override public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_IMAGE || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (!"content".equals(uri.getScheme())) { error("请选择系统文件选择器中的图片"); return; }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String previous = config.uri;
            boolean gif = "image/gif".equals(getContentResolver().getType(uri));
            config = copy(uri.toString(), config.scale, config.radiusDp, config.opacity, config.background, config.click, gif);
            config = animationConfig(gif, config.gifFps);
            if (!previous.equals(config.uri)) store.releaseIfUnused(previous);
            refreshPreview();
        } catch (SecurityException | IllegalArgumentException e) {
            error("无法保留此图片的读取权限，请换用本机图片或其他文件提供方");
        }
    }
    private void refreshPreview() {
        if (preview == null || save == null || busy) return;
        refreshGifOptions();
        handler.removeCallbacks(previewWork);
        generation++;
        ready = false;
        save.setEnabled(false);
        if (config.uri.isEmpty()) { playback.clear(); preview.setImageResource(R.drawable.ic_mizu); status.setText("未选择图片"); return; }
        status.setText("生成预览…");
        handler.postDelayed(previewWork, 100);
    }
    private void refreshGifOptions() {
        gifOptions.setVisibility(config.gif ? android.view.View.VISIBLE : android.view.View.GONE);
        gifSwitch.setChecked(config.gifPlayback);
        gifRate.setSelection(rateIndex());
        gifRate.setEnabled(config.gifPlayback);
    }
    private final Runnable previewWork = () -> {
        final int version = generation;
        final WidgetConfig requested = config;
        final int[] bounds = widgetId == AppWidgetManager.INVALID_APPWIDGET_ID ? new int[]{540, 540} : WidgetEngine.size(this, widgetId);
        final float density = getResources().getDisplayMetrics().density;
        WidgetEngine.IO.execute(() -> {
            if (version != generation) return;
            Bitmap bitmap = null;
            GifFrames.Sequence frames = null;
            String failure = null;
            WidgetConfig snapshot = requested;
            try {
                boolean gif = GifFrames.isGif(getApplicationContext(), requested.uri);
                if (gif != requested.gif) snapshot = new WidgetConfig(requested.uri, requested.scale,
                    requested.radiusDp, requested.opacity, requested.background, requested.click, gif, gif, requested.gifFps);
                if (snapshot.gifPlayback) {
                    try { frames = GifFrames.render(getApplicationContext(), snapshot, bounds[0], bounds[1], density); }
                    catch (IOException | RuntimeException | OutOfMemoryError e) { failure = "GIF 无法播放或超限 · 静态预览"; }
                }
                if (frames == null) bitmap = WidgetRenderer.render(getApplicationContext(), snapshot, bounds[0], bounds[1], density);
            }
            catch (IOException | RuntimeException | OutOfMemoryError e) { failure = "图片无法读取，请重新选择一张图片"; }
            final Bitmap result = bitmap;
            final GifFrames.Sequence animation = frames;
            final WidgetConfig identified = snapshot;
            final String message = failure;
            handler.post(() -> {
                if (isFinishing() || isDestroyed() || version != generation) {
                    if (result != null) result.recycle(); if (animation != null) animation.close(); return;
                }
                if (animation != null) playback.show(animation); else playback.show(result);
                if (config.gif != identified.gif) {
                    config = new WidgetConfig(config.uri, config.scale, config.radiusDp, config.opacity,
                        config.background, config.click, identified.gif, identified.gifPlayback, config.gifFps);
                    refreshGifOptions();
                }
                ready = result != null || animation != null;
                save.setEnabled(ready && !busy);
                status.setText(message != null ? message : animation != null
                    ? "GIF · " + (animation.intervalMs > GifFrames.intervalFor(identified.gifFps) ? "已降帧" : identified.gifFps + " FPS")
                        + (identified.gifFps > 5 ? " · 细节降低" : "")
                    : identified.gif ? "GIF 静态预览" : "预览");
            });
        });
    };
    private void saveWidget() {
        if (busy || !ready) return;
        busy = true;
        save.setEnabled(false);
        status.setText("正在保存…");
        WidgetConfig snapshot = config;
        WidgetEngine.IO.execute(() -> {
            try {
                if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    String token = UUID.randomUUID().toString();
                    store.putDraft(token, snapshot);
                    handler.post(() -> pin(token, snapshot));
                } else {
                    if (!WidgetEngine.owns(this, widgetId)) throw new IOException("这个组件已从桌面移除");
                    store.save(widgetId, snapshot);
                    WidgetEngine.update(getApplicationContext(), widgetId);
                    handler.post(() -> {
                        committed = true;
                        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
                        finish();
                    });
                }
            } catch (IOException | RuntimeException e) {
                handler.post(() -> { busy = false; save.setEnabled(ready); error("保存失败，请重试"); });
            }
        });
    }
    private void pin(String token, WidgetConfig snapshot) {
        if (isFinishing() || isDestroyed()) { store.removeDraft(token); return; }
        // Earlier pin confirmations may still be pending; preserve their independent drafts.
        retainedDraft = token;
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        if (!manager.isRequestPinAppWidgetSupported()) {
            store.removeDraft(token); retainedDraft = null;
            busy = false; save.setEnabled(ready);
            error("当前桌面不支持自动添加。请长按桌面 → 小组件 → Mizu Widget，再选择图片。");
            return;
        }
        Intent callback = new Intent(this, PinReceiver.class).setData(Uri.parse("mizu://pin/" + token));
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, callback,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        Bundle extras = new Bundle();
        if (playback.first() != null) extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
            WidgetEngine.views(this, 0, snapshot, playback.first(), null));
        try {
            boolean requested = manager.requestPinAppWidget(WidgetEngine.provider(this), extras, pending);
            if (!requested) { store.removeDraft(token); retainedDraft = null; error("桌面未接受添加请求，请使用桌面的小组件入口"); }
            else status.setText("请确认添加到桌面");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            store.removeDraft(token); retainedDraft = null; error("暂时无法添加，请使用桌面的小组件入口");
        } finally { busy = false; save.setEnabled(ready); }
    }
    private void error(String message) {
        status.setText(message);
        new AlertDialog.Builder(this).setTitle("Mizu Widget").setMessage(message).setPositiveButton("知道了", null).show();
    }
    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        try { state.putString("config", config.toJson().toString()); } catch (org.json.JSONException ignored) { }
        state.putString("draft", retainedDraft);
    }
    @Override public void onDestroy() {
        generation++;
        handler.removeCallbacks(previewWork);
        if (playback != null) playback.clear();
        // Pin drafts retain the URI until callback or expiry; ordinary cancellation releases it.
        if (isFinishing() && !busy && !committed && config != null) store.releaseIfUnused(config.uri);
        super.onDestroy();
    }
    @Override public void onStart() { super.onStart(); if (playback != null) playback.setActive(true); }
    @Override public void onStop() { if (playback != null) playback.setActive(false); super.onStop(); }
}
