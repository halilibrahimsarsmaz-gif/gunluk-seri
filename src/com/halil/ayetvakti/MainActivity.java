package com.halil.ayetvakti;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private float dp;
    private ScrollView scroll;
    private ImageView preview;
    private TextView title, subtitle, status, mainBtn, nextBtn, modeBtn, keepBtn, batteryBtn, themeLabel, note;
    private LinearLayout chips;
    private int previewSeq = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        buildUi();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(px(22), px(36), px(22), px(28));
        scroll.addView(root, new FrameLayout.LayoutParams(-1, -2));

        title = text("Ayet Vakti", 30, true);
        title.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        root.addView(title);

        subtitle = text("Ekranı her açışınızda kilit ekranında,\ngüzel ahlak üzerine yeni bir ayet.", 15, false);
        subtitle.setPadding(0, px(6), 0, px(22));
        root.addView(subtitle);

        Point s = Engine.screenSize(this);
        int pw = (int) (getResources().getDisplayMetrics().widthPixels * 0.62f);
        int ph = (int) (pw * (s.y / (float) s.x));
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_XY);
        preview.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), px(26));
            }
        });
        preview.setClipToOutline(true);
        preview.setElevation(px(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(pw, ph);
        lp.bottomMargin = px(18);
        root.addView(preview, lp);

        status = text("", 14, false);
        status.setPadding(0, 0, 0, px(16));
        root.addView(status);

        mainBtn = button();
        mainBtn.setOnClickListener(v -> toggle());
        root.addView(mainBtn, btnLp(0));

        nextBtn = button();
        nextBtn.setText("Başka bir ayet göster");
        nextBtn.setOnClickListener(v -> next());
        root.addView(nextBtn, btnLp(10));

        modeBtn = button();
        modeBtn.setTextSize(14);
        modeBtn.setOnClickListener(v -> toggleMode());
        root.addView(modeBtn, btnLp(10));

        keepBtn = button();
        keepBtn.setTextSize(14);
        keepBtn.setOnClickListener(v -> cycleKeep());
        root.addView(keepBtn, btnLp(10));

        themeLabel = text("Tema", 13, true);
        themeLabel.setLetterSpacing(0.2f);
        themeLabel.setPadding(0, px(26), 0, px(10));
        root.addView(themeLabel);

        chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER);
        for (int i = 0; i < Renderer.THEMES.length; i++) {
            final int idx = i;
            View chip = new View(this);
            chip.setContentDescription(Renderer.THEME_NAMES[i]);
            chip.setOnClickListener(v -> setThemeIdx(idx));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(px(44), px(44));
            cp.leftMargin = px(7);
            cp.rightMargin = px(7);
            chips.addView(chip, cp);
        }
        root.addView(chips);

        batteryBtn = button();
        batteryBtn.setText("Pil kısıtlamasını kaldır (önerilir)");
        batteryBtn.setOnClickListener(v -> askBattery());
        root.addView(batteryBtn, btnLp(28));

        note = text("Ayet yalnızca kilit ekranı duvar kâğıdına yazılır; ana ekranınız değişmez. "
                + "Ekran kapandığında sıradaki ayet hazırlanır, ekranı açtığınızda o karşılar. "
                + "Toplam " + AyetData.ALL.length + " ayet var; hepsi bitmeden aynısı tekrar gelmez.\n\n"
                + "Samsung'da: Ayarlar › Kilit ekranı'nda \"Dinamik kilit ekranı\" veya \"Kilit ekranı duvar kâğıdı servisleri\" açıksa kapatın; yoksa ayetin üstüne yazabilir.\n\n"
                + "Mealler anlam aktarımıdır; bazı ayetlerin yalnızca bir bölümü verilmiştir.", 12.5f, false);
        note.setPadding(px(6), px(22), px(6), 0);
        note.setLineSpacing(0, 1.15f);
        root.addView(note);

        setContentView(scroll);
    }

    private void refresh() {
        int th = Engine.theme(this);
        int[] t = Renderer.THEMES[th];
        boolean light = Renderer.isLight(th);
        boolean active = Engine.isActive(this);

        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{t[0], t[1]});
        scroll.setBackground(bg);
        Window w = getWindow();
        w.setStatusBarColor(t[0]);
        w.setNavigationBarColor(t[1]);
        int sysFlags = 0;
        if (light) sysFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        w.getDecorView().setSystemUiVisibility(sysFlags);

        title.setTextColor(t[3]);
        subtitle.setTextColor(Renderer.alpha(t[3], 190));
        status.setTextColor(Renderer.alpha(t[3], 220));
        themeLabel.setTextColor(t[2]);
        note.setTextColor(Renderer.alpha(t[3], 150));

        if (active) {
            if (Engine.onScreen(this)) {
                status.setText("● Açık  ·  ekranı her açışınızda yeni ayet");
            } else {
                String at = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(Engine.nextHour()));
                status.setText("● Açık  ·  sıradaki ayet " + at + "'de");
            }
            mainBtn.setText("Durdur");
            styleSecondary(mainBtn, t);
        } else {
            status.setText("○ Kapalı");
            mainBtn.setText("Kilit ekranına uygula ve başlat");
            stylePrimary(mainBtn, t);
        }
        styleSecondary(nextBtn, t);
        styleSecondary(modeBtn, t);
        styleSecondary(keepBtn, t);
        int ks = Engine.keepSeconds(this);
        keepBtn.setText(ks > 0
                ? "Okuma süresi: ekran " + ks + " sn açık kalır  (dokun: değiştir)"
                : "Okuma süresi: telefonun kendi ayarı  (dokun: değiştir)");
        modeBtn.setText(Engine.onScreen(this)
                ? "Değişim: her ekran açılışında ✓  (dokun: sadece saat başı)"
                : "Değişim: sadece saat başı ✓  (dokun: her ekran açılışında)");
        styleSecondary(batteryBtn, t);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        batteryBtn.setVisibility(pm.isIgnoringBatteryOptimizations(getPackageName()) ? View.GONE : View.VISIBLE);

        for (int i = 0; i < chips.getChildCount(); i++) {
            int[] c = Renderer.THEMES[i];
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c[0], c[1]});
            g.setShape(GradientDrawable.OVAL);
            g.setStroke(px(i == th ? 3 : 1), i == th ? t[2] : Renderer.alpha(t[3], 70));
            chips.getChildAt(i).setBackground(g);
        }
        renderPreview();
    }

    private void renderPreview() {
        final int seq = ++previewSeq;
        final AyetData.Ayet a = Engine.current(this);
        final int th = Engine.theme(this);
        final Point s = Engine.screenSize(this);
        new Thread(() -> {
            final Bitmap bmp = Renderer.render(a, th, s.x / 2, s.y / 2);
            ui.post(() -> {
                if (seq == previewSeq) preview.setImageBitmap(bmp);
            });
        }).start();
    }

    private void toggle() {
        if (Engine.isActive(this)) {
            Engine.prefs(this).edit().putBoolean("active", false).apply();
            Engine.cancel(this);
            Engine.syncService(this);
            toast("Durduruldu. Kilit ekranındaki son ayet yerinde kalır.");
            refresh();
        } else {
            Engine.prefs(this).edit().putBoolean("active", true).apply();
            Engine.syncService(this);
            applyInBackground("Kilit ekranı güncellendi. Ekranı kapatıp açınca yeni ayet gelecek.");
            refresh();
        }
    }

    private void next() {
        Engine.advance(this);
        refresh();
        if (Engine.isActive(this)) applyInBackground("Kilit ekranı güncellendi.");
    }

    private void cycleKeep() {
        int cur = Engine.keepSeconds(this);
        int[] o = Engine.KEEP_OPTIONS;
        int next = o[0];
        for (int i = 0; i < o.length; i++) if (o[i] == cur) { next = o[(i + 1) % o.length]; break; }
        Engine.prefs(this).edit().putInt("keepSec", next).apply();
        refresh();
        toast(next > 0 ? "Kilit ekranı " + next + " saniye açık kalacak." : "Telefonun kendi ekran süresi kullanılacak.");
    }

    private void toggleMode() {
        boolean on = !Engine.onScreen(this);
        Engine.prefs(this).edit().putBoolean("onScreen", on).apply();
        Engine.syncService(this);
        refresh();
        toast(on ? "Ekranı her açışınızda yeni ayet gelecek." : "Ayet sadece saat başı değişecek.");
    }

    private void setThemeIdx(int i) {
        if (i == Engine.theme(this)) return;
        Engine.prefs(this).edit().putInt("theme", i).apply();
        refresh();
        if (Engine.isActive(this)) applyInBackground("Tema: " + Renderer.THEME_NAMES[i]);
    }

    private void applyInBackground(final String okMsg) {
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            String msg = okMsg;
            try {
                Engine.applyNow(app);
                Engine.scheduleNext(app);
            } catch (Throwable e) {
                msg = "Duvar kâğıdı ayarlanamadı: " + e.getMessage();
            }
            final String m = msg;
            ui.post(() -> toast(m));
        }).start();
    }

    private void askBattery() {
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                toast("Ayarlar › Uygulamalar › Ayet Vakti › Pil › Kısıtlanmamış");
            }
        }
    }

    // --- küçük yardımcılar ---

    private TextView text(String s, float sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setGravity(Gravity.CENTER);
        if (bold) tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return tv;
    }

    private TextView button() {
        TextView b = new TextView(this);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(16);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setPadding(px(18), px(15), px(18), px(15));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    private LinearLayout.LayoutParams btnLp(int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(topDp);
        return lp;
    }

    private void stylePrimary(TextView b, int[] t) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(px(30));
        g.setColor(t[2]);
        b.setBackground(g);
        b.setTextColor(t[0]);
    }

    private void styleSecondary(TextView b, int[] t) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(px(30));
        g.setColor(Renderer.alpha(t[3], 18));
        g.setStroke(px(1), Renderer.alpha(t[2], 150));
        b.setBackground(g);
        b.setTextColor(t[3]);
    }

    private int px(float v) { return Math.round(v * dp); }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
