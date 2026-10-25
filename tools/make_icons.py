"""Membuat ikon & splash kelinci (ramah anak) dan menimpa ikon default Capacitor.
Jalankan SETELAH `npx cap add android`. Pratinjau: python tools/make_icons.py --preview icon.png"""
import glob, math, os, sys
from PIL import Image, ImageDraw

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = os.path.join(ROOT, "android", "app", "src", "main", "res")

def bunny(S, k):
    F = 4; N = S * F; u = N / 108
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    T = lambda x, y: (N / 2 + (x - 54) * k * u, N / 2 + (y - 54) * k * u)
    def E(x0, y0, x1, y1, c):
        a, b = T(x0, y0), T(x1, y1); d.ellipse([a[0], a[1], b[0], b[1]], fill=c)
    W, P, K = (255, 255, 255, 255), (255, 170, 190, 255), (43, 45, 66, 255)
    for dx in (0, 18):                                   # telinga
        E(40 + dx, 10, 51 + dx, 46, W); E(43 + dx, 16, 48 + dx, 42, P)
    E(29, 38, 79, 88, W)                                 # wajah
    E(34, 64, 43, 71, (255, 190, 205, 255)); E(65, 64, 74, 71, (255, 190, 205, 255))  # pipi
    for x in (45, 63):                                   # mata
        E(x - 4, 55, x + 4, 64, K); E(x - 1.5, 56, x + 1.5, 59, W)
    E(51, 65, 57, 70, (255, 111, 163, 255))              # hidung
    for x0 in (47, 54):                                  # senyum
        a, b = T(x0, 66), T(x0 + 7, 76); d.arc([a[0], a[1], b[0], b[1]], 0, 180, fill=K, width=max(2, int(1.3 * k * u)))
    pts = [T(84 + (7 if i % 2 == 0 else 3) * math.cos(-math.pi / 2 + i * math.pi / 5),
             22 + (7 if i % 2 == 0 else 3) * math.sin(-math.pi / 2 + i * math.pi / 5)) for i in range(10)]
    d.polygon(pts, fill=(255, 201, 60, 255))             # bintang
    return im.resize((S, S), Image.LANCZOS)

def tile(S, shape):
    g = Image.new("RGBA", (S, S))
    gd = ImageDraw.Draw(g)
    for y in range(S):
        t = y / S; gd.line([(0, y), (S, y)], fill=(int(47 + 80 * t), int(163 + 45 * t), int(107 + 45 * t), 255))
    g.alpha_composite(bunny(S, 1.0))
    m = Image.new("L", (S, S), 0); md = ImageDraw.Draw(m)
    if shape == "round": md.ellipse([0, 0, S - 1, S - 1], fill=255)
    else: md.rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * .22), fill=255)
    g.putalpha(m); return g

if "--preview" in sys.argv:
    tile(512, "sq").save(sys.argv[sys.argv.index("--preview") + 1]); sys.exit()

D = {"mdpi": (48, 108), "hdpi": (72, 162), "xhdpi": (96, 216), "xxhdpi": (144, 324), "xxxhdpi": (192, 432)}
for n, (s, f) in D.items():
    p = os.path.join(RES, "mipmap-" + n); os.makedirs(p, exist_ok=True)
    tile(s, "sq").save(os.path.join(p, "ic_launcher.png"))
    tile(s, "round").save(os.path.join(p, "ic_launcher_round.png"))
    bunny(f, 0.72).save(os.path.join(p, "ic_fg.png"))
a = os.path.join(RES, "mipmap-anydpi-v26"); os.makedirs(a, exist_ok=True)
xml = '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n<background android:drawable="@color/ic_bg"/>\n<foreground android:drawable="@mipmap/ic_fg"/>\n</adaptive-icon>\n'
for n in ("ic_launcher.xml", "ic_launcher_round.xml"): open(os.path.join(a, n), "w").write(xml)
open(os.path.join(RES, "values", "ic_bg.xml"), "w").write('<?xml version="1.0" encoding="utf-8"?>\n<resources><color name="ic_bg">#2FA36B</color></resources>\n')
for p in glob.glob(os.path.join(RES, "drawable*", "splash.png")):   # splash bawaan -> kelinci
    w, h = Image.open(p).size; im = Image.new("RGBA", (w, h), (214, 241, 228, 255)); m = int(min(w, h) * .4)
    im.alpha_composite(bunny(m, 0.95), ((w - m) // 2, (h - m) // 2)); im.convert("RGB").save(p)
print("ikon & splash dibuat")
