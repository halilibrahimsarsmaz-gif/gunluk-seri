package com.halil.kilohedefim;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Günlük hatırlatmalar. */
public class Reminders {

    public static final String CHANNEL = "hatirlatmalar";

    // id, ad, varsayılan saat, varsayılan dakika
    public static final int[] IDS = {1, 2, 5, 3, 4};
    public static final String[] NAMES = {
            "Sabah tartılma",
            "Öğle yemeğini gir",
            "Su iç",
            "Akşam yemeğini gir",
            "Gün sonu özeti"
    };
    public static final int[] DEF_H = {8, 13, 16, 19, 21};
    public static final int[] DEF_M = {0, 0, 0, 30, 30};

    static int idx(int id) {
        for (int i = 0; i < IDS.length; i++) if (IDS[i] == id) return i;
        return 0;
    }

    public static boolean isOn(Store s, int id) { return s.getBool("rem_on_" + id, true); }

    public static int hour(Store s, int id) { return s.getInt("rem_h_" + id, DEF_H[idx(id)]); }

    public static int minute(Store s, int id) { return s.getInt("rem_m_" + id, DEF_M[idx(id)]); }

    public static void channel(Context c) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Tartı, öğün ve su hatırlatmaları");
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    public static void scheduleAll(Context c) {
        Store s = new Store(c);
        for (int id : IDS) {
            if (s.hasProfile() && isOn(s, id)) scheduleOne(c, id);
            else cancel(c, id);
        }
    }

    public static void scheduleOne(Context c, int id) {
        Store s = new Store(c);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour(s, id));
        cal.set(Calendar.MINUTE, minute(s, id));
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis() + 30_000) cal.add(Calendar.DAY_OF_MONTH, 1);
        AlarmManager am = c.getSystemService(AlarmManager.class);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending(c, id));
    }

    public static void cancel(Context c, int id) {
        c.getSystemService(AlarmManager.class).cancel(pending(c, id));
    }

    static PendingIntent pending(Context c, int id) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void show(Context c, int id, String title, String text) {
        channel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 100 + id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(0xFF2E7D32)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(id, n);
        } catch (SecurityException ignored) { }
    }
}
