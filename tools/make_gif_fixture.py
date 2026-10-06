"""Generate an original, synthetic looping GIF for device QA (stdlib only).

SPDX-License-Identifier: GPL-2.0-only
"""
import argparse
import pathlib
import struct


def lzw(pixels):
    # Clear before every pixel keeps the code width at three bits.
    codes = [code for pixel in pixels for code in (4, pixel)] + [5]
    packed = bytearray()
    bits = value = 0
    for code in codes:
        value |= code << bits
        bits += 3
        while bits >= 8:
            packed.append(value & 255)
            value >>= 8
            bits -= 8
    if bits:
        packed.append(value & 255)
    return b"".join(bytes([len(packed[i:i + 255])]) + packed[i:i + 255]
                    for i in range(0, len(packed), 255)) + b"\0"


def fixture(transparent=False):
    side = 64
    data = bytearray(b"GIF89a" + struct.pack("<HHBBB", side, side, 0x81, 0, 0))
    data += bytes((0, 0, 0, 255, 0, 0, 0, 0, 255, 0, 255, 0))
    data += b"\x21\xff\x0bNETSCAPE2.0\x03\x01\x00\x00\x00"
    for color in ([1, 2] if transparent else [1, 2, 3, 1]):
        data += b"\x21\xf9\x04" + bytes([9 if transparent else 4]) + struct.pack("<H", 50) + b"\0\0"
        data += b"," + struct.pack("<HHHHB", 0, 0, side, side, 0) + b"\x02"
        pixels = [color if not transparent or ((x < side // 2) == (color == 1)) else 0
                  for y in range(side) for x in range(side)]
        data += lzw(pixels)
    return bytes(data + b";")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("output", type=pathlib.Path)
    parser.add_argument("--transparent", action="store_true")
    args = parser.parse_args()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(fixture(args.transparent))
    print(f"Synthetic GIF: {args.output.name}")
