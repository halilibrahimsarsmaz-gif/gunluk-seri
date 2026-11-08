package com.halil.ayetvakti;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Saat başı alarmı, vakit alarmları, telefon açılışı ve saat değişikliklerinde çalışır. */
public class TickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context c = context.getApplicationContext();
        if (!Engine.isActive(c)) return;
        final String action = intent.getAction();
        final PendingResult result = goAsync();
        new Thread(() -> {
            try {
                if (Engine.ACTION_VAKIT.equals(action)) {
                    int kind = intent.getIntExtra("kind", 0);
                    long at = intent.getLongExtra("at", 0);
                    // geç gelen (ör. telefon kapalıyken kaçan) bildirimleri gösterme
                    if (System.currentTimeMillis() - at < 10 * 60_000L) {
                        Engine.postVakitNotice(c, kind, intent.getIntExtra("idx", 0));
                    }
                    Engine.applyNow(c);
                } else {
                    Engine.refreshData(c, false);
                    if (Engine.ACTION_TICK.equals(action) && !Engine.onScreen(c)) Engine.advanceAndApply(c);
                    else Engine.applyNow(c);
                }
            } catch (Throwable ignored) {
            } finally {
                try { Engine.scheduleNext(c); } catch (Throwable ignored) {}
                try { Engine.scheduleVakit(c); } catch (Throwable ignored) {}
                Engine.syncService(c);
                result.finish();
            }
        }).start();
    }
}
