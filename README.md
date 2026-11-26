# 🏋️‍♂️ IRONPULSE - Offline-First Gym & Routine Tracker

<p align="center">
  <img src="app/res/mipmap-hdpi/ic_launcher.png" alt="IRONPULSE Logo" width="110"/>
</p>

<p align="center">
  <b>Modern Gym Aesthetic (Dark Matte + Neon Orange Glow)</b><br>
  100% Offline Lokal SQLite3 Storage • Zero Data Collection • In-App OTA Sync
</p>

---

## ✨ Fitur Utama

- 🔒 **100% Offline-First (SQLite3 Native)**: Semua data repetisi dan rutinitas tersimpan langsung di database internal smartphone (`ironpulse_gym.db`). Privasi terjaga, tanpa login, dan bisa dipakai di area gym tanpa sinyal.
- ⚡ **OTA (Over-The-Air) Bundle Sync**: Tidak perlu repot uninstall/install ulang APK saat ada pembaruan tampilan atau fitur baru. Cukup tekan tombol sync pembaruan.
- 🎯 **Fleksibel Target Repetisi**: Bisa tambah latihan baru kapan saja (Push Up, Squat, Pull Up, Plank, Dips, dll) serta edit target harian langsung dengan sekali klik.
- 🚀 **Quick Add & Custom Notes**: Tombol cepat `+10`, `+25`, `+50` repetisi sekali sentuh, atau input kustom dengan catatan set.
- 📳 **Haptic Feedback**: Getaran responsif saat repetisi berhasil dicatat ke database.
- 📊 **Statistik & Riwayat**: Menghitung otomatis repetisi hari ini, total all-time, streak hari aktif, dan daftar log lengkap.

---

## 📲 Unduh & Instal APK

Bagi pengguna baru, unduh file APK resmi pada rilis:
- **Download Langsung**: [IRONPULSE-GymTracker.apk](./release/IRONPULSE-GymTracker.apk)
- **Ukuran File**: Sangat ringan (~25 KB)
- **Kompatibilitas**: Android 7.0 (Nougat) ke atas (API 24 - 34+)

---

## 🔄 Cara Kerja OTA Sync (Vibe Coding Friendly)

Aplikasi ini menggunakan arsitektur hybrid modern:
1. **Runtime:** Native Android Activity + Native SQLiteOpenHelper Bridge.
2. **Offline Assets:** Disimpan di internal storage perangkat (`files/www/index.html`).
3. **Pembaruan:** Saat online dan tombol sinkronisasi ditekan, aplikasi memeriksa `version.json` dari repositori ini dan mengunduh `bundle.zip` terbaru tanpa mengubah atau mereset data database latihan Anda.

---

## 🛠️ Build dari Source

```bash
# 1. Compile resource dengan AAPT2
aapt2 compile --dir app/res -o build/res.zip
aapt2 link -I $ANDROID_JAR --manifest app/AndroidManifest.xml -A app/assets -o build/app-unaligned.apk --java build/gen build/res.zip

# 2. Compile Java Source
javac -cp $ANDROID_JAR -d build/obj build/gen/com/ironpulse/gym/R.java app/src/com/ironpulse/gym/MainActivity.java

# 3. Dexing & Signing
d8 --release --min-api 24 --output build/apk build/obj/com/ironpulse/gym/*.class
cd build/apk && zip -u ../app-unaligned.apk classes.dex
zipalign -f -p 4 build/app-unaligned.apk build/app-release.apk
apksigner sign --ks debug.keystore build/app-release.apk
```

---

## 📄 Lisensi
MIT License - Bebas digunakan dan dimodifikasi untuk kebutuhan personal maupun komunitas fitness.
