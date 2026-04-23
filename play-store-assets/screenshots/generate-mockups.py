#!/usr/bin/env python3
"""
Render mock Play-Store screenshots (1080×1920, 9:16 portrait) that show
Word Wheel's UI with the real background and asset palette.

Usage:
    python3 generate-mockups.py

Writes:
    mockup-01-gameplay.png
    mockup-02-selection.png
    mockup-03-complete.png

These are placeholders — real device screenshots look much better. Use
them only as a stop-gap until you can capture from a device/emulator.
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math
import os
import sys

W, H = 1080, 1920

# Game palette
BG_TOP    = (60, 100, 200)
BG_BOTTOM = (140, 180, 240)
DEEP      = (25, 55, 120)
WHITE     = (255, 255, 255)
ACCENT    = (80, 160, 230)
GOLD      = (255, 220, 80)

ASSETS = os.path.join(os.path.dirname(__file__), "..", "..", "assets")
BACKGROUND_JPG = os.path.join(ASSETS, "background.jpg")

FONT_BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
FONT_REG  = "/System/Library/Fonts/Supplemental/Arial.ttf"


def load_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()


def make_base():
    """Background: real game_background.jpg + dark overlay, full-screen portrait."""
    bg = Image.open(BACKGROUND_JPG).convert("RGB")
    # Cover-fit into 1080x1920
    ratio = max(W / bg.width, H / bg.height)
    nw, nh = int(bg.width * ratio), int(bg.height * ratio)
    bg = bg.resize((nw, nh), Image.LANCZOS)
    left = (nw - W) // 2
    top = (nh - H) // 2
    bg = bg.crop((left, top, left + W, top + H))

    # Dark gradient overlay matching the in-game one
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for y in range(H):
        t = y / (H - 1)
        a = int(102 + (128 - 102) * t)   # 0x66 → 0x80
        ImageDraw.Draw(overlay).line([(0, y), (W, y)],
                                     fill=(0, 0, 20, a))
    return Image.alpha_composite(bg.convert("RGBA"), overlay)


def draw_topbar(img, coins=210, found=2, total=4, level=3):
    d = ImageDraw.Draw(img)
    pad = 40
    bar = Image.new("RGBA", (W - pad * 2, 90), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bar)
    bd.rounded_rectangle([0, 0, bar.width - 1, bar.height - 1],
                         radius=45, fill=(20, 40, 80, 220))
    f_bold = load_font(FONT_BOLD, 30)
    f_badge = load_font(FONT_BOLD, 26)

    # gem circle
    gem_r = 18
    gem_x = 26
    gem_y = bar.height // 2 - gem_r
    bd.ellipse([gem_x, gem_y, gem_x + gem_r * 2, gem_y + gem_r * 2],
               fill=(50, 200, 80))
    bd.text((gem_x + gem_r * 2 + 14, bar.height // 2 - 18),
            str(coins), fill=WHITE, font=f_bold)

    # words badge
    wb = f"W {found}/{total}"
    wbbox = bd.textbbox((0, 0), wb, font=f_badge)
    ww = wbbox[2] - wbbox[0]
    bx = gem_x + gem_r * 2 + 16 + 80
    bd.rounded_rectangle([bx, 20, bx + ww + 32, 70],
                         radius=20, fill=ACCENT)
    bd.text((bx + 16, 26), wb, fill=WHITE, font=f_badge)

    # level badge right
    lv = f"Lv.{level}"
    lvbbox = bd.textbbox((0, 0), lv, font=f_badge)
    lw = lvbbox[2] - lvbbox[0]
    lx = bar.width - lw - 40
    bd.rounded_rectangle([lx - 16, 20, bar.width - 20, 70],
                         radius=20, fill=(255, 255, 255, 60))
    bd.text((lx, 26), lv, fill=WHITE, font=f_badge)

    img.alpha_composite(bar, (pad, 100))


def draw_grid(img, rows, cols, filled_cells, y_top):
    """Draw a crossword grid with filled_cells dict: {(r,c): 'X'}."""
    d = ImageDraw.Draw(img)
    cell = 90
    gap = 6
    total_w = cols * cell + (cols - 1) * gap
    x0 = (W - total_w) // 2

    # Backdrop
    pad = 24
    back = Image.new("RGBA", (total_w + pad * 2,
                              rows * cell + (rows - 1) * gap + pad * 2),
                     (0, 0, 0, 0))
    bd = ImageDraw.Draw(back)
    bd.rounded_rectangle([0, 0, back.width - 1, back.height - 1],
                         radius=32, fill=(10, 25, 60, 180))
    img.alpha_composite(back, (x0 - pad, y_top - pad))

    f_cell = load_font(FONT_BOLD, 50)
    for r in range(rows):
        for c in range(cols):
            if (r, c) not in filled_cells and not filled_cells.get(
                ("used", r, c), False):
                continue
            x = x0 + c * (cell + gap)
            y = y_top + r * (cell + gap)
            letter = filled_cells.get((r, c))
            fill = DEEP if letter else (200, 215, 240)
            d.rounded_rectangle([x, y, x + cell, y + cell],
                                radius=10, fill=fill)
            if letter:
                bb = d.textbbox((0, 0), letter, font=f_cell)
                tw, th = bb[2] - bb[0], bb[3] - bb[1]
                d.text((x + cell / 2 - tw / 2 - bb[0],
                        y + cell / 2 - th / 2 - bb[1]),
                       letter, fill=WHITE, font=f_cell)


def draw_wheel(img, tiles, selected_indices, cx, cy, wheel_r=340):
    d = ImageDraw.Draw(img)
    # White disc
    wheel_layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    wd = ImageDraw.Draw(wheel_layer)
    wd.ellipse([cx - wheel_r, cy - wheel_r, cx + wheel_r, cy + wheel_r],
               fill=(255, 255, 255, 230),
               outline=(255, 255, 255, 180), width=3)
    img.alpha_composite(wheel_layer)

    f_tile = load_font(FONT_BOLD, 72)
    f_icon = load_font(FONT_BOLD, 90)
    positions = []
    for i, ch in enumerate(tiles):
        angle = i / len(tiles) * 2 * math.pi - math.pi / 2
        tx = int(cx + math.cos(angle) * wheel_r * 0.6)
        ty = int(cy + math.sin(angle) * wheel_r * 0.6)
        positions.append((tx, ty))

    # Selection line
    if len(selected_indices) >= 2:
        line_pts = [positions[i] for i in selected_indices]
        for a, b in zip(line_pts, line_pts[1:]):
            ImageDraw.Draw(img).line([a, b], fill=ACCENT + (255,), width=14)

    # Tiles
    tile_r = 58
    for i, (tx, ty) in enumerate(positions):
        selected = i in selected_indices
        # shadow
        ImageDraw.Draw(img).ellipse(
            [tx - tile_r, ty - tile_r + 4, tx + tile_r, ty + tile_r + 4],
            fill=(0, 0, 20, 100))
        fill = ACCENT if selected else (250, 250, 255)
        ImageDraw.Draw(img).ellipse(
            [tx - tile_r, ty - tile_r, tx + tile_r, ty + tile_r],
            fill=fill)
        ch = tiles[i]
        bb = ImageDraw.Draw(img).textbbox((0, 0), ch, font=f_tile)
        tw, th = bb[2] - bb[0], bb[3] - bb[1]
        ImageDraw.Draw(img).text(
            (tx - tw / 2 - bb[0], ty - th / 2 - bb[1]),
            ch, fill=WHITE if selected else DEEP, font=f_tile)

    # Shuffle icon center
    ImageDraw.Draw(img).text(
        (cx - 32, cy - 50), "\u21C4",
        fill=(150, 160, 175), font=f_icon)


def draw_bottom_buttons(img, y=1750):
    d = ImageDraw.Draw(img)
    f_btn = load_font(FONT_BOLD, 36)
    f_icon = load_font(FONT_BOLD, 44)

    # Hint
    hint_r = 54
    hx = 120
    d.ellipse([hx - hint_r, y - hint_r, hx + hint_r, y + hint_r],
              fill=(40, 40, 40, 220))
    d.text((hx - 22, y - 32), "💡", fill=WHITE, font=f_icon)
    # badge
    d.ellipse([hx + 26, y - 56, hx + 66, y - 16], fill=(50, 180, 80))
    d.text((hx + 37, y - 52), "5", fill=WHITE, font=load_font(FONT_BOLD, 28))

    # Submit
    d.rounded_rectangle([W / 2 - 140, y - 40, W / 2 + 140, y + 40],
                        radius=40, fill=ACCENT)
    d.text((W / 2 - 78, y - 24), "Submit", fill=WHITE, font=f_btn)

    # Backspace
    bx = W - 120
    d.ellipse([bx - hint_r, y - hint_r, bx + hint_r, y + hint_r],
              fill=(40, 40, 40, 220))
    d.text((bx - 26, y - 34), "\u232B", fill=WHITE, font=f_icon)


def save(img, path):
    img.convert("RGB").save(path, "PNG", optimize=True)
    print(f"Wrote {path}: {os.path.getsize(path)/1024:.1f} KB")


# ─── Screenshot 1: Clean gameplay (level 3 HASTE mid-game) ────────────
def screenshot_1():
    img = make_base()
    draw_topbar(img, coins=210, found=2, total=4, level=3)
    # HASTE layout: HASTE across row 1, HATE down col 0, SEAT down col 2
    cells = {
        (1, 0): "H", (1, 1): "A", (1, 2): "S", (1, 3): "T", (1, 4): "E",
        (2, 0): "A", (2, 2): "E",
        (3, 0): "T",
    }
    draw_grid(img, rows=5, cols=6, filled_cells=cells, y_top=330)
    draw_wheel(img, list("HASTE"), [], W // 2, 1200)
    draw_bottom_buttons(img)
    save(img, "play-store-assets/screenshots/mockup-01-gameplay.png")


# ─── Screenshot 2: Mid-drag with selection line ───────────────────────
def screenshot_2():
    img = make_base()
    draw_topbar(img, coins=208, found=1, total=4, level=2)
    # SPINE layout — partial progress
    cells = {
        (1, 0): "S", (1, 1): "P", (1, 2): "I", (1, 3): "N", (1, 4): "E",
    }
    draw_grid(img, rows=5, cols=6, filled_cells=cells, y_top=330)
    # Word preview badge
    d = ImageDraw.Draw(img)
    f_word = load_font(FONT_BOLD, 52)
    preview = "SIN"
    bb = d.textbbox((0, 0), preview, font=f_word)
    pw = bb[2] - bb[0]
    d.rounded_rectangle([W / 2 - pw / 2 - 40, 800,
                         W / 2 + pw / 2 + 40, 880],
                        radius=40, fill=(255, 255, 255, 230))
    d.text((W / 2 - pw / 2 - bb[0], 812),
           preview, fill=DEEP, font=f_word)
    # Wheel with 0 and 2 and 3 selected in order (S→I→N)
    draw_wheel(img, list("SPINE"), [0, 2, 3], W // 2, 1200)
    draw_bottom_buttons(img)
    save(img, "play-store-assets/screenshots/mockup-02-selection.png")


# ─── Screenshot 3: Level complete dialog ──────────────────────────────
def screenshot_3():
    img = make_base()
    draw_topbar(img, coins=224, found=4, total=4, level=3)
    cells = {
        (1, 0): "H", (1, 1): "A", (1, 2): "S", (1, 3): "T", (1, 4): "E",
        (2, 0): "A", (2, 2): "E",
        (3, 0): "T", (3, 2): "A",
        (4, 0): "E", (4, 1): "A", (4, 2): "T",
    }
    draw_grid(img, rows=6, cols=6, filled_cells=cells, y_top=330)
    draw_wheel(img, list("HASTE"), [], W // 2, 1200)
    draw_bottom_buttons(img)

    # Overlay — scrim
    scrim = Image.new("RGBA", (W, H), (0, 0, 0, 120))
    img.alpha_composite(scrim)

    # Dialog
    dw, dh = 700, 360
    dx = (W - dw) // 2
    dy = (H - dh) // 2
    dlg = Image.new("RGBA", (dw, dh), (0, 0, 0, 0))
    dd = ImageDraw.Draw(dlg)
    dd.rounded_rectangle([0, 0, dw - 1, dh - 1],
                         radius=32, fill=(20, 40, 80, 245),
                         outline=(100, 160, 255), width=4)
    f_h = load_font(FONT_BOLD, 50)
    f_sub = load_font(FONT_REG, 30)
    f_btn = load_font(FONT_BOLD, 36)
    # Title
    title = "Level Complete!"
    bb = dd.textbbox((0, 0), title, font=f_h)
    tw = bb[2] - bb[0]
    dd.text(((dw - tw) / 2 - bb[0], 60), title, fill=WHITE, font=f_h)
    # +10 pts
    sub = "+10 pts bonus!"
    bb = dd.textbbox((0, 0), sub, font=f_sub)
    sw = bb[2] - bb[0]
    dd.text(((dw - sw) / 2 - bb[0], 150), sub, fill=GOLD, font=f_sub)
    # Button
    bw, bh = 340, 80
    bx = (dw - bw) / 2
    by = 240
    dd.rounded_rectangle([bx, by, bx + bw, by + bh],
                         radius=40, fill=(40, 180, 70))
    btn = "Next Level"
    bb = dd.textbbox((0, 0), btn, font=f_btn)
    btw = bb[2] - bb[0]
    dd.text((bx + (bw - btw) / 2 - bb[0], by + 18),
            btn, fill=WHITE, font=f_btn)
    img.alpha_composite(dlg, (dx, dy))

    save(img, "play-store-assets/screenshots/mockup-03-complete.png")


if __name__ == "__main__":
    screenshot_1()
    screenshot_2()
    screenshot_3()
