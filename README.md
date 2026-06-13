# QR Alarm — Android Uygulaması

QR/Barkod okutmadan kapanmayan bir alarm uygulaması. Alarm sesi dışarıdan (telefon hafızasından) eklenebilir, alarmı kapatmanın **tek yolu** önceden kullanıcının kendi tanımladığı QR kodu veya barkodu okutmaktır.

## ⚠️ Samsung S23 / One UI 6 — Kurulumdan Sonra Mutlaka Yapın

One UI, pil tasarrufu için arka planda zamanlanan alarmları geciktirebilir veya hiç çalıştırmayabilir. Uygulamayı ilk açtığınızda **2 izin isteği** göreceksiniz, ikisini de onaylayın:

1. **"Alarm ve Hatırlatıcılar"** (Tam zamanlı alarm izni) → **İzin Ver**
2. **"Pil optimizasyonundan muaf tut"** → **İzin Ver**

Ayrıca elle de kontrol edebilirsiniz:
- **Ayarlar → Uygulamalar → QR Alarm → Pil → "Sınırsız"** (Unrestricted) seçin
- **Ayarlar → Uygulamalar → QR Alarm → Bildirimler** → açık olduğundan emin olun
- Telefonu yeniden başlattıktan sonra QR Alarm uygulamasını **bir kere açıp kapatın** (alarmların yeniden kurulduğunu garantilemek için)

> Not: Uygulama, alarm saatine kadar **hiçbir arka plan işlemi çalıştırmaz** — sistemin `AlarmManager`'ı (Android'in kendi alarm/saat mekanizması) tarafından, sadece ayarlanan saat geldiğinde uyandırılır. Bu sayede pil tüketimi olmaz; yukarıdaki izinler sadece "o anda" uyandırmanın One UI tarafından engellenmemesi içindir.

## Bu Sürümde Yapılan Güncellemeler

- **Kilitlenme / kendini tekrar tekrar başlatma sorunu giderildi**: Alarm tetiklendiğinde artık tek bir servis (AlarmService) tüm akışı yönetiyor; aynı alarm için gelen tekrarlı tetiklemeler artık görmezden geliniyor.
- **Kademeli ses sistemi**: Alarm çalmaya başladığında ses kısıktan (≈%8) başlayıp 2 dakika içinde tam sese (%100) kadar yükselir. 2 dakika içinde alarm kapatılmazsa **farklı bir alarm sesine** geçilir ve aynı kademeli artış tekrar başlar. Bu işlem toplam **3 farklı ses / 3 tur** boyunca tekrarlanır (6 dakika). 3. tur da bitip alarm kapatılmazsa, alarm kendiliğinden susar.
- **Titreşim kaldırıldı**: İstek üzerine titreşim tamamen devre dışı bırakıldı.
- **Tam ekran parlaklığı**: Alarm çalarken ekran, telefonun mevcut parlaklık ayarından bağımsız olarak **%100 parlaklığa** geçer. Alarm kapatıldığında (QR okutulduğunda) ekran otomatik olarak telefonun normal (manuel/otomatik) parlaklık ayarına geri döner.
- **Çalan alarm silinemez / kapatılamaz**: Bir alarm çalmaya başladıktan sonra, listeden o alarmı silmeye veya anahtarla (switch) kapatmaya çalışırsanız uyarı verir ve işlemi engeller — alarm yalnızca doğru QR/Barkod okutularak durdurulabilir.
- **Pil optimizasyonu istisnası**: Uygulama açılışında, Samsung'un agresif pil yönetiminden muaf tutulma izni otomatik olarak isteniyor (yukarıdaki bölüme bakın).
- **Bauhaus esintili modern tasarım**: Krem/beyaz arka plan, lacivert + kırmızı + sarı vurgu renkleri, geometrik ama yumuşatılmış (büyük köşe yarıçaplı) kartlar ve butonlar.

---

## Özellikler

- ⏰ Birden fazla alarm, saat/dakika seçimi
- 🔁 Tekrarlayan günler (Pzt–Paz) veya tek seferlik alarm
- 🎵 Özel alarm sesi: sistem zil sesleri veya telefondaki herhangi bir MP3/ses dosyası
- 📳 Titreşim açma/kapama
- 📷 **QR kod / Barkod ile kapatma**: Kullanıcı, kapatma kodunu kendi telefon kamerasıyla tarayarak veya elle girerek alarma kayıt eder. Alarm çaldığında SADECE bu kod tekrar okutulursa kapanır.
- 🔒 Alarm ekranı geri tuşu ile kapanmaz, kilit ekranının üzerinde açılır
- 🔄 Telefon yeniden başlatıldığında alarmlar otomatik yeniden kurulur (BOOT_COMPLETED)

## Bu Proje Nedir?

Bu klasör, eksiksiz bir **Android Studio (Gradle) projesidir**. Native Java + Android SDK ile yazılmıştır (ZXing kütüphanesi ile QR/Barkod tarama, Room ile yerel veritabanı, AlarmManager + Foreground Service ile güvenilir alarm tetikleme).

Bu ortamda Android SDK / Gradle bulunmadığı için APK doğrudan burada derlenemedi. Aşağıdaki **2 yöntemden biriyle** APK'yı kolayca kendiniz oluşturabilirsiniz.

---

## YÖNTEM 1 — Android Studio ile (Önerilen, En Kolay)

1. [Android Studio](https://developer.android.com/studio) indirip kurun (ücretsiz).
2. Android Studio'yu açın → **"Open"** → bu `QRAlarm` klasörünü seçin.
3. Gradle senkronizasyonu otomatik başlayacak (ilk açılışta internet gerekir, bağımlılıklar indirilir — birkaç dakika sürebilir).
4. Üst menüden **Build → Build Bundle(s) / APK(s) → Build APK(s)** seçin.
5. Build bitince çıkan bildirimden **"locate"** linkine tıklayın → `app/build/outputs/apk/debug/app-debug.apk` dosyasını bulun.
6. Bu `.apk` dosyasını telefonunuza (USB, e-posta, Drive vb. ile) aktarın ve kurun.
   - Telefonda **Ayarlar → Güvenlik → Bilinmeyen kaynaklardan yükleme** iznini açmanız gerekebilir.

> Gerçek cihazda test etmek isterseniz: telefonu USB ile bağlayıp **Run ▶** butonuna basarak da doğrudan kurabilirsiniz.

---

## YÖNTEM 2 — GitHub Actions ile Otomatik APK (Bilgisayara bir şey kurmadan)

Bu proje, push edildiğinde otomatik APK üreten bir GitHub Actions iş akışı (`.github/workflows/build.yml`) içerir.

1. [github.com](https://github.com) üzerinde yeni, **boş** bir repo oluşturun (örn. `qr-alarm`).
2. Bu `QRAlarm` klasörünün tüm içeriğini o repoya yükleyin (push edin).
3. GitHub'da repo sayfasında **Actions** sekmesine gidin → "Build APK" iş akışının çalıştığını göreceksiniz (birkaç dakika sürer).
4. Tamamlandığında, ilgili çalıştırmanın (run) altındaki **Artifacts** bölümünden `QRAlarm-debug-apk` dosyasını indirin — içinde `app-debug.apk` bulunur.
5. APK'yı telefonunuza aktarıp kurun.

---

## Kullanım (Uygulama İçinde)

1. **+ butonuna** dokunarak yeni alarm ekleyin.
2. Saat seçin, isteğe bağlı etiket yazın, tekrar günlerini işaretleyin.
3. **"Ses Seç"** ile alarm sesini değiştirin (sistem sesleri veya telefonunuzdaki bir ses dosyası).
4. **"QR / Barkod Ayarla"** bölümüne dokunun:
   - **"Tarayarak Ayarla"** ile kamerayı kullanarak istediğiniz bir QR kodu/barkodu (örn. bir ürün kutusu, kendi bastırdığınız bir QR kod) tarayın — bu kod, alarmı kapatmak için gereken kod olarak kaydedilir.
   - Veya kod değerini elle yazabilirsiniz.
5. **Kaydet**'e basın.
6. Alarm çaldığında ekranda **"QR / BARKOD TARA"** butonu çıkar. Kullanıcı, kayıtlı kodu tekrar okutmadan alarm **kapanmaz** ve geri tuşu çalışmaz.

> ⚠️ Önemli: Android 12+ (API 31+) cihazlarda ilk açılışta "Alarm ve Hatırlatıcılar" iznini vermeniz istenecektir — bu izin, alarmın tam zamanında çalması için gereklidir.

---

## Proje Yapısı

```
QRAlarm/
├── app/
│   ├── build.gradle              # Bağımlılıklar (ZXing, Room, WorkManager...)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/qralarm/app/
│       │   ├── MainActivity.java         # Alarm listesi
│       │   ├── AddAlarmActivity.java     # Alarm ekle/düzenle
│       │   ├── QRSetupActivity.java      # QR/Barkod tarama & ayarlama
│       │   ├── AlarmRingActivity.java    # Alarm çalma ekranı (QR ile kapatma)
│       │   ├── AlarmReceiver.java        # Zamanı gelince tetiklenir
│       │   ├── AlarmService.java         # Sesi çalan foreground servis
│       │   ├── AlarmScheduler.java       # AlarmManager ile zamanlama
│       │   ├── Alarm.java / AlarmDao.java / AlarmDatabase.java  # Room DB
│       │   ├── AlarmRepository.java / AlarmViewModel.java
│       │   └── AlarmAdapter.java         # Liste görünümü
│       └── res/                          # Layout, string, ikon, tema dosyaları
└── .github/workflows/build.yml   # Otomatik APK derleme (GitHub Actions)
```

## Kullanılan Kütüphaneler

- **ZXing (journeyapps embedded)** — QR kod & barkod tarama (kamera erişimi, internet gerektirmez)
- **Room** — alarmların yerel veritabanında saklanması
- **AndroidX / Material 3** — modern koyu tema arayüz

## Notlar / Geliştirme Önerileri

- Uygulama paket adı: `com.qralarm.app` (`app/build.gradle` içinde `applicationId` ile değiştirilebilir)
- minSdk 21 (Android 5.0+), targetSdk 34
- Yayınlamak için imzalı (release) APK/AAB oluşturmanız gerekir — Android Studio'da **Build → Generate Signed Bundle/APK** menüsünü kullanın.
