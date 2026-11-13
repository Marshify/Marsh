#!/usr/bin/env python3
"""Renders mockups of the HUD and menus from the built pack textures (preview/*.png)."""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = os.environ.get("MODID", "marsh")
G = os.path.join(ROOT, "pack/assets/%s/textures/gui/" % MODID)
V = os.path.join(ROOT, "pack/assets/minecraft/textures/gui/")
OUT = os.path.join(ROOT, "preview")
S = 4  # gui scale for the preview
FONT = "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf"


def tex(path):
    return Image.open(path).convert("RGBA")


def crop(img, x, y, w, h):
    return img.crop((x, y, x + w, y + h))


def up(img):
    return img.resize((img.width * S, img.height * S), Image.NEAREST)


def stretch(sprite, w, edge=4):
    """Horizontal 3-slice so buttons/rows keep their rounded ends at any width."""
    h = sprite.height
    out = Image.new("RGBA", (w, h))
    out.paste(sprite.crop((0, 0, edge, h)), (0, 0))
    mid = sprite.crop((edge, 0, sprite.width - edge, h)).resize((w - 2 * edge, h), Image.NEAREST)
    out.paste(mid, (edge, 0))
    out.paste(sprite.crop((sprite.width - edge, 0, sprite.width, h)), (w - edge, 0))
    return out


def put(dst, sprite, x, y):
    s = up(sprite)
    dst.alpha_composite(s, (x * S, y * S))


def text(dst, x, y, s, size=9, fill=(255, 255, 255), outline=True, anchor="la"):
    d = ImageDraw.Draw(dst)
    f = ImageFont.truetype(FONT, size * S)
    d.text((x * S, y * S), s, font=f, fill=fill, anchor=anchor,
           stroke_width=(S if outline else 0), stroke_fill=(10, 10, 10))


def bg(w, h):
    img = Image.new("RGBA", (w * S, h * S), (70, 110, 90, 255))
    d = ImageDraw.Draw(img)
    for k in range(0, w * S, 24 * S):  # fake world backdrop
        d.rectangle((k, 0, k + 12 * S, h * S), fill=(78, 120, 98, 255))
    return img


def stats():
    win, btn, rows = tex(G + "window.png"), tex(G + "buttons.png"), tex(G + "rows.png")
    img = bg(256, 214)
    put(img, crop(win, 0, 0, 240, 180), 8, 8)
    text(img, 128, 20, "Stats", 13, anchor="mm")
    put(img, crop(btn, 48, 144, 20, 20), 200, 10)
    text(img, 210, 20, "?", 10, anchor="mm")
    put(img, crop(btn, 0, 144, 20, 20), 222, 10)
    names = [("Melee", (255, 130, 120), "Lv. 3000 (MAX)", 1), ("Defense", (120, 180, 255), "Lv. 3000 (MAX)", 1),
             ("Sword", (160, 240, 110), "Lv. 1", 0), ("Gun", (255, 210, 90), "Lv. 1", 0),
             ("Blox Fruit", (220, 160, 255), "Lv. 3000 (MAX)", 1)]
    text(img, 14, 35, "Race: Cyborg V4", 7, outline=False, fill=(235, 235, 235))
    for k, (n, col, lv, maxed) in enumerate(names):
        y = 44 + k * 26
        put(img, crop(rows, 0, (k + 1) * 30, 220, 24).resize((220, 24)), 18, y)
        text(img, 26, y + 12, n, 9, fill=col, anchor="lm")
        text(img, 168, y + 12, lv, 8, anchor="rm")
        put(img, stretch(crop(btn, 0, 24 if maxed else 0, 60, 20), 44), 172, y + 2)
        text(img, 194, y + 12, "+1", 8, anchor="mm", fill=(255, 255, 255))
    put(img, crop(win, 0, 186, 240, 18), 8, 190)
    text(img, 14, 199, "Stat Points: 0", 8, anchor="lm", outline=False)
    put(img, stretch(crop(btn, 0, 0, 60, 20), 50), 190, 191)
    text(img, 215, 200, "Refund", 7, anchor="mm")
    return img


def settings():
    win, btn, rows = tex(G + "window.png"), tex(G + "buttons.png"), tex(G + "rows.png")
    img = bg(256, 200)
    put(img, crop(win, 0, 0, 240, 180), 8, 8)
    text(img, 128, 20, "Settings", 13, anchor="mm")
    put(img, crop(btn, 0, 144, 20, 20), 222, 10)
    items = [("Background Music", 0), ("Ally VFX", 1), ("Camera Shake", 0), ("Damage Counter", 1)]
    for k, (n, on) in enumerate(items):
        y = 38 + k * 30
        put(img, crop(rows, 0, 0, 220, 28), 18, y)
        text(img, 24, y + 14, n, 8, anchor="lm", outline=False)
        put(img, stretch(crop(btn, 0, 48 if on else 24, 60, 20), 40), 150, y + 4)
        text(img, 170, y + 14, "On", 8, anchor="mm")
        put(img, stretch(crop(btn, 0, 24 if on else 72, 60, 20), 40), 194, y + 4)
        text(img, 214, y + 14, "Off", 8, anchor="mm")
    return img


def hud():
    bars, hb, ab = tex(G + "hud_bars.png"), tex(G + "hud_buttons.png"), tex(G + "hud_abilities.png")
    img = bg(240, 220)
    for k, n in enumerate(["Stats", "Items", "Close", "Shop"]):
        x, y = 8 + (k % 2) * 88, 8 + (k // 2) * 32
        put(img, crop(hb, 0, 32 if k == 1 else 0, 84, 28), x, y)
        text(img, x + 42, y + 14, n, 12, anchor="mm", fill=(20, 20, 20), outline=False)
    put(img, crop(hb, 96, 0, 16, 16), 166, 2)
    text(img, 174, 10, "99+", 5, anchor="mm")
    text(img, 8, 84, "$ 11,873,257", 15, fill=(120, 255, 90))
    text(img, 8, 104, "Lv. 3000", 15)
    put(img, crop(bars, 0, 80, 180, 8), 8, 126)
    text(img, 98, 130, "Max Level", 5, anchor="mm")
    put(img, crop(bars, 0, 24, 180, 22), 8, 138)
    text(img, 14, 149, "Health", 11, anchor="lm")
    text(img, 182, 149, "15595/15595", 8, anchor="rm")
    put(img, crop(bars, 0, 48, 180, 22), 8, 164)
    text(img, 14, 175, "Energy", 11, anchor="lm")
    text(img, 182, 175, "16195/16195", 8, anchor="rm")
    for k, key in enumerate("ZXCVF"):
        put(img, crop(ab, k * 34, 34 if k == 0 else (68 if k == 3 else 0), 32, 32) if k != 3 else
            crop(ab, k * 34, 0, 32, 32), 8 + k * 36, 192 - 4)
        if k == 3:
            put(img, crop(ab, k * 34, 68, 32, 32), 8 + k * 36, 188)
            text(img, 24 + k * 36, 204, "4s", 9, anchor="mm")
        put(img, crop(ab, k * 34 + 9, 104, 14, 10), 17 + k * 36, 210)
        text(img, 24 + k * 36, 215, key, 6, anchor="mm")
    return img


def vanilla():
    w, ic = tex(V + "widgets.png"), tex(V + "icons.png")
    img = bg(200, 96)
    y = 66
    put(img, crop(w, 0, 0, 182, 22), 9, y)
    put(img, crop(w, 0, 22, 24, 24), 9 - 1 + 2 * 20, y - 1)
    put(img, crop(ic, 0, 64, 182, 5), 9, y - 7)
    put(img, crop(ic, 0, 69, 120, 5), 9, y - 7)
    for k in range(10):
        put(img, crop(ic, 16, 0, 9, 9), 9 + k * 8, y - 18)
        put(img, crop(ic, 52 if k < 7 else (61 if k == 7 else 16), 0, 9, 9), 9 + k * 8, y - 18)
        put(img, crop(ic, 16, 27, 9, 9), 182 + 9 - 9 - k * 8, y - 18)
        put(img, crop(ic, 52 if k < 6 else 16, 27, 9, 9), 182 + 9 - 9 - k * 8, y - 18)
        put(img, crop(ic, 34 if k < 5 else 16, 9, 9, 9), 9 + k * 8, y - 28)
    put(img, stretch(crop(w, 0, 66, 200, 20), 90), 6, 8)
    put(img, stretch(crop(w, 0, 86, 200, 20), 90), 104, 8)
    text(img, 51, 18, "Done", 7, anchor="mm", outline=False)
    text(img, 149, 18, "Hovered", 7, anchor="mm", outline=False)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    shots = {"stats": stats(), "settings": settings(), "hud": hud(), "vanilla": vanilla()}
    for n, im in shots.items():
        im.save(os.path.join(OUT, n + ".png"))
    # contact sheet
    W = sum(i.width for i in shots.values()) + 20 * S * 5
    H = max(i.height for i in shots.values()) + 40 * S
    sheet = Image.new("RGBA", (W, H), (24, 24, 26, 255))
    x = 20 * S
    for im in shots.values():
        sheet.alpha_composite(im, (x, 20 * S)); x += im.width + 20 * S
    sheet = sheet.resize((sheet.width // 2, sheet.height // 2), Image.LANCZOS)
    sheet.save(os.path.join(OUT, "overview.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
