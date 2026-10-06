// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

final class Geometry {
    static final int MAX_PIXELS = 524288;
    static int[] boundedSize(int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        double factor = Math.min(1, Math.min(1024d / Math.max(width, height),
            Math.sqrt(MAX_PIXELS / ((double) width * height))));
        return new int[] { Math.max(1, (int) (width * factor)), Math.max(1, (int) (height * factor)) };
    }

    static float[] destination(int sourceWidth, int sourceHeight, int width, int height, WidgetConfig.Scale mode) {
        if (mode == WidgetConfig.Scale.STRETCH) return new float[] {0, 0, width, height};
        float factor = mode == WidgetConfig.Scale.CROP
            ? Math.max(width / (float) sourceWidth, height / (float) sourceHeight)
            : Math.min(width / (float) sourceWidth, height / (float) sourceHeight);
        float w = sourceWidth * factor, h = sourceHeight * factor;
        return new float[] {(width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2};
    }
}
