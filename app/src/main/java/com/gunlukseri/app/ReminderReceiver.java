package com.gunlukseri.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Alarm çalınca bildirim gösterir; "Yaptım" düğmesini ve telefon yeniden başlayınca alarmları kurmayı da yönetir. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);

        if (Reminders.ACTION_FIRE.equals(action)) {
            String id = intent.getStringExtra("id"), name = intent.getStringExtra("name"), time = intent.getStringExtra("time");
            if (id == null) return;
            // yarın için yeniden kur
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (time != null) Reminders.schedule(c, am, id, name, time);
            if (Reminders.isDoneToday(c, id)) return;

            Reminders.ensureChannel(c);
            Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent openPi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Intent done = new Intent(c, ReminderReceiver.class).setAction(Reminders.ACTION_DONE).putExtra("id", id);
            PendingIntent donePi = PendingIntent.getBroadcast(c, id.hashCode() + 1, done, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification n = new Notification.Builder(c, Reminders.CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(name)
                .setContentText("Bugün için hatırlatma")
                .setContentIntent(openPi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .addAction(new Notification.Action.Builder(null, "Yaptım", donePi).build())
                .build();
            try { nm.notify(id.hashCode(), n); } catch (SecurityException ignored) { }

        } else if (Reminders.ACTION_DONE.equals(action)) {
            String id = intent.getStringExtra("id");
            if (id == null) return;
            Reminders.markDoneNative(c, id);
            nm.cancel(id.hashCode());

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
                || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            Reminders.scheduleAll(c);
        }
    }
}
