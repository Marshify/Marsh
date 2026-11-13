#!/usr/bin/env python3
"""Generates the Marsh UI resource pack (Minecraft 1.20.1, pack_format 15).

Style: Blox Fruits menus (yellow header bars, dark charcoal bodies, chunky
two-tone buttons, green/cyan/purple HUD bars) with a glossier, livelier finish.

Everything is drawn procedurally, so the whole look can be retuned from the
PALETTE block.   Run:  python3 tools/build_pack.py
Output: pack/ (unzipped pack), dist/MarshUI.zip, preview/*.png
"""
import json, math, os, shutil, zipfile
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = os.path.join(ROOT, "pack")
DIST = os.path.join(ROOT, "dist")
MODID = os.environ.get("MODID", "marsh")  # your mod id: MODID=yourmod python3 tools/build_pack.py

# ---------------------------------------------------------------- PALETTE
INK      = (14, 12, 10, 255)        # chunky outline
RIM      = (246, 196, 36, 255)      # thin gold rim around windows
# yellow (headers, primary buttons)
Y_TOP, Y_MID, Y_BOT = (255, 246, 140, 255), (255, 222, 40, 255), (246, 178, 8, 255)
Y_SHADE  = (176, 112, 0, 255)
YH_TOP, YH_MID, YH_BOT = (255, 252, 200, 255), (255, 236, 90, 255), (255, 200, 40, 255)  # hovered
# charcoal (window bodies, rows)
C_TOP, C_BOT = (46, 46, 48, 245), (30, 30, 32, 245)
C_LINE   = (72, 72, 76, 255)        # top highlight inside dark things
C_DEEP   = (18, 18, 20, 255)
ROW_TOP, ROW_BOT = (40, 40, 42, 255), (32, 32, 34, 255)
# greys / greens / reds for buttons
G_TOP, G_MID, G_BOT = (226, 226, 226, 255), (186, 186, 186, 255), (150, 150, 150, 255)
ON_TOP, ON_MID, ON_BOT = (120, 236, 130, 255), (60, 200, 84, 255), (36, 160, 60, 255)
OFF_TOP, OFF_MID, OFF_BOT = (255, 140, 140, 255), (244, 76, 76, 255), (204, 44, 50, 255)
BLUE_TOP, BLUE_MID, BLUE_BOT = (140, 200, 255, 255), (52, 140, 236, 255), (30, 96, 200, 255)
# HUD bars
HP  = ((150, 255, 110, 255), (56, 212, 40, 255), (24, 150, 20, 255))
EN  = ((150, 250, 255, 255), (30, 210, 236, 255), (10, 150, 196, 255))
XP  = ((220, 150, 255, 255), (160, 70, 230, 255), (110, 40, 180, 255))
# stat row tints (left side glow, fades into charcoal)
TINTS = {
    "melee":   (190, 70, 60, 255),
    "defense": (50, 110, 210, 255),
    "sword":   (80, 170, 60, 255),
    "gun":     (200, 140, 40, 255),
    "fruit":   (150, 70, 210, 255),
}
# vanilla icon colours
RED  = ((255, 150, 140, 255), (236, 50, 56, 255))
FOOD = ((255, 196, 120, 255), (206, 110, 40, 255))
ARM  = ((240, 246, 255, 255), (150, 168, 196, 255))
POI  = ((196, 236, 90, 255), (104, 150, 30, 255))
WIT  = ((96, 86, 100, 255), (40, 34, 46, 255))
ABS  = ((255, 244, 130, 255), (236, 176, 20, 255))
ICE  = ((210, 244, 255, 255), (110, 180, 230, 255))
AIR  = ((200, 246, 255, 255), (40, 170, 236, 255))
EMPTY = (34, 34, 36, 230)
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)


# ---------------------------------------------------------------- drawing helpers
def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


def mix3(top, mid, bot, t, split=0.45):
    """Two-tone 'Roblox button' gradient: light band on top, solid lower body."""
    if t < split:
        return lerp(top, lerp(top, mid, 0.55), t / split)
    return lerp(mid, bot, (t - split) / (1 - split))


def canvas(w, h):
    return Image.new("RGBA", (w, h), CLEAR)


def rmask(w, h, r):
    def inside(i, j):
        cx = r if i < r else (w - 1 - r if i > w - 1 - r else i)
        cy = r if j < r else (h - 1 - r if j > h - 1 - r else j)
        return (i - cx) ** 2 + (j - cy) ** 2 <= r * r + r * 0.6
    return [[inside(i, j) for i in range(w)] for j in range(h)]


def shape(img, x, y, w, h, fill, r=3, outline=INK, shine=None, shade=None, thick=1):
    """Rounded rect. fill(i, j, w, h) -> colour. shine/shade = 1px inner top/bottom lines."""
    px = img.load()
    m = rmask(w, h, r)

    def edge(i, j, d):
        for dj in range(-d, d + 1):
            for di in range(-d, d + 1):
                if abs(di) + abs(dj) > d:
                    continue
                ii, jj = i + di, j + dj
                if not (0 <= ii < w and 0 <= jj < h and m[jj][ii]):
                    return True
        return False

    for j in range(h):
        for i in range(w):
            if not m[j][i]:
                continue
            if outline and edge(i, j, thick):
                px[x + i, y + j] = outline
            else:
                px[x + i, y + j] = fill(i, j, w, h)
    if outline:
        for i in range(w):
            col = [j for j in range(h) if m[j][i] and px[x + i, y + j] != outline]
            if col and shine and len(col) > 3:
                px[x + i, y + col[0]] = shine
            if col and shade and len(col) > 3:
                px[x + i, y + col[-1]] = shade


def vgrad(top, mid, bot, split=0.45):
    return lambda i, j, w, h: mix3(top, mid, bot, j / max(1, h - 1), split)


def flat(c1, c2):
    return lambda i, j, w, h: lerp(c1, c2, j / max(1, h - 1))


def button(img, x, y, w, h, kind="yellow", r=3):
    pal = {
        "yellow": (Y_TOP, Y_MID, Y_BOT, Y_SHADE),
        "yellow_hot": (YH_TOP, YH_MID, YH_BOT, Y_SHADE),
        "grey": (G_TOP, G_MID, G_BOT, (110, 110, 110, 255)),
        "on": (ON_TOP, ON_MID, ON_BOT, (20, 110, 40, 255)),
        "off": (OFF_TOP, OFF_MID, OFF_BOT, (140, 20, 30, 255)),
        "blue": (BLUE_TOP, BLUE_MID, BLUE_BOT, (20, 60, 150, 255)),
        "dark": ((70, 70, 74, 255), (50, 50, 54, 255), (38, 38, 40, 255), C_DEEP),
        "dark_hot": ((96, 90, 70, 255), (66, 62, 52, 255), (48, 46, 40, 255), C_DEEP),
    }[kind]
    shape(img, x, y, w, h, vgrad(*pal[:3]), r, shine=lerp(pal[0], WHITE, 0.5), shade=pal[3])


def dark(img, x, y, w, h, r=3, rim=False):
    shape(img, x, y, w, h, flat(C_TOP, C_BOT), r, outline=RIM if rim else INK, shine=C_LINE, shade=C_DEEP)


def slot(img, x, y, s=18):
    """Inset slot. Vanilla draws items at +1,+1 inside an 18x18 cell."""
    shape(img, x, y, s, s, flat((26, 26, 28, 255), (40, 40, 44, 255)), 2,
          outline=INK, shine=(12, 12, 14, 255), shade=(80, 80, 86, 255))


def window(img, x, y, w, h, header=16, r=4):
    """Blox Fruits window: gold rim, yellow header band, charcoal body."""
    def fill(i, j, ww, hh):
        if j < header:
            return mix3(Y_TOP, Y_MID, Y_BOT, j / max(1, header - 1), 0.4)
        if j == header:
            return Y_SHADE
        return lerp(C_TOP, C_BOT, (j - header) / max(1, hh - header))
    shape(img, x, y, w, h, fill, r, outline=INK, shine=lerp(Y_TOP, WHITE, 0.6))
    # thin gold rim just inside the black outline along the body
    px = img.load()
    for j in range(y + header + 1, y + h - 1):
        for i in (x + 1, x + w - 2):
            if px[i, j][3] and px[i, j] != INK:
                px[i, j] = RIM
    for i in range(x + r, x + w - r):
        px[i, y + h - 2] = RIM


def bar(img, x, y, w, h, cols, fill=True, ticks=0):
    if fill:
        shape(img, x, y, w, h, vgrad(cols[0], cols[1], cols[2], 0.4), 2,
              shine=lerp(cols[0], WHITE, 0.6), shade=lerp(cols[2], INK, 0.3))
    else:
        shape(img, x, y, w, h, flat((22, 22, 24, 240), (34, 34, 36, 240)), 2, shine=C_DEEP)
    if ticks:
        px = img.load()
        for k in range(1, ticks):
            tx = x + round(k * w / ticks)
            for j in range(y + 1, y + max(2, h // 3)):
                px[tx, j] = lerp(INK, CLEAR, 0.3)


def save(img, rel):
    path = os.path.join(PACK, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return img


# ---------------------------------------------------------------- tiny pixel icons (7x7 in 9x9 cells)
HEART = [".##.##.", "#aa#bb#", "#abbbb#", "#bbbbb#", ".#bbb#.", "..#b#..", "...#..."]
SHIELD = ["#######", "#aabbb#", "#abbbb#", "#bbbbb#", ".#bbb#.", "..#b#..", "...#..."]
DRUM = ["...###.", "..#abb#", ".#abbb#", ".#bbbb#", "#bbb##.", "#ww#...", ".##...."]
BUBL = [".###.", "#aab#", "#abb#", "#bbb#", ".###."]


def icon(img, x, y, rows, cols, half=False, empty=False, outline=INK, ox=1, oy=1):
    px = img.load()
    w = len(rows[0])
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == ".":
                continue
            X, Y = x + ox + i, y + oy + j
            if ch == "#":
                px[X, Y] = outline
            elif empty or (half and i > w // 2):
                px[X, Y] = EMPTY
            elif ch == "a":
                px[X, Y] = WHITE
            elif ch == "w":
                px[X, Y] = (250, 240, 220, 255)
            else:
                px[X, Y] = lerp(cols[0], cols[1], j / 6)


# ---------------------------------------------------------------- vanilla HUD + widgets
def build_widgets():
    img = canvas(256, 256)
    px = img.load()
    # hotbar 182x22 (0,0): charcoal tray, gold rim, cell dividers
    shape(img, 0, 0, 182, 22, flat(C_TOP, C_BOT), 3, outline=INK, shine=RIM, shade=Y_SHADE)
    for k in range(1, 9):
        for j in range(4, 18):
            px[1 + k * 20, j] = C_LINE
    # selected slot 24x24 (0,22): glowing yellow ring
    sel = canvas(24, 24)
    shape(sel, 0, 0, 24, 24, vgrad(YH_TOP, YH_MID, YH_BOT), 4, shine=WHITE, shade=Y_SHADE)
    sp = sel.load()
    for j in range(3, 21):
        for i in range(3, 21):
            sp[i, j] = INK if i in (3, 20) or j in (3, 20) else CLEAR
    img.paste(sel, (0, 22), sel)
    # offhand slots 29x24 at (24,22) [left] and (53,22) [right]
    for ox in (24, 60):
        shape(img, ox, 22, 22, 22, flat(C_TOP, C_BOT), 3, shine=RIM, shade=Y_SHADE)
    # buttons 200x20: disabled y46, normal y66, hovered y86.
    # Vanilla button text is hard-coded WHITE, so normal buttons are dark with a gold
    # trim (readable) and light up yellow-amber on hover.
    button(img, 0, 46, 200, 20, "grey", r=3)
    shape(img, 0, 66, 200, 20, vgrad((78, 74, 64, 255), (52, 50, 46, 255), (38, 36, 34, 255)), 3,
          outline=INK, shine=RIM, shade=Y_SHADE)
    shape(img, 0, 86, 200, 20, vgrad((255, 214, 80, 255), (236, 160, 20, 255), (196, 112, 8, 255)), 3,
          outline=INK, shine=YH_TOP, shade=(120, 60, 0, 255))
    # small icon buttons (language / accessibility) 20x20
    for yy, hot in ((106, False), (126, True), (146, False), (166, True)):
        button(img, 0, yy, 20, 20, "yellow_hot" if hot else "yellow", r=3)
    save(img, "assets/minecraft/textures/gui/widgets.png")


def build_icons():
    img = canvas(256, 256)
    px = img.load()
    for d in (-5, -4, -3, 3, 4, 5):           # crosshair (inverted by the game; white = visible)
        px[7 + d, 7] = WHITE; px[7, 7 + d] = WHITE
    px[7, 7] = WHITE

    def hearts(y):
        icon(img, 16, y, HEART, RED, empty=True)
        icon(img, 25, y, HEART, RED, empty=True, outline=WHITE)
        for x, cols, alt in ((52, RED, False), (70, RED, True), (88, POI, False), (106, POI, True),
                             (124, WIT, False), (142, WIT, True), (160, ABS, False), (178, ICE, False)):
            c = (lerp(cols[0], WHITE, 0.5), cols[0]) if alt else cols
            icon(img, x, y, HEART, c)
            icon(img, x + 9, y, HEART, c, half=True)

    hearts(0); hearts(45)                       # normal + hardcore
    icon(img, 16, 9, SHIELD, ARM, empty=True)   # armor
    icon(img, 25, 9, SHIELD, ARM, half=True)
    icon(img, 34, 9, SHIELD, ARM)
    icon(img, 52, 9, HEART, HP[1:], empty=True)  # mount hearts
    icon(img, 88, 9, HEART, HP[1:])
    icon(img, 97, 9, HEART, HP[1:], half=True)
    icon(img, 16, 18, BUBL, AIR, ox=2, oy=2)     # air
    icon(img, 25, 18, BUBL, (WHITE, AIR[0]), ox=2, oy=2)
    icon(img, 16, 27, DRUM, FOOD, empty=True)    # food
    icon(img, 25, 27, DRUM, FOOD, empty=True, outline=WHITE)
    icon(img, 133, 27, DRUM, POI, empty=True)
    for x, cols in ((52, FOOD), (70, (lerp(FOOD[0], WHITE, .5), FOOD[0])), (88, POI)):
        icon(img, x, 27, DRUM, cols)
        icon(img, x + 9, 27, DRUM, cols, half=True)

    # 182x5 bars: XP (purple like Blox Fruits level bar), jump bar (cyan)
    def hbar(y, cols):
        for i in range(182):
            for j in range(5):
                edge = j in (0, 4) or i in (0, 181)
                px[i, y + j] = INK if edge else (C_DEEP if j > 1 else (40, 40, 44, 255))
                if not edge and i % 18 == 0:
                    px[i, y + j] = (60, 60, 64, 255)
        for i in range(1, 181):
            for j in range(1, 4):
                px[i, y + 5 + j] = WHITE if j == 1 else mix3(cols[0], cols[1], cols[2], (j - 1) / 2)
            px[i, y + 5] = INK; px[i, y + 9] = INK
        px[0, y + 5:y + 10] if False else None
        for j in range(5, 10):
            px[0, y + j] = INK; px[181, y + j] = INK

    hbar(64, XP)
    hbar(74, EN)
    hbar(84, EN)
    save(img, "assets/minecraft/textures/gui/icons.png")


def build_controls():
    s = canvas(256, 256)   # slider: track y0, hovered track y20, handle y40, hovered handle y60
    shape(s, 0, 0, 200, 20, flat((24, 24, 26, 255), (40, 40, 44, 255)), 3, shine=C_DEEP, shade=C_LINE)
    shape(s, 0, 20, 200, 20, flat((24, 24, 26, 255), (40, 40, 44, 255)), 3, outline=RIM, shine=C_DEEP, shade=C_LINE)
    button(s, 0, 40, 8, 20, "yellow", r=2)
    button(s, 0, 60, 8, 20, "yellow_hot", r=2)
    save(s, "assets/minecraft/textures/gui/slider.png")

    c = canvas(64, 64)     # checkbox 20x20: (0,0) off, (20,0) on, (0,20) off hover, (20,20) on hover
    for x, y in ((0, 0), (0, 20)):
        shape(c, x, y, 20, 20, flat((24, 24, 26, 255), (40, 40, 44, 255)), 3,
              outline=RIM if y else INK, shine=C_DEEP, shade=C_LINE)
    for x, y in ((20, 0), (20, 20)):
        button(c, x, y, 20, 20, "on", r=3)
        cp = c.load()
        for k, (i, j) in enumerate([(5, 10), (6, 11), (7, 12), (8, 13), (9, 12), (10, 11),
                                    (11, 10), (12, 9), (13, 8), (14, 7)]):
            for d in (-1, 0, 1):
                cp[x + i, y + j + d] = WHITE if d < 1 else INK
        if y:
            for i in range(x + 2, x + 18):
                cp[i, y + 1] = WHITE
    save(c, "assets/minecraft/textures/gui/checkbox.png")

    # menu background tile: charcoal with faint Blox Fruits-style cloud swirls
    bg = canvas(32, 32)
    bp = bg.load()
    for j in range(32):
        for i in range(32):
            a = math.sin(i / 32 * 2 * math.pi) + math.cos((j + i * 0.5) / 32 * 2 * math.pi)
            ring = abs(((i - 16) ** 2 + (j - 16) ** 2) ** 0.5 % 7 - 3.5) < 0.6
            base = (58, 58, 62, 255) if ring else (44, 44, 48, 255)
            bp[i, j] = lerp(base, (66, 64, 60, 255), 0.15 * (a + 2) / 4)
    save(bg, "assets/minecraft/textures/gui/options_background.png")
    save(bg, "assets/minecraft/textures/gui/light_dirt_background.png")


# ---------------------------------------------------------------- vanilla containers
# Vanilla draws container titles in dark grey (#404040) at fixed spots, so those label
# rows sit on the yellow; the slots sit in dark charcoal wells.
def yellow_frame(w, h):
    img = canvas(256, 256)
    shape(img, 0, 0, w, h, vgrad(Y_TOP, Y_MID, Y_BOT, 0.15), 5, shine=WHITE, shade=Y_SHADE)
    return img


def slots(img, x0, y0, cols, rows):
    for r in range(rows):
        for c in range(cols):
            slot(img, x0 - 1 + c * 18, y0 - 1 + r * 18)


def player_inv(img, y0):
    dark(img, 5, y0 - 4, 166, 80, r=3)
    slots(img, 8, y0, 9, 3)
    slots(img, 8, y0 + 58, 9, 1)


def arrow(img, x, y):
    px = img.load()
    for i in range(16):
        for j in range(-5, 6):
            if (i < 10 and abs(j) <= 2) or (i >= 10 and abs(j) <= 15 - i):
                px[x + i, y + j] = INK


def build_containers():
    inv = yellow_frame(176, 166)
    dark(inv, 25, 7, 52, 72, r=3)            # player preview window
    slots(inv, 8, 8, 1, 4)                   # armor
    slots(inv, 77, 62, 1, 1)                 # offhand
    slots(inv, 98, 18, 2, 2)                 # 2x2 crafting
    arrow(inv, 135, 36)
    slots(inv, 154, 28, 1, 1)
    player_inv(inv, 84)
    # status effect boxes (1.20.1: wide 120x32 at (0,166), compact 32x32 at (120,166))
    window(inv, 0, 166, 120, 32, header=0)
    dark(inv, 120, 166, 32, 32, r=3, rim=True)
    save(inv, "assets/minecraft/textures/gui/container/inventory.png")

    g = yellow_frame(176, 222)               # large chest; small chest uses the top 3 rows + bottom part
    dark(g, 5, 15, 166, 112, r=3)
    slots(g, 8, 18, 9, 6)
    player_inv(g, 140)
    save(g, "assets/minecraft/textures/gui/container/generic_54.png")

    ct = yellow_frame(176, 166)
    dark(ct, 26, 15, 58, 58, r=3)
    slots(ct, 30, 17, 3, 3)
    arrow(ct, 92, 35)
    slots(ct, 124, 35, 1, 1)
    player_inv(ct, 84)
    save(ct, "assets/minecraft/textures/gui/container/crafting_table.png")


# ---------------------------------------------------------------- mod textures (custom HUD + screens)
def build_mod():
    base = f"assets/{MODID}/textures/gui/"

    # --- hud_bars.png 256x128 ------------------------------------------------
    # y0   : big bar background 180x22 (Health / Energy share it)
    # y24  : Health fill 180x22      y48 : Energy fill 180x22
    # y72  : level bar background 180x8   y80 : level bar fill 180x8 (purple, ticks)
    # y92  : stamina/footer bar background 180x8  y100: its fill (yellow)
    hb = canvas(256, 128)
    bar(hb, 0, 0, 180, 22, None, fill=False)
    bar(hb, 0, 24, 180, 22, HP)
    bar(hb, 0, 48, 180, 22, EN)
    bar(hb, 0, 72, 180, 8, None, fill=False, ticks=10)
    bar(hb, 0, 80, 180, 8, XP, ticks=10)
    bar(hb, 0, 92, 180, 8, None, fill=False)
    bar(hb, 0, 100, 180, 8, (Y_TOP, Y_MID, Y_BOT))
    save(hb, base + "hud_bars.png")

    # --- hud_buttons.png: Stats/Items/Close/Shop buttons 84x28 ------------------
    # y0 normal, y32 hovered, y64 pressed; notification bubble 16x16 at (96,0)
    bt = canvas(128, 96)
    button(bt, 0, 0, 84, 28, "yellow", r=2)
    button(bt, 0, 32, 84, 28, "yellow_hot", r=2)
    shape(bt, 0, 64, 84, 28, vgrad(Y_BOT, Y_MID, Y_MID), 2, shine=Y_SHADE, shade=Y_TOP)
    shape(bt, 96, 0, 16, 16, vgrad(BLUE_TOP, BLUE_MID, BLUE_BOT), 7, outline=WHITE)
    save(bt, base + "hud_buttons.png")

    # --- hud_abilities.png: Z X C V F skill slots 32x32 -------------------------
    # y0 ready, y34 selected (gold), y68 cooldown overlay, (0..,104) key tags 14x10
    ab = canvas(256, 128)
    for k in range(5):
        x = k * 34
        dark(ab, x, 0, 32, 32, r=4, rim=True)
        window(ab, x, 34, 32, 32, header=0, r=4)
        shape(ab, x + 3, 37, 26, 26, flat(C_TOP, C_BOT), 3, outline=None)
        shape(ab, x, 68, 32, 32, flat((0, 0, 0, 150), (0, 0, 0, 180)), 4, outline=None)
        button(ab, x + 9, 104, 14, 10, "dark", r=2)
    save(ab, base + "hud_abilities.png")

    # --- window.png 256x256: generic menu window (title bar 24px) + parts --------
    win = canvas(256, 256)
    window(win, 0, 0, 240, 180, header=24, r=4)
    # footer strip (Stat Points / hint text bar) 238x18 at y=186
    shape(win, 0, 186, 240, 18, flat((10, 10, 10, 255), (4, 4, 4, 255)), 2, outline=RIM)
    save(win, base + "window.png")

    # --- buttons.png: all small screen buttons ----------------------------------
    # 60x20 each, columns: x0 normal, x64 hovered, x128 pressed
    # rows: y0 yellow(+1/Redeem/Refund), y24 grey(maxed/inactive), y48 On(green),
    #       y72 Off(red), y96 blue(help '?'), y120 dark tab, y144 close red 20x20 squares
    bs = canvas(256, 192)
    rows = [("yellow", "yellow_hot"), ("grey", "grey"), ("on", "on"), ("off", "off"),
            ("blue", "blue"), ("dark", "dark_hot")]
    for r, (n, h) in enumerate(rows):
        y = r * 24
        button(bs, 0, y, 60, 20, n)
        button(bs, 64, y, 60, 20, h)
        bp = bs.load()
        if n == h:   # brighter hover for single-colour kinds
            for j in range(y + 1, y + 19):
                for i in range(65, 123):
                    if bp[i, j] != INK and bp[i, j][3]:
                        bp[i, j] = lerp(bp[i, j], WHITE, 0.18)
        shape(bs, 128, y, 60, 20, lambda i, j, w, hh, n=n: lerp(
            {"yellow": Y_BOT, "grey": G_BOT, "on": ON_BOT, "off": OFF_BOT, "blue": BLUE_BOT, "dark": C_BOT}[n],
            {"yellow": Y_MID, "grey": G_MID, "on": ON_MID, "off": OFF_MID, "blue": BLUE_MID, "dark": C_TOP}[n],
            j / hh), 3)
    for k, kind in enumerate(("off", "off")):
        button(bs, k * 24, 144, 20, 20, kind)
        bp = bs.load()
        for d in range(-4, 5):
            for t in (-1, 0, 1):
                for X, Y in ((k * 24 + 10 + d, 154 + d + t), (k * 24 + 10 + d, 154 - d + t)):
                    bp[X, Y] = WHITE if t != 1 else INK
        if k:
            for i in range(25, 43):
                bp[i, 145] = WHITE
    button(bs, 48, 144, 20, 20, "blue")       # '?' help square (draw the glyph as text)
    save(bs, base + "buttons.png")

    # --- rows.png: list rows 220x28 (Stats / Settings / Shop lists) -------------
    # y0 plain, then one tinted row per stat: melee, defense, sword, gun, fruit
    rw = canvas(256, 192)
    tints = [None] + list(TINTS.values())
    for k, tint in enumerate(tints):
        y = k * 30
        def fill(i, j, w, h, tint=tint):
            c = lerp(ROW_TOP, ROW_BOT, j / h)
            if tint:
                c = lerp(lerp(tint, (0, 0, 0, 255), 0.45), c, min(1, i / (w * 0.55)))
            return c
        shape(rw, 0, y, 220, 28, fill, 3, outline=C_DEEP, shine=(74, 74, 78, 255), shade=(14, 14, 16, 255))
    save(rw, base + "rows.png")

    # --- item_frame.png: shop/inventory card 40x40 (gold Blox Fruits card) -------
    fr = canvas(128, 64)
    shape(fr, 0, 0, 40, 40, vgrad(YH_TOP, Y_MID, Y_BOT, 0.3), 4, shine=WHITE, shade=Y_SHADE, thick=1)
    fp = fr.load()
    for j in range(4, 36):
        for i in range(4, 36):
            if i in (4, 35) or j in (4, 35):
                fp[i, j] = Y_SHADE
    for k, col in enumerate([(160, 160, 170), (90, 200, 220), (170, 90, 240), (240, 60, 230), (240, 70, 70)]):
        # rarity glow cards 20x20 at y=44: common, uncommon, rare, legendary, mythical
        x = 44 + k * 0 if False else k * 22
        shape(fr, x, 44, 20, 20, lambda i, j, w, h, col=col: lerp(
            (*col, 255), (24, 24, 26, 255), ((i - 10) ** 2 + (j - 10) ** 2) ** 0.5 / 12), 3, outline=INK)
    shape(fr, 44, 0, 50, 12, vgrad(OFF_TOP, OFF_MID, OFF_BOT), 2, shine=(255, 200, 200, 255))  # "Permanent" tag
    save(fr, base + "item_frame.png")


# ---------------------------------------------------------------- metadata
def build_meta():
    os.makedirs(PACK, exist_ok=True)
    with open(os.path.join(PACK, "pack.mcmeta"), "w") as f:
        json.dump({"pack": {"pack_format": 15,
                            "description": "§6§lMarsh UI §r§e- Blox-style HUD & menus"}},
                  f, indent=2)
    ic = canvas(64, 64)
    window(ic, 2, 2, 60, 60, header=18, r=6)
    p = ic.load()
    for t in range(0, 900, 2):  # fruit swirl
        a = math.radians(t)
        r = 2 + t / 900 * 15
        x, y = int(32 + r * math.cos(a)), int(40 + r * 0.8 * math.sin(a))
        for dx in (0, 1):
            for dy in (0, 1):
                p[x + dx, y + dy] = (190, 110, 255, 255)
    save(ic, "pack.png")


def main():
    if os.path.exists(PACK):
        shutil.rmtree(PACK)
    build_meta(); build_widgets(); build_icons(); build_controls(); build_containers(); build_mod()
    os.makedirs(DIST, exist_ok=True)
    out = os.path.join(DIST, "MarshUI.zip")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for d, _, files in sorted(os.walk(PACK)):
            for fn in sorted(files):
                full = os.path.join(d, fn)
                z.write(full, os.path.relpath(full, PACK))
    print("built", out)


if __name__ == "__main__":
    main()
