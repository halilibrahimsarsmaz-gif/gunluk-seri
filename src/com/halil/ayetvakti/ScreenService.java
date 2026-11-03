package com.halil.ayetvakti;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.app.KeyguardManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ekran her kapandığında bir sonraki ayeti kilit ekranına yazar;
 * böylece ekranı her açışınızda yeni bir ayet karşılar.
 */
public class ScreenService extends Service {
    private static final String CHANNEL = "ayet_servis";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private long lastRun = 0;
    private PowerManager.WakeLock keepOn;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) { holdScreen(); return; }
            if (Intent.ACTION_USER_PRESENT.equals(action)) { releaseScreen(); return; }
            if (!Intent.ACTION_SCREEN_OFF.equals(action)) return;
            releaseScreen();
            if (!Engine.onScreen(context)) return;
            long now = SystemClock.elapsedRealtime();
            if (now - lastRun < 1500 || !busy.compareAndSet(false, true)) return;
            lastRun = now;
            final Context app = getApplicationContext();
            worker.execute(() -> {
                try {
                    if (Engine.isActive(app)) Engine.advanceAndApply(app);
                } catch (Throwable ignored) {
                } finally {
                    busy.set(false);
                }
            });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Ayet yenileme", NotificationManager.IMPORTANCE_MIN);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);

        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        int icon = getResources().getIdentifier("ic_stat", "drawable", getPackageName());
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(icon)
                .setContentTitle("Ayet Vakti")
                .setContentText("Ekranı her açışınızda yeni bir ayet")
                .setContentIntent(open)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, n);
        }
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(screenReceiver, f);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Engine.isActive(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try { unregisterReceiver(screenReceiver); } catch (Throwable ignored) {}
        releaseScreen();
        worker.shutdown();
        super.onDestroy();
    }

    /** Kilit ekranı açıkken ayeti okuyabilmeniz için ekranı bir süre açık tutar. */
    @SuppressWarnings("deprecation")
    private void holdScreen() {
        int sec = Engine.keepSeconds(this);
        if (sec <= 0) return;
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km == null || !km.isKeyguardLocked()) return;
        releaseScreen();
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            keepOn = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "AyetVakti:okuma");
            keepOn.setReferenceCounted(false);
            keepOn.acquire(sec * 1000L);
        } catch (Throwable ignored) {
            keepOn = null;
        }
    }

    private void releaseScreen() {
        try { if (keepOn != null && keepOn.isHeld()) keepOn.release(); } catch (Throwable ignored) {}
        keepOn = null;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
