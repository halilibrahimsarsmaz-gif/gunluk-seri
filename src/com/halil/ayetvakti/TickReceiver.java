package com.halil.ayetvakti;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Saat başı alarmı, telefon açılışı ve saat değişikliklerinde ayeti yeniler. */
public class TickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Context c = context.getApplicationContext();
        if (!Engine.isActive(c)) return;
        final PendingResult result = goAsync();
        new Thread(() -> {
            try {
                Engine.applyNow(c);
            } catch (Throwable ignored) {
            } finally {
                try { Engine.scheduleNext(c); } catch (Throwable ignored) {}
                result.finish();
            }
        }).start();
    }
}
