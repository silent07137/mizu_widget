// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class PinReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        String token = intent.getData() == null ? null : intent.getData().getLastPathSegment();
        if (token == null || !WidgetEngine.owns(context, id)) return;
        PendingResult pending = goAsync();
        WidgetEngine.IO.execute(() -> {
            try {
                ConfigStore store = new ConfigStore(context);
                WidgetConfig config = store.draft(token);
                if (config == null) return;
                store.save(id, config);
                store.removeDraft(token);
                WidgetEngine.update(context, id);
            } catch (java.io.IOException ignored) {
                // Keep the draft to allow recovery; the new widget remains tappable.
            } finally { pending.finish(); }
        });
    }
}
