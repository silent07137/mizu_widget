// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.app.Activity;
import android.app.Instrumentation;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import java.util.ArrayList;

/** Platform-only instrumentation runner; no third-party test runtime. */
public final class WidgetTestRunner extends Instrumentation {
    private interface Test { void run() throws Exception; }
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<Test> tests = new ArrayList<>();
    private void add(String name, Test test) { names.add(name); tests.add(test); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private WidgetConfig config(String name, WidgetConfig.Scale scale, float radius, float alpha, int bg) {
        return new WidgetConfig(FixtureProvider.ROOT + name, scale, radius, alpha, bg, WidgetConfig.Click.CONFIGURE, "gif".equals(name));
    }
    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void near(float actual, float expected, String message) { check(Math.abs(actual - expected) < .01, message + ": " + actual); }
    @Override public void onStart() {
        Context target = getTargetContext();
        add("boundedBitmapMemory", () -> {
            for (int[] input : new int[][]{{100000, 100000}, {40000, 1}, {1, 40000}, {0, 0}, {1080, 2400}}) {
                int[] size = Geometry.boundedSize(input[0], input[1]);
                check(size[0] >= 1 && size[1] >= 1 && size[0] <= 1024 && size[1] <= 1024, "bounded dimensions");
                check((long) size[0] * size[1] <= Geometry.MAX_PIXELS, "bitmap memory budget");
            }
        });
        add("scaleGeometry", () -> {
            float[] fit = Geometry.destination(200, 100, 100, 100, WidgetConfig.Scale.FIT);
            near(fit[1], 25, "fit top"); near(fit[3], 75, "fit bottom");
            float[] crop = Geometry.destination(200, 100, 100, 100, WidgetConfig.Scale.CROP);
            near(crop[0], -50, "crop left"); near(crop[2], 150, "crop right");
            float[] stretch = Geometry.destination(200, 100, 100, 100, WidgetConfig.Scale.STRETCH);
            near(stretch[0], 0, "stretch left"); near(stretch[3], 100, "stretch bottom");
        });
        add("transparentPngAndCorners", () -> {
            Bitmap image = WidgetRenderer.render(target, config("transparent", WidgetConfig.Scale.FIT, 20, 1, 0), 200, 200, 1);
            try {
                check(Color.alpha(image.getPixel(150, 100)) == 255, "opaque half retained");
                check(Color.alpha(image.getPixel(30, 100)) == 0, "PNG alpha retained");
                check(Color.alpha(image.getPixel(100, 10)) == 0, "fit letterbox transparent");
            } finally { image.recycle(); }
            image = WidgetRenderer.render(target, config("red", WidgetConfig.Scale.STRETCH, 40, 1, 0), 200, 200, 1);
            try {
                check(Color.alpha(image.getPixel(0, 0)) == 0, "rounded corner clipped");
                check(Color.red(image.getPixel(100, 100)) == 255, "center intact");
            } finally { image.recycle(); }
        });
        add("opacityAndBackground", () -> {
            Bitmap image = WidgetRenderer.render(target, config("red", WidgetConfig.Scale.FIT, 0, .5f, 0), 200, 200, 1);
            try { check(Math.abs(Color.alpha(image.getPixel(100, 100)) - 128) <= 1, "alpha applied"); }
            finally { image.recycle(); }
            image = WidgetRenderer.render(target, config("red", WidgetConfig.Scale.FIT, 0, 0, Color.BLUE), 200, 200, 1);
            try { check(image.getPixel(100, 100) == Color.BLUE, "background independent of image alpha"); }
            finally { image.recycle(); }
        });
        add("jpegWebpGifAndLargeImage", () -> {
            for (String name : new String[]{"jpeg", "webp", "gif", "large"}) {
                Bitmap image = WidgetRenderer.render(target, config(name, WidgetConfig.Scale.FIT, 0, 1, 0), 4000, 3000, 1);
                check(image.getAllocationByteCount() <= Geometry.MAX_PIXELS * 4, name + " bitmap budget");
                image.recycle();
            }
        });
        add("corruptAndInvalidSource", () -> {
            boolean rejected = false;
            try { WidgetRenderer.render(target, config("corrupt", WidgetConfig.Scale.FIT, 0, 1, 0), 100, 100, 1); }
            catch (java.io.IOException e) { rejected = true; }
            check(rejected, "corrupt image rejected");
            rejected = false;
            try { WidgetRenderer.render(target, new WidgetConfig("file:///invalid", null, 0, 1, 0, null, false), 100, 100, 1); }
            catch (java.io.IOException e) { rejected = true; }
            check(rejected, "only authorized content URI accepted");
        });
        add("independentConfigsAndCleanup", () -> {
            ConfigStore store = new ConfigStore(target);
            int a = 910001, b = 910002;
            try {
                store.save(a, config("red", WidgetConfig.Scale.CROP, 23, .4f, Color.WHITE));
                store.save(b, config("blue", WidgetConfig.Scale.FIT, 0, 1, 0));
                check(store.get(a).scale == WidgetConfig.Scale.CROP, "A crop");
                check(store.get(b).scale == WidgetConfig.Scale.FIT, "B fit");
                check(!store.get(a).uri.equals(store.get(b).uri), "independent images");
                Bitmap bitmap = WidgetRenderer.render(target, store.get(a), 100, 100, 1);
                WidgetEngine.cache(target, a, bitmap); bitmap.recycle();
                check(WidgetEngine.cacheFile(target, a).exists(), "cache created");
                store.delete(a);
                check(store.get(a) == null && !WidgetEngine.cacheFile(target, a).exists(), "A fully removed");
                check(store.get(b) != null, "B preserved");
                check(new ConfigStore(target).get(b) != null, "persistent across store instances");
            } finally { store.delete(a); store.delete(b); }
        });
        add("restoreOverlappingIdsAndDraftLifecycle", () -> {
            ConfigStore store = new ConfigStore(target);
            int a = 910003, b = 910004, c = 910005;
            try {
                store.save(a, config("red", WidgetConfig.Scale.FIT, 1, 1, 0));
                store.save(b, config("blue", WidgetConfig.Scale.CROP, 2, 1, 0));
                store.restore(new int[]{a, b}, new int[]{b, c});
                check(store.get(a) == null, "old ID removed");
                check(store.get(b).uri.endsWith("/red"), "first image restored");
                check(store.get(c).uri.endsWith("/blue"), "second image restored");
                store.putDraft("test", store.get(c));
                check(store.draft("test").scale == WidgetConfig.Scale.CROP, "pin draft persisted");
                store.removeDraft("test");
                check(store.draft("test") == null && store.get(c) != null, "draft cleanup preserves widget");
            } finally { store.delete(a); store.delete(b); store.delete(c); store.removeDraft("test"); }
        });
        add("actualWidgetHostRenderResizeDelete", () -> {
            final AppWidgetHost[] host = new AppWidgetHost[1];
            runOnMainSync(() -> host[0] = new AppWidgetHost(target, 1737001));
            int id = host[0].allocateAppWidgetId();
            ConfigStore store = new ConfigStore(target);
            try {
                AppWidgetManager manager = AppWidgetManager.getInstance(target);
                check(manager.bindAppWidgetIdIfAllowed(id, WidgetEngine.provider(target)), "test bind permission required");
                Bundle options = new Bundle();
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 100);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 150);
                manager.updateAppWidgetOptions(id, options);
                store.save(id, config("red", WidgetConfig.Scale.CROP, 20, 1, 0));
                WidgetEngine.update(target, id);
                check(WidgetEngine.cacheFile(target, id).exists(), "actual widget render cached");
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                manager.updateAppWidgetOptions(id, options);
                WidgetEngine.update(target, id);
                int[] size = WidgetEngine.size(target, id);
                check(size[0] >= 250, "host resize options honored");
                runOnMainSync(() -> {
                    android.appwidget.AppWidgetHostView view = host[0].createView(target, id, manager.getAppWidgetInfo(id));
                    check(view != null, "RemoteViews inflate in actual host");
                });
                store.save(id, config("corrupt", WidgetConfig.Scale.FIT, 0, 1, 0));
                WidgetEngine.update(target, id); // Visible repair placeholder; no crash.
            } finally {
                host[0].deleteAppWidgetId(id);
                store.delete(id);
                host[0].deleteHost();
            }
        });
        add("offlineManifestAndRuntime", () -> {
            PackageInfo info = target.getPackageManager().getPackageInfo(target.getPackageName(), android.content.pm.PackageManager.GET_PERMISSIONS);
            check(info.requestedPermissions == null || info.requestedPermissions.length == 0, "no requested permissions");
            boolean kotlinAbsent = false;
            try { Class.forName("kotlin.jvm.internal.Intrinsics"); } catch (ClassNotFoundException e) { kotlinAbsent = true; }
            check(kotlinAbsent, "no Kotlin runtime bundled");
        });
        int failed = 0;
        for (int i = 0; i < tests.size(); i++) {
            Bundle status = new Bundle();
            status.putString("class", getClass().getName());
            status.putString("test", names.get(i));
            status.putInt("numtests", tests.size());
            status.putInt("current", i + 1);
            sendStatus(1, status);
            try {
                tests.get(i).run();
                status.putString("stream", "."); sendStatus(0, status);
            } catch (Throwable e) {
                failed++;
                status.putString("stack", android.util.Log.getStackTraceString(e));
                status.putString("stream", "\nFAIL: " + names.get(i) + "\n" + e);
                sendStatus(-2, status);
            }
        }
        Bundle result = new Bundle();
        result.putString("stream", "\nTests: " + tests.size() + ", failures: " + failed + "\n");
        finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
}
