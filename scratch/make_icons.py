"""
Generate SHIRI app launcher icons with white background and rounded corners.
Source: SHIRI_BRAND_ASSETS/shiri_app_icon_playstore.png
"""
from PIL import Image, ImageDraw
import os, math

BASE = r"E:\go\GoBajaaj_DriverPass_Fixed_Source"
SRC  = os.path.join(BASE, "SHIRI_BRAND_ASSETS", "shiri_app_icon_playstore.png")
RES  = os.path.join(BASE, "app", "src", "main", "res")

# Android mipmap sizes
SIZES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

def make_rounded_icon(size, logo_src, padding_pct=0.14, corner_pct=0.22):
    """White background, rounded corners, centered logo with padding."""
    canvas = Image.new("RGBA", (size, size), (255, 255, 255, 255))

    # Rounded mask
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    radius = int(size * corner_pct)
    draw.rounded_rectangle([0, 0, size-1, size-1], radius=radius, fill=255)
    canvas.putalpha(mask)

    # Load + resize logo with padding
    logo = Image.open(logo_src).convert("RGBA")
    pad = int(size * padding_pct)
    inner = size - 2 * pad
    logo = logo.resize((inner, inner), Image.LANCZOS)

    # Paste logo centred on white card
    canvas.paste(logo, (pad, pad), logo)
    return canvas

# --- Generate square + round icons ---
logo = Image.open(SRC)
print(f"Source: {SRC}  size={logo.size}")

for folder, px in SIZES.items():
    dest = os.path.join(RES, folder)
    os.makedirs(dest, exist_ok=True)

    icon = make_rounded_icon(px, SRC)
    icon.save(os.path.join(dest, "ic_launcher.png"), "PNG", optimize=True)

    # Round icon (fully circular)
    round_icon = make_rounded_icon(px, SRC, padding_pct=0.14, corner_pct=0.5)
    round_icon.save(os.path.join(dest, "ic_launcher_round.png"), "PNG", optimize=True)

    print(f"  ✅ {folder}: {px}x{px}px")

print("\nDone — all icons generated with white background + rounded corners.")

