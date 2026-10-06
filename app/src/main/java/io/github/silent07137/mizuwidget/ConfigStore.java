// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.IOException;
import java.util.Map;
import org.json.JSONException;

final class ConfigStore {
    private final Context context;
    private final SharedPreferences prefs;
    ConfigStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = context.getSharedPreferences("widget_configs_v1", Context.MODE_PRIVATE);
    }
    synchronized WidgetConfig get(int id) { return read("widget." + id); }
    private WidgetConfig read(String key) {
        String value = prefs.getString(key, null);
        if (value == null) return null;
        try { return WidgetConfig.fromJson(value); }
        catch (JSONException | ClassCastException e) { return null; }
    }
    synchronized void save(int id, WidgetConfig config) throws IOException {
        WidgetConfig previous = get(id);
        write("widget." + id, config);
        if (previous != null && !previous.uri.equals(config.uri)) releaseIfUnused(previous.uri);
    }
    private void write(String key, WidgetConfig config) throws IOException {
        try {
            if (!prefs.edit().putString(key, config.toJson().toString()).commit())
                throw new IOException("配置保存失败");
        } catch (JSONException e) { throw new IOException("配置保存失败", e); }
    }
    synchronized void delete(int id) {
        WidgetConfig previous = get(id);
        if (prefs.edit().remove("widget." + id).commit() && previous != null) releaseIfUnused(previous.uri);
        WidgetEngine.cacheFile(context, id).delete();
    }
    synchronized void putDraft(String token, WidgetConfig config) throws IOException {
        write("draft." + token, config);
        if (!prefs.edit().putLong("time." + token, System.currentTimeMillis()).commit())
            throw new IOException("草稿保存失败");
    }
    synchronized WidgetConfig draft(String token) { return read("draft." + token); }
    synchronized void removeDraft(String token) {
        WidgetConfig config = draft(token);
        if (prefs.edit().remove("draft." + token).remove("time." + token).commit() && config != null)
            releaseIfUnused(config.uri);
    }
    synchronized void pruneDrafts() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("draft.")) {
                String token = entry.getKey().substring(6);
                long time = prefs.getLong("time." + token, 0);
                if (now - time > 86400000L || time > now) removeDraft(token);
            }
        }
    }
    synchronized void releaseIfUnused(String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith("widget.") && !entry.getKey().startsWith("draft.")) continue;
            WidgetConfig config = read(entry.getKey());
            if (config != null && raw.equals(config.uri)) return;
        }
        try { context.getContentResolver().releasePersistableUriPermission(Uri.parse(raw), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException | IllegalArgumentException ignored) { /* Already revoked by the document provider. */ }
    }
    synchronized void restore(int[] oldIds, int[] newIds) throws IOException {
        int count = Math.min(oldIds.length, newIds.length);
        WidgetConfig[] configs = new WidgetConfig[count];
        for (int i = 0; i < count; i++) configs[i] = get(oldIds[i]);
        SharedPreferences.Editor edit = prefs.edit();
        try {
            for (int i = 0; i < count; i++) {
                boolean destination = false;
                for (int j = 0; j < count; j++) if (oldIds[i] == newIds[j]) destination = true;
                if (!destination) edit.remove("widget." + oldIds[i]);
            }
            for (int i = 0; i < count; i++) if (configs[i] != null)
                edit.putString("widget." + newIds[i], configs[i].toJson().toString());
        } catch (JSONException e) { throw new IOException("恢复配置失败", e); }
        if (!edit.commit()) throw new IOException("恢复配置失败");
        for (int i = 0; i < count; i++) WidgetEngine.cacheFile(context, oldIds[i]).delete();
    }
}
