"""Install debug + test APKs and run the platform test suite on one device.

SPDX-License-Identifier: GPL-2.0-only
Usage: python tools/device_tests.py --adb <adb-path> [--serial <device>]
"""
import argparse
import pathlib
import re
import subprocess
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--adb", default="adb")
parser.add_argument("--serial")
args = parser.parse_args()
root = pathlib.Path(__file__).resolve().parents[1]
adb = [args.adb] + (["-s", args.serial] if args.serial else [])
package = "io.github.silent07137.mizuwidget.debug"


def run(*parts):
    result = subprocess.run(adb + list(parts), text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    if result.returncode:
        raise RuntimeError(result.stdout)
    return result.stdout


try:
    print(run("install", "-r", str(root / "app/build/outputs/apk/debug/app-debug.apk")))
    print(run("install", "-r", str(root / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk")))
    run("shell", "am", "start", "-n", package + "/io.github.silent07137.mizuwidget.MainActivity")
    # The CLI expects a numeric user ID; '--user current' silently grants no permission on some ROMs.
    user = run("shell", "am", "get-current-user").strip()
    run("shell", "appwidget", "grantbind", "--package", package, "--user", user)
    try:
        output = run("shell", "am", "instrument", "-w", "-r",
            package + ".test/io.github.silent07137.mizuwidget.WidgetTestRunner")
    finally:
        run("shell", "appwidget", "revokebind", "--package", package, "--user", user)
    evidence = root / "test-artifacts/instrumentation.txt"
    evidence.parent.mkdir(exist_ok=True)
    evidence.write_text(output, encoding="utf-8")
    result = re.search(r"Tests: (\d+), failures: (\d+)", output)
    if not result or int(result[1]) == 0 or int(result[2]) != 0 or "INSTRUMENTATION_CODE: -1" not in output:
        print(output)
        sys.exit(1)
    print(f"Passed {result[1]} device tests.")
except (RuntimeError, OSError) as error:
    print(error, file=sys.stderr)
    sys.exit(1)
