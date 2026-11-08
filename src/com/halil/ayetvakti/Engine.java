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

    static boolean onScreen(Context c) { return prefs(c).getBoolean("onScreen", true); }

    static final int[] KEEP_OPTIONS = {0, 20, 30, 45, 60};
    /** Kilit ekranı açıldığında ekranın kaç saniye açık kalacağı (0 = telefonun kendi ayarı). */
    static int keepSeconds(Context c) { return prefs(c).getInt("keepSec", 30); }

    /** Sıradaki ayetin konumu; her değişimde bir ilerler, liste bitmeden aynı ayet gelmez. */
    static AyetData.Ayet current(Context c) {
        int n = AyetData.ALL.length;
        int pos = Math.floorMod(prefs(c).getInt("pos", 0), n);
        return AyetData.ALL[order()[pos]];
    }

    static synchronized void advance(Context c) {
        SharedPreferences p = prefs(c);
        p.edit().putInt("pos", p.getInt("pos", 0) + 1).commit();
    }

    /** Bir sonraki ayete geç ve kilit ekranına yaz. */
    static void advanceAndApply(Context c) throws IOException {
        advance(c);
        applyNow(c);
    }

    /** Ekran kapanınca ayet değiştiren arka plan servisini açar ya da kapatır. */
    static void syncService(Context c) {
        Intent i = new Intent(c, ScreenService.class);
        if (isActive(c)) {
            try { c.startForegroundService(i); } catch (Throwable ignored) {}
        } else {
            c.stopService(i);
        }
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
        Bitmap b = Renderer.render(current(c), theme(c), s.x, s.y, Data.now(c));
        try {
            WallpaperManager.getInstance(c).setBitmap(b, null, true, WallpaperManager.FLAG_LOCK);
            prefs(c).edit().putLong("rendered", System.currentTimeMillis()).apply();
        } finally {
            b.recycle();
        }
    }

    static long renderedAt(Context c) { return prefs(c).getLong("rendered", 0); }

    /** Vakit ve hava durumu verisini gerekiyorsa internetten yeniler. Yeni veri geldiyse true. */
    static boolean refreshData(Context c, boolean eager) {
        boolean a = false, b = false;
        try { a = Data.refreshPrayer(c, false); } catch (Throwable ignored) {}
        try { b = Data.refreshWeather(c, eager ? 15 * 60_000L : 50 * 60_000L); } catch (Throwable ignored) {}
        return a || b;
    }

    // ---------------- Vakit alarmları ve bildirimleri ----------------

    static final String ACTION_VAKIT = "com.halil.ayetvakti.VAKIT";
    static final int WARN_MIN = 15;
    static boolean notify(Context c) { return prefs(c).getBoolean("notify", true); }

    /** Sıradaki vakit olayı: [zaman, tür, vakit no]. tür 0 = vakit girdi, 1 = çıkmasına az kaldı. */
    static long[] nextVakitEvent(Context c) {
        long now = System.currentTimeMillis() + 5_000L;
        long[] best = null;
        Calendar day = Calendar.getInstance();
        for (int k = 0; k < 2; k++) {
            Data.Day d = Data.day(c, day);
            for (int i = 0; i < 6; i++) {
                long t = Data.at(day, d.min[i]);
                if (t > now && (best == null || t < best[0])) best = new long[]{t, 0, i};
                // çıkış uyarısı: güneş (sabah çıkar), ikindi (öğle çıkar), akşam (ikindi çıkar), yatsı (akşam çıkar)
                if (i == 1 || i == 3 || i == 4 || i == 5) {
                    long wt = t - WARN_MIN * 60_000L;
                    if (wt > now && (best == null || wt < best[0])) best = new long[]{wt, 1, i};
                }
            }
            day.add(Calendar.DAY_OF_YEAR, 1);
        }
        return best;
    }

    private static PendingIntent vakitIntent(Context c, long[] ev) {
        Intent i = new Intent(c, TickReceiver.class).setAction(ACTION_VAKIT);
        if (ev != null) { i.putExtra("kind", (int) ev[1]); i.putExtra("idx", (int) ev[2]); i.putExtra("at", ev[0]); }
        return PendingIntent.getBroadcast(c, 8, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void scheduleVakit(Context c) {
        long[] ev = nextVakitEvent(c);
        if (ev == null) return;
        setExact(c, ev[0], vakitIntent(c, ev));
    }

    private static void setExact(Context c, long at, PendingIntent pi) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
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

    static void postVakitNotice(Context c, int kind, int idx) {
        if (!notify(c)) return;
        String[] enter = {
                "İmsak vakti girdi", "Güneş doğdu", "Öğle vakti girdi",
                "İkindi vakti girdi", "Akşam vakti girdi", "Yatsı vakti girdi"};
        String[] enterSub = {
                "Sabah namazı vakti başladı · Yatsı vakti çıktı", "Sabah namazı vakti çıktı",
                "Öğle namazı vakti başladı", "Öğle vakti çıktı", "İkindi vakti çıktı", "Akşam vakti çıktı"};
        String[] warn = {"", "Sabah namazı vaktinin", "", "Öğle vaktinin", "İkindi vaktinin", "Akşam vaktinin"};
        Data.Day d = Data.day(c, Calendar.getInstance());
        String title, text;
        if (kind == 0) {
            title = enter[idx] + " · " + Data.clockMin(d.min[idx]);
            text = enterSub[idx];
        } else {
            title = warn[idx] + " çıkmasına " + WARN_MIN + " dk kaldı";
            text = "Çıkış: " + Data.clockMin(d.min[idx]);
        }
        android.app.NotificationManager nm = (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.NotificationChannel ch = new android.app.NotificationChannel(
                "vakit", "Vakit bildirimleri", android.app.NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Vakit girdiğinde ve çıkmasına az kaldığında haber verir");
        nm.createNotificationChannel(ch);
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        int icon = c.getResources().getIdentifier("ic_stat", "drawable", c.getPackageName());
        android.app.Notification n = new android.app.Notification.Builder(c, "vakit")
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setTimeoutAfter(kind == 0 ? 60 * 60_000L : WARN_MIN * 60_000L)
                .build();
        try { nm.notify(100 + kind, n); } catch (Throwable ignored) {}
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
        am.cancel(vakitIntent(c, null));
    }
}
