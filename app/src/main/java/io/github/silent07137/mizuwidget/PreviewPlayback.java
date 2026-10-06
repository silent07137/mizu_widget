// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

/** Owns preview bitmaps and only animates while its activity is visible. */
final class PreviewPlayback {
    private final ImageView image;
    private Bitmap still;
    private GifFrames.Sequence sequence;
    private AnimationDrawable animation;
    private boolean active;
    PreviewPlayback(ImageView image) { this.image = image; }
    Bitmap first() { return sequence == null ? still : sequence.frames.get(0); }
    void show(Bitmap bitmap) { clear(); still = bitmap; image.setImageBitmap(bitmap); }
    void show(GifFrames.Sequence frames) {
        clear();
        sequence = frames;
        animation = new AnimationDrawable();
        animation.setOneShot(false);
        for (Bitmap frame : frames.frames)
            animation.addFrame(new BitmapDrawable(image.getResources(), frame), frames.intervalMs);
        image.setImageDrawable(animation);
        if (active) animation.start();
    }
    void setActive(boolean value) {
        active = value;
        if (animation != null) { if (value) animation.start(); else animation.stop(); }
    }
    void clear() {
        if (animation != null) animation.stop();
        image.setImageDrawable(null);
        animation = null;
        if (sequence != null) sequence.close();
        if (still != null) still.recycle();
        sequence = null; still = null;
    }
}
