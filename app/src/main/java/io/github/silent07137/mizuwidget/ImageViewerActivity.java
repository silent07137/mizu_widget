// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public final class ImageViewerActivity extends Activity {
    private Bitmap shown;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int id = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        WidgetConfig config = new ConfigStore(this).get(id);
        if (config == null || !WidgetEngine.owns(this, id)) { finish(); return; }
        LinearLayout page = Ui.page(this);
        Ui.heading(this, page, "", "图片预览", config.gif ? "GIF 静态预览" : "");
        ImageView image = Ui.preview(this, page, 420);
        Button close = Ui.button(this, "返回", false);
        Ui.gap(this, page, 20);
        page.addView(close);
        close.setOnClickListener(v -> finish());
        WidgetEngine.IO.execute(() -> {
            try {
                WidgetConfig full = new WidgetConfig(config.uri, WidgetConfig.Scale.FIT, 0, 1, 0, config.click, config.gif);
                Bitmap bitmap = WidgetRenderer.render(getApplicationContext(), full, 720, 960, 1);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) { bitmap.recycle(); return; }
                    shown = bitmap;
                    image.setImageBitmap(bitmap);
                });
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError e) {
                runOnUiThread(() -> android.widget.Toast.makeText(this, "图片无法读取，请在组件设置中重新选择", android.widget.Toast.LENGTH_LONG).show());
            }
        });
    }
    @Override public void onDestroy() { if (shown != null) shown.recycle(); super.onDestroy(); }
}
