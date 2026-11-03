package com.gunlukseri.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Hatırlatma verisini saklar ve her alışkanlık için günlük alarmı kurar. */
final class Reminders {
    static final String CHANNEL = "hatirlatma";
    static final String ACTION_FIRE = "com.gunlukseri.app.FIRE";
    static final String ACTION_DONE = "com.gunlukseri.app.DONE";

    private Reminders() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("gunluk", Context.MODE_PRIVATE);
    }

    static String todayKey() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
            c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** JS'ten gelen tam liste: [{id,name,time:"HH:MM"}], bugünün tarihi ve bugün yapılanlar. */
    static void sync(Context c, String remindersJson, String date, String doneIds) {
        prefs(c).edit()
            .putString("rem", remindersJson == null ? "[]" : remindersJson)
            .putString("doneDate", date)
            .putString("doneIds", doneIds == null ? "" : doneIds)
            .apply();
        scheduleAll(c);
    }

    static boolean isDoneToday(Context c, String id) {
        SharedPreferences p = prefs(c);
        if (!todayKey().equals(p.getString("doneDate", ""))) return false;
        for (String s : p.getString("doneIds", "").split(",")) if (s.equals(id)) return true;
        return false;
    }

    static void markDoneNative(Context c, String id) {
        SharedPreferences p = prefs(c);
        String today = todayKey();
        String ids = today.equals(p.getString("doneDate", "")) ? p.getString("doneIds", "") : "";
        ids = ids.isEmpty() ? id : ids + "," + id;
        String pending = p.getString("pendingDone", "");
        pending = pending.isEmpty() ? today + "|" + id : pending + "," + today + "|" + id;
        p.edit().putString("doneDate", today).putString("doneIds", ids).putString("pendingDone", pending).apply();
    }

    /** Bildirimden "Yaptım" denenleri JS'e verir ve listeyi temizler. */
    static String takePending(Context c) {
        SharedPreferences p = prefs(c);
        String s = p.getString("pendingDone", "");
        p.edit().putString("pendingDone", "").apply();
        return s;
    }

    static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences p = prefs(c);
        Set<String> now = new HashSet<>();
        try {
            JSONArray arr = new JSONArray(p.getString("rem", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String id = o.optString("id"), name = o.optString("name"), time = o.optString("time");
                if (id.isEmpty() || !time.matches("\\d{1,2}:\\d{2}")) continue;
                now.add(id);
                schedule(c, am, id, name, time);
            }
        } catch (Exception ignored) { }
        for (String old : p.getStringSet("scheduled", new HashSet<>())) {
            if (!now.contains(old)) am.cancel(pi(c, old, "", ""));
        }
        p.edit().putStringSet("scheduled", now).apply();
    }

    static void schedule(Context c, AlarmManager am, String id, String name, String time) {
        String[] hm = time.split(":");
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
        t.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 1000) t.add(Calendar.DAY_OF_MONTH, 1);
        PendingIntent pi = pi(c, id, name, time);
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
        else am.setWindow(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), 5 * 60 * 1000, pi);
    }

    static PendingIntent pi(Context c, String id, String name, String time) {
        Intent i = new Intent(c, ReminderReceiver.class).setAction(ACTION_FIRE)
            .putExtra("id", id).putExtra("name", name).putExtra("time", time);
        return PendingIntent.getBroadcast(c, id.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void ensureChannel(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Hatırlatmalar", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Alışkanlıkların için günlük hatırlatmalar");
            nm.createNotificationChannel(ch);
        }
    }
}
