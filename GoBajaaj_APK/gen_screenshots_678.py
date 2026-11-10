#!/usr/bin/env python3
"""
Generate Play Store screenshots 6, 7, 8 for SHIRI v4.0.0
Dimensions: 1080 x 1920 px (9:16)
"""
from PIL import Image, ImageDraw, ImageFont
import os, math

OUT_DIR = os.path.join(os.path.dirname(__file__), "PlayStore_Screenshots")
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1080, 1920
RED       = (220, 38, 38)
RED_DARK  = (153, 27, 27)
RED_LIGHT = (254, 226, 226)
WHITE     = (255, 255, 255)
BLACK     = (15, 15, 15)
GRAY_BG   = (245, 245, 245)
GRAY2     = (200, 200, 200)
GREEN     = (34, 197, 94)
ORANGE    = (249, 115, 22)
BLUE      = (59, 130, 246)
TEXT_DARK = (30, 30, 30)
TEXT_MID  = (100, 100, 100)

def try_font(size, bold=False):
    paths = [
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/Arial.ttf",
    ]
    for p in paths:
        try:
            return ImageFont.truetype(p, size)
        except:
            pass
    return ImageFont.load_default()

def draw_rounded_rect(draw, xy, radius, fill, outline=None, outline_width=2):
    x0,y0,x1,y1 = xy
    draw.rounded_rectangle([x0,y0,x1,y1], radius=radius, fill=fill,
                           outline=outline, width=outline_width)

def draw_phone_frame(img, draw):
    """Draw a phone bezel at the top (status bar)"""
    draw.rectangle([0, 0, W, 60], fill=(20, 20, 20))
    draw.text((40, 18), "9:41", fill=WHITE, font=try_font(22, bold=True))
    # battery icon
    bx = W - 90
    draw.rounded_rectangle([bx, 20, bx+50, 38], radius=4, fill=WHITE)
    draw.rectangle([bx, 24, bx+36, 34], fill=GREEN)
    draw.rectangle([bx+50, 26, bx+56, 32], fill=WHITE)

def centered_text(draw, text, y, font, fill, max_w=W):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    draw.text(((max_w - tw) // 2, y), text, fill=fill, font=font)

# ─────────────────────────────────────────────────────────────────────────────
# SCREENSHOT 6 — Admin Web Dashboard
# ─────────────────────────────────────────────────────────────────────────────
def make_screenshot_6():
    img = Image.new("RGB", (W, H), GRAY_BG)
    draw = ImageDraw.Draw(img)

    # gradient header
    for y in range(220):
        t = y / 220
        r = int(220 + (153-220)*t)
        g = int(38  + (27 -38)*t)
        b = int(38  + (27 -38)*t)
        draw.line([(0, y), (W, y)], fill=(r, g, b))

    draw_phone_frame(img, draw)

    # Banner text
    draw.text((40, 70), "SHIRI", fill=WHITE, font=try_font(42, bold=True))
    draw.text((40, 118), "Admin Web Dashboard", fill=(254,202,202), font=try_font(24))

    # Stats row
    stats = [
        ("12,482", "Dalabyada", ORANGE),
        ("842",    "Ganacsiyada", BLUE),
        ("1,208",  "Darawalada", GREEN),
        ("2,481",  "Macaamiisha", RED),
    ]
    sx, sy, sw, sh = 30, 230, 240, 110
    for i, (val, label, col) in enumerate(stats):
        col_i = i % 2
        row_i = i // 2
        x = sx + col_i * (sw + 20)
        y = sy + row_i * (sh + 15)
        draw_rounded_rect(draw, [x, y, x+sw, y+sh], 14, WHITE)
        draw.text((x+16, y+14), val, fill=col, font=try_font(30, bold=True))
        draw.text((x+16, y+52), label, fill=TEXT_MID, font=try_font(20))
        # small color bar at bottom
        draw.rounded_rectangle([x+16, y+sh-14, x+sw-16, y+sh-6], radius=4, fill=col)

    # Revenue chart area
    chart_y = 490
    draw_rounded_rect(draw, [30, chart_y, W-30, chart_y+280], 14, WHITE)
    draw.text((50, chart_y+18), "Dakhliga Toddobaadlaha ah ($)", fill=TEXT_DARK, font=try_font(22, bold=True))
    # fake bar chart
    bars = [120, 180, 140, 220, 260, 190, 310]
    days = ["Isn","Talaado","Arbaco","Kham","Jim","Sabt","Axad"]
    bw = 80
    bstart = 50
    max_h = 180
    max_v = 350
    for i, (bh, day) in enumerate(zip(bars, days)):
        bx = bstart + i * (bw + 24)
        by_top = chart_y + 240 - int(bh * max_h / max_v)
        by_bot = chart_y + 240
        draw_rounded_rect(draw, [bx, by_top, bx+bw, by_bot], 6,
                          RED if i == 6 else RED_LIGHT)
        draw.text((bx + bw//2 - 20, chart_y + 248), day, fill=TEXT_MID, font=try_font(17))
        if i == 6:
            draw.text((bx, by_top - 28), f"${bars[i]}", fill=RED, font=try_font(18, bold=True))

    # Recent orders table header
    tbl_y = chart_y + 310
    draw_rounded_rect(draw, [30, tbl_y, W-30, tbl_y+40], 0, RED)
    for x_, lbl in [(46,"#"), (120,"Macmiil"), (380,"Xaalad"), (600,"Lacag"), (820,"Waqti")]:
        draw.text((x_, tbl_y+10), lbl, fill=WHITE, font=try_font(19, bold=True))

    orders = [
        ("#48291","Ahmed M.",   "Delivered", "$12.50","2 min"),
        ("#48290","Faadumo A.","In Transit", "$8.00", "5 min"),
        ("#48289","Omar H.",   "Preparing",  "$15.00","8 min"),
        ("#48288","Halimo I.", "Pending",     "$6.50","12 min"),
        ("#48287","Cabdi X.",  "Delivered",   "$9.00","15 min"),
        ("#48286","Maryam S.", "Accepted",    "$11.00","20 min"),
    ]
    status_colors = {"Delivered":GREEN,"In Transit":BLUE,"Preparing":ORANGE,"Pending":RED,"Accepted":(168,85,247)}
    for i, (num, name, status, price, time_) in enumerate(orders):
        row_y = tbl_y + 40 + i * 52
        bg = WHITE if i%2==0 else (250,250,250)
        draw.rectangle([30, row_y, W-30, row_y+52], fill=bg)
        draw.text((46,  row_y+14), num,   fill=TEXT_MID, font=try_font(17))
        draw.text((120, row_y+14), name,  fill=TEXT_DARK, font=try_font(17))
        sc = status_colors.get(status, GRAY2)
        draw_rounded_rect(draw, [380, row_y+10, 560, row_y+40], 10, sc)
        draw.text((390, row_y+14), status, fill=WHITE, font=try_font(16))
        draw.text((600, row_y+14), price, fill=TEXT_DARK, font=try_font(17, bold=True))
        draw.text((820, row_y+14), time_, fill=TEXT_MID, font=try_font(17))

    # Footer
    for y in range(H-120, H):
        t = (y - (H-120)) / 120
        r = int(220 + (153-220)*t)
        g = int(38 + (27-38)*t)
        b = int(38 + (27-38)*t)
        draw.line([(0,y),(W,y)], fill=(r,g,b))

    centered_text(draw, "Admin Web Dashboard • Maamul Dhammaan Waxyaabaha", H-85, try_font(22), WHITE)
    centered_text(draw, "SHIRI v4.0.0 — Mogadishu, Somalia", H-50, try_font(20), (254,202,202))

    path = os.path.join(OUT_DIR, "screenshot_6_admin.png")
    img.save(path, "PNG")
    print(f"✅ Saved: {path}")

# ─────────────────────────────────────────────────────────────────────────────
# SCREENSHOT 7 — Handover PIN & Delivery Confirmation
# ─────────────────────────────────────────────────────────────────────────────
def make_screenshot_7():
    img = Image.new("RGB", (W, H), GRAY_BG)
    draw = ImageDraw.Draw(img)

    # top gradient
    for y in range(260):
        t = y/260
        r = int(220+(153-220)*t); g = int(38+(27-38)*t); b = int(38+(27-38)*t)
        draw.line([(0,y),(W,y)], fill=(r,g,b))

    draw_phone_frame(img, draw)

    draw.text((40, 70), "SHIRI", fill=WHITE, font=try_font(38, bold=True))
    draw.text((40,118), "Delivery Confirmation", fill=(254,202,202), font=try_font(26))

    # Success icon circle
    cx, cy, cr = W//2, 310, 72
    draw.ellipse([cx-cr, cy-cr, cx+cr, cy+cr], fill=GREEN)
    draw.text((cx-30, cy-28), "✓", fill=WHITE, font=try_font(60, bold=True))

    centered_text(draw, "Dalab Lagu Guuleystay!", 400, try_font(34, bold=True), TEXT_DARK)
    centered_text(draw, "Order #SHR-48291 waa la keenay!", 448, try_font(22), TEXT_MID)

    # Handover PIN card
    pin_y = 510
    draw_rounded_rect(draw, [60, pin_y, W-60, pin_y+200], 18, (20,20,20))
    centered_text(draw, "🔐  Handover PIN", pin_y+22, try_font(24), (200,200,200))

    # PIN digits
    digits = ["4", "7", "9", "2"]
    dx_start = 120
    for i, d in enumerate(digits):
        bx = dx_start + i*200
        draw_rounded_rect(draw, [bx, pin_y+65, bx+140, pin_y+165], 14, RED)
        draw.text((bx+34, pin_y+72), d, fill=WHITE, font=try_font(72, bold=True))

    centered_text(draw, "Darawalka wuu geliyay koodhkan — Dalab Dhammaaday", pin_y+178, try_font(18), (150,150,150))

    # Order summary card
    sum_y = 740
    draw_rounded_rect(draw, [40, sum_y, W-40, sum_y+340], 16, WHITE)
    draw.text((64, sum_y+18), "Faahfaahinta Dalabka", fill=TEXT_DARK, font=try_font(26, bold=True))

    items = [
        ("🍕 Baasto Maraq", "$8.50"),
        ("🥤 Shaah Caleen", "$2.00"),
        ("🍞 Canjeero ×2",  "$3.00"),
    ]
    iy = sum_y + 62
    for name, price in items:
        draw.text((64, iy), name, fill=TEXT_DARK, font=try_font(22))
        draw.text((W-100-40, iy), price, fill=TEXT_DARK, font=try_font(22, bold=True))
        iy += 42

    draw.line([(64, iy+8), (W-64, iy+8)], fill=GRAY2, width=1)
    iy += 20
    for label, val, bold_ in [("Gaadiidka","$2.00",False),("Adeegga","$0.50",False),("Wadarta","$16.00",True)]:
        draw.text((64, iy), label, fill=TEXT_MID if not bold_ else TEXT_DARK, font=try_font(22, bold=bold_))
        col = RED if bold_ else TEXT_MID
        draw.text((W-100-40, iy), val, fill=col, font=try_font(22, bold=bold_))
        iy += 42

    # Courier info
    cour_y = sum_y + 360
    draw_rounded_rect(draw, [40, cour_y, W-40, cour_y+120], 16, RED_LIGHT)
    draw.ellipse([64, cour_y+22, 118, cour_y+76], fill=RED)
    draw.text((74, cour_y+30), "A", fill=WHITE, font=try_font(36, bold=True))
    draw.text((134, cour_y+22), "Ahmed Hassan", fill=TEXT_DARK, font=try_font(24, bold=True))
    draw.text((134, cour_y+54), "⭐ 4.9  •  Darawal Sharafeed", fill=TEXT_MID, font=try_font(20))
    draw_rounded_rect(draw, [W-220, cour_y+30, W-64, cour_y+90], 24, GREEN)
    draw.text((W-204, cour_y+42), "📞 Wac", fill=WHITE, font=try_font(22, bold=True))

    # Rate button
    btn_y = cour_y + 148
    draw_rounded_rect(draw, [60, btn_y, W-60, btn_y+90], 24, RED)
    centered_text(draw, "⭐  Qiimee Darawalka", btn_y+24, try_font(28, bold=True), WHITE)

    draw_rounded_rect(draw, [60, btn_y+108, W-60, btn_y+190], 24, WHITE, outline=RED, outline_width=3)
    centered_text(draw, "🏠  Ku Noqo Bogga Hore", btn_y+132, try_font(26, bold=True), RED)

    # Footer
    for y in range(H-100, H):
        t=(y-(H-100))/100; r=int(220+(153-220)*t); g=int(38+(27-38)*t); b=int(38+(27-38)*t)
        draw.line([(0,y),(W,y)],fill=(r,g,b))
    centered_text(draw, "Handover PIN — Ammaan & Xaqiijin", H-72, try_font(22), WHITE)
    centered_text(draw, "SHIRI v4.0.0 — Your City, Our Priority", H-40, try_font(20), (254,202,202))

    path = os.path.join(OUT_DIR, "screenshot_7_handover_pin.png")
    img.save(path, "PNG")
    print(f"✅ Saved: {path}")

# ─────────────────────────────────────────────────────────────────────────────
# SCREENSHOT 8 — Live Order Tracking
# ─────────────────────────────────────────────────────────────────────────────
def make_screenshot_8():
    img = Image.new("RGB", (W, H), (230, 240, 255))
    draw = ImageDraw.Draw(img)

    # top gradient
    for y in range(220):
        t=y/220; r=int(220+(153-220)*t); g=int(38+(27-38)*t); b=int(38+(27-38)*t)
        draw.line([(0,y),(W,y)], fill=(r,g,b))

    draw_phone_frame(img, draw)
    draw.text((40,70), "SHIRI", fill=WHITE, font=try_font(38, bold=True))
    draw.text((40,118), "Live Order Tracking", fill=(254,202,202), font=try_font(26))

    # Map placeholder
    map_y = 220
    map_h = 440
    draw.rectangle([0, map_y, W, map_y+map_h], fill=(200, 220, 255))
    # Road lines
    for lx in [150, 400, 650, 900]:
        draw.line([(lx, map_y), (lx, map_y+map_h)], fill=(180,200,240), width=2)
    for ly in [map_y+80, map_y+180, map_y+280, map_y+360]:
        draw.line([(0, ly), (W, ly)], fill=(180,200,240), width=2)

    # Main road
    draw.line([(0, map_y+200), (W, map_y+200)], fill=WHITE, width=22)
    draw.line([(380, map_y), (380, map_y+map_h)], fill=WHITE, width=22)
    # Dashed center
    for i in range(0, W, 60):
        draw.line([(i, map_y+200), (i+30, map_y+200)], fill=GRAY2, width=3)
    for i in range(map_y, map_y+map_h, 60):
        draw.line([(380, i), (380, i+30)], fill=GRAY2, width=3)

    # Route path (dashed red)
    pts = [(160, map_y+350), (200, map_y+300), (280, map_y+200), (380, map_y+200), (500, map_y+140), (640, map_y+90)]
    for i in range(len(pts)-1):
        x0,y0 = pts[i]; x1,y1 = pts[i+1]
        # draw dashed
        steps = 10
        for s in range(0, steps, 2):
            px0 = int(x0 + (x1-x0)*s/steps)
            py0 = int(y0 + (y1-y0)*s/steps)
            px1 = int(x0 + (x1-x0)*(s+1)/steps)
            py1 = int(y0 + (y1-y0)*(s+1)/steps)
            draw.line([(px0,py0),(px1,py1)], fill=RED, width=5)

    # Merchant pin (origin)
    mx, my_ = 160, map_y+350
    draw.ellipse([mx-18, my_-18, mx+18, my_+18], fill=ORANGE)
    draw.text((mx-10, my_-14), "🏪", font=try_font(22))

    # Courier (motorcycle) marker
    cox, coy = 420, map_y+185
    draw.ellipse([cox-26, coy-26, cox+26, coy+26], fill=RED)
    draw.text((cox-16, coy-16), "🛵", font=try_font(28))

    # Destination pin
    dx_, dy_ = 640, map_y+90
    draw.ellipse([dx_-20, dy_-20, dx_+20, dy_+20], fill=GREEN)
    draw.text((dx_-12, dy_-16), "🏠", font=try_font(26))

    # ETA badge
    draw_rounded_rect(draw, [cox+34, coy-24, cox+170, coy+24], 14, WHITE)
    draw.text((cox+44, coy-16), "ETA 8 min", fill=RED, font=try_font(20, bold=True))

    # Status timeline card
    tl_y = map_y + map_h + 20
    draw_rounded_rect(draw, [30, tl_y, W-30, tl_y+280], 16, WHITE)
    draw.text((54, tl_y+18), "Xaaladda Dalabka", fill=TEXT_DARK, font=try_font(26, bold=True))

    steps_list = [
        ("✅", "Dalab La Qaaday",    "10:30 AM", GREEN,  True),
        ("✅", "Lacag La Xaqiijiyay","10:32 AM", GREEN,  True),
        ("✅", "Waa Diyaar",         "10:38 AM", GREEN,  True),
        ("🛵", "Jidka Ku Jira",      "10:41 AM", RED,    True),   # active
        ("⬜", "La Keenay",          "—",        GRAY2,  False),
    ]
    sy2 = tl_y + 60
    for icon, label, time_, col, done in steps_list:
        draw.ellipse([54, sy2+2, 86, sy2+34], fill=col)
        draw.text((60, sy2+4), icon, font=try_font(20))
        draw.text((106, sy2+4), label, fill=TEXT_DARK if done else TEXT_MID, font=try_font(22, bold=done))
        draw.text((W-180, sy2+4), time_, fill=TEXT_MID, font=try_font(20))
        if steps_list.index((icon,label,time_,col,done)) < len(steps_list)-1:
            draw.line([(70, sy2+36), (70, sy2+52)], fill=GRAY2, width=2)
        sy2 += 52

    # Courier card
    ccard_y = tl_y + 320
    draw_rounded_rect(draw, [30, ccard_y, W-30, ccard_y+140], 16, RED_LIGHT)
    draw.ellipse([54, ccard_y+28, 114, ccard_y+88], fill=RED)
    draw.text((64, ccard_y+34), "A", fill=WHITE, font=try_font(38, bold=True))
    draw.text((130, ccard_y+28), "Ahmed Hassan", fill=TEXT_DARK, font=try_font(24, bold=True))
    draw.text((130, ccard_y+58), "⭐ 4.9  •  2.3 km u jiraa", fill=TEXT_MID, font=try_font(20))

    for bx, label, col in [(130, "📞 Wac", GREEN), (340, "💬 WhatsApp", (37,211,102)), (560, "🎙 Cod", ORANGE)]:
        draw_rounded_rect(draw, [bx, ccard_y+90, bx+170, ccard_y+130], 20, col)
        draw.text((bx+14, ccard_y+99), label, fill=WHITE, font=try_font(19, bold=True))

    # voice note player
    vn_y = ccard_y + 158
    draw_rounded_rect(draw, [30, vn_y, W-30, vn_y+90], 14, (20,20,20))
    draw.text((54, vn_y+12), "🎙 Hubi Ciwaanka — Dhageyso", fill=(200,200,200), font=try_font(20))
    draw_rounded_rect(draw, [54, vn_y+46, 54+60, vn_y+78], 20, RED)
    draw.text((64, vn_y+50), "▶", fill=WHITE, font=try_font(26, bold=True))
    draw.line([(130, vn_y+62), (W-80, vn_y+62)], fill=(80,80,80), width=4)
    draw.ellipse([340, vn_y+54, 362, vn_y+70], fill=RED)
    draw.text((W-70, vn_y+50), "0:12", fill=(200,200,200), font=try_font(18))

    # Footer
    for y in range(H-100, H):
        t=(y-(H-100))/100; r=int(220+(153-220)*t); g=int(38+(27-38)*t); b=int(38+(27-38)*t)
        draw.line([(0,y),(W,y)],fill=(r,g,b))
    centered_text(draw, "Live Tracking • Voice Landmark • Direct Call", H-72, try_font(22), WHITE)
    centered_text(draw, "SHIRI v4.0.0 — Ku dhow, Ku gaara", H-40, try_font(20), (254,202,202))

    path = os.path.join(OUT_DIR, "screenshot_8_live_tracking.png")
    img.save(path, "PNG")
    print(f"✅ Saved: {path}")

# Run all
make_screenshot_6()
make_screenshot_7()
make_screenshot_8()
print("\n🎉 Done! Screenshots 6, 7, 8 generated.")

