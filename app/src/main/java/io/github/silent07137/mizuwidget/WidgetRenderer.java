// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import java.io.IOException;

final class WidgetRenderer {
    static Bitmap render(Context context, WidgetConfig config, int width, int height, float dpScale) throws IOException {
        if (config == null || config.uri.isEmpty()) throw new IOException("请先选择图片");
        Uri uri = Uri.parse(config.uri);
        if (!"content".equals(uri.getScheme())) throw new IOException("请选择系统文件选择器中的图片");
        int[] size = Geometry.boundedSize(width, height);
        // The decoder also applies EXIF orientation. GIFs decode to a still frame in this release.
        Bitmap source = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri),
            (decoder, info, unused) -> {
                int sourceWidth = info.getSize().getWidth(), sourceHeight = info.getSize().getHeight();
                if (sourceWidth < 1 || sourceHeight < 1) throw new IllegalArgumentException("图片尺寸无效");
                // Bound even panoramic sources, before allocating a bitmap.
                double factor = Math.min(1, Math.min(
                    Math.max(size[0], size[1]) * 2d / Math.max(sourceWidth, sourceHeight),
                    Math.sqrt(2097152d / ((double) sourceWidth * sourceHeight))));
                decoder.setTargetSize(Math.max(1, (int) (sourceWidth * factor)),
                    Math.max(1, (int) (sourceHeight * factor)));
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                decoder.setOnPartialImageListener(exception -> false);
            });
        Bitmap output = null;
        try {
            output = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            float radius = Math.min(Math.min(size[0], size[1]) / 2f,
                config.radiusDp * dpScale * size[0] / Math.max(1f, width));
            Path clip = new Path();
            clip.addRoundRect(new RectF(0, 0, size[0], size[1]), radius, radius, Path.Direction.CW);
            canvas.clipPath(clip);
            canvas.drawColor(config.background);
            float[] rect = Geometry.destination(source.getWidth(), source.getHeight(), size[0], size[1], config.scale);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            paint.setAlpha(Math.round(config.opacity * 255));
            canvas.drawBitmap(source, null, new RectF(rect[0], rect[1], rect[2], rect[3]), paint);
            return output;
        } catch (RuntimeException | OutOfMemoryError e) {
            if (output != null) output.recycle();
            throw e;
        } finally { source.recycle(); }
    }
}
