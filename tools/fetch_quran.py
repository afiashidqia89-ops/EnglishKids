"""Dijalankan GitHub Actions saat build: mengunduh teks (Utsmani), terjemah Indonesia, transliterasi,
dan audio per ayat Juz 30 dari sumber publik, lalu menulis www/quran.js + www/audio/*.mp3."""
import json, os, re, sys, time, urllib.request
from concurrent.futures import ThreadPoolExecutor

W = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "www"))
AUD = os.path.join(W, "audio"); os.makedirs(AUD, exist_ok=True)
DIR = os.environ.get("AUDIO_DIR", "Alafasy_64kbps")
CNT = dict(zip(range(78, 115), [40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6]))
BIS = ["بسم", "الله", "الرحمن", "الرحيم"]

def get(u, n=4):
    err = None
    for i in range(n):
        try:
            return urllib.request.urlopen(urllib.request.Request(u, headers={"User-Agent": "Mozilla/5.0"}), timeout=60).read()
        except Exception as e:
            err = e; time.sleep(2 * (i + 1))
    raise err

def norm(w):
    return re.sub("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]", "", w).replace("\u0671", "\u0627")

def audio(code):
    f = os.path.join(AUD, code + ".mp3")
    if os.path.exists(f) and os.path.getsize(f) > 1000: return True
    try:
        b = get(f"https://everyayah.com/data/{DIR}/{code}.mp3")
        if len(b) < 1000: return False
        open(f, "wb").write(b); return True
    except Exception as e:
        print("audio gagal", code, e); return False

out = {}
for n in range(78, 115):
    d = json.loads(get(f"https://api.alquran.cloud/v1/surah/{n}/editions/quran-uthmani,id.indonesian,en.transliteration"))["data"]
    by = {e["edition"]["identifier"]: e["ayahs"] for e in d}
    ar, idn, tr = by["quran-uthmani"], by["id.indonesian"], by["en.transliteration"]
    if not (len(ar) == len(idn) == len(tr) == CNT[n]):
        sys.exit(f"Jumlah ayat surah {n} tidak cocok ({len(ar)} != {CNT[n]})")
    rows = []
    for k in range(len(ar)):
        t = ar[k]["text"]
        if k == 0:
            ws = t.split(" ")
            if [norm(x) for x in ws[:4]] == BIS: t = " ".join(ws[4:])
        rows.append({"t": t, "i": idn[k]["text"], "l": tr[k]["text"]})
    out[n] = rows
    print("teks", n, len(rows))

codes = [(n, k + 1, f"{n:03d}{k+1:03d}") for n in out for k in range(len(out[n]))]
with ThreadPoolExecutor(8) as ex:
    res = list(ex.map(lambda c: audio(c[2]), codes))
for (n, a, _), ok in zip(codes, res):
    if ok: out[n][a - 1]["a"] = 1
b = audio("001001")
open(os.path.join(W, "quran.js"), "w", encoding="utf-8").write("const QURAN=" + json.dumps({"s": out, "b": 1 if b else 0}, ensure_ascii=False) + ";")
print("audio lokal:", sum(res), "dari", len(res))
