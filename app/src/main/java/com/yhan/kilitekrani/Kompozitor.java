package com.yhan.kilitekrani;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Resim + bilgi kutusunu ekran boyutunda tek bir duvar kağıdına birleştirir. */
public final class Kompozitor {

    private Kompozitor() { }

    public static class Ayar {
        public String baslik = "", ad = "", tel1 = "", tel2 = "", adres = "", not = "";
        public int konum = 2;      // 0 üst, 1 orta, 2 alt
        public int boyut = 40;     // 0..100  -> %60..%160
        public int opak = 65;      // 0..100
        public int yaziRenk = 0;   // YAZI dizisi
        public int kutuRenk = 0;   // KUTU dizisi
    }

    static final int[] YAZI = {0xFFFFFFFF, 0xFF111111, 0xFFFFD54F};
    static final int[] KUTU = {0xFF000000, 0xFFFFFFFF, 0xFF0D1B2A, 0xFF8B0000};

    private static class Parca {
        final StaticLayout yerlesim;
        final boolean cizgiSonra;
        Parca(StaticLayout l, boolean c) { yerlesim = l; cizgiSonra = c; }
    }

    private static int sinirla(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    /** Dosyadaki resmi, istenen boyuta yakın ve EXIF yönü düzeltilmiş olarak açar. */
    public static Bitmap dosyadanYukle(File f, int reqW, int reqH) {
        if (f == null || !f.exists()) return null;
        String yol = f.getAbsolutePath();
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(yol, o);
        if (o.outWidth <= 0 || o.outHeight <= 0) return null;

        int aci = 0;
        try {
            int yon = new ExifInterface(yol).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (yon == ExifInterface.ORIENTATION_ROTATE_90) aci = 90;
            else if (yon == ExifInterface.ORIENTATION_ROTATE_180) aci = 180;
            else if (yon == ExifInterface.ORIENTATION_ROTATE_270) aci = 270;
        } catch (Exception ignored) { }

        int bw = o.outWidth, bh = o.outHeight;
        if (aci == 90 || aci == 270) { int t = bw; bw = bh; bh = t; }
        int ornek = 1;
        while (bw / (ornek * 2) >= reqW && bh / (ornek * 2) >= reqH) ornek *= 2;

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = ornek;
        Bitmap b = BitmapFactory.decodeFile(yol, o2);
        if (b != null && aci != 0) {
            Matrix m = new Matrix();
            m.postRotate(aci);
            Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (r != b) b.recycle();
            b = r;
        }
        return b;
    }

    private static StaticLayout yazi(String metin, float boyut, int stil, int renk,
                                     int genislik, boolean golge, float s) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(renk);
        tp.setTextSize(boyut);
        tp.setTypeface(Typeface.create(Typeface.SANS_SERIF, stil));
        if (golge) {
            boolean acik = (renk & 0x00FFFFFF) > 0x808080;
            tp.setShadowLayer(7f * s, 0f, 2f * s, acik ? 0xDD000000 : 0xDDFFFFFF);
        }
        return StaticLayout.Builder.obtain(metin, 0, metin.length(), tp, genislik)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1.08f)
                .setIncludePad(false)
                .build();
    }

    public static Bitmap olustur(Bitmap arka, Ayar a, int w, int h) {
        Bitmap cikti = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(cikti);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        // 1) Arka plan: resmi ekranı tam dolduracak şekilde ortala-kırp
        if (arka != null) {
            float sc = Math.max(w / (float) arka.getWidth(), h / (float) arka.getHeight());
            float dw = arka.getWidth() * sc, dh = arka.getHeight() * sc;
            c.drawBitmap(arka, null, new RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f), p);
        } else {
            p.setShader(new LinearGradient(0, 0, 0, h, 0xFF1B3A5C, 0xFF0D1B2A, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, p);
            p.setShader(null);
        }

        // 2) Bilgi kutusu
        float s = w / 1080f * (60 + sinirla(a.boyut, 0, 100)) / 100f;
        int yaziRenk = YAZI[sinirla(a.yaziRenk, 0, YAZI.length - 1)];
        int kutuRenk = KUTU[sinirla(a.kutuRenk, 0, KUTU.length - 1)];
        int opak = sinirla(a.opak, 0, 100);
        boolean golge = opak < 45;

        float kenar = w * 0.06f;
        float ic = 38f * s;
        int metinW = (int) (w - 2 * kenar - 2 * ic);
        if (metinW < 50) metinW = 50;

        List<Parca> parcalar = new ArrayList<>();
        String baslik = a.baslik.trim();
        boolean altVar = !a.ad.trim().isEmpty() || !a.tel1.trim().isEmpty() || !a.tel2.trim().isEmpty()
                || !a.adres.trim().isEmpty() || !a.not.trim().isEmpty();
        if (!baslik.isEmpty())
            parcalar.add(new Parca(yazi(baslik, 38f * s, Typeface.BOLD, yaziRenk, metinW, golge, s), altVar));
        if (!a.ad.trim().isEmpty())
            parcalar.add(new Parca(yazi(a.ad.trim(), 58f * s, Typeface.BOLD, yaziRenk, metinW, golge, s), false));
        if (!a.tel1.trim().isEmpty())
            parcalar.add(new Parca(yazi("📞 " + a.tel1.trim(), 56f * s, Typeface.BOLD, yaziRenk, metinW, golge, s), false));
        if (!a.tel2.trim().isEmpty())
            parcalar.add(new Parca(yazi("📞 " + a.tel2.trim(), 46f * s, Typeface.BOLD, yaziRenk, metinW, golge, s), false));
        if (!a.adres.trim().isEmpty())
            parcalar.add(new Parca(yazi("📍 " + a.adres.trim(), 38f * s, Typeface.NORMAL, yaziRenk, metinW, golge, s), false));
        if (!a.not.trim().isEmpty())
            parcalar.add(new Parca(yazi(a.not.trim(), 34f * s, Typeface.ITALIC, yaziRenk, metinW, golge, s), false));

        if (parcalar.isEmpty()) return cikti;

        float aralik = 16f * s, cizgiAlani = 30f * s;
        float toplam = 2 * ic;
        for (int i = 0; i < parcalar.size(); i++) {
            Parca pr = parcalar.get(i);
            toplam += pr.yerlesim.getHeight();
            if (i < parcalar.size() - 1) toplam += pr.cizgiSonra ? cizgiAlani : aralik;
        }

        float y0;
        if (a.konum == 0) y0 = h * 0.27f;               // saatin altı
        else if (a.konum == 1) y0 = (h - toplam) / 2f;  // orta
        else y0 = h * 0.86f - toplam;                   // alt (kilit kısayollarının üstü)
        float enAz = h * 0.04f, enCok = h * 0.97f - toplam;
        if (y0 > enCok) y0 = enCok;
        if (y0 < enAz) y0 = enAz;

        RectF kutu = new RectF(kenar, y0, w - kenar, y0 + toplam);
        if (opak > 0) {
            Paint kp = new Paint(Paint.ANTI_ALIAS_FLAG);
            kp.setColor(kutuRenk);
            kp.setAlpha(opak * 255 / 100);
            c.drawRoundRect(kutu, 36f * s, 36f * s, kp);

            Paint cp = new Paint(Paint.ANTI_ALIAS_FLAG);
            cp.setStyle(Paint.Style.STROKE);
            cp.setStrokeWidth(2f * s);
            cp.setColor(yaziRenk);
            cp.setAlpha(90);
            c.drawRoundRect(kutu, 36f * s, 36f * s, cp);
        }

        Paint cizgi = new Paint(Paint.ANTI_ALIAS_FLAG);
        cizgi.setColor(yaziRenk);
        cizgi.setAlpha(150);
        cizgi.setStrokeWidth(2.5f * s);

        float y = y0 + ic;
        for (int i = 0; i < parcalar.size(); i++) {
            Parca pr = parcalar.get(i);
            c.save();
            c.translate(kenar + ic, y);
            pr.yerlesim.draw(c);
            c.restore();
            y += pr.yerlesim.getHeight();
            if (i < parcalar.size() - 1) {
                if (pr.cizgiSonra) {
                    float cy = y + cizgiAlani / 2f;
                    c.drawLine(w * 0.30f, cy, w * 0.70f, cy, cizgi);
                    y += cizgiAlani;
                } else {
                    y += aralik;
                }
            }
        }
        return cikti;
    }
}
