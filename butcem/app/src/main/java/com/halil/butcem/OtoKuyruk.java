package com.halil.butcem;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Bildirimlerden yakalanan ham kayıtları geçici olarak saklar; uygulama açılınca işlenir. */
final class OtoKuyruk {
    private static final String PREF = "butcem_oto";
    private static final Object LOCK = new Object();

    static void ekle(Context c, String id, String pkg, String baslik, String metin, long zaman) {
        synchronized (LOCK) {
            try {
                SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                JSONArray q = new JSONArray(sp.getString("q", "[]"));
                for (int i = 0; i < q.length(); i++) if (id.equals(q.getJSONObject(i).optString("id"))) return;
                JSONObject o = new JSONObject();
                o.put("id", id); o.put("pkg", pkg); o.put("title", baslik); o.put("text", metin); o.put("ts", zaman);
                q.put(o);
                while (q.length() > 200) q.remove(0);
                sp.edit().putString("q", q.toString()).apply();
            } catch (Exception ignored) { }
        }
    }

    /** Bekleyenleri döndürür ve kuyruğu boşaltır. */
    static String al(Context c) {
        synchronized (LOCK) {
            SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String q = sp.getString("q", "[]");
            if (!"[]".equals(q)) sp.edit().putString("q", "[]").commit();
            return q;
        }
    }
}
