package com.halil.kilohedefim;

/** Kalori ve plan hesapları (Mifflin-St Jeor formülü). */
public class Calc {

    public static final double KCAL_PER_KG = 7700.0;

    public static final String[] ACTIVITY_NAMES = {
            "Hareketsiz (masa başı, spor yok)",
            "Az hareketli (haftada 1-3 gün hafif spor)",
            "Orta (haftada 3-5 gün spor)",
            "Çok hareketli (haftada 6-7 gün spor)",
            "Aşırı (ağır iş / günde 2 antrenman)"
    };
    public static final double[] ACTIVITY_FACTORS = {1.2, 1.375, 1.55, 1.725, 1.9};

    public static double bmr(Store.Profile p, double w) {
        return 10 * w + 6.25 * p.height - 5 * p.age + (p.male ? 5 : -161);
    }

    public static double tdee(Store.Profile p, double w) {
        return bmr(p, w) * p.activity;
    }

    /** Güvenli alt sınır: kadın 1200, erkek 1500 kcal */
    public static int minCal(Store.Profile p) {
        return p.male ? 1500 : 1200;
    }

    public static double bmi(double w, double heightCm) {
        double m = heightCm / 100.0;
        return w / (m * m);
    }

    public static double minHealthyWeight(double heightCm) {
        double m = heightCm / 100.0;
        return 18.5 * m * m;
    }

    /** Güvenli haftalık kayıp: kilonun %1'i, en fazla 1 kg */
    public static double safeWeeklyLoss(double w) {
        return Math.min(1.0, w * 0.01);
    }

    public static class Plan {
        public int target;        // günlük kalori hedefi
        public int tdee;          // günlük harcama
        public int deficit;       // günlük açık
        public double weeklyLoss; // beklenen haftalık kayıp (kg)
        public double toLose;     // kalan kilo
        public int daysLeft;      // hedef tarihe kalan gün
        public boolean limited;   // hedef süre güvenli sınırın üstünde mi
        public boolean reached;   // hedefe ulaşıldı mı
        public String realisticEnd;
    }

    /** Plan her gün son tartıya göre yeniden hesaplanır. */
    public static Plan plan(Store.Profile p, double current) {
        Plan r = new Plan();
        double tdee = tdee(p, current);
        r.tdee = (int) Math.round(tdee);
        r.toLose = Math.max(0, current - p.targetWeight);
        r.daysLeft = (int) Math.max(1, Store.daysBetween(Store.today(), p.endDate));

        if (r.toLose < 0.1) {
            r.reached = true;
            r.target = r.tdee;
            r.deficit = 0;
            r.weeklyLoss = 0;
            r.realisticEnd = Store.today();
            return r;
        }

        double need = r.toLose * KCAL_PER_KG / r.daysLeft;
        double maxBySpeed = safeWeeklyLoss(current) * KCAL_PER_KG / 7.0;
        double maxByFloor = Math.max(0, tdee - minCal(p));
        double maxDef = Math.min(maxBySpeed, maxByFloor);

        double def = Math.min(need, maxDef);
        r.limited = need > maxDef + 5;
        r.deficit = (int) Math.round(def);
        r.target = (int) Math.round(tdee - def);
        r.weeklyLoss = def * 7 / KCAL_PER_KG;
        if (def > 1) {
            int days = (int) Math.ceil(r.toLose * KCAL_PER_KG / def);
            r.realisticEnd = Store.addDays(Store.today(), days);
        } else {
            r.realisticEnd = null;
        }
        return r;
    }

    /** Başlangıçtan hedef tarihe doğrusal planlanan kilo */
    public static double plannedOn(Store.Profile p, String date) {
        long total = Math.max(1, Store.daysBetween(p.startDate, p.endDate));
        long d = Store.daysBetween(p.startDate, date);
        if (d <= 0) return p.startWeight;
        if (d >= total) return p.targetWeight;
        return p.startWeight + (p.targetWeight - p.startWeight) * d / (double) total;
    }
}
