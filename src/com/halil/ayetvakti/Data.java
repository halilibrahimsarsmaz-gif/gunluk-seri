package com.halil.ayetvakti;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Bursa namaz vakitleri (Diyanet verisi, 30 günlük önbellek) ve hava durumu (Open-Meteo).
 * İnternet yoksa vakitler yaklaşık olarak telefonda hesaplanır.
 */
final class Data {
    private Data() {}

    static final String CITY = "Bursa";
    private static final String DIYANET_URL = "https://ezanvakti.emushaf.net/vakitler/9335"; // Bursa merkez
    private static final double LAT = 40.1826, LNG = 29.0665;
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=40.1826&longitude=29.0665"
            + "&current=temperature_2m,weather_code,is_day"
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
            + "&timezone=Europe%2FIstanbul&forecast_days=1";

    static final String[] NAMES = {"İmsak", "Güneş", "Öğle", "İkindi", "Akşam", "Yatsı"};

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("veri", Context.MODE_PRIVATE); }

    // ---------------- Namaz vakitleri ----------------

    static final class Day {
        final int[] min = new int[6]; // gece yarısından itibaren dakika
        String hijri = "";
        boolean approx;
    }

    private static String key(Calendar cal) {
        SimpleDateFormat f = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
        f.setTimeZone(cal.getTimeZone());
        return f.format(cal.getTime());
    }

    static Day day(Context c, Calendar cal) {
        String raw = prefs(c).getString("v_" + key(cal), null);
        if (raw != null) {
            try {
                String[] p = raw.split("\\|", -1);
                Day d = new Day();
                for (int i = 0; i < 6; i++) d.min[i] = hm(p[i]);
                d.hijri = p.length > 6 ? p[6] : "";
                return d;
            } catch (Exception ignored) {}
        }
        return calc(cal);
    }

    private static int hm(String s) {
        s = s.trim();
        return Integer.parseInt(s.substring(0, 2)) * 60 + Integer.parseInt(s.substring(3, 5));
    }

    /** Diyanet verisi eksikse indirir. true dönerse yeni veri geldi. */
    static boolean refreshPrayer(Context c, boolean force) {
        SharedPreferences p = prefs(c);
        Calendar in5 = Calendar.getInstance();
        in5.add(Calendar.DAY_OF_YEAR, 5);
        boolean have = p.contains("v_" + key(Calendar.getInstance())) && p.contains("v_" + key(in5));
        long last = p.getLong("v_try", 0);
        long now = System.currentTimeMillis();
        if (have && !force) return false;
        if (!force && now - last < 3 * 3600_000L) return false;
        if (force && now - last < 60_000L) return false;
        p.edit().putLong("v_try", now).apply();
        try {
            JSONArray arr = new JSONArray(get(DIYANET_URL));
            SharedPreferences.Editor e = p.edit();
            // eski günleri temizle
            Calendar old = Calendar.getInstance();
            old.add(Calendar.DAY_OF_YEAR, -3);
            for (String k : p.getAll().keySet()) {
                if (k.startsWith("v_") && k.length() == 12) {
                    try {
                        Calendar kc = Calendar.getInstance();
                        kc.setTime(new SimpleDateFormat("dd.MM.yyyy", Locale.US).parse(k.substring(2)));
                        if (kc.before(old)) e.remove(k);
                    } catch (Exception ignored) {}
                }
            }
            int n = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String date = o.getString("MiladiTarihKisa");
                String v = o.getString("Imsak") + "|" + o.getString("Gunes") + "|" + o.getString("Ogle") + "|"
                        + o.getString("Ikindi") + "|" + o.getString("Aksam") + "|" + o.getString("Yatsi") + "|"
                        + o.optString("HicriTarihUzun", "").replace("|", " ");
                for (int j = 0; j < 6; j++) hm(v.split("\\|")[j]); // biçim kontrolü
                e.putString("v_" + date, v);
                n++;
            }
            e.apply();
            return n > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    // ---------------- Hava durumu ----------------

    static final class Weather {
        int temp, max, min, code, rain = -1;
        boolean day = true;
        long time;
    }

    static Weather weather(Context c) {
        SharedPreferences p = prefs(c);
        long t = p.getLong("w_time", 0);
        if (t == 0 || System.currentTimeMillis() - t > 6 * 3600_000L) return null;
        Weather w = new Weather();
        w.time = t;
        w.temp = p.getInt("w_temp", 0);
        w.max = p.getInt("w_max", 0);
        w.min = p.getInt("w_min", 0);
        w.code = p.getInt("w_code", 0);
        w.rain = p.getInt("w_rain", -1);
        w.day = p.getBoolean("w_day", true);
        return w;
    }

    static boolean refreshWeather(Context c, long maxAgeMs) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        if (now - p.getLong("w_time", 0) < maxAgeMs) return false;
        if (now - p.getLong("w_try", 0) < 5 * 60_000L) return false;
        p.edit().putLong("w_try", now).apply();
        try {
            JSONObject o = new JSONObject(get(WEATHER_URL));
            JSONObject cur = o.getJSONObject("current");
            JSONObject d = o.getJSONObject("daily");
            SharedPreferences.Editor e = p.edit();
            e.putInt("w_temp", (int) Math.round(cur.getDouble("temperature_2m")));
            e.putInt("w_code", cur.getInt("weather_code"));
            e.putBoolean("w_day", cur.optInt("is_day", 1) == 1);
            e.putInt("w_max", (int) Math.round(d.getJSONArray("temperature_2m_max").getDouble(0)));
            e.putInt("w_min", (int) Math.round(d.getJSONArray("temperature_2m_min").getDouble(0)));
            JSONArray pr = d.optJSONArray("precipitation_probability_max");
            e.putInt("w_rain", pr != null && !pr.isNull(0) ? pr.getInt(0) : -1);
            e.putLong("w_time", now);
            e.apply();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    static String weatherText(int code) {
        switch (code) {
            case 0: return "Açık";
            case 1: return "Az bulutlu";
            case 2: return "Parçalı bulutlu";
            case 3: return "Kapalı";
            case 45: case 48: return "Sisli";
            case 51: case 53: case 55: return "Çisenti";
            case 56: case 57: return "Dondurucu çisenti";
            case 61: return "Hafif yağmur";
            case 63: return "Yağmurlu";
            case 65: return "Kuvvetli yağmur";
            case 66: case 67: return "Dondurucu yağmur";
            case 71: return "Hafif kar";
            case 73: return "Karlı";
            case 75: return "Yoğun kar";
            case 77: return "Kar taneleri";
            case 80: return "Hafif sağanak";
            case 81: return "Sağanak yağış";
            case 82: return "Kuvvetli sağanak";
            case 85: case 86: return "Kar sağanağı";
            case 95: return "Gök gürültülü";
            case 96: case 99: return "Dolu, fırtına";
            default: return "";
        }
    }

    /** 0 açık, 1 parçalı, 2 bulut, 3 sis, 4 yağmur, 5 kar, 6 fırtına */
    static int weatherKind(int code) {
        if (code <= 1) return 0;
        if (code == 2) return 1;
        if (code == 3) return 2;
        if (code == 45 || code == 48) return 3;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return 5;
        if (code >= 95) return 6;
        return 4;
    }

    // ---------------- Şu anki vakit ----------------

    static final class Now {
        int idx;          // vurgulanacak vakit sütunu (0..5)
        boolean prayer;   // true: bir namaz vaktinin içindeyiz; false: güneş doğdu, öğle bekleniyor
        long end;         // vaktin çıkış anı (ya da öğlenin girişi)
        Day today;
        Weather weather;
        long now;
    }

    static long at(Calendar base, int minutes) {
        Calendar c = (Calendar) base.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, minutes);
        return c.getTimeInMillis();
    }

    static Now now(Context c) {
        Now n = new Now();
        n.now = System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        Day d = day(c, today);
        n.today = d;
        n.weather = weather(c);
        long[] t = new long[6];
        for (int i = 0; i < 6; i++) t[i] = at(today, d.min[i]);
        if (n.now < t[0]) { n.idx = 5; n.prayer = true; n.end = t[0]; }
        else if (n.now < t[1]) { n.idx = 0; n.prayer = true; n.end = t[1]; }
        else if (n.now < t[2]) { n.idx = 1; n.prayer = false; n.end = t[2]; }
        else if (n.now < t[3]) { n.idx = 2; n.prayer = true; n.end = t[3]; }
        else if (n.now < t[4]) { n.idx = 3; n.prayer = true; n.end = t[4]; }
        else if (n.now < t[5]) { n.idx = 4; n.prayer = true; n.end = t[5]; }
        else { n.idx = 5; n.prayer = true; n.end = at(tomorrow, day(c, tomorrow).min[0]); }
        return n;
    }

    static String vakitLabel(int idx) {
        switch (idx) {
            case 0: return "Sabah namazı vakti";
            case 1: return "Sabah vakti çıktı";
            case 2: return "Öğle vakti";
            case 3: return "İkindi vakti";
            case 4: return "Akşam vakti";
            default: return "Yatsı vakti";
        }
    }

    static String clock(long ms) {
        return new SimpleDateFormat("HH:mm", Locale.US).format(ms);
    }

    static String clockMin(int m) { return String.format(Locale.US, "%02d:%02d", (m / 60) % 24, m % 60); }

    static String left(long ms) {
        long m = Math.max(0, (ms + 59_999) / 60_000);
        if (m >= 60) return (m / 60) + " sa " + (m % 60) + " dk";
        return m + " dk";
    }

    static String http(String url) throws Exception { return get(url); }

    private static String get(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setConnectTimeout(7000);
        con.setReadTimeout(9000);
        con.setRequestProperty("User-Agent", "AyetVakti/1.0 (Android)");
        con.setRequestProperty("Accept", "application/json");
        try {
            if (con.getResponseCode() != 200) throw new Exception("HTTP " + con.getResponseCode());
            InputStream in = con.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) out.write(buf, 0, r);
            return out.toString("UTF-8");
        } finally {
            con.disconnect();
        }
    }

    // ---------------- Çevrimdışı yaklaşık hesap ----------------
    // Diyanet açıları (imsak 18°, yatsı 17°, ikindi gölge 1 misli) ve Bursa için
    // 2026 Diyanet vakitlerine göre ayarlanmış temkin farkları; sapma genelde ±1 dk.

    private static final double[] TEMKIN = {-0.2, -6.9, 5.0, 4.5, 8.0, 0.9};

    static Day calc(Calendar cal) {
        Day d = new Day();
        d.approx = true;
        Calendar u = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        u.clear();
        u.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        double jd = u.getTimeInMillis() / 86400000.0 + 2440587.5 - LNG / 360.0;
        double tz = cal.getTimeZone().getOffset(at(cal, 12 * 60)) / 3600000.0;
        double[] T = {5 / 24.0, 6 / 24.0, 0.5, 13 / 24.0, 18 / 24.0, 18 / 24.0};
        double[] r = new double[6];
        for (int it = 0; it < 2; it++) {
            r[0] = ha(jd + T[0], 18, true);
            r[1] = ha(jd + T[1], 0.833, true);
            r[2] = noon(jd + T[2]);
            r[3] = asr(jd + T[3]);
            r[4] = ha(jd + T[4], 0.833, false);
            r[5] = ha(jd + T[5], 17, false);
            for (int i = 0; i < 6; i++) T[i] = r[i] / 24.0;
        }
        for (int i = 0; i < 6; i++) {
            double h = r[i] + tz - LNG / 15.0;
            d.min[i] = (int) Math.floor(h * 60 + TEMKIN[i] + 0.5);
        }
        return d;
    }

    private static double[] sun(double jd) {
        double D = jd - 2451545.0;
        double g = Math.toRadians(fix(357.529 + 0.98560028 * D, 360));
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = Math.toRadians(fix(q + 1.915 * Math.sin(g) + 0.020 * Math.sin(2 * g), 360));
        double e = Math.toRadians(23.439 - 0.00000036 * D);
        double ra = fix(Math.toDegrees(Math.atan2(Math.cos(e) * Math.sin(L), Math.cos(L))) / 15, 24);
        double decl = Math.toDegrees(Math.asin(Math.sin(e) * Math.sin(L)));
        double eqt = q / 15 - ra;
        eqt = fix(eqt + 12, 24) - 12;
        return new double[]{decl, eqt};
    }

    private static double noon(double jd) { return fix(12 - sun(jd)[1], 24); }

    private static double ha(double jd, double angle, boolean before) {
        double decl = Math.toRadians(sun(jd)[0]);
        double lat = Math.toRadians(LAT);
        double v = (-Math.sin(Math.toRadians(angle)) - Math.sin(decl) * Math.sin(lat)) / (Math.cos(decl) * Math.cos(lat));
        double h = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, v)))) / 15;
        return noon(jd) + (before ? -h : h);
    }

    private static double asr(double jd) {
        double decl = sun(jd)[0];
        double a = -Math.toDegrees(Math.atan(1 / (1 + Math.tan(Math.toRadians(Math.abs(LAT - decl))))));
        return ha(jd, a, false);
    }

    private static double fix(double a, double b) { a = a - b * Math.floor(a / b); return a < 0 ? a + b : a; }
}
