package com.halil.ayetvakti;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

import java.io.IOException;
import java.util.Calendar;
import java.util.Random;

/** Sıradaki ayeti seçer, kilit ekranına uygular ve saat başı alarmını kurar. */
final class Engine {
    private Engine() {}

    static final String ACTION_TICK = "com.halil.ayetvakti.TICK";
    private static int[] order;

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("ayet", Context.MODE_PRIVATE); }
    static boolean isActive(Context c) { return prefs(c).getBoolean("active", false); }
    static int theme(Context c) { return prefs(c).getInt("theme", 0); }

    /** Ayetler karışık bir sırayla döner; her saat bir sonrakine geçilir, liste bitmeden tekrar etmez. */
    private static synchronized int[] order() {
        if (order == null) {
            int n = AyetData.ALL.length;
            order = new int[n];
            for (int i = 0; i < n; i++) order[i] = i;
            Random r = new Random(1453);
            for (int i = n - 1; i > 0; i--) {
                int j = r.nextInt(i + 1);
                int tmp = order[i]; order[i] = order[j]; order[j] = tmp;
            }
        }
        return order;
    }

    static AyetData.Ayet current(Context c) {
        long hour = System.currentTimeMillis() / 3_600_000L;
        int n = AyetData.ALL.length;
        long pos = Math.floorMod(hour + prefs(c).getInt("offset", 0), (long) n);
        return AyetData.ALL[order()[(int) pos]];
    }

    static Point screenSize(Context c) {
        DisplayManager dm = (DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
        Display d = dm.getDisplay(Display.DEFAULT_DISPLAY);
        DisplayMetrics m = new DisplayMetrics();
        d.getRealMetrics(m);
        int w = Math.min(m.widthPixels, m.heightPixels);
        int h = Math.max(m.widthPixels, m.heightPixels);
        return new Point(w, h);
    }

    static void applyNow(Context c) throws IOException {
        Point s = screenSize(c);
        Bitmap b = Renderer.render(current(c), theme(c), s.x, s.y);
        try {
            WallpaperManager.getInstance(c).setBitmap(b, null, true, WallpaperManager.FLAG_LOCK);
        } finally {
            b.recycle();
        }
    }

    static long nextHour() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, 1);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 2);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static PendingIntent tickIntent(Context c) {
        Intent i = new Intent(c, TickReceiver.class).setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void scheduleNext(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = tickIntent(c);
        long at = nextHour();
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }

    static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(tickIntent(c));
    }
}
