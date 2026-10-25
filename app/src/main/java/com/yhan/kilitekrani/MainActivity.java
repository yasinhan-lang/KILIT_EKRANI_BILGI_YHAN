package com.yhan.kilitekrani;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final int REQ_RESIM = 11;
    private static final int REQ_IZIN = 12;
    private static final String VARSAYILAN_BASLIK = "BU TELEFON BULUNURSA LÜTFEN ARAYINIZ";

    private EditText etBaslik, etAd, etTel1, etTel2, etAdres, etNot;
    private RadioGroup rgKonum, rgYazi, rgKutu;
    private SeekBar sbBoyut, sbOpak;
    private ImageView preview;
    private TextView tvDurum;
    private SharedPreferences sp;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable onizlemeRun = this::onizlemeGuncelle;
    private Bitmap onizlemeArka;
    private int ekranW, ekranH;
    private Runnable bekleyenIs;
    private boolean yukleniyor = true;
    private volatile boolean mesgul = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etBaslik = findViewById(R.id.etBaslik);
        etAd = findViewById(R.id.etAd);
        etTel1 = findViewById(R.id.etTel1);
        etTel2 = findViewById(R.id.etTel2);
        etAdres = findViewById(R.id.etAdres);
        etNot = findViewById(R.id.etNot);
        rgKonum = findViewById(R.id.rgKonum);
        rgYazi = findViewById(R.id.rgYazi);
        rgKutu = findViewById(R.id.rgKutu);
        sbBoyut = findViewById(R.id.sbBoyut);
        sbOpak = findViewById(R.id.sbOpak);
        preview = findViewById(R.id.preview);
        tvDurum = findViewById(R.id.tvDurum);
        sp = getSharedPreferences("ayarlar", MODE_PRIVATE);

        ekranOlc();
        ayarlariYukle();
        onizlemeArka = Kompozitor.dosyadanYukle(arkaDosya(), ekranW / 3, ekranH / 3);

        TextWatcher izleyici = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { onizlemePlanla(); }
        };
        for (EditText et : new EditText[]{etBaslik, etAd, etTel1, etTel2, etAdres, etNot})
            et.addTextChangedListener(izleyici);

        RadioGroup.OnCheckedChangeListener secimDinle = (g, id) -> onizlemePlanla();
        rgKonum.setOnCheckedChangeListener(secimDinle);
        rgYazi.setOnCheckedChangeListener(secimDinle);
        rgKutu.setOnCheckedChangeListener(secimDinle);

        SeekBar.OnSeekBarChangeListener kaydirDinle = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int v, boolean u) { onizlemePlanla(); }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        };
        sbBoyut.setOnSeekBarChangeListener(kaydirDinle);
        sbOpak.setOnSeekBarChangeListener(kaydirDinle);

        Button btnResim = findViewById(R.id.btnResim);
        btnResim.setOnClickListener(v -> resimSec());
        findViewById(R.id.btnResimKaldir).setOnClickListener(v -> {
            File f = arkaDosya();
            if (f.exists()) //noinspection ResultOfMethodCallIgnored
                f.delete();
            onizlemeArka = null;
            onizlemeGuncelle();
            durum("Resim kaldırıldı, düz renk arka plan kullanılacak.");
        });
        findViewById(R.id.btnKilit).setOnClickListener(v ->
                uygula(WallpaperManager.FLAG_LOCK, "Kilit ekranına"));
        findViewById(R.id.btnAna).setOnClickListener(v ->
                uygula(WallpaperManager.FLAG_SYSTEM, "Ana ekrana"));
        findViewById(R.id.btnIkisi).setOnClickListener(v ->
                uygula(WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM, "Kilit ve ana ekrana"));
        findViewById(R.id.btnKaydet).setOnClickListener(v -> galeriyeKaydet(false));
        findViewById(R.id.btnSistem).setOnClickListener(v -> galeriyeKaydet(true));

        yukleniyor = false;
        onizlemeGuncelle();
    }

    // ---------------------------------------------------------------- yardımcılar

    private void ekranOlc() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        ekranW = Math.min(dm.widthPixels, dm.heightPixels);
        ekranH = Math.max(dm.widthPixels, dm.heightPixels);
        if (ekranW <= 0) { ekranW = 1080; ekranH = 2400; }
    }

    private File arkaDosya() {
        return new File(getFilesDir(), "arka_plan.img");
    }

    private void durum(String s) {
        tvDurum.setText(s);
    }

    private static int secili(RadioGroup g) {
        int id = g.getCheckedRadioButtonId();
        for (int i = 0; i < g.getChildCount(); i++)
            if (g.getChildAt(i).getId() == id) return i;
        return 0;
    }

    private static void sec(RadioGroup g, int i) {
        if (i < 0 || i >= g.getChildCount()) i = 0;
        View v = g.getChildAt(i);
        if (v instanceof RadioButton) ((RadioButton) v).setChecked(true);
    }

    private Kompozitor.Ayar ayarOku() {
        Kompozitor.Ayar a = new Kompozitor.Ayar();
        a.baslik = etBaslik.getText().toString();
        a.ad = etAd.getText().toString();
        a.tel1 = etTel1.getText().toString();
        a.tel2 = etTel2.getText().toString();
        a.adres = etAdres.getText().toString();
        a.not = etNot.getText().toString();
        a.konum = secili(rgKonum);
        a.yaziRenk = secili(rgYazi);
        a.kutuRenk = secili(rgKutu);
        a.boyut = sbBoyut.getProgress();
        a.opak = sbOpak.getProgress();
        return a;
    }

    private void ayarlariYukle() {
        etBaslik.setText(sp.getString("baslik", VARSAYILAN_BASLIK));
        etAd.setText(sp.getString("ad", ""));
        etTel1.setText(sp.getString("tel1", ""));
        etTel2.setText(sp.getString("tel2", ""));
        etAdres.setText(sp.getString("adres", ""));
        etNot.setText(sp.getString("not", ""));
        sec(rgKonum, sp.getInt("konum", 2));
        sec(rgYazi, sp.getInt("yazi", 0));
        sec(rgKutu, sp.getInt("kutu", 0));
        sbBoyut.setProgress(sp.getInt("boyut", 40));
        sbOpak.setProgress(sp.getInt("opak", 65));
    }

    private void ayarlariKaydet(Kompozitor.Ayar a) {
        sp.edit()
                .putString("baslik", a.baslik)
                .putString("ad", a.ad)
                .putString("tel1", a.tel1)
                .putString("tel2", a.tel2)
                .putString("adres", a.adres)
                .putString("not", a.not)
                .putInt("konum", a.konum)
                .putInt("yazi", a.yaziRenk)
                .putInt("kutu", a.kutuRenk)
                .putInt("boyut", a.boyut)
                .putInt("opak", a.opak)
                .apply();
    }

    // ---------------------------------------------------------------- önizleme

    private void onizlemePlanla() {
        if (yukleniyor) return;
        handler.removeCallbacks(onizlemeRun);
        handler.postDelayed(onizlemeRun, 200);
    }

    private void onizlemeGuncelle() {
        Kompozitor.Ayar a = ayarOku();
        ayarlariKaydet(a);
        try {
            Bitmap b = Kompozitor.olustur(onizlemeArka, a, ekranW / 3, ekranH / 3);
            preview.setImageBitmap(b);
        } catch (Throwable t) {
            durum("Önizleme hatası: " + t.getMessage());
        }
    }

    // ---------------------------------------------------------------- resim seçme

    private void resimSec() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Resim seçin"), REQ_RESIM);
        } catch (Exception e) {
            Toast.makeText(this, "Resim seçici açılamadı", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_RESIM || resultCode != RESULT_OK || data == null || data.getData() == null)
            return;
        final Uri uri = data.getData();
        durum("Resim yükleniyor...");
        new Thread(() -> {
            String hata = null;
            Bitmap kucuk = null;
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(arkaDosya())) {
                if (in == null) throw new IOException("Resim okunamadı");
                byte[] tampon = new byte[64 * 1024];
                int n;
                while ((n = in.read(tampon)) > 0) out.write(tampon, 0, n);
            } catch (Exception e) {
                hata = e.getMessage();
            }
            if (hata == null) {
                kucuk = Kompozitor.dosyadanYukle(arkaDosya(), ekranW / 3, ekranH / 3);
                if (kucuk == null) hata = "Bu dosya resim olarak açılamadı";
            }
            final String h = hata;
            final Bitmap k = kucuk;
            runOnUiThread(() -> {
                if (h != null) {
                    durum("Hata: " + h);
                } else {
                    onizlemeArka = k;
                    onizlemeGuncelle();
                    durum("Resim eklendi ✔");
                }
            });
        }).start();
    }

    // ---------------------------------------------------------------- tam boy görüntü

    private Bitmap tamBoyOlustur(Kompozitor.Ayar a) {
        Bitmap arka = Kompozitor.dosyadanYukle(arkaDosya(), ekranW, ekranH);
        return Kompozitor.olustur(arka, a, ekranW, ekranH);
    }

    private void uygula(final int hangisi, final String ad) {
        if (mesgul) return;
        mesgul = true;
        durum("Hazırlanıyor...");
        final Kompozitor.Ayar a = ayarOku();
        new Thread(() -> {
            String sonuc;
            try {
                Bitmap b = tamBoyOlustur(a);
                WallpaperManager.getInstance(MainActivity.this).setBitmap(b, null, true, hangisi);
                sonuc = ad + " uygulandı ✔";
            } catch (Throwable e) {
                sonuc = "Uygulanamadı: " + e.getMessage();
            }
            final String s = sonuc;
            runOnUiThread(() -> {
                mesgul = false;
                durum(s);
                Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void galeriyeKaydet(final boolean sonraAyarla) {
        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            bekleyenIs = () -> galeriyeKaydet(sonraAyarla);
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_IZIN);
            return;
        }
        if (mesgul) return;
        mesgul = true;
        durum("Kaydediliyor...");
        final Kompozitor.Ayar a = ayarOku();
        new Thread(() -> {
            Uri u = null;
            String hata = null;
            try {
                u = galeriyeYaz(tamBoyOlustur(a));
            } catch (Throwable e) {
                hata = e.getMessage();
            }
            final Uri uri = u;
            final String h = hata;
            runOnUiThread(() -> {
                mesgul = false;
                if (h != null || uri == null) {
                    durum("Kaydedilemedi: " + h);
                    return;
                }
                if (sonraAyarla) {
                    durum("Açılan ekranda 'Kilit ekranı' seçeneğini seçin.");
                    sistemleAyarla(uri);
                } else {
                    durum("Galeriye kaydedildi ✔ (Resimler / KilitEkrani)");
                    Toast.makeText(this, "Galeriye kaydedildi", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private Uri galeriyeYaz(Bitmap b) throws IOException {
        String ad = "kilit_ekrani_" + System.currentTimeMillis() + ".png";
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, ad);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KilitEkrani");
            Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (u == null) throw new IOException("Galeriye yazılamadı");
            try (OutputStream os = getContentResolver().openOutputStream(u)) {
                if (os == null) throw new IOException("Galeriye yazılamadı");
                b.compress(Bitmap.CompressFormat.PNG, 100, os);
            }
            return u;
        } else {
            String s = MediaStore.Images.Media.insertImage(getContentResolver(), b, ad, "Kilit ekranı");
            if (s == null) throw new IOException("Galeriye yazılamadı");
            return Uri.parse(s);
        }
    }

    private void sistemleAyarla(Uri u) {
        Intent i = new Intent(Intent.ACTION_ATTACH_DATA);
        i.setDataAndType(u, "image/*");
        i.putExtra("mimeType", "image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(i, "Duvar kağıdı olarak ayarla"));
        } catch (Exception e) {
            try {
                startActivity(WallpaperManager.getInstance(this).getCropAndSetWallpaperIntent(u));
            } catch (Exception e2) {
                durum("Resim galeriye kaydedildi. Galeriden açıp 'Duvar kağıdı olarak ayarla' deyin.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_IZIN) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (bekleyenIs != null) {
                Runnable r = bekleyenIs;
                bekleyenIs = null;
                r.run();
            }
        } else {
            durum("Galeriye kaydetmek için depolama izni gerekli.");
        }
    }
}
