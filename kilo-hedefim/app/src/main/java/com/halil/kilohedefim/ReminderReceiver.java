package com.halil.kilohedefim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        int id = intent.getIntExtra("id", 1);
        Store s = new Store(c);
        if (!s.hasProfile()) return;

        String t = Store.today();
        Store.Profile p = s.profile();
        Calc.Plan plan = Calc.plan(p, s.currentWeight());
        int eaten = (int) Math.round(s.eaten(t));
        int left = plan.target - eaten;

        switch (id) {
            case 1:
                if (s.weightOn(t) == null) {
                    Reminders.show(c, id, "⚖️ Tartılma zamanı",
                            "Tuvaletten sonra, aç karnına tartıl ve sonucu uygulamaya gir.");
                }
                break;
            case 2:
                Reminders.show(c, id, "🍽️ Öğle yemeği",
                        "Öğle yemeğini kaydetmeyi unutma. Bugün kalan hakkın: " + left + " kcal");
                break;
            case 5:
                Reminders.show(c, id, "💧 Su molası",
                        "Bugün " + s.water(t) + "/8 bardak su içtin. Bir bardak daha iç!");
                break;
            case 3:
                Reminders.show(c, id, "🍲 Akşam yemeği",
                        "Akşam yemeğini gir. Kalan hakkın: " + left + " kcal");
                break;
            case 4:
                String msg = "Bugün " + eaten + " kcal aldın, hedefin " + plan.target + " kcal. ";
                if (eaten == 0) msg = "Bugün hiç yemek girmedin. Yediklerini eklemek ister misin?";
                else if (left >= 0) msg += "Harika, hedefin içindesin! 👏";
                else msg += "Hedefi " + (-left) + " kcal aştın. Yarın yeni bir gün 💪";
                Reminders.show(c, id, "📊 Gün sonu özeti", msg);
                break;
        }
        // yarın için tekrar kur
        if (Reminders.isOn(s, id)) Reminders.scheduleOne(c, id);
    }
}
