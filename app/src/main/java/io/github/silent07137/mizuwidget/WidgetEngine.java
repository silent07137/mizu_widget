// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.AtomicFile;
import android.view.View;
import android.widget.RemoteViews;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class WidgetEngine {
    static final ExecutorService IO = Executors.newSingleThreadExecutor();
    static ComponentName provider(Context context) { return new ComponentName(context, MizuWidgetProvider.class); }
    static boolean owns(Context context, int id) {
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id);
        return info != null && provider(context).equals(info.provider);
    }
    static File cacheFile(Context context, int id) { return new File(context.getCacheDir(), "widget_" + id + ".png"); }
    static int[] size(Context context, int id) {
        Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(id);
        boolean landscape = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int w = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH : AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180);
        int h = options.getInt(landscape ? AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT : AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 180);
        float density = context.getResources().getDisplayMetrics().density;
        return new int[] {Math.round(Math.max(40, w) * density), Math.round(Math.max(40, h) * density)};
    }
    static void update(Context context, int id) {
        if (!owns(context, id)) return;
        WidgetConfig config = new ConfigStore(context).get(id);
        if (config == null) { publish(context, id, null, null, "轻点选择图片"); return; }
        if (config.gifPlayback) {
            int[] size = size(context, id);
            try (GifFrames.Sequence sequence = GifFrames.render(context, config, size[0], size[1],
                    context.getResources().getDisplayMetrics().density)) {
                try { cache(context, id, sequence.frames.get(0)); } catch (IOException ignored) { }
                if (owns(context, id)) AppWidgetManager.getInstance(context).updateAppWidget(id,
                    animatedViews(context, id, config, sequence));
                return;
            } catch (IOException | RuntimeException | OutOfMemoryError ignored) {
                // Unsupported/oversized GIFs retain the ordinary static-image path.
            }
        }
        Bitmap bitmap = null;
        try {
            int[] size = size(context, id);
            bitmap = WidgetRenderer.render(context, config, size[0], size[1], context.getResources().getDisplayMetrics().density);
            // Storage failure must not prevent an otherwise valid desktop update.
            try { cache(context, id, bitmap); } catch (IOException ignored) { }
            publish(context, id, config, bitmap, null);
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            publish(context, id, config, null, "图片无法读取\n轻点重新选择");
        } finally { if (bitmap != null) bitmap.recycle(); }
    }
    static void cache(Context context, int id, Bitmap bitmap) throws IOException {
        AtomicFile file = new AtomicFile(cacheFile(context, id));
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException("缓存失败");
            file.finishWrite(stream);
        } catch (IOException | RuntimeException e) {
            if (stream != null) file.failWrite(stream);
            throw e;
        }
    }
    static RemoteViews views(Context context, int id, WidgetConfig config, Bitmap bitmap, String error) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mizu_widget);
        views.setViewVisibility(R.id.widget_message, bitmap == null ? View.VISIBLE : View.GONE);
        views.setImageViewBitmap(R.id.widget_image, bitmap);
        if (error != null) views.setTextViewText(R.id.widget_message, error);
        applyClick(context, id, config, views, bitmap == null);
        return views;
    }
    static RemoteViews animatedViews(Context context, int id, WidgetConfig config, GifFrames.Sequence sequence) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mizu_widget_gif);
        views.removeAllViews(R.id.widget_animation);
        views.setInt(R.id.widget_animation, "setFlipInterval", sequence.intervalMs);
        for (Bitmap bitmap : sequence.frames) {
            RemoteViews frame = new RemoteViews(context.getPackageName(), R.layout.mizu_gif_frame);
            frame.setImageViewBitmap(R.id.gif_frame, bitmap);
            views.addView(R.id.widget_animation, frame);
        }
        applyClick(context, id, config, views, false);
        return views;
    }
    private static void applyClick(Context context, int id, WidgetConfig config, RemoteViews views, boolean repair) {
        WidgetConfig.Click click = config == null ? WidgetConfig.Click.CONFIGURE : config.click;
        if (repair || click != WidgetConfig.Click.NONE) {
            Class<?> activity = !repair && click == WidgetConfig.Click.VIEW ? ImageViewerActivity.class : ConfigureActivity.class;
            Intent intent = new Intent(context, activity)
                .setData(Uri.parse("mizu://widget/" + id + "/" + activity.getSimpleName()))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            views.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        } else views.setOnClickPendingIntent(R.id.widget_root, null);
    }
    private static void publish(Context context, int id, WidgetConfig config, Bitmap bitmap, String error) {
        if (owns(context, id))
            AppWidgetManager.getInstance(context).updateAppWidget(id, views(context, id, config, bitmap, error));
    }
}
