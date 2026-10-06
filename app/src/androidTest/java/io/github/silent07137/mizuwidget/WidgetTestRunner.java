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
    private String realGifIds = "";
    private boolean realGifOnly;
    private void add(String name, Test test) { names.add(name); tests.add(test); }
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        if (args != null) {
            realGifIds = args.getString("realGifWidgetIds", "");
            realGifOnly = "true".equals(args.getString("realGifOnly"));
        }
        start();
    }
    private WidgetConfig config(String name, WidgetConfig.Scale scale, float radius, float alpha, int bg) {
        return new WidgetConfig(FixtureProvider.ROOT + name, scale, radius, alpha, bg, WidgetConfig.Click.CONFIGURE, "gif".equals(name));
    }
    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void near(float actual, float expected, String message) { check(Math.abs(actual - expected) < .01, message + ": " + actual); }
    private void update(Context context, int id) throws Exception {
        // Share the production update queue with the provider's initial binding broadcast.
        WidgetEngine.IO.submit(() -> WidgetEngine.update(context, id)).get();
    }
    private WidgetConfig gif(String name, float radius, float alpha, int bg, int fps) {
        return new WidgetConfig(FixtureProvider.ROOT + name, WidgetConfig.Scale.STRETCH, radius, alpha,
            bg, WidgetConfig.Click.CONFIGURE, true, true, fps);
    }
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
                update(target, id);
                check(WidgetEngine.cacheFile(target, id).exists(), "actual widget render cached");
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250);
                manager.updateAppWidgetOptions(id, options);
                update(target, id);
                int[] size = WidgetEngine.size(target, id);
                check(size[0] >= 250, "host resize options honored");
                runOnMainSync(() -> {
                    android.appwidget.AppWidgetHostView view = host[0].createView(target, id, manager.getAppWidgetInfo(id));
                    check(view != null, "RemoteViews inflate in actual host");
                });
                store.save(id, config("corrupt", WidgetConfig.Scale.FIT, 0, 1, 0));
                update(target, id); // Visible repair placeholder; no crash.
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
        add("gifConfigMigrationAndPersistence", () -> {
            WidgetConfig old = WidgetConfig.fromJson("{\"gif\":true,\"uri\":\"legacy\"}");
            check(!old.gifPlayback && old.gifFps == 2, "existing GIF remains static");
            WidgetConfig restored = WidgetConfig.fromJson(gif("animated", 12, .5f, Color.BLUE, 5).toJson().toString());
            check(restored.gifPlayback && restored.gifFps == 5 && restored.radiusDp == 12, "GIF options persist");
            check(WidgetConfig.defaults().gifFps == 2, "high frame rate is off by default");
            check(WidgetConfig.fromJson(gif("animated", 0, 1, 0, 30).toJson().toString()).gifFps == 30, "high rate persists when chosen");
            check(gif("animated", 0, 1, 0, 99).gifFps == 2, "unsupported rate defaults safely");
            WidgetConfig png = new WidgetConfig("png", null, 0, 1, 0, null, false, true, 5);
            check(!png.gifPlayback, "static images do not animate");
            check(!old.gifLarge && old.gifQuality, "legacy settings default to ordinary input and quality priority");
            WidgetConfig choices = new WidgetConfig("gif", null, 20, 1, 0, null, true, true, 30, true, false);
            choices = WidgetConfig.fromJson(choices.toJson().toString());
            check(choices.gifLarge && !choices.gifQuality, "large input and rate priority persist independently");
        });
        add("gifQualityPriorityPreservesDetail", () -> {
            WidgetConfig quality = gif("animated", 0, 1, 0, 30);
            WidgetConfig rate = new WidgetConfig(quality.uri, quality.scale, 0, 1, 0, quality.click,
                true, true, 30, false, false);
            try (GifFrames.Sequence clear = GifFrames.render(target, quality, 4000, 3000, 1);
                 GifFrames.Sequence fast = GifFrames.render(target, rate, 4000, 3000, 1)) {
                check(clear.frames.get(0).getWidth() > fast.frames.get(0).getWidth(), "quality priority keeps more detail");
                check(clear.frames.size() < fast.frames.size() && clear.intervalMs > fast.intervalMs,
                    "quality priority trades sampling rate for resolution");
                int[] bounded = Geometry.boundedSize(4000, 3000);
                if (GifFrames.pixelBudget(target) >= (long) bounded[0] * bounded[1] * 9 / 8)
                    check(clear.frames.get(0).getWidth() >= bounded[0] * .75 - 1,
                        "detail floor honored when two frames fit");
            }
        });
        add("gifFramesTimingAndMemory", () -> {
            for (int fps : new int[]{1, 2, 5, 15, 30}) {
                try (GifFrames.Sequence frames = GifFrames.render(target, gif("animated", 0, 1, 0, fps), 4000, 3000, 1)) {
                    long bytes = 0;
                    boolean red = false, blue = false;
                    for (Bitmap image : frames.frames) {
                        bytes += image.getAllocationByteCount();
                        int color = image.getPixel(image.getWidth() / 2, image.getHeight() / 2);
                        red |= Color.red(color) == 255;
                        blue |= Color.blue(color) == 255;
                    }
                    check(bytes <= GifFrames.pixelBudget(target) * 4 && frames.frames.size() <= (fps > 5 ? 120 : 24), "total GIF payload bounded");
                    check(red && (fps == 1 || blue), "sampled timeline retains distinct frames");
                    check(frames.intervalMs >= GifFrames.intervalFor(fps), "rate limit honored");
                }
            }
        });
        add("gifTransparencyDisposalCornersAndOpacity", () -> {
            try (GifFrames.Sequence frames = GifFrames.render(target, gif("gifalpha", 20, .5f, 0, 5), 200, 200, 1)) {
                Bitmap first = frames.frames.get(0), last = frames.frames.get(frames.frames.size() - 1);
                check(Color.alpha(first.getPixel(0, 0)) == 0, "GIF rounded corner clipped");
                check(Math.abs(Color.alpha(first.getPixel(50, 100)) - 128) <= 1, "GIF opacity applied");
                check(Color.alpha(first.getPixel(150, 100)) == 0, "GIF transparent area preserved");
                check(Color.alpha(last.getPixel(50, 100)) == 0, "previous red frame disposed");
                check(Color.blue(last.getPixel(150, 100)) == 255, "next blue frame rendered");
            }
            try (GifFrames.Sequence frames = GifFrames.render(target, gif("animated", 0, 0, Color.GREEN, 2), 100, 100, 1)) {
                check(frames.frames.get(0).getPixel(50, 50) == Color.GREEN, "GIF background independent of image opacity");
            }
        });
        add("gifHeaderDetectionAndLongSampling", () -> {
            check(GifFrames.isGif(target, FixtureProvider.ROOT + "gifasjpeg"), "GIF identified despite JPEG MIME type");
            check(!GifFrames.isGif(target, FixtureProvider.ROOT + "jpeg"), "JPEG stays static");
            try (GifFrames.Sequence frames = GifFrames.render(target, gif("giflong", 0, 1, 0, 5), 100000, 1, 1)) {
                long bytes = 0;
                for (Bitmap bitmap : frames.frames) bytes += bitmap.getAllocationByteCount();
                check(frames.frames.size() == 24 && frames.intervalMs > 200, "long GIF reduces rate at sample limit");
                check(bytes <= Geometry.MAX_PIXELS * 4, "panoramic GIF stays within payload budget");
            }
        });
        add("gifRejectsOversizedAndBrokenInput", () -> {
            byte[] valid = GifFixture.create(false);
            byte[] large = valid.clone(); large[6] = (byte) 255; large[7] = 127; large[8] = (byte) 128;
            for (byte[] data : new byte[][]{large, java.util.Arrays.copyOf(valid, valid.length - 1), new byte[]{1, 2, 3}}) {
                boolean rejected = false;
                try { GifFrames.inspect(data); } catch (java.io.IOException e) { rejected = true; }
                check(rejected, "invalid GIF rejected before native decode");
            }
            int header = 44, body = valid.length - header - 1;
            byte[] many = java.util.Arrays.copyOf(valid, valid.length + body);
            System.arraycopy(valid, header, many, valid.length - 1, body);
            many[many.length - 1] = 0x3b;
            many[6] = 0; many[7] = 8; many[8] = 0; many[9] = 4;
            boolean rejected = false;
            try { GifFrames.inspect(many); } catch (java.io.IOException e) { rejected = true; }
            // 2048*1024*8 frames fits the exact aggregate limit.
            check(!rejected, "aggregate boundary accepted");
            byte[] extra = java.util.Arrays.copyOf(many, many.length + body);
            System.arraycopy(valid, header, extra, many.length - 1, body);
            extra[extra.length - 1] = 0x3b;
            try { GifFrames.inspect(extra); } catch (java.io.IOException e) { rejected = true; }
            check(rejected, "aggregate source frames exceed native decode budget");
            check(GifFrames.inspect(extra, true).frames > 8, "large GIF opt-in accepts expanded source budget");
        });
        add("gifActualHostPlaybackFallbackAndDetach", () -> {
            final AppWidgetHost[] host = new AppWidgetHost[1];
            runOnMainSync(() -> host[0] = new AppWidgetHost(target, 1737001));
            int id = host[0].allocateAppWidgetId();
            ConfigStore store = new ConfigStore(target);
            Activity activity = null;
            final android.appwidget.AppWidgetHostView[] view = new android.appwidget.AppWidgetHostView[1];
            try {
                AppWidgetManager manager = AppWidgetManager.getInstance(target);
                check(manager.bindAppWidgetIdIfAllowed(id, WidgetEngine.provider(target)), "GIF test bind required");
                store.save(id, gif("animated", 10, 1, 0, 30));
                update(target, id);
                activity = startActivitySync(new android.content.Intent(target, MainActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                final Activity screen = activity;
                runOnMainSync(() -> {
                    screen.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    host[0].startListening();
                    view[0] = host[0].createView(target, id, manager.getAppWidgetInfo(id));
                    screen.setContentView(view[0]);
                });
                waitForIdleSync();
                final android.widget.ViewFlipper[] flipper = new android.widget.ViewFlipper[1];
                runOnMainSync(() -> flipper[0] = view[0].findViewById(R.id.widget_animation));
                android.util.DisplayMetrics display = target.getResources().getDisplayMetrics();
                check(flipper[0] != null && flipper[0].getChildCount() >= 2,
                    "GIF RemoteViews survived service parceling on " + display.widthPixels + "x" + display.heightPixels);
                final int[] child = new int[1];
                runOnMainSync(() -> child[0] = flipper[0].getDisplayedChild());
                boolean advanced = false;
                for (int attempt = 0; attempt < 15 && !advanced; attempt++) {
                    android.os.SystemClock.sleep(100);
                    final boolean[] changed = new boolean[1];
                    runOnMainSync(() -> changed[0] = child[0] != flipper[0].getDisplayedChild());
                    advanced = changed[0];
                }
                check(advanced, "launcher timer advances frames without application updates");
                runOnMainSync(() -> { screen.setContentView(new android.widget.FrameLayout(target)); screen.finish(); });
                waitForIdleSync();
                android.os.SystemClock.sleep(300);
                final boolean[] detached = new boolean[1];
                runOnMainSync(() -> { detached[0] = !flipper[0].isAttachedToWindow(); child[0] = flipper[0].getDisplayedChild(); });
                check(detached[0], "host detached");
                android.os.SystemClock.sleep(500);
                final boolean[] stopped = new boolean[1];
                runOnMainSync(() -> stopped[0] = child[0] == flipper[0].getDisplayedChild());
                check(stopped[0], "detached host stops flipping");
                for (WidgetConfig fallback : new WidgetConfig[]{config("red", WidgetConfig.Scale.FIT, 0, 1, 0), gif("gif", 0, 1, 0, 5)}) {
                    store.save(id, fallback);
                    update(target, id);
                    final boolean[] staticLayout = new boolean[1];
                    runOnMainSync(() -> {
                        android.appwidget.AppWidgetHostView still = host[0].createView(target, id, manager.getAppWidgetInfo(id));
                        staticLayout[0] = still.findViewById(R.id.widget_image) != null;
                    });
                    check(staticLayout[0], "static/unsupported GIF fallback layout");
                }
            } finally {
                final Activity screen = activity;
                runOnMainSync(() -> {
                    if (screen != null) screen.finish();
                    host[0].stopListening(); host[0].deleteAppWidgetId(id); host[0].deleteHost();
                });
                store.delete(id);
            }
        });
        if (realGifOnly) { names.clear(); tests.clear(); }
        if (!realGifIds.isEmpty()) {
            for (String value : realGifIds.split(",")) {
                int id = Integer.parseInt(value.trim());
                add("realGifWidget" + id, () -> {
                    check(WidgetEngine.owns(target, id), "real GIF widget still belongs to this app");
                    WidgetConfig saved = new ConfigStore(target).get(id);
                    check(saved != null && saved.gif && saved.gifLarge, "large GIF settings persisted");
                    WidgetConfig ordinary = new WidgetConfig(saved.uri, saved.scale, saved.radiusDp, saved.opacity,
                        saved.background, saved.click, true, true, 30, false, true);
                    boolean rejected = false;
                    try (GifFrames.Sequence ignored = GifFrames.render(target, ordinary, 540, 540, 1)) { }
                    catch (java.io.IOException expected) { rejected = true; }
                    check(rejected, "ordinary mode rejects this over-budget source");
                    int qualityWidth = 0;
                    for (boolean quality : new boolean[]{true, false}) {
                        WidgetConfig selected = new WidgetConfig(saved.uri, saved.scale, saved.radiusDp, saved.opacity,
                            saved.background, saved.click, true, true, 30, true, quality);
                        try (GifFrames.Sequence sequence = GifFrames.render(target, selected, 540, 540, 1)) {
                            long bytes = 0;
                            java.util.HashSet<Integer> images = new java.util.HashSet<>();
                            for (Bitmap bitmap : sequence.frames) {
                                bytes += bitmap.getAllocationByteCount();
                                int hash = 1;
                                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
                                    hash = 31 * hash + bitmap.getPixel(x * bitmap.getWidth() / 16, y * bitmap.getHeight() / 16);
                                images.add(hash);
                            }
                            check(images.size() > 1 && bytes <= GifFrames.pixelBudget(target) * 4,
                                "real animation changes pixels within desktop memory budget");
                            int width = sequence.frames.get(0).getWidth();
                            if (quality) qualityWidth = width;
                            else check(qualityWidth > width, "real GIF retains more detail with quality priority");
                            Bundle note = new Bundle();
                            note.putString("stream", "\nReal GIF " + id + (quality ? " quality" : " rate") + ": " +
                                width + "x" + sequence.frames.get(0).getHeight() + ", frames=" + sequence.frames.size() +
                                ", interval=" + sequence.intervalMs + "ms, bytes=" + bytes + "\n");
                            sendStatus(0, note);
                        }
                    }
                });
            }
        }
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
