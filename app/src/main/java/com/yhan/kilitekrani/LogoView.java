package com.yhan.kilitekrani;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** YH logosu: beyaz daire + siyah halka + el yazısı "YH". */
public class LogoView extends View {

    private final Paint dolgu = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halka = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint yazi = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LogoView(Context c) { super(c); init(); }
    public LogoView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        dolgu.setColor(Color.WHITE);
        halka.setColor(Color.BLACK);
        halka.setStyle(Paint.Style.STROKE);
        yazi.setColor(Color.BLACK);
        yazi.setTextAlign(Paint.Align.CENTER);
        yazi.setTypeface(Typeface.create("cursive", Typeface.BOLD_ITALIC));
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = s / 2f - 1f;
        c.drawCircle(cx, cy, r, dolgu);
        halka.setStrokeWidth(s * 0.07f);
        c.drawCircle(cx, cy, r - s * 0.05f, halka);
        yazi.setTextSize(s * 0.44f);
        Paint.FontMetrics fm = yazi.getFontMetrics();
        c.drawText("YH", cx, cy - (fm.ascent + fm.descent) / 2f, yazi);
    }
}
