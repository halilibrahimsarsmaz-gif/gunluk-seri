package com.halil.kilohedefim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.Map;
import java.util.TreeMap;

/** Tartı grafiği: yeşil = senin tartıların, gri kesikli = plan. */
public class ChartView extends View {

    private final Store.Profile p;
    private final TreeMap<String, Double> w;
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint plan = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;

    public ChartView(Context c, Store.Profile p, TreeMap<String, Double> w) {
        super(c);
        this.p = p;
        this.w = w;
        d = c.getResources().getDisplayMetrics().density;
        line.setColor(0xFF2E7D32);
        line.setStrokeWidth(3 * d);
        line.setStyle(Paint.Style.STROKE);
        dot.setColor(0xFF2E7D32);
        plan.setColor(0xFF9E9E9E);
        plan.setStrokeWidth(2 * d);
        plan.setStyle(Paint.Style.STROKE);
        plan.setPathEffect(new DashPathEffect(new float[]{8 * d, 6 * d}, 0));
        grid.setColor(0xFFE0E0E0);
        grid.setStrokeWidth(1 * d);
        text.setColor(0xFF757575);
        text.setTextSize(11 * d);
    }

    @Override
    protected void onDraw(Canvas c) {
        float left = 40 * d, right = getWidth() - 10 * d, top = 10 * d, bottom = getHeight() - 24 * d;

        String start = p.startDate;
        if (!w.isEmpty() && w.firstKey().compareTo(start) < 0) start = w.firstKey();
        String end = p.endDate;
        if (!w.isEmpty() && w.lastKey().compareTo(end) > 0) end = w.lastKey();
        long span = Math.max(1, Store.daysBetween(start, end));

        double min = Math.min(p.targetWeight, p.startWeight), max = Math.max(p.targetWeight, p.startWeight);
        for (double v : w.values()) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        min = Math.floor(min - 1);
        max = Math.ceil(max + 1);
        double range = Math.max(1, max - min);

        // ızgara ve etiketler
        for (int i = 0; i <= 4; i++) {
            float y = top + (bottom - top) * i / 4f;
            c.drawLine(left, y, right, y, grid);
            double val = max - range * i / 4.0;
            c.drawText(String.format(java.util.Locale.US, "%.0f", val), 4 * d, y + 4 * d, text);
        }
        c.drawText(Store.prettyShort(start), left, getHeight() - 6 * d, text);
        String endLabel = Store.prettyShort(end);
        c.drawText(endLabel, right - text.measureText(endLabel), getHeight() - 6 * d, text);

        // plan çizgisi
        float px1 = left + (right - left) * Store.daysBetween(start, p.startDate) / (float) span;
        float px2 = left + (right - left) * Store.daysBetween(start, p.endDate) / (float) span;
        float py1 = (float) (top + (max - p.startWeight) / range * (bottom - top));
        float py2 = (float) (top + (max - p.targetWeight) / range * (bottom - top));
        Path pp = new Path();
        pp.moveTo(px1, py1);
        pp.lineTo(px2, py2);
        c.drawPath(pp, plan);

        // tartılar
        Path path = new Path();
        boolean first = true;
        for (Map.Entry<String, Double> e : w.entrySet()) {
            float x = left + (right - left) * Store.daysBetween(start, e.getKey()) / (float) span;
            float y = (float) (top + (max - e.getValue()) / range * (bottom - top));
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
            c.drawCircle(x, y, 3.5f * d, dot);
        }
        c.drawPath(path, line);
    }
}
