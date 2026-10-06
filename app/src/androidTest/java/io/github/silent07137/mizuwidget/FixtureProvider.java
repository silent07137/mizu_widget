// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/** Synthetic images only, installed in the test APK, never in production. */
public final class FixtureProvider extends ContentProvider {
    static final String ROOT = "content://io.github.silent07137.mizuwidget.test.fixtures/";
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if ("gif".equals(name) || "animated".equals(name) || "gifalpha".equals(name) || "giflong".equals(name)) return "image/gif";
        if ("gifasjpeg".equals(name)) return "image/jpeg";
        if ("jpeg".equals(name)) return "image/jpeg";
        if ("webp".equals(name)) return "image/webp";
        return "image/png";
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (!"r".equals(mode) || name == null ||
            !java.util.Arrays.asList("transparent", "red", "blue", "large", "jpeg", "webp", "gif", "animated", "gifalpha", "giflong", "gifasjpeg", "corrupt").contains(name))
            throw new FileNotFoundException("Unknown fixture");
        File file = new File(getContext().getCacheDir(), "mizu_fixture_" + name);
        if (!file.exists()) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                if ("giflong".equals(name)) out.write(GifFixture.create(false, 1000));
                else if ("gifasjpeg".equals(name) || "animated".equals(name) || "gifalpha".equals(name)) out.write(GifFixture.create("gifalpha".equals(name)));
                else if ("gif".equals(name)) {
                    out.write(android.util.Base64.decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7", android.util.Base64.DEFAULT));
                } else if ("corrupt".equals(name)) out.write(new byte[]{1, 2, 3, 4});
                else {
                    boolean large = "large".equals(name);
                    Bitmap image = Bitmap.createBitmap(large ? 4000 : 120, large ? 3000 : 60, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(image);
                    Paint paint = new Paint();
                    paint.setColor("blue".equals(name) ? Color.BLUE : Color.RED);
                    canvas.drawRect("transparent".equals(name) ? 60 : 0, 0, image.getWidth(), image.getHeight(), paint);
                    Bitmap.CompressFormat format = "jpeg".equals(name) ? Bitmap.CompressFormat.JPEG :
                        "webp".equals(name) ? Bitmap.CompressFormat.WEBP : Bitmap.CompressFormat.PNG;
                    image.compress(format, 100, out);
                    image.recycle();
                }
            } catch (java.io.IOException e) { throw new FileNotFoundException("Could not create fixture"); }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String s, String[] a) { throw new UnsupportedOperationException(); }
}
