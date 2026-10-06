// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Original four-color fixture, including transparent frames with disposal-to-background. */
final class GifFixture {
    static byte[] create(boolean transparent) throws IOException {
        return create(transparent, 50);
    }
    static byte[] create(boolean transparent, int delay) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("GIF89a".getBytes(StandardCharsets.US_ASCII));
        word(out, 64); word(out, 64); out.write(new byte[]{(byte) 0x81, 0, 0});
        out.write(new byte[]{0, 0, 0, (byte) 255, 0, 0, 0, 0, (byte) 255, 0, (byte) 255, 0});
        out.write(new byte[]{0x21, (byte) 0xff, 11});
        out.write("NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII));
        out.write(new byte[]{3, 1, 0, 0, 0});
        for (int color : transparent ? new int[]{1, 2} : new int[]{1, 2, 3, 1}) {
            out.write(new byte[]{0x21, (byte) 0xf9, 4, (byte) (transparent ? 9 : 4)});
            word(out, delay); out.write(new byte[]{0, 0, 0x2c});
            word(out, 0); word(out, 0); word(out, 64); word(out, 64); out.write(0); out.write(2);
            ByteArrayOutputStream packed = new ByteArrayOutputStream();
            int bits = 0, value = 0;
            for (int pixel = 0; pixel < 4096; pixel++) {
                int index = !transparent || ((pixel % 64 < 32) == (color == 1)) ? color : 0;
                for (int code : new int[]{4, index}) {
                    value |= code << bits; bits += 3;
                    if (bits >= 8) { packed.write(value & 255); value >>>= 8; bits -= 8; }
                }
            }
            value |= 5 << bits; bits += 3;
            while (bits > 0) { packed.write(value & 255); value >>>= 8; bits -= 8; }
            byte[] data = packed.toByteArray();
            for (int offset = 0; offset < data.length; offset += 255) {
                int count = Math.min(255, data.length - offset);
                out.write(count); out.write(data, offset, count);
            }
            out.write(0);
        }
        out.write(0x3b);
        return out.toByteArray();
    }
    private static void word(ByteArrayOutputStream out, int value) { out.write(value & 255); out.write(value >> 8); }
}
