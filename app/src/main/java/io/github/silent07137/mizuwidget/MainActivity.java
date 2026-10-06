// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); }
    @Override public void onResume() {
        super.onResume();
        new ConfigStore(this).pruneDrafts();
        LinearLayout page = Ui.page(this);
        Ui.heading(this, page, "MIZU / 小小的桌面风景", "把喜欢，留在桌面。", "一张照片，一点心情。\n为你的桌面留一处安静的角落。");
        LinearLayout hero = Ui.card(this, page);
        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(R.drawable.ic_mizu);
        icon.setContentDescription("Mizu 水滴");
        hero.addView(icon, new LinearLayout.LayoutParams(-1, Ui.dp(this, 100)));
        Ui.gap(this, hero, 16);
        hero.addView(Ui.text(this, "你的图片，只留在本机", 19, Ui.INK));
        Ui.gap(this, hero, 8);
        hero.addView(Ui.text(this, "透明 PNG · 自由缩放 · 独立设置\n无需账号，没有广告。GIF 在初版显示静态预览。", 14, Ui.MUTED));
        Ui.gap(this, page, 22);
        Button add = Ui.button(this, "＋  添加图片组件", true);
        add.setOnClickListener(v -> startActivity(new Intent(this, ConfigureActivity.class).setAction(ConfigureActivity.ACTION_CREATE)));
        page.addView(add);
        Ui.gap(this, page, 26);
        int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(WidgetEngine.provider(this));
        page.addView(Ui.text(this, "桌面上的组件  ·  " + ids.length, 18, Ui.INK));
        Ui.gap(this, page, 14);
        if (ids.length == 0) {
            LinearLayout empty = Ui.card(this, page);
            empty.addView(Ui.text(this, "还没有桌面组件", 16, Ui.INK));
            Ui.gap(this, empty, 6);
            empty.addView(Ui.text(this, "点上方按钮开始，或长按桌面 → 小组件 → Mizu Widget。", 14, Ui.MUTED));
        } else {
            ConfigStore store = new ConfigStore(this);
            for (int id : ids) {
                WidgetConfig config = store.get(id);
                LinearLayout card = Ui.card(this, page);
                card.addView(Ui.text(this, "图片组件 #" + id, 18, Ui.INK));
                Ui.gap(this, card, 5);
                card.addView(Ui.text(this, config == null ? "等待选择图片" :
                    scaleLabel(config.scale) + " · 圆角 " + Math.round(config.radiusDp) + " dp · " +
                    Math.round(config.opacity * 100) + "% 不透明度" + (config.gif ? " · GIF 静态预览" : ""), 13, Ui.MUTED));
                Ui.gap(this, card, 12);
                Button edit = Ui.button(this, "调整这张图片", false);
                edit.setOnClickListener(v -> startActivity(new Intent(this, ConfigureActivity.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)));
                card.addView(edit);
                Ui.gap(this, page, 12);
            }
        }
        Ui.gap(this, page, 22);
        Button refresh = Ui.button(this, "刷新桌面组件", false);
        refresh.setOnClickListener(v -> {
            WidgetEngine.IO.execute(() -> { for (int id : ids) WidgetEngine.update(getApplicationContext(), id); });
            android.widget.Toast.makeText(this, "正在刷新", android.widget.Toast.LENGTH_SHORT).show();
        });
        page.addView(refresh);
        Ui.gap(this, page, 12);
        Button about = Ui.button(this, "关于 Mizu Widget", false);
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        page.addView(about);
    }
    static String scaleLabel(WidgetConfig.Scale mode) {
        return switch (mode) { case FIT -> "完整显示"; case CROP -> "居中裁剪"; case STRETCH -> "拉伸铺满"; };
    }
}
