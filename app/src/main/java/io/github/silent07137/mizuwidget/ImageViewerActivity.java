// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ImageViewerActivity extends Activity {
    private PreviewPlayback playback;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        WidgetConfig config = new ConfigStore(this).get(id);
        if (config == null || !WidgetEngine.owns(this, id)) { finish(); return; }
        LinearLayout page = Ui.page(this);
        Ui.heading(this, page, "", "图片预览", "");
        ImageView image = Ui.preview(this, page, 420);
        playback = new PreviewPlayback(image);
        TextView status = Ui.text(this, "", 13, Ui.MUTED);
        page.addView(status);
        Button close = Ui.button(this, "返回", false);
        Ui.gap(this, page, 20);
        page.addView(close);
        close.setOnClickListener(v -> finish());
        WidgetEngine.IO.execute(() -> {
            try {
                WidgetConfig full = new WidgetConfig(config.uri, WidgetConfig.Scale.FIT, 0, 1, 0,
                    config.click, config.gif, config.gifPlayback, config.gifFps);
                GifFrames.Sequence frames = null;
                String label = config.gif ? "GIF 静态预览" : "";
                if (full.gifPlayback) {
                    try { frames = GifFrames.render(getApplicationContext(), full, 720, 960, 1); label = "GIF 预览"; }
                    catch (java.io.IOException | RuntimeException | OutOfMemoryError e) { label = "GIF 无法播放或超限 · 静态预览"; }
                }
                Bitmap bitmap = frames == null ? WidgetRenderer.render(getApplicationContext(), full, 720, 960, 1) : null;
                final GifFrames.Sequence animation = frames;
                final String message = label;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        if (bitmap != null) bitmap.recycle(); if (animation != null) animation.close(); return;
                    }
                    if (animation != null) playback.show(animation); else playback.show(bitmap);
                    status.setText(message);
                });
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) {
                runOnUiThread(() -> android.widget.Toast.makeText(this, "图片无法读取，请在组件设置中重新选择", android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }
    @Override public void onStart() { super.onStart(); if (playback != null) playback.setActive(true); }
    @Override public void onStop() { if (playback != null) playback.setActive(false); super.onStop(); }
    @Override public void onDestroy() { if (playback != null) playback.clear(); super.onDestroy(); }
}
