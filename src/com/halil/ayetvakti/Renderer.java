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

    static Bitmap render(AyetData.Ayet a, int theme, int w, int h) {
        int[] t = THEMES[Math.floorMod(theme, THEMES.length)];
        boolean light = isLight(theme);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Arka plan
        p.setShader(new LinearGradient(0, 0, 0, h, t[0], t[1], Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);

        float cy = h * 0.575f;
        p.setShader(new RadialGradient(w / 2f, cy, w * 0.85f,
                alpha(t[2], light ? 50 : 38), alpha(t[2], 0), Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);
        p.setShader(null);

        drawPattern(cv, w, h, cy, t[4], light);

        // Yazı ayarları
        float margin = w * 0.10f;
        int tw = (int) (w - 2 * margin);

        TextPaint label = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(t[2]);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        label.setTextSize(w * 0.029f);
        label.setLetterSpacing(0.28f);
        label.setTextAlign(Paint.Align.CENTER);

        TextPaint body = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(t[3]);
        body.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        if (!light) body.setShadowLayer(w * 0.008f, 0, w * 0.002f, 0x66000000);

        TextPaint ref = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        ref.setColor(t[2]);
        ref.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        ref.setTextSize(w * 0.036f);
        ref.setTextAlign(Paint.Align.CENTER);

        String txt = "“" + a.text + "”";
        float size = w * 0.058f;
        StaticLayout lay;
        while (true) {
            body.setTextSize(size);
            lay = StaticLayout.Builder.obtain(txt, 0, txt.length(), body, tw)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                    .build();
            if (lay.getHeight() <= h * 0.36f || size <= w * 0.034f) break;
            size *= 0.95f;
        }

        String topic = a.topic.toUpperCase(new Locale("tr", "TR"));
        float labelH = -label.ascent() + label.descent();
        float refH = -ref.ascent() + ref.descent();
        float ornH = w * 0.03f;
        float gap = w * 0.042f;
        float total = labelH + gap + ornH + gap * 1.2f + lay.getHeight() + gap * 1.2f + ornH + gap + refH;
        float y = cy - total / 2f;

        cv.drawText(topic, w / 2f, y - label.ascent(), label);
        y += labelH + gap;
        drawOrnament(cv, w / 2f, y + ornH / 2f, w * 0.20f, w * 0.012f, t[2]);
        y += ornH + gap * 1.2f;

        cv.save();
        cv.translate(margin, y);
        lay.draw(cv);
        cv.restore();
        y += lay.getHeight() + gap * 1.2f;

        drawOrnament(cv, w / 2f, y + ornH / 2f, w * 0.12f, w * 0.009f, t[2]);
        y += ornH + gap;
        cv.drawText(a.ref, w / 2f, y - ref.ascent(), ref);

        return bmp;
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
