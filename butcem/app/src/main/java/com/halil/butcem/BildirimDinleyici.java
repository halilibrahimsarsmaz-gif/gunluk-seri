package com.halil.butcem;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Locale;

/** Garanti BBVA ve Bonus Flaş bildirimlerini okuyup Bütçem'e iletir. */
public class BildirimDinleyici extends NotificationListenerService {

    static boolean bankaPaketi(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase(Locale.ROOT);
        return p.contains("garanti") || p.contains("bonus") || p.contains("bbva");
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            if (sbn == null || !bankaPaketi(sbn.getPackageName())) return;
            Notification n = sbn.getNotification();
            if (n == null || n.extras == null) return;
            Bundle x = n.extras;
            CharSequence t = x.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence big = x.getCharSequence(Notification.EXTRA_BIG_TEXT);
            CharSequence tx = big != null ? big : x.getCharSequence(Notification.EXTRA_TEXT);
            String baslik = t == null ? "" : t.toString();
            String metin = tx == null ? "" : tx.toString();
            if (baslik.isEmpty() && metin.isEmpty()) return;
            String id = sbn.getPackageName() + "|" + sbn.getPostTime() + "|" + (baslik + metin).hashCode();
            OtoKuyruk.ekle(this, id, sbn.getPackageName(), baslik, metin, sbn.getPostTime());
        } catch (Exception ignored) { }
    }
}
