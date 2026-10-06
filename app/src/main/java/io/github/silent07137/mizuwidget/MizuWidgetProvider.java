// SPDX-License-Identifier: GPL-2.0-only
package io.github.silent07137.mizuwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

public final class MizuWidgetProvider extends AppWidgetProvider {
    @Override public void onReceive(Context context, android.content.Intent intent) {
        // The platform default dispatches both onRestored and onUpdate for this action.
        // Handle it once so a single asynchronous result covers restore + render.
        if (AppWidgetManager.ACTION_APPWIDGET_RESTORED.equals(intent.getAction())) {
            int[] oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_OLD_IDS);
            int[] newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (oldIds != null && newIds != null) onRestored(context, oldIds, newIds);
        } else super.onReceive(context, intent);
    }
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        run(() -> { for (int id : ids) WidgetEngine.update(context, id); });
    }
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        run(() -> WidgetEngine.update(context, id));
    }
    @Override public void onDeleted(Context context, int[] ids) {
        run(() -> { ConfigStore store = new ConfigStore(context); for (int id : ids) store.delete(id); });
    }
    @Override public void onRestored(Context context, int[] oldIds, int[] newIds) {
        run(() -> {
            try {
                new ConfigStore(context).restore(oldIds, newIds);
                for (int id : newIds) WidgetEngine.update(context, id);
            } catch (java.io.IOException ignored) { /* Keep source records if a write fails. */ }
        });
    }
    private void run(Runnable work) {
        PendingResult pending = goAsync();
        WidgetEngine.IO.execute(() -> { try { work.run(); } finally { if (pending != null) pending.finish(); } });
    }
}
