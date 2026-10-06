// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Bounded, platform-decoded GIF samples; the launcher owns the playback timer. */
@SuppressWarnings("deprecation")
final class GifFrames {
    static final int MAX_BYTES = 8 * 1024 * 1024;
    static final int MAX_FRAMES = 24;
    static final int MAX_HIGH_FRAMES = 120;
    static int intervalFor(int fps) { return (1000 + fps - 1) / fps; }
    static long pixelBudget(Context context) {
        android.util.DisplayMetrics display = context.getApplicationContext().getResources().getDisplayMetrics();
        // AppWidgetService caps bitmaps at 1.5 screenfuls; retain a margin on small displays.
        long screen = (long) Math.max(1, display.widthPixels) * Math.max(1, display.heightPixels);
        return Math.max(MAX_HIGH_FRAMES, Math.min(Geometry.MAX_PIXELS, screen));
    }
    static final long MAX_SOURCE_PIXELS = 2097152L;
    // Bound even a decoder that retains every source frame, before invoking native code.
    static final long MAX_SOURCE_FRAME_PIXELS = 16777216L;

    static boolean isGif(Context context, String source) throws IOException {
        Uri uri = Uri.parse(source);
        if (!"content".equals(uri.getScheme())) throw new IOException("请选择系统文件选择器中的图片");
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IOException("图片无法读取");
            byte[] signature = new byte[6];
            int offset = 0, count;
            while (offset < signature.length && (count = stream.read(signature, offset, signature.length - offset)) != -1) {
                if (count == 0) break;
                offset += count;
            }
            String header = new String(signature, StandardCharsets.US_ASCII);
            return "GIF87a".equals(header) || "GIF89a".equals(header);
        }
    }

    static final class Sequence implements AutoCloseable {
        final List<Bitmap> frames = new ArrayList<>();
        final int intervalMs;
        Sequence(int intervalMs) { this.intervalMs = intervalMs; }
        @Override public void close() { for (Bitmap bitmap : frames) bitmap.recycle(); frames.clear(); }
    }

    static final class Metadata {
        final int width, height, frames, durationMs;
        Metadata(int width, int height, int frames, int durationMs) {
            this.width = width; this.height = height; this.frames = frames; this.durationMs = durationMs;
        }
    }

    static Metadata inspect(byte[] data) throws IOException {
        Reader reader = new Reader(data);
        String header = new String(reader.bytes(6), StandardCharsets.US_ASCII);
        if (!"GIF87a".equals(header) && !"GIF89a".equals(header)) throw new IOException("不是有效的 GIF");
        int width = reader.word(), height = reader.word();
        long pixels = (long) width * height;
        if (width < 1 || height < 1 || pixels > MAX_SOURCE_PIXELS) throw new IOException("GIF 尺寸超出播放上限");
        int packed = reader.next();
        reader.skip(2);
        if ((packed & 128) != 0) reader.skip(3 * (1 << ((packed & 7) + 1)));
        int frames = 0, duration = 0, delay = 100;
        while (true) {
            int marker = reader.next();
            if (marker == 0x3b) break;
            if (marker == 0x21) {
                int extension = reader.next();
                if (extension == 0xf9) {
                    if (reader.next() != 4) throw new IOException("GIF 控制块无效");
                    reader.skip(1);
                    int centiseconds = reader.word();
                    delay = centiseconds < 2 ? 100 : centiseconds * 10;
                    reader.skip(1);
                    if (reader.next() != 0) throw new IOException("GIF 控制块无效");
                } else reader.blocks();
            } else if (marker == 0x2c) {
                int left = reader.word(), top = reader.word(), frameWidth = reader.word(), frameHeight = reader.word();
                if (frameWidth < 1 || frameHeight < 1 || (long) left + frameWidth > width || (long) top + frameHeight > height)
                    throw new IOException("GIF 帧尺寸无效");
                packed = reader.next();
                if ((packed & 128) != 0) reader.skip(3 * (1 << ((packed & 7) + 1)));
                int codeSize = reader.next();
                if (codeSize < 2 || codeSize > 8) throw new IOException("GIF 图像数据无效");
                reader.blocks();
                frames++;
                duration += delay;
                if (frames > 128 || pixels * frames > MAX_SOURCE_FRAME_PIXELS || duration > 120000)
                    throw new IOException("GIF 长度或帧数超出播放上限");
                delay = 100;
            } else throw new IOException("GIF 数据损坏");
        }
        if (frames == 0) throw new IOException("GIF 没有图像帧");
        return new Metadata(width, height, frames, duration);
    }

    static Sequence render(Context context, WidgetConfig config, int width, int height, float dpScale) throws IOException {
        Uri uri = Uri.parse(config.uri);
        if (!"content".equals(uri.getScheme())) throw new IOException("请选择系统文件选择器中的 GIF");
        byte[] data;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IOException("GIF 无法读取");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (bytes.size() + count > MAX_BYTES) throw new IOException("GIF 文件超过 8 MiB");
                bytes.write(buffer, 0, count);
            }
            data = bytes.toByteArray();
        }
        Metadata metadata = inspect(data);
        if (metadata.frames < 2) throw new IOException("GIF 只有一帧");
        Movie movie = Movie.decodeByteArray(data, 0, data.length);
        if (movie == null || movie.width() != metadata.width || movie.height() != metadata.height)
            throw new IOException("GIF 无法解码");
        int duration = movie.duration() > 0 ? movie.duration() : metadata.durationMs;
        int requestedInterval = intervalFor(config.gifFps);
        int limit = config.gifFps > 5 ? MAX_HIGH_FRAMES : MAX_FRAMES;
        int count = Math.min(limit, Math.max(2, (duration + requestedInterval - 1) / requestedInterval));
        int interval = Math.max(requestedInterval, (duration + count - 1) / count);
        int[] size = Geometry.boundedSize(width, height);
        long budget = pixelBudget(context);
        double factor = Math.min(1, Math.sqrt(budget / (double) count / ((double) size[0] * size[1])));
        size[0] = Math.max(1, (int) (size[0] * factor));
        size[1] = Math.max(1, (int) (size[1] * factor));
        // Rounding a very long, one-pixel-tall output up must still honor the total budget.
        while ((long) size[0] * size[1] * count > budget) {
            if (size[0] >= size[1]) size[0]--; else size[1]--;
        }
        Sequence result = new Sequence(interval);
        try {
            for (int i = 0; i < count; i++) {
                Bitmap output = Bitmap.createBitmap(size[0], size[1], Bitmap.Config.ARGB_8888);
                result.frames.add(output);
                Canvas canvas = new Canvas(output);
                float radius = Math.min(Math.min(size[0], size[1]) / 2f,
                    config.radiusDp * dpScale * size[0] / Math.max(1f, width));
                Path clip = new Path();
                clip.addRoundRect(new RectF(0, 0, size[0], size[1]), radius, radius, Path.Direction.CW);
                canvas.clipPath(clip);
                canvas.drawColor(config.background);
                float[] rect = Geometry.destination(movie.width(), movie.height(), size[0], size[1], config.scale);
                canvas.translate(rect[0], rect[1]);
                canvas.scale((rect[2] - rect[0]) / movie.width(), (rect[3] - rect[1]) / movie.height());
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                paint.setAlpha(Math.round(config.opacity * 255));
                movie.setTime((int) ((long) duration * i / count));
                movie.draw(canvas, 0, 0, paint);
            }
            return result;
        } catch (RuntimeException | OutOfMemoryError e) { result.close(); throw e; }
    }

    private static final class Reader {
        private final byte[] data;
        private int offset;
        Reader(byte[] data) throws IOException {
            if (data.length > MAX_BYTES) throw new IOException("GIF 文件超过 8 MiB");
            this.data = data;
        }
        int next() throws IOException {
            if (offset >= data.length) throw new IOException("GIF 数据不完整");
            return data[offset++] & 255;
        }
        int word() throws IOException { return next() | (next() << 8); }
        void skip(int count) throws IOException {
            if (count < 0 || count > data.length - offset) throw new IOException("GIF 数据不完整");
            offset += count;
        }
        byte[] bytes(int count) throws IOException {
            int start = offset; skip(count); return java.util.Arrays.copyOfRange(data, start, offset);
        }
        void blocks() throws IOException { int size; while ((size = next()) != 0) skip(size); }
    }
}
