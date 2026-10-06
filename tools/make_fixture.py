"""Create a redistributable synthetic PNG for manual SAF / launcher testing.

SPDX-License-Identifier: GPL-2.0-only
Uses only Python's standard library. No user images are read.
"""
import pathlib
import struct
import zlib


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


def make_png(path):
    width, height = 640, 480
    scanlines = bytearray()
    for y in range(height):
        scanlines.append(0)
        for x in range(width):
            # Transparent edges and an original geometric landscape.
            alpha = 255 if 24 < x < width - 24 and 24 < y < height - 24 else 0
            rgb = (210 - y // 8, 234 - y // 12, 230 - y // 15)
            if (x - 470) ** 2 + (y - 112) ** 2 < 40 ** 2:
                rgb = (250, 245, 211)
            if y > 320 - abs(x - 205) * .5:
                rgb = (117, 167, 151)
            if y > 420 - abs(x - 400) * .7:
                rgb = (56, 120, 123)
            scanlines.extend((*rgb, alpha))
    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(scanlines))
    data += chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


if __name__ == "__main__":
    import sys
    make_png(pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else "test-artifacts/Mizu-widget-test.png"))
