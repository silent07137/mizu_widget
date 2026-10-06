// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import org.json.JSONException;
import org.json.JSONObject;

public final class WidgetConfig {
    public enum Scale { FIT, CROP, STRETCH }
    public enum Click { CONFIGURE, VIEW, NONE }
    public final String uri;
    public final Scale scale;
    public final float radiusDp;
    public final float opacity;
    public final int background;
    public final Click click;
    public final boolean gif;
    public final boolean gifPlayback;
    public final int gifFps;

    public WidgetConfig(String uri, Scale scale, float radiusDp, float opacity,
                        int background, Click click, boolean gif) {
        this(uri, scale, radiusDp, opacity, background, click, gif, false, 2);
    }

    public WidgetConfig(String uri, Scale scale, float radiusDp, float opacity,
                        int background, Click click, boolean gif, boolean gifPlayback, int gifFps) {
        this.uri = uri == null ? "" : uri;
        this.scale = scale == null ? Scale.FIT : scale;
        this.radiusDp = Float.isFinite(radiusDp) ? Math.max(0, Math.min(64, radiusDp)) : 20;
        this.opacity = Float.isFinite(opacity) ? Math.max(0, Math.min(1, opacity)) : 1;
        this.background = background;
        this.click = click == null ? Click.CONFIGURE : click;
        this.gif = gif;
        this.gifPlayback = gif && gifPlayback;
        this.gifFps = gifFps == 1 || gifFps == 5 || gifFps == 15 || gifFps == 30 ? gifFps : 2;
    }

    public static WidgetConfig defaults() {
        return new WidgetConfig("", Scale.FIT, 20, 1, 0, Click.CONFIGURE, false);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject().put("uri", uri).put("scale", scale.name())
            .put("radius", radiusDp).put("opacity", opacity).put("background", background)
            .put("click", click.name()).put("gif", gif)
            .put("gifPlayback", gifPlayback).put("gifFps", gifFps);
    }

    public static WidgetConfig fromJson(String raw) throws JSONException {
        JSONObject json = new JSONObject(raw);
        Scale scale;
        Click click;
        try { scale = Scale.valueOf(json.optString("scale", "FIT")); }
        catch (IllegalArgumentException e) { scale = Scale.FIT; }
        try { click = Click.valueOf(json.optString("click", "CONFIGURE")); }
        catch (IllegalArgumentException e) { click = Click.CONFIGURE; }
        return new WidgetConfig(json.optString("uri"), scale,
            (float) json.optDouble("radius", 20), (float) json.optDouble("opacity", 1),
            json.optInt("background", 0), click, json.optBoolean("gif", false),
            json.optBoolean("gifPlayback", false), json.optInt("gifFps", 2));
    }
}
