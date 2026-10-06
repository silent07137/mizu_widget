// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class AboutActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = Ui.page(this);
        Ui.heading(this, page, "MIZU / 关于", "轻一点，纯粹一点。", "Mizu Widget  " + BuildConfig.VERSION_NAME);
        LinearLayout card = Ui.card(this, page);
        card.addView(Ui.text(this, "桌面上的小小风景", 20, Ui.INK));
        Ui.gap(this, card, 12);
        card.addView(Ui.text(this, "一个用于在 Android 桌面放置图片与 GIF 静态预览的轻量组件应用。\n\n图片在本机处理，不上传。应用未声明网络权限，没有账号、广告或遥测。", 15, Ui.MUTED));
        Ui.gap(this, page, 22);
        page.addView(Ui.text(this, "开源协议", 20, Ui.INK));
        Ui.gap(this, page, 10);
        page.addView(Ui.text(this, "GNU General Public License version 2 only\nGPL-2.0-only\n\n你可以在 GPLv2 条款允许的范围内使用、研究、修改和重新分发本项目源代码。本程序不提供任何担保。", 14, Ui.MUTED));
        Ui.gap(this, page, 20);
        Button source = Ui.button(this, "源代码 · silent07137/mizu_widget", false);
        source.setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/silent07137/mizu_widget"))); }
            catch (android.content.ActivityNotFoundException e) {
                new AlertDialog.Builder(this).setMessage("https://github.com/silent07137/mizu_widget").setPositiveButton("关闭", null).show();
            }
        });
        page.addView(source);
        Ui.gap(this, page, 10);
        Button gpl = Ui.button(this, "GNU GPL v2 · 完整许可", false);
        gpl.setOnClickListener(v -> {
            try (InputStream stream = getAssets().open("gpl-2.0.txt")) {
                java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int count;
                while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
                showText("GNU GPL version 2", bytes.toString(StandardCharsets.UTF_8.name()));
            } catch (IOException e) { showText("GNU GPL version 2", "许可正文无法读取，请查看源代码仓库中的 LICENSE。"); }
        });
        page.addView(gpl);
        Ui.gap(this, page, 10);
        Button third = Ui.button(this, "第三方开源许可", false);
        third.setOnClickListener(v -> showText("第三方开源许可",
            "本版本未打包第三方运行库。\n\n应用使用设备提供的 Android 平台 API。Android 平台的许可信息请查看设备的设置 → 关于手机 → 法律信息。\n\nGradle 和 Android Gradle Plugin 仅用于构建，不随 APK 分发。"));
        page.addView(third);
        Ui.gap(this, page, 10);
        Button version = Ui.button(this, "版本信息", false);
        version.setOnClickListener(v -> showText("版本信息", "Mizu Widget\n版本：" + BuildConfig.VERSION_NAME +
            "\n版本代码：" + BuildConfig.VERSION_CODE + "\n最低系统：Android 9\nGIF：静态预览\n无常驻后台服务"));
        page.addView(version);
        Ui.gap(this, page, 24);
        page.addView(Ui.text(this, "Copyright © 2026 silent07137", 12, Ui.MUTED));
    }
    private void showText(String title, String value) {
        ScrollView scroll = new ScrollView(this);
        TextView text = Ui.text(this, value, 13, Ui.INK);
        text.setTextIsSelectable(true);
        text.setPadding(Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16));
        scroll.addView(text);
        new AlertDialog.Builder(this).setTitle(title).setView(scroll).setPositiveButton("关闭", null).show();
    }
}
