# Mimo English – Belajar Inggris untuk Anak SD

Aplikasi Android native (Kotlin + Jetpack Compose). Maskot **Mimo si Kucing**.

## Build APK lewat GitHub
1. Buat repo **publik** di GitHub, upload seluruh isi folder ini, branch `main`.
2. Buka tab **Actions** → workflow **Build APK** berjalan otomatis (atau klik *Run workflow*).
3. Setelah hijau, unduh APK dari bagian **Artifacts** (`MimoEnglish-debug-apk`), lalu install di HP.

## Menambah / memperbarui materi (tanpa update APK)
Semua materi ada di folder `content/`. Format satu paket (`content/packs/xxx.json`):

    {"id":"animals","title":"Hewan","emoji":"🐾","kind":"word",
     "items":[{"en":"cat","id":"kucing","e":"🐱","l":1,"ex":"The cat is sleeping."}]}

- `kind`: `word` (kata) atau `phrase` (kalimat)
- `l`: level 1–5 (materi baru muncul saat level anak naik)
- `ex`: contoh kalimat (opsional)
- Paket baru: tambahkan nama file ke `content/index.json` dan **naikkan `version`**.
- Commit ke `main` → aplikasi mengunduh materi baru saat dibuka (butuh internet).

## Teman AI (Mimo Chat)
- Tanpa server: Mimo berjalan **offline** (jawaban sederhana dari materi).
- Dengan AI: deploy `backend/worker.js` ke Cloudflare Workers, simpan secret `ANTHROPIC_API_KEY`,
  lalu di GitHub: Settings → Secrets and variables → Actions → **Variables** → buat `AI_PROXY_URL`
  berisi URL worker. Build ulang APK.
- API key tidak pernah disimpan di dalam aplikasi.
