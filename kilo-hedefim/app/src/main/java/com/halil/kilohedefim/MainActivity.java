package com.halil.kilohedefim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends Activity {

    static final int GREEN = 0xFF2E7D32, GREEN_L = 0xFFE8F5E9, BG = 0xFFF4F6F4, TXT = 0xFF1B1B1B,
            SUB = 0xFF666666, RED = 0xFFC62828, ORANGE = 0xFFE65100, ORANGE_L = 0xFFFFF3E0;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final Locale TR = new Locale("tr");

    Store s;
    FrameLayout content;
    LinearLayout nav;
    int tab = 0;
    int menuOffset = 0;
    final String[] TABS = {"🏠\nBugün", "🍽️\nYemekler", "📋\nProgram", "⚖️\nTartı"};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        s = new Store(this);
        Reminders.channel(this);
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(MATCH, 0, 1));
        nav = new LinearLayout(this);
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dp(8));
        root.addView(nav, new LinearLayout.LayoutParams(MATCH, dp(64)));
        setContentView(root);

        Reminders.scheduleAll(this);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (s.hasProfile()) {
            render();
            askDailyWeight();
        }
    }

    // ================= yardımcılar =================
    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    static String f1(double v) { return String.format(TR, "%.1f", v); }

    static double num(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim().replace(',', '.'));
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    TextView tv(String t, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT_BOLD);
        return v;
    }

    TextView h1(String t) {
        TextView v = tv(t, 24, TXT, true);
        v.setPadding(dp(4), dp(8), 0, dp(12));
        return v;
    }

    TextView h2(String t) {
        TextView v = tv(t, 17, TXT, true);
        v.setPadding(0, 0, 0, dp(8));
        return v;
    }

    TextView body(String t) {
        TextView v = tv(t, 15, SUB, false);
        v.setLineSpacing(0, 1.2f);
        v.setPadding(0, 0, 0, dp(6));
        return v;
    }

    LinearLayout card(LinearLayout parent) { return card(parent, Color.WHITE); }

    LinearLayout card(LinearLayout parent, int color) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(16));
        c.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.bottomMargin = dp(12);
        parent.addView(c, lp);
        return c;
    }

    Button button(String t, boolean primary) {
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(15);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        if (primary) {
            g.setColor(GREEN);
            b.setTextColor(Color.WHITE);
        } else {
            g.setColor(GREEN_L);
            b.setTextColor(GREEN);
        }
        b.setBackground(g);
        b.setStateListAnimator(null);
        return b;
    }

    void addButton(LinearLayout parent, Button b) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, dp(48));
        lp.topMargin = dp(8);
        parent.addView(b, lp);
    }

    EditText input(String hint, boolean decimal) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(decimal ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                : InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    TextView label(String t) {
        TextView v = tv(t, 13, SUB, true);
        v.setPadding(0, dp(10), 0, 0);
        return v;
    }

    LinearLayout row(String left, String right, int rightColor) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, dp(6), 0, dp(6));
        TextView a = tv(left, 15, TXT, false);
        r.addView(a, new LinearLayout.LayoutParams(0, WRAP, 1));
        TextView b = tv(right, 15, rightColor, true);
        r.addView(b, new LinearLayout.LayoutParams(WRAP, WRAP));
        return r;
    }

    LinearLayout page() {
        content.removeAllViews();
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(12), dp(16), dp(24));
        sv.addView(col);
        content.addView(sv);
        return col;
    }

    // ================= gezinme =================
    void render() {
        if (!s.hasProfile()) {
            nav.setVisibility(View.GONE);
            showSetup(false);
            return;
        }
        nav.setVisibility(View.VISIBLE);
        nav.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            TextView t = tv(TABS[i], 12, i == tab ? GREEN : SUB, i == tab);
            t.setGravity(Gravity.CENTER);
            t.setOnClickListener(v -> {
                tab = idx;
                render();
            });
            nav.addView(t, new LinearLayout.LayoutParams(0, MATCH, 1));
        }
        switch (tab) {
            case 0: showToday(); break;
            case 1: showFoods(); break;
            case 2: showProgram(); break;
            default: showWeights(); break;
        }
    }

    // ================= ilk kurulum =================
    void showSetup(boolean editing) {
        LinearLayout col = page();
        Store.Profile old = editing ? s.profile() : null;

        col.addView(h1(editing ? "Bilgilerini güncelle" : "Hoş geldin! 👋"));
        LinearLayout c = card(col);
        c.addView(body("Bilgilerini gir, günlük kalori ihtiyacını hesaplayıp sana bir program hazırlayayım."));

        c.addView(label("Cinsiyet"));
        RadioGroup sex = new RadioGroup(this);
        sex.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton m = new RadioButton(this);
        m.setText("Erkek");
        m.setId(View.generateViewId());
        RadioButton k = new RadioButton(this);
        k.setText("Kadın");
        k.setId(View.generateViewId());
        sex.addView(m);
        sex.addView(k);
        c.addView(sex);
        if (old != null) {
            if (old.male) m.setChecked(true); else k.setChecked(true);
        }

        c.addView(label("Yaş"));
        EditText age = input("örn. 32", false);
        c.addView(age);
        c.addView(label("Boy (cm)"));
        EditText height = input("örn. 175", true);
        c.addView(height);
        c.addView(label("Şu anki kilon (kg)"));
        EditText cur = input("örn. 92,5", true);
        c.addView(cur);
        c.addView(label("Hedef kilon (kg)"));
        EditText target = input("örn. 80", true);
        c.addView(target);
        c.addView(label("Kaç haftada vermek istiyorsun?"));
        EditText weeks = input("örn. 16", false);
        c.addView(weeks);
        c.addView(label("Günlük hareket düzeyin"));
        Spinner act = new Spinner(this);
        act.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Calc.ACTIVITY_NAMES));
        c.addView(act);

        if (old != null) {
            age.setText(String.valueOf(old.age));
            height.setText(f1(old.height));
            cur.setText(f1(s.currentWeight()));
            target.setText(f1(old.targetWeight));
            long w = Math.max(1, Math.round(Store.daysBetween(Store.today(), old.endDate) / 7.0));
            weeks.setText(String.valueOf(w));
            act.setSelection(old.activityIndex);
        }

        Button save = button(editing ? "Kaydet ve programı yenile" : "Programımı oluştur", true);
        addButton(c, save);
        if (editing) {
            Button cancel = button("Vazgeç", false);
            addButton(c, cancel);
            cancel.setOnClickListener(v -> render());
        }

        save.setOnClickListener(v -> {
            if (sex.getCheckedRadioButtonId() == -1) { toast("Cinsiyet seç"); return; }
            double a = num(age), h = num(height), cw = num(cur), tw = num(target), wk = num(weeks);
            if (Double.isNaN(a) || Double.isNaN(h) || Double.isNaN(cw) || Double.isNaN(tw) || Double.isNaN(wk)) {
                toast("Lütfen tüm alanları doldur");
                return;
            }
            if (a < 18) {
                new AlertDialog.Builder(this).setTitle("Yaş sınırı")
                        .setMessage("Bu uygulamadaki hesaplar yetişkinler içindir. 18 yaşından küçüksen kilo planını bir doktor ya da diyetisyenle yapmalısın.")
                        .setPositiveButton("Tamam", null).show();
                return;
            }
            if (a > 100 || h < 120 || h > 230 || cw < 35 || cw > 300 || wk < 1 || wk > 104) {
                toast("Girdiğin değerleri kontrol et");
                return;
            }
            if (tw >= cw) { toast("Hedef kilo, şu anki kilondan az olmalı"); return; }
            double minW = Calc.minHealthyWeight(h);
            if (tw < minW) {
                new AlertDialog.Builder(this).setTitle("Hedef çok düşük")
                        .setMessage("Boyun için sağlıklı en düşük kilo yaklaşık " + f1(Math.ceil(minW)) +
                                " kg (vücut kitle indeksi 18,5). Hedefini bu değerin üstünde belirle.")
                        .setPositiveButton("Tamam", null).show();
                return;
            }

            final Store.Profile p = new Store.Profile();
            p.male = sex.getCheckedRadioButtonId() == m.getId();
            p.age = (int) a;
            p.height = h;
            p.startWeight = cw;
            p.targetWeight = tw;
            p.startDate = Store.today();
            p.activityIndex = act.getSelectedItemPosition();
            p.activity = Calc.ACTIVITY_FACTORS[p.activityIndex];

            double perWeek = (cw - tw) / wk;
            double safe = Calc.safeWeeklyLoss(cw);
            if (perWeek > safe + 0.01) {
                final int safeWeeks = (int) Math.ceil((cw - tw) / safe);
                final int userWeeks = (int) wk;
                new AlertDialog.Builder(this).setTitle("Bu hız sağlıklı değil")
                        .setMessage("Haftada " + f1(perWeek) + " kg vermen gerekiyor. Güvenli hız haftada en fazla " +
                                f1(safe) + " kg. Bu yüzden önerilen süre " + safeWeeks + " hafta.\n\n" +
                                "Kendi süreni seçersen de kalori hedefin güvenli sınırın altına inmez; sadece hedef tarih kayabilir.")
                        .setPositiveButton(safeWeeks + " hafta kullan", (d, x) -> finishSetup(p, cw, safeWeeks))
                        .setNegativeButton("Benim sürem kalsın", (d, x) -> finishSetup(p, cw, userWeeks))
                        .show();
            } else {
                finishSetup(p, cw, (int) wk);
            }
        });
    }

    void finishSetup(Store.Profile p, double cw, int weeks) {
        p.endDate = Store.addDays(Store.today(), weeks * 7);
        s.saveProfile(p);
        s.setWeight(Store.today(), cw);
        Reminders.scheduleAll(this);
        tab = 2;
        render();
        toast("Programın hazır! 🎉");
    }

    // ================= günlük tartı isteği =================
    void askDailyWeight() {
        String t = Store.today();
        if (s.weightOn(t) != null) return;
        if (t.equals(s.getStr("asked_weight", ""))) return;
        s.putStr("asked_weight", t);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(body("Bugün henüz tartılmadın. Tartıl ve sonucu gir — programın buna göre güncellenecek."));
        EditText e = input("kg (örn. 84,3)", true);
        box.addView(e);
        new AlertDialog.Builder(this).setTitle("⚖️ Günlük tartı")
                .setView(box)
                .setPositiveButton("Kaydet", (d, x) -> saveWeight(e))
                .setNegativeButton("Sonra", null)
                .show();
    }

    void saveWeight(EditText e) {
        double w = num(e);
        if (Double.isNaN(w) || w < 30 || w > 300) { toast("Geçerli bir kilo gir"); return; }
        s.setWeight(Store.today(), w);
        toast("Kaydedildi: " + f1(w) + " kg");
        render();
    }

    LinearLayout weightInputRow() {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        EditText e = input("kg (örn. 84,3)", true);
        r.addView(e, new LinearLayout.LayoutParams(0, WRAP, 1));
        Button b = button("Kaydet", true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(110), dp(48));
        lp.leftMargin = dp(8);
        r.addView(b, lp);
        b.setOnClickListener(v -> saveWeight(e));
        return r;
    }

    // ================= BUGÜN =================
    void showToday() {
        LinearLayout col = page();
        Store.Profile p = s.profile();
        String t = Store.today();
        Calc.Plan pl = Calc.plan(p, s.currentWeight());

        col.addView(h1("Bugün · " + Store.prettyShort(t)));

        // tartı kartı
        Double w = s.weightOn(t);
        if (w == null) {
            LinearLayout c = card(col, ORANGE_L);
            c.addView(h2("⚖️ Günlük tartı"));
            c.addView(body("Bugün henüz tartılmadın. Sabah tuvaletten sonra, aç karnına tartılıp sonucu gir."));
            c.addView(weightInputRow());
        } else {
            LinearLayout c = card(col);
            Double prev = s.prevWeight(t);
            String diff = "";
            int color = TXT;
            if (prev != null) {
                double dd = w - prev;
                diff = (dd > 0 ? "  (+" : "  (") + f1(dd) + " kg)";
                color = dd <= 0 ? GREEN : ORANGE;
            }
            c.addView(row("⚖️ Bugünkü tartı", f1(w) + " kg" + diff, color));
        }

        // kalori kartı
        int eaten = (int) Math.round(s.eaten(t));
        int left = pl.target - eaten;
        LinearLayout c2 = card(col);
        c2.addView(h2("🔥 Kalori"));
        TextView big = tv(left >= 0 ? left + " kcal kaldı" : (-left) + " kcal aştın", 28, left >= 0 ? GREEN : RED, true);
        c2.addView(big);
        c2.addView(body("Hedef " + pl.target + " kcal · Yenen " + eaten + " kcal"));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, pl.target));
        bar.setProgress(Math.min(eaten, pl.target));
        bar.setProgressTintList(ColorStateList.valueOf(left >= 0 ? GREEN : RED));
        c2.addView(bar, new LinearLayout.LayoutParams(MATCH, dp(12)));

        // yenenler
        LinearLayout c3 = card(col);
        c3.addView(h2("🍽️ Bugün yediklerin"));
        JSONArray log = s.log(t);
        if (log.length() == 0) c3.addView(body("Henüz bir şey eklemedin."));
        for (int i = 0; i < log.length(); i++) {
            JSONObject o = log.optJSONObject(i);
            if (o == null) continue;
            final int idx = i;
            LinearLayout r = row(o.optString("name"), Math.round(o.optDouble("kcal")) + " kcal", TXT);
            r.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setMessage("\"" + o.optString("name") + "\" silinsin mi?")
                        .setPositiveButton("Sil", (d, x) -> { s.removeFood(t, idx); render(); })
                        .setNegativeButton("Vazgeç", null).show();
                return true;
            });
            c3.addView(r);
        }
        if (log.length() > 0) c3.addView(tv("Silmek için üzerine basılı tut", 12, SUB, false));
        Button add = button("+ Yemek ekle", true);
        addButton(c3, add);
        add.setOnClickListener(v -> { tab = 1; render(); });

        // su
        LinearLayout c4 = card(col);
        c4.addView(h2("💧 Su"));
        int water = s.water(t);
        StringBuilder glasses = new StringBuilder();
        for (int i = 0; i < 8; i++) glasses.append(i < water ? "🥛" : "▫️");
        c4.addView(tv(glasses.toString(), 22, TXT, false));
        c4.addView(body(water + " / 8 bardak (yaklaşık 2 litre)"));
        LinearLayout wr = new LinearLayout(this);
        Button minus = button("−", false), plus = button("+ 1 bardak", true);
        wr.addView(minus, new LinearLayout.LayoutParams(dp(64), dp(48)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
        lp.leftMargin = dp(8);
        wr.addView(plus, lp);
        c4.addView(wr);
        minus.setOnClickListener(v -> { s.setWater(t, water - 1); render(); });
        plus.setOnClickListener(v -> { s.setWater(t, water + 1); render(); });
    }

    // ================= YEMEKLER =================
    void showFoods() {
        LinearLayout col = page();
        String t = Store.today();
        Calc.Plan pl = Calc.plan(s.profile(), s.currentWeight());
        int eaten = (int) Math.round(s.eaten(t));

        col.addView(h1("Yemek ekle"));
        LinearLayout top = card(col, GREEN_L);
        top.addView(row("Bugün", eaten + " / " + pl.target + " kcal", eaten <= pl.target ? GREEN : RED));

        EditText search = new EditText(this);
        search.setHint("🔍 Yemek ara (örn. mercimek)");
        search.setSingleLine(true);
        search.setBackground(null);
        LinearLayout sc = card(col);
        sc.setPadding(dp(12), dp(4), dp(12), dp(4));
        sc.addView(search);

        Button custom = button("+ Listede olmayan yemek ekle", false);
        LinearLayout.LayoutParams lpc = new LinearLayout.LayoutParams(MATCH, dp(48));
        lpc.bottomMargin = dp(12);
        col.addView(custom, lpc);
        custom.setOnClickListener(v -> customFoodDialog());

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        col.addView(list);

        final List<FoodDb.Food> foods = allFoods();
        fillFoods(list, foods, "");
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence a, int b, int c, int d) { }
            public void onTextChanged(CharSequence a, int b, int c, int d) { }
            public void afterTextChanged(Editable e) { fillFoods(list, foods, e.toString()); }
        });
        col.addView(tv("Kaloriler ortalama değerlerdir; pişirme şekline ve porsiyona göre değişebilir.", 12, SUB, false));
    }

    List<FoodDb.Food> allFoods() {
        List<FoodDb.Food> l = new ArrayList<>();
        JSONArray cf = s.customFoods();
        for (int i = 0; i < cf.length(); i++) {
            JSONObject o = cf.optJSONObject(i);
            if (o != null) l.add(new FoodDb.Food("Benim yemeklerim", o.optString("name"), o.optString("portion"), o.optInt("kcal")));
        }
        for (FoodDb.Food f : FoodDb.ALL) l.add(f);
        return l;
    }

    void fillFoods(LinearLayout list, List<FoodDb.Food> foods, String q) {
        list.removeAllViews();
        String query = q.trim().toLowerCase(TR);
        String lastCat = null;
        LinearLayout c = null;
        int shown = 0;
        for (FoodDb.Food f : foods) {
            if (!query.isEmpty() && !f.name.toLowerCase(TR).contains(query)) continue;
            if (!f.cat.equals(lastCat)) {
                c = card(list);
                c.addView(h2(f.cat));
                lastCat = f.cat;
            }
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.VERTICAL);
            r.setPadding(0, dp(8), 0, dp(8));
            LinearLayout top = row(f.name, f.kcal + " kcal", GREEN);
            top.setPadding(0, 0, 0, 0);
            r.addView(top);
            r.addView(tv(f.portion, 13, SUB, false));
            r.setOnClickListener(v -> addFoodDialog(f));
            c.addView(r);
            shown++;
        }
        if (shown == 0) {
            LinearLayout e = card(list);
            e.addView(body("Bulunamadı. \"Listede olmayan yemek ekle\" ile kendin ekleyebilirsin."));
        }
    }

    void addFoodDialog(FoodDb.Food f) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(body("1 porsiyon = " + f.portion + " = " + f.kcal + " kcal\nKaç porsiyon yedin?"));
        EditText e = input("porsiyon", true);
        e.setText("1");
        e.selectAll();
        box.addView(e);
        new AlertDialog.Builder(this).setTitle(f.name)
                .setView(box)
                .setPositiveButton("Ekle", (d, x) -> {
                    double n = num(e);
                    if (Double.isNaN(n) || n <= 0 || n > 20) { toast("Geçerli bir porsiyon gir"); return; }
                    String name = n == 1 ? f.name : f.name + " ×" + (n == Math.floor(n) ? String.valueOf((int) n) : f1(n));
                    s.addFood(Store.today(), name, f.kcal * n);
                    toast(name + " eklendi");
                    render();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    void customFoodDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        EditText name = new EditText(this);
        name.setHint("Yemek adı");
        name.setSingleLine(true);
        box.addView(name);
        EditText portion = new EditText(this);
        portion.setHint("Porsiyon (örn. 1 kase)");
        portion.setSingleLine(true);
        box.addView(portion);
        EditText kcal = input("Kalori (kcal)", false);
        box.addView(kcal);
        new AlertDialog.Builder(this).setTitle("Yeni yemek")
                .setView(box)
                .setPositiveButton("Kaydet", (d, x) -> {
                    String n = name.getText().toString().trim();
                    double k = num(kcal);
                    if (n.isEmpty() || Double.isNaN(k) || k < 0 || k > 5000) { toast("Ad ve kaloriyi doğru gir"); return; }
                    String pr = portion.getText().toString().trim();
                    s.addCustomFood(n, pr.isEmpty() ? "1 porsiyon" : pr, (int) k);
                    toast(n + " listene eklendi");
                    render();
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    // ================= PROGRAM =================
    void showProgram() {
        LinearLayout col = page();
        Store.Profile p = s.profile();
        double cur = s.currentWeight();
        Calc.Plan pl = Calc.plan(p, cur);

        col.addView(h1("Programın"));

        LinearLayout c = card(col);
        if (pl.reached) {
            c.addView(h2("🎉 Hedefine ulaştın!"));
            c.addView(body("Kilonu korumak için günde yaklaşık " + pl.tdee + " kcal alabilirsin. Yeni bir hedef koymak istersen bilgilerini güncelle."));
        } else {
            c.addView(h2("🎯 Günlük kalori hedefin"));
            c.addView(tv(pl.target + " kcal", 32, GREEN, true));
            c.addView(row("Günlük harcaman", pl.tdee + " kcal", TXT));
            c.addView(row("Günlük açık", pl.deficit + " kcal", TXT));
            c.addView(row("Beklenen kayıp", "haftada " + f1(pl.weeklyLoss) + " kg", TXT));
            c.addView(row("Şimdi / hedef", f1(cur) + " → " + f1(p.targetWeight) + " kg", TXT));
            c.addView(row("Hedef tarih", Store.pretty(p.endDate), TXT));
            c.addView(row("Kalan süre", pl.daysLeft + " gün", TXT));
        }
        if (pl.limited && !pl.reached) {
            LinearLayout wc = card(col, ORANGE_L);
            wc.addView(h2("⚠️ Süre kısa kalıyor"));
            String m = "Hedef tarihe yetişmek için güvenli sınırın altına inmek gerekirdi. Kalori hedefin güvenli düzeyde tutuldu.";
            if (pl.realisticEnd != null) m += "\nBu tempoyla hedefine yaklaşık " + Store.pretty(pl.realisticEnd) + " tarihinde ulaşırsın.";
            wc.addView(body(m));
        }

        // makrolar
        if (!pl.reached) {
            LinearLayout mc = card(col);
            mc.addView(h2("🥩 Besin dağılımı (günlük)"));
            int protein = (int) Math.round(Math.min(cur, p.targetWeight + 10) * 1.6);
            int fat = (int) Math.round(pl.target * 0.30 / 9);
            int carb = (int) Math.max(50, Math.round((pl.target - protein * 4 - fat * 9) / 4.0));
            mc.addView(row("Protein", protein + " g", TXT));
            mc.addView(row("Yağ", fat + " g", TXT));
            mc.addView(row("Karbonhidrat", carb + " g", TXT));
            mc.addView(body("Protein tok tutar ve kas kaybını azaltır: her öğünde yumurta, tavuk, balık, yoğurt, baklagil olsun."));

            LinearLayout oc = card(col);
            oc.addView(h2("⏰ Öğünlere dağılım"));
            oc.addView(row("Kahvaltı", Math.round(pl.target * 0.25) + " kcal", TXT));
            oc.addView(row("Öğle", Math.round(pl.target * 0.35) + " kcal", TXT));
            oc.addView(row("Ara öğün", Math.round(pl.target * 0.10) + " kcal", TXT));
            oc.addView(row("Akşam", Math.round(pl.target * 0.30) + " kcal", TXT));
        }

        // örnek menü
        LinearLayout menu = card(col);
        int day = (int) ((Store.daysBetween(p.startDate, Store.today()) + menuOffset) % FoodDb.MENUS.length);
        if (day < 0) day += FoodDb.MENUS.length;
        String[][] mm = FoodDb.MENUS[day];
        menu.addView(h2("📝 Örnek günlük menü (" + (day + 1) + "/" + FoodDb.MENUS.length + ")"));
        int total = 0;
        for (String[] meal : mm) {
            menu.addView(tv(meal[0] + " · ~" + meal[2] + " kcal", 15, TXT, true));
            menu.addView(body(meal[1]));
            total += Integer.parseInt(meal[2]);
        }
        String note = "Toplam ~" + total + " kcal.";
        int gap = pl.target - total;
        if (gap > 150) note += " Hedefine " + gap + " kcal daha var: porsiyonları biraz büyütebilir ya da bir ara öğün (ör. 1 muz + 10 badem) ekleyebilirsin.";
        else if (gap < -150) note += " Hedefinin " + (-gap) + " kcal üstünde: ekmek ve pilav porsiyonlarını azalt.";
        else note += " Hedefine çok yakın 👍";
        menu.addView(tv(note, 14, GREEN, false));
        Button other = button("Başka menü göster", false);
        addButton(menu, other);
        other.setOnClickListener(v -> { menuOffset++; render(); });

        // haftalık hedefler
        if (!pl.reached && pl.weeklyLoss > 0) {
            LinearLayout wc = card(col);
            wc.addView(h2("📅 Haftalık hedeflerin"));
            double w = cur;
            String d = Store.today();
            for (int i = 1; i <= 52 && w > p.targetWeight + 0.05; i++) {
                w = Math.max(p.targetWeight, w - pl.weeklyLoss);
                d = Store.addDays(d, 7);
                wc.addView(row(i + ". hafta · " + Store.prettyShort(d), f1(w) + " kg", TXT));
            }
        }

        // ipuçları
        LinearLayout tc = card(col);
        tc.addView(h2("💡 Günlük alışkanlıklar"));
        tc.addView(body("• Günde en az 30-45 dakika tempolu yürüyüş\n" +
                "• Günde 8 bardak su; yemekten önce 1 bardak\n" +
                "• Şekerli içecek ve paketli atıştırmalıkları azalt\n" +
                "• Akşam yemeğini yatmadan 2-3 saat önce bitir\n" +
                "• 7-8 saat uyu; uykusuzluk iştahı artırır\n" +
                "• Her sabah aynı saatte, aç karnına tartıl\n" +
                "• Tartıdaki günlük iniş-çıkışlar normaldir; haftalık eğilime bak"));

        // hatırlatmalar
        LinearLayout rc = card(col);
        rc.addView(h2("🔔 Hatırlatmalar"));
        rc.addView(tv("Saati değiştirmek için saate dokun", 12, SUB, false));
        for (int i = 0; i < Reminders.IDS.length; i++) {
            final int id = Reminders.IDS[i];
            LinearLayout r = new LinearLayout(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(0, dp(6), 0, dp(6));
            r.addView(tv(Reminders.NAMES[i], 15, TXT, false), new LinearLayout.LayoutParams(0, WRAP, 1));
            final TextView time = tv(String.format(Locale.US, "%02d:%02d", Reminders.hour(s, id), Reminders.minute(s, id)), 15, GREEN, true);
            time.setPadding(dp(12), dp(8), dp(12), dp(8));
            r.addView(time);
            Switch sw = new Switch(this);
            sw.setChecked(Reminders.isOn(s, id));
            r.addView(sw);
            sw.setOnCheckedChangeListener((b, on) -> {
                s.putBool("rem_on_" + id, on);
                if (on) Reminders.scheduleOne(this, id); else Reminders.cancel(this, id);
            });
            time.setOnClickListener(v -> new TimePickerDialog(this, (tp, h, m) -> {
                s.putInt("rem_h_" + id, h);
                s.putInt("rem_m_" + id, m);
                if (Reminders.isOn(s, id)) Reminders.scheduleOne(this, id);
                render();
            }, Reminders.hour(s, id), Reminders.minute(s, id), true).show());
            rc.addView(r);
        }

        LinearLayout ac = card(col);
        Button edit = button("✏️ Bilgilerimi / hedefimi güncelle", false);
        addButton(ac, edit);
        edit.setOnClickListener(v -> showSetup(true));
        Button reset = button("Tüm verileri sil", false);
        reset.setTextColor(RED);
        addButton(ac, reset);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Emin misin?")
                .setMessage("Profilin, tartıların ve yemek kayıtların silinecek.")
                .setPositiveButton("Sil", (d, x) -> {
                    s.clearAll();
                    Reminders.scheduleAll(this);
                    tab = 0;
                    render();
                })
                .setNegativeButton("Vazgeç", null).show());

        col.addView(tv("Bu uygulama genel bilgilendirme amaçlıdır. Bir sağlık sorunun varsa programına başlamadan önce doktoruna danış.", 12, SUB, false));
    }

    // ================= TARTI =================
    void showWeights() {
        LinearLayout col = page();
        Store.Profile p = s.profile();
        TreeMap<String, Double> w = s.weights();
        double cur = s.currentWeight();
        String t = Store.today();

        col.addView(h1("Tartı"));

        LinearLayout ic = card(col);
        Double today = s.weightOn(t);
        ic.addView(h2(today == null ? "Bugünkü tartını gir" : "Bugün: " + f1(today) + " kg (değiştir)"));
        ic.addView(weightInputRow());

        LinearLayout sc = card(col);
        sc.addView(h2("📊 Durum"));
        double lost = p.startWeight - cur;
        double remain = Math.max(0, cur - p.targetWeight);
        sc.addView(row("Başlangıç (" + Store.prettyShort(p.startDate) + ")", f1(p.startWeight) + " kg", TXT));
        sc.addView(row("Şu an", f1(cur) + " kg", TXT));
        sc.addView(row("Verilen", (lost >= 0 ? "−" : "+") + f1(Math.abs(lost)) + " kg", lost >= 0 ? GREEN : ORANGE));
        sc.addView(row("Hedefe kalan", f1(remain) + " kg", TXT));
        sc.addView(row("Vücut kitle indeksi", f1(Calc.bmi(cur, p.height)), TXT));
        double total = p.startWeight - p.targetWeight;
        if (total > 0) {
            int pct = (int) Math.max(0, Math.min(100, Math.round(lost / total * 100)));
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(pct);
            bar.setProgressTintList(ColorStateList.valueOf(GREEN));
            sc.addView(bar, new LinearLayout.LayoutParams(MATCH, dp(12)));
            sc.addView(body("Hedefin %" + pct + "'i tamamlandı"));
        }
        double planned = Calc.plannedOn(p, t);
        double diff = cur - planned;
        String status;
        int sColor;
        if (diff <= 0.2) { status = "Plana göre " + (diff < -0.2 ? f1(-diff) + " kg öndesin 🚀" : "tam yolundasın 👍"); sColor = GREEN; }
        else { status = "Plana göre " + f1(diff) + " kg gerisindesin; kalori hedefin buna göre ayarlandı."; sColor = ORANGE; }
        sc.addView(tv(status, 14, sColor, true));

        LinearLayout gc = card(col);
        gc.addView(h2("📈 Grafik"));
        gc.addView(tv("Yeşil: tartıların · Gri kesikli: plan", 12, SUB, false));
        gc.addView(new ChartView(this, p, w), new LinearLayout.LayoutParams(MATCH, dp(220)));

        LinearLayout hc = card(col);
        hc.addView(h2("🗓️ Geçmiş"));
        if (w.isEmpty()) hc.addView(body("Henüz tartı kaydı yok."));
        List<Map.Entry<String, Double>> entries = new ArrayList<>(w.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            final String date = entries.get(i).getKey();
            double val = entries.get(i).getValue();
            String right = f1(val) + " kg";
            int color = TXT;
            if (i > 0) {
                double dd = val - entries.get(i - 1).getValue();
                right += (dd > 0 ? "  +" : "  ") + f1(dd);
                color = dd <= 0 ? GREEN : ORANGE;
            }
            LinearLayout r = row(Store.prettyShort(date), right, color);
            r.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setMessage(Store.prettyShort(date) + " kaydı silinsin mi?")
                        .setPositiveButton("Sil", (d, x) -> { s.removeWeight(date); render(); })
                        .setNegativeButton("Vazgeç", null).show();
                return true;
            });
            hc.addView(r);
        }
        if (!w.isEmpty()) hc.addView(tv("Silmek için üzerine basılı tut", 12, SUB, false));
    }

    void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
