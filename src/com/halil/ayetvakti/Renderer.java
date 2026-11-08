package com.halil.ayetvakti;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.Locale;

/** Ayeti, kilit ekranı duvar kâğıdı olarak çizer. */
final class Renderer {
    private Renderer() {}

    // üst renk, alt renk, vurgu (altın), metin, desen
    static final int[][] THEMES = {
        {0xFF0A1128, 0xFF1C2B57, 0xFFD9B26A, 0xFFF5EDDF, 0xFFD9B26A}, // Gece
        {0xFF06231E, 0xFF12503F, 0xFFE6C47F, 0xFFF0F5EE, 0xFFE6C47F}, // Zümrüt
        {0xFFF7F0E3, 0xFFE8D9BE, 0xFF8E5F28, 0xFF2A2018, 0xFF8E5F28}, // Şafak
        {0xFF221019, 0xFF4A2337, 0xFFEBB8A2, 0xFFF8ECE8, 0xFFEBB8A2}, // Gül
        {0xFF0E0E10, 0xFF1E1E22, 0xFFC9A66B, 0xFFEDEDED, 0xFFC9A66B}, // Siyah
    };
    static final String[] THEME_NAMES = {"Gece", "Zümrüt", "Şafak", "Gül", "Siyah"};

    static boolean isLight(int theme) { return theme == 2; }

    static Bitmap render(AyetData.Ayet a, int theme, int w, int h, Data.Now info) {
        int[] t = THEMES[Math.floorMod(theme, THEMES.length)];
        boolean light = isLight(theme);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Arka plan
        p.setShader(new LinearGradient(0, 0, 0, h, t[0], t[1], Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);

        float cardTop = h * 0.268f;
        float cardBottom = info != null ? drawInfoCard(cv, w, h, cardTop, t, light, info, true) : cardTop;
        float regionTop = info != null ? cardBottom + h * 0.022f : h * 0.30f;
        float regionBottom = h * 0.865f;
        float cy = (regionTop + regionBottom) / 2f;

        p.setShader(new RadialGradient(w / 2f, cy, w * 0.85f,
                alpha(t[2], light ? 50 : 38), alpha(t[2], 0), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);

        drawPattern(cv, w, h, cy, t[4], light);
        if (info != null) {
            float mx = w * 0.055f, r = w * 0.05f;
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(light ? 0x78FFFFFF : 0x52000000);
            cv.drawRoundRect(mx, cardTop, w - mx, cardBottom, r, r, bg);
            Paint br = new Paint(Paint.ANTI_ALIAS_FLAG);
            br.setStyle(Paint.Style.STROKE);
            br.setStrokeWidth(Math.max(1f, w * 0.0018f));
            br.setColor(alpha(t[2], 85));
            cv.drawRoundRect(mx, cardTop, w - mx, cardBottom, r, r, br);
            drawInfoCard(cv, w, h, cardTop, t, light, info, false);
        }

        // Yazı ayarları
        float margin = w * 0.09f;
        int tw = (int) (w - 2 * margin);

        TextPaint label = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(t[2]);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setTextSize(w * 0.028f);
        label.setLetterSpacing(0.28f);
        label.setTextAlign(Paint.Align.CENTER);

        TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(t[3]);
        body.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        if (!light) body.setShadowLayer(w * 0.008f, 0, w * 0.002f, 0x66000000);

        TextPaint ref = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        ref.setColor(t[2]);
        ref.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        ref.setTextSize(w * 0.035f);
        ref.setTextAlign(Paint.Align.CENTER);

        String txt = "\u201C" + innerQuotes(a.text) + "\u201D";
        float labelH = -label.ascent() + label.descent();
        float refH = -ref.ascent() + ref.descent();
        float ornH = w * 0.026f;
        float gap = w * 0.034f;
        float deco = labelH + gap + ornH + gap * 1.1f + gap * 1.1f + ornH + gap + refH;
        float budget = regionBottom - regionTop;
        float size = w * 0.056f;
        StaticLayout lay;
        while (true) {
            body.setTextSize(size);
            lay = StaticLayout.Builder.obtain(txt, 0, txt.length(), body, tw)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.18f)
                    .setIncludePad(false)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                    .build();
            if (lay.getHeight() + deco <= budget || size <= w * 0.030f) break;
            size *= 0.95f;
        }

        String topic = a.topic.toUpperCase(TR);
        float total = deco + lay.getHeight();
        float y = cy - total / 2f;

        cv.drawText(topic, w / 2f, y - label.ascent(), label);
        y += labelH + gap;
        drawOrnament(cv, w / 2f, y + ornH / 2f, w * 0.20f, w * 0.011f, t[2]);
        y += ornH + gap * 1.1f;

        cv.save();
        cv.translate(margin, y);
        lay.draw(cv);
        cv.restore();
        y += lay.getHeight() + gap * 1.1f;

        drawOrnament(cv, w / 2f, y + ornH / 2f, w * 0.12f, w * 0.008f, t[2]);
        y += ornH + gap;
        cv.drawText(a.ref, w / 2f, y - ref.ascent(), ref);

        return bmp;
    }

    static final Locale TR = new Locale("tr", "TR");

    /** Ayetin içindeki düz tırnakları tek tırnağa çevirir; dıştaki çift tırnakla karışmasın. */
    private static String innerQuotes(String s) {
        StringBuilder b = new StringBuilder(s.length());
        boolean open = true;
        for (char ch : s.toCharArray()) {
            if (ch == '"') { b.append(open ? '\u2018' : '\u2019'); open = !open; }
            else b.append(ch);
        }
        return b.toString();
    }

    /**
     * Hava durumu + namaz vakitleri kartı. measureOnly=true ise sadece yüksekliği hesaplar.
     * Kartın alt kenarının y değerini döndürür.
     */
    private static float drawInfoCard(Canvas cv, int w, int h, float top, int[] t, boolean light,
                                      Data.Now n, boolean measureOnly) {
        float mx = w * 0.055f;
        float left = mx, right = w - mx;
        float pad = w * 0.042f;
        float inL = left + pad, inR = right - pad;

        TextPaint big = tp(t[3], w * 0.082f, "sans-serif-light", Paint.Align.LEFT);
        TextPaint small = tp(alpha(t[3], 215), w * 0.035f, "sans-serif", Paint.Align.LEFT);
        TextPaint tiny = tp(alpha(t[3], 170), w * 0.030f, "sans-serif", Paint.Align.LEFT);
        TextPaint goldLbl = tp(t[2], w * 0.030f, "sans-serif-medium", Paint.Align.CENTER);
        goldLbl.setLetterSpacing(0.2f);
        TextPaint line = tp(t[3], w * 0.046f, "sans-serif", Paint.Align.CENTER);
        TextPaint colName = tp(alpha(t[3], 175), w * 0.028f, "sans-serif", Paint.Align.CENTER);
        TextPaint colTime = tp(t[3], w * 0.040f, "sans-serif-medium", Paint.Align.CENTER);

        float y = top + pad;

        // --- 1. satır: hava durumu ---
        float rowA = w * 0.092f;
        if (!measureOnly) {
            Data.Weather wt = n.weather;
            float midA = y + rowA / 2f;
            float x = inL;
            if (wt != null) {
                drawWeatherIcon(cv, x + rowA * 0.5f, midA, rowA * 0.92f, Data.weatherKind(wt.code), wt.day, t[2], t[3]);
                x += rowA + w * 0.02f;
                String deg = wt.temp + "°";
                cv.drawText(deg, x, midA - (big.ascent() + big.descent()) / 2f, big);
                x += big.measureText(deg) + w * 0.03f;
                String desc = Data.weatherText(wt.code);
                String hl = "↑" + wt.max + "°  ↓" + wt.min + "°" + (wt.rain >= 20 ? "   ☂ %" + wt.rain : "");
                cv.drawText(desc, x, midA - w * 0.006f, small);
                cv.drawText(hl, x, midA - tiny.ascent() + w * 0.004f, tiny);
            } else {
                cv.drawText("Hava durumu bekleniyor…", x, midA - (small.ascent() + small.descent()) / 2f, small);
            }
            TextPaint city = tp(t[2], w * 0.031f, "sans-serif-medium", Paint.Align.RIGHT);
            city.setLetterSpacing(0.18f);
            cv.drawText(Data.CITY.toUpperCase(TR), inR, midA - w * 0.006f, city);
            TextPaint hij = tp(alpha(t[3], 160), w * 0.027f, "sans-serif", Paint.Align.RIGHT);
            String hijri = n.today.hijri;
            if (hijri.isEmpty()) hijri = new java.text.SimpleDateFormat("d MMMM EEEE", TR).format(n.now);
            cv.drawText(fit(hijri, hij, w * 0.30f), inR, midA - hij.ascent() + w * 0.004f, hij);
        }
        y += rowA + w * 0.03f;

        // ayraç
        if (!measureOnly) {
            Paint d = new Paint(Paint.ANTI_ALIAS_FLAG);
            d.setColor(alpha(t[2], 90));
            d.setStrokeWidth(Math.max(1f, w * 0.0015f));
            cv.drawLine(inL, y, inR, y, d);
        }
        y += w * 0.032f;

        // --- 2. satır: şu anki vakit ve çıkışı ---
        long rem = n.end - n.now;
        boolean soon = n.prayer && rem <= 30 * 60_000L;
        String lbl = Data.vakitLabel(n.idx).toUpperCase(TR);
        String msg = n.prayer
                ? "Çıkmasına " + Data.left(rem) + "  ·  " + Data.clock(n.end)
                : "Öğle vaktine " + Data.left(rem) + "  ·  " + Data.clock(n.end);
        float lblH = -goldLbl.ascent() + goldLbl.descent();
        if (!measureOnly) {
            cv.drawText(lbl, w / 2f, y - goldLbl.ascent(), goldLbl);
            line.setColor(soon ? t[2] : t[3]);
            if (soon) line.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            cv.drawText(msg, w / 2f, y + lblH + w * 0.012f - line.ascent(), line);
        }
        y += lblH + w * 0.012f + (-line.ascent() + line.descent()) + w * 0.034f;

        // --- 3. satır: 6 vakit ---
        float colW = (inR - inL) / 6f;
        float nameH = -colName.ascent() + colName.descent();
        float timeH = -colTime.ascent() + colTime.descent();
        float pillPad = w * 0.012f;
        float rowC = pillPad + nameH + w * 0.006f + timeH + pillPad;
        if (!measureOnly) {
            for (int i = 0; i < 6; i++) {
                float cx = inL + colW * (i + 0.5f);
                boolean cur = i == n.idx;
                if (cur) {
                    Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
                    pill.setColor(t[2]);
                    float r = w * 0.022f;
                    cv.drawRoundRect(cx - colW * 0.47f, y, cx + colW * 0.47f, y + rowC, r, r, pill);
                }
                colName.setColor(cur ? alpha(t[0], 220) : alpha(t[3], 165));
                colTime.setColor(cur ? t[0] : t[3]);
                cv.drawText(Data.NAMES[i], cx, y + pillPad - colName.ascent(), colName);
                cv.drawText(Data.clockMin(n.today.min[i]), cx, y + pillPad + nameH + w * 0.006f - colTime.ascent(), colTime);
            }
        }
        y += rowC;
        if (n.today.approx) {
            y += w * 0.012f;
            if (!measureOnly) {
                TextPaint ap = tp(alpha(t[3], 140), w * 0.022f, "sans-serif", Paint.Align.CENTER);
                cv.drawText("Vakitler yaklaşık · Diyanet verisi için internet gerekli", w / 2f, y - ap.ascent(), ap);
            }
            y += w * 0.03f;
        }
        y += pad;

        return y;
    }

    private static TextPaint tp(int color, float size, String family, Paint.Align al) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(Typeface.create(family, Typeface.NORMAL));
        p.setTextAlign(al);
        return p;
    }

    private static String fit(String s, Paint p, float max) {
        if (p.measureText(s) <= max) return s;
        while (s.length() > 3 && p.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s.trim() + "…";
    }

    /** Basit hava durumu simgeleri: 0 açık, 1 parçalı, 2 bulut, 3 sis, 4 yağmur, 5 kar, 6 fırtına */
    private static void drawWeatherIcon(Canvas cv, float cx, float cy, float s, int kind, boolean day, int gold, int fg) {
        Paint f = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint st = new Paint(Paint.ANTI_ALIAS_FLAG);
        st.setStyle(Paint.Style.STROKE);
        st.setStrokeCap(Paint.Cap.ROUND);
        st.setStrokeWidth(s * 0.06f);
        if (kind == 0 || kind == 1) {
            float r = kind == 0 ? s * 0.22f : s * 0.17f;
            float sx = kind == 0 ? cx : cx - s * 0.16f, sy = kind == 0 ? cy : cy - s * 0.14f;
            f.setColor(gold);
            st.setColor(gold);
            if (day) {
                cv.drawCircle(sx, sy, r, f);
                for (int i = 0; i < 8; i++) {
                    double an = Math.PI / 4 * i;
                    float r1 = r * 1.45f, r2 = r * 1.9f;
                    cv.drawLine(sx + (float) Math.cos(an) * r1, sy + (float) Math.sin(an) * r1,
                            sx + (float) Math.cos(an) * r2, sy + (float) Math.sin(an) * r2, st);
                }
            } else {
                Path m = new Path();
                m.addCircle(sx, sy, r * 1.3f, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(sx + r * 0.7f, sy - r * 0.45f, r * 1.1f, Path.Direction.CW);
                m.op(cut, Path.Op.DIFFERENCE);
                cv.drawPath(m, f);
            }
            if (kind == 0) return;
        }
        float ccy = kind == 1 ? cy + s * 0.08f : cy - (kind >= 4 ? s * 0.10f : 0);
        if (kind != 3) {
            f.setColor(fg);
            Path c = new Path();
            c.addCircle(cx - s * 0.15f, ccy + s * 0.04f, s * 0.15f, Path.Direction.CW);
            c.addCircle(cx + s * 0.05f, ccy - s * 0.05f, s * 0.2f, Path.Direction.CW);
            c.addCircle(cx + s * 0.24f, ccy + s * 0.06f, s * 0.13f, Path.Direction.CW);
            c.addRoundRect(cx - s * 0.30f, ccy + s * 0.02f, cx + s * 0.37f, ccy + s * 0.19f, s * 0.08f, s * 0.08f, Path.Direction.CW);
            cv.drawPath(c, f);
        }
        st.setColor(gold);
        float by = ccy + s * 0.30f;
        if (kind == 3) {
            st.setColor(fg);
            for (int i = 0; i < 3; i++) {
                float yy = cy - s * 0.16f + i * s * 0.16f;
                float off = (i % 2) * s * 0.08f;
                cv.drawLine(cx - s * 0.32f + off, yy, cx + s * 0.32f - (0.08f * s - off), yy, st);
            }
        } else if (kind == 4) {
            for (int i = 0; i < 3; i++) {
                float x = cx - s * 0.16f + i * s * 0.16f;
                cv.drawLine(x, by, x - s * 0.05f, by + s * 0.14f, st);
            }
        } else if (kind == 5) {
            f.setColor(gold);
            for (int i = 0; i < 3; i++) {
                float x = cx - s * 0.16f + i * s * 0.16f;
                cv.drawCircle(x, by + s * 0.05f + (i % 2) * s * 0.06f, s * 0.04f, f);
            }
        } else if (kind == 6) {
            f.setColor(gold);
            Path b = new Path();
            b.moveTo(cx + s * 0.04f, by - s * 0.06f);
            b.lineTo(cx - s * 0.09f, by + s * 0.10f);
            b.lineTo(cx, by + s * 0.10f);
            b.lineTo(cx - s * 0.05f, by + s * 0.24f);
            b.lineTo(cx + s * 0.11f, by + s * 0.04f);
            b.lineTo(cx + s * 0.02f, by + s * 0.04f);
            b.close();
            cv.drawPath(b, f);
        }
    }

    /** Hafif, sekiz köşeli yıldız deseni; yazının çevresinde solar. */
    private static void drawPattern(Canvas cv, int w, int h, float cy, int color, boolean light) {
        Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
        s.setStyle(Paint.Style.STROKE);
        s.setStrokeWidth(Math.max(1f, w * 0.0016f));
        float step = w / 4.5f;
        float r = step * 0.30f;
        int base = light ? 34 : 26;
        Path star = new Path();
        for (float yy = step * 0.25f; yy < h + step; yy += step * 0.86f) {
            boolean odd = Math.round(yy / (step * 0.86f)) % 2 == 1;
            float dist = Math.min(1f, Math.abs(yy - cy) / (h * 0.5f));
            int al = (int) (base * (0.15f + 0.85f * dist));
            s.setColor(alpha(color, al));
            for (float xx = odd ? step / 2f : 0f; xx < w + step; xx += step) {
                star.reset();
                star.addRect(xx - r, yy - r, xx + r, yy + r, Path.Direction.CW);
                float d = r * 1.4142f;
                star.moveTo(xx, yy - d);
                star.lineTo(xx + d, yy);
                star.lineTo(xx, yy + d);
                star.lineTo(xx - d, yy);
                star.close();
                cv.drawPath(star, s);
                cv.drawCircle(xx, yy, r * 0.45f, s);
            }
        }
    }

    private static void drawOrnament(Canvas cv, float cx, float cy, float half, float dm, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(alpha(color, 200));
        p.setStrokeWidth(Math.max(1f, dm * 0.18f));
        float inner = dm * 2.4f;
        cv.drawLine(cx - half, cy, cx - inner, cy, p);
        cv.drawLine(cx + inner, cy, cx + half, cy, p);
        Path d = new Path();
        d.moveTo(cx, cy - dm);
        d.lineTo(cx + dm, cy);
        d.lineTo(cx, cy + dm);
        d.lineTo(cx - dm, cy);
        d.close();
        p.setStyle(Paint.Style.FILL);
        cv.drawPath(d, p);
        cv.drawCircle(cx - half - dm * 0.8f, cy, dm * 0.28f, p);
        cv.drawCircle(cx + half + dm * 0.8f, cy, dm * 0.28f, p);
    }

    static int alpha(int c, int a) { return Color.argb(Math.max(0, Math.min(255, a)), Color.red(c), Color.green(c), Color.blue(c)); }
}
