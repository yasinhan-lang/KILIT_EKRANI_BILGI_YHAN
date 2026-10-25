package com.yhan.kilitekrani;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Gönderde dalgalanan Türk bayrağı. */
public class FlagView extends View {

    private static final int MW = 24, MH = 8;
    private final float[] noktalar = new float[(MW + 1) * (MH + 1) * 2];
    private final int[] renkler = new int[(MW + 1) * (MH + 1)];
    private final Paint gonder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap bayrak;
    private float faz = 0f;
    private float fw, fh, gonderW, ust;

    public FlagView(Context c) { super(c); init(); }
    public FlagView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        gonder.setColor(0xFFC9CED6);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        gonderW = Math.max(2f, w * 0.05f);
        fw = (w - gonderW) * 0.92f;
        fh = fw / 1.5f;
        if (fh > h * 0.78f) { fh = h * 0.78f; fw = fh * 1.5f; }
        ust = (h - fh) / 2f - h * 0.06f;
        if (bayrak != null) bayrak.recycle();
        bayrak = bayrakCiz(Math.max(1, (int) fw), Math.max(1, (int) fh));
    }

    /** Resmi ölçülerle (Wikipedia SVG koordinatları, G = 60000). */
    private static Bitmap bayrakCiz(int w, int h) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFE30A17);
        c.drawRect(0, 0, w, h, p);
        float k = h / 60000f;
        c.save();
        c.translate(0, h / 2f);
        c.scale(k, k);
        p.setColor(Color.WHITE);
        c.drawCircle(22500, 0, 15000, p);
        p.setColor(0xFFE30A17);
        c.drawCircle(26250, 0, 12000, p);
        p.setColor(Color.WHITE);
        Path yildiz = new Path();
        yildiz.moveTo(28750, 0);
        yildiz.lineTo(45000, -5280);
        yildiz.lineTo(34950, 8560);
        yildiz.lineTo(34950, -8560);
        yildiz.lineTo(45000, 5280);
        yildiz.close();
        c.drawPath(yildiz, p);
        c.restore();
        return b;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bayrak == null) return;
        float h = getHeight();
        // gönder
        c.drawRoundRect(0, h * 0.04f, gonderW, h, gonderW / 2f, gonderW / 2f, gonder);
        c.drawCircle(gonderW / 2f, h * 0.04f, gonderW * 0.9f, gonder);

        float genlik = fh * 0.08f;
        int i = 0, j = 0;
        for (int y = 0; y <= MH; y++) {
            for (int x = 0; x <= MW; x++) {
                float fx = x / (float) MW;
                double dalga = Math.sin(fx * 2.6 * Math.PI - faz);
                noktalar[i++] = gonderW + fx * fw;
                noktalar[i++] = ust + (y / (float) MH) * fh + (float) dalga * genlik * fx;
                int v = (int) (232 + 23 * Math.cos(fx * 2.6 * Math.PI - faz));
                if (v > 255) v = 255;
                renkler[j++] = Color.rgb(v, v, v);
            }
        }
        c.drawBitmapMesh(bayrak, MW, MH, noktalar, 0, renkler, 0, null);
        faz += 0.12f;
        postInvalidateOnAnimation();
    }
}
