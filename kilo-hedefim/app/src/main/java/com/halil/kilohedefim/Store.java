package com.halil.kilohedefim;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Tüm veriler telefonda (SharedPreferences) saklanır. */
public class Store {

    public static class Profile {
        public boolean male;
        public int age;
        public double height;       // cm
        public double startWeight;  // kg
        public double targetWeight; // kg
        public String startDate;    // yyyy-MM-dd
        public String endDate;      // yyyy-MM-dd
        public double activity;     // 1.2 .. 1.9
        public int activityIndex;

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("male", male);
            o.put("age", age);
            o.put("height", height);
            o.put("startWeight", startWeight);
            o.put("targetWeight", targetWeight);
            o.put("startDate", startDate);
            o.put("endDate", endDate);
            o.put("activity", activity);
            o.put("activityIndex", activityIndex);
            return o;
        }

        static Profile fromJson(JSONObject o) {
            Profile p = new Profile();
            p.male = o.optBoolean("male", false);
            p.age = o.optInt("age", 30);
            p.height = o.optDouble("height", 170);
            p.startWeight = o.optDouble("startWeight", 80);
            p.targetWeight = o.optDouble("targetWeight", 70);
            p.startDate = o.optString("startDate", today());
            p.endDate = o.optString("endDate", addDays(today(), 90));
            p.activity = o.optDouble("activity", 1.375);
            p.activityIndex = o.optInt("activityIndex", 1);
            return p;
        }
    }

    private final SharedPreferences p;

    public Store(Context c) {
        p = c.getSharedPreferences("kilo_hedefim", Context.MODE_PRIVATE);
    }

    // ---------- tarih yardımcıları ----------
    public static String today() { return fmt(new Date()); }

    public static String fmt(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    public static Date parse(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s);
        } catch (Exception e) {
            return new Date();
        }
    }

    static Calendar cal(String s) {
        Calendar c = Calendar.getInstance();
        c.setTime(parse(s));
        c.set(Calendar.HOUR_OF_DAY, 12);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    /** b - a, gün olarak */
    public static long daysBetween(String a, String b) {
        return Math.round((cal(b).getTimeInMillis() - cal(a).getTimeInMillis()) / 86400000.0);
    }

    public static String addDays(String s, int n) {
        Calendar c = cal(s);
        c.add(Calendar.DAY_OF_MONTH, n);
        return fmt(c.getTime());
    }

    public static String pretty(String s) {
        return new SimpleDateFormat("d MMMM yyyy", new Locale("tr")).format(parse(s));
    }

    public static String prettyShort(String s) {
        return new SimpleDateFormat("d MMM EEE", new Locale("tr")).format(parse(s));
    }

    // ---------- profil ----------
    public boolean hasProfile() { return p.contains("profile"); }

    public Profile profile() {
        try {
            return Profile.fromJson(new JSONObject(p.getString("profile", "{}")));
        } catch (Exception e) {
            return Profile.fromJson(new JSONObject());
        }
    }

    public void saveProfile(Profile pr) {
        try {
            p.edit().putString("profile", pr.toJson().toString()).apply();
        } catch (Exception ignored) { }
    }

    // ---------- tartı ----------
    public TreeMap<String, Double> weights() {
        TreeMap<String, Double> m = new TreeMap<>();
        try {
            JSONObject o = new JSONObject(p.getString("weights", "{}"));
            Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                m.put(k, o.getDouble(k));
            }
        } catch (Exception ignored) { }
        return m;
    }

    private void saveWeights(TreeMap<String, Double> m) {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, Double> e : m.entrySet()) o.put(e.getKey(), e.getValue());
            p.edit().putString("weights", o.toString()).apply();
        } catch (Exception ignored) { }
    }

    public void setWeight(String date, double kg) {
        TreeMap<String, Double> m = weights();
        m.put(date, kg);
        saveWeights(m);
    }

    public void removeWeight(String date) {
        TreeMap<String, Double> m = weights();
        m.remove(date);
        saveWeights(m);
    }

    public Double weightOn(String date) { return weights().get(date); }

    /** verilen tarihten önceki son tartı */
    public Double prevWeight(String date) {
        Map.Entry<String, Double> e = weights().lowerEntry(date);
        return e == null ? null : e.getValue();
    }

    public double currentWeight() {
        TreeMap<String, Double> m = weights();
        if (!m.isEmpty()) return m.lastEntry().getValue();
        return profile().startWeight;
    }

    // ---------- yemek günlüğü ----------
    public JSONArray log(String date) {
        try {
            return new JSONArray(p.getString("log_" + date, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void addFood(String date, String name, double kcal) {
        try {
            JSONArray a = log(date);
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("kcal", kcal);
            a.put(o);
            p.edit().putString("log_" + date, a.toString()).apply();
        } catch (Exception ignored) { }
    }

    public void removeFood(String date, int index) {
        JSONArray a = log(date);
        JSONArray b = new JSONArray();
        for (int i = 0; i < a.length(); i++) if (i != index) b.put(a.opt(i));
        p.edit().putString("log_" + date, b.toString()).apply();
    }

    public double eaten(String date) {
        JSONArray a = log(date);
        double sum = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) sum += o.optDouble("kcal", 0);
        }
        return sum;
    }

    // ---------- kendi eklediğin yemekler ----------
    public JSONArray customFoods() {
        try {
            return new JSONArray(p.getString("custom_foods", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void addCustomFood(String name, String portion, int kcal) {
        try {
            JSONArray a = customFoods();
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("portion", portion);
            o.put("kcal", kcal);
            a.put(o);
            p.edit().putString("custom_foods", a.toString()).apply();
        } catch (Exception ignored) { }
    }

    // ---------- su ----------
    public int water(String date) { return p.getInt("water_" + date, 0); }

    public void setWater(String date, int n) { p.edit().putInt("water_" + date, Math.max(0, n)).apply(); }

    // ---------- genel ayarlar ----------
    public int getInt(String k, int d) { return p.getInt(k, d); }

    public void putInt(String k, int v) { p.edit().putInt(k, v).apply(); }

    public boolean getBool(String k, boolean d) { return p.getBoolean(k, d); }

    public void putBool(String k, boolean v) { p.edit().putBoolean(k, v).apply(); }

    public String getStr(String k, String d) { return p.getString(k, d); }

    public void putStr(String k, String v) { p.edit().putString(k, v).apply(); }

    public void clearAll() { p.edit().clear().apply(); }
}
