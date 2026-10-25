# KİLİT EKRANI BİLGİ KARTI (Bzk.Kilit) — Yasin HAN

Telefonun kilit ekranına / ana ekranına; seçtiğiniz resmin üzerine
ad soyad, telefon, yedek telefon, adres ve not yazılı bir bilgi kutusu
yerleştirir. Telefon kaybolursa bulan kişi kilidi açmadan sizi arayabilir.

## Özellikler
- Galeriden resim seçme (resim yoksa lacivert düz renk arka plan)
- Başlık, Ad Soyad, Telefon, Yedek telefon, Adres, Ek not
- Kutunun yeri (Üst / Orta / Alt), yazı boyutu, kutu koyuluğu,
  yazı rengi ve kutu rengi seçimi — canlı önizleme
- Tek tuşla: Kilit ekranına / Ana ekrana / İkisine birden uygula
- Galeriye PNG olarak kaydet (Resimler/KilitEkrani)
- "Telefonun kendi ayarıyla uygula" (Xiaomi/Huawei gibi kilit ekranını
  uygulamalara kapatan telefonlar için)
- Bilgiler otomatik saklanır, uygulama tekrar açıldığında hazır gelir
- Android 8.0 ve üzeri

## APK nasıl alınır (GitHub Actions)
1. GitHub'da yeni bir depo açın: KILIT_EKRANI_BILGI_YHAN
2. Bu klasördeki TÜM dosyaları (gizli .github klasörü dahil) depoya yükleyin.
3. Actions sekmesi → "APK Olustur" → çalışmasını bekleyin (~5 dk).
4. Bitince alttaki "Artifacts" bölümünden Bzk_Kilit_Ekrani_APK'yı indirin,
   içindeki Bzk_Kilit_Ekrani_v1.0.apk dosyasını telefona kurun.

Sabit imza anahtarı (app/yhan.jks) kullanıldığı için yeni sürümler eski
sürümün üzerine sorunsuz kurulur.
