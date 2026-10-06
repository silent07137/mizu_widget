// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int INK = Color.rgb(30, 57, 60);
    static final int MUTED = Color.rgb(92, 114, 116);
    static final int TEAL = Color.rgb(53, 115, 118);
    static final int PAPER = Color.rgb(245, 247, 246);
    static int dp(Activity a, float value) { return Math.round(a.getResources().getDisplayMetrics().density * value); }
    static LinearLayout page(Activity a) {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PAPER);
        LinearLayout column = new LinearLayout(a);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(a, 24), dp(a, 24), dp(a, 24), dp(a, 32));
        scroll.addView(column);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                scroll.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        } else {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                scroll.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            });
        }
        a.setContentView(scroll);
        scroll.requestApplyInsets();
        return column;
    }
    static TextView text(Activity a, String value, float size, int color) {
        TextView text = new TextView(a);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setLineSpacing(dp(a, 3), 1);
        return text;
    }
    static void gap(Activity a, LinearLayout parent, int size) {
        View gap = new View(a);
        parent.addView(gap, new LinearLayout.LayoutParams(1, dp(a, size)));
    }
    static void heading(Activity a, LinearLayout parent, String eyebrow, String title, String subtitle) {
        TextView small = text(a, eyebrow, 12, TEAL);
        small.setLetterSpacing(.12f);
        parent.addView(small);
        gap(a, parent, 10);
        TextView heading = text(a, title, 30, INK);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(heading);
        gap(a, parent, 8);
        parent.addView(text(a, subtitle, 14, MUTED));
        gap(a, parent, 24);
    }
    static GradientDrawable background(Activity a, int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(a, radius));
        return drawable;
    }
    static Button button(Activity a, String label, boolean primary) {
        Button button = new Button(a);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : TEAL);
        button.setMinHeight(dp(a, 52));
        button.setMinimumHeight(dp(a, 52));
        button.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 8));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22427F82),
            background(a, primary ? TEAL : Color.rgb(227, 239, 236), 16), null));
        return button;
    }
    static LinearLayout card(Activity a, LinearLayout parent) {
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a, 20), dp(a, 18), dp(a, 20), dp(a, 18));
        card.setBackground(background(a, Color.WHITE, 22));
        parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }
    static ImageView preview(Activity a, LinearLayout parent, int height) {
        FrameLayout frame = new FrameLayout(a);
        frame.setBackground(new Checker(a));
        ImageView image = new ImageView(a);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("组件效果预览");
        frame.addView(image, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        parent.addView(frame, new LinearLayout.LayoutParams(-1, dp(a, height)));
        return image;
    }
    private static final class Checker extends Drawable {
        private final Paint paint = new Paint();
        private final int cell;
        Checker(Activity a) { cell = dp(a, 12); }
        @Override public void draw(Canvas canvas) {
            android.graphics.Rect bounds = getBounds();
            for (int y = bounds.top; y < bounds.bottom; y += cell)
                for (int x = bounds.left; x < bounds.right; x += cell) {
                    paint.setColor(((x / cell + y / cell) & 1) == 0 ? 0xFFE3E9E6 : 0xFFF2F5F3);
                    canvas.drawRect(x, y, Math.min(x + cell, bounds.right), Math.min(y + cell, bounds.bottom), paint);
                }
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { }
        @SuppressWarnings("deprecation") @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
    }
}
