import os
from PIL import Image, ImageDraw, ImageFont

W, H = 1080, 1920
OUT_DIR = r"E:/go/GoBajaaj_DriverPass_Fixed_Source/GoBajaaj_APK/PlayStore_Screenshots"
os.makedirs(OUT_DIR, exist_ok=True)

# Fonts
f_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 42)
f_sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 23)
f_app_brand = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 26)
f_app_sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 15)
f_h1 = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 22)
f_h2 = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 18)
f_body = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 16)
f_bold = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 16)
f_small = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 13)
f_tiny = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 11)

# Helper: Draw rounded phone frame
def create_base(banner_color, title, subtitle):
    img = Image.new("RGB", (W, H), (248, 250, 252))
    d = ImageDraw.Draw(img)
    
    # Top Banner
    d.rectangle([(0, 0), (W, 250)], fill=banner_color)
    d.text((W // 2, 85), title, fill="white", font=f_title, anchor="mm")
    d.text((W // 2, 160), subtitle, fill=(255, 255, 255, 230), font=f_sub, anchor="mm")
    
    # Phone Outer Bezel (Sleek Dark Titanium)
    px1, py1, px2, py2 = 110, 310, 970, 1860
    d.rounded_rectangle([(px1 - 8, py1 - 8), (px2 + 8, py2 + 8)], radius=44, fill=(30, 41, 59))
    
    # Phone Screen Base
    screen = Image.new("RGB", (px2 - px1, py2 - py1), (255, 255, 255))
    return img, screen, (px1, py1, px2, py2)

# Helper: Finish and paste screen into base
def finish_screen(img, screen, box):
    px1, py1, px2, py2 = box
    sw, sh = px2 - px1, py2 - py1
    
    # Rounded mask for screen
    mask = Image.new("L", (sw, sh), 0)
    mdraw = ImageDraw.Draw(mask)
    mdraw.rounded_rectangle([(0, 0), (sw, sh)], radius=36, fill=255)
    
    # Punch Hole / Dynamic Island
    sdraw = ImageDraw.Draw(screen)
    sdraw.rounded_rectangle([(sw // 2 - 60, 14), (sw // 2 + 60, 42)], radius=14, fill=(15, 23, 42))
    
    img.paste(screen, (px1, py1), mask)

# ═════════════════════════════════════════════════════════════════════════════
# 1. SCREENSHOT 1: SHIRI SUPER APP & SUUQA GUUD (Marketplace Home)
# ═════════════════════════════════════════════════════════════════════════════
def gen_s1():
    img, sc, box = create_base(
        banner_color=(220, 38, 38), # Red
        title="SHIRI — SUPER APP-KA MUQDISHO",
        subtitle="Cunto, Gaadiid, Supermarket, Daawo & Qalab Hal Meel Ah"
    )
    d = ImageDraw.Draw(sc)
    sw, sh = sc.size
    
    # Status bar
    d.text((40, 20), "09:41", fill=(15, 23, 42), font=f_small)
    d.text((sw - 100, 20), "5G  100%", fill=(15, 23, 42), font=f_small)
    
    # App Header
    # Brand Pill
    d.rounded_rectangle([(30, 60), (74, 104)], radius=10, fill=(220, 38, 38))
    d.text((52, 82), "S", fill="white", font=ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 26), anchor="mm")
    d.text((86, 68), "SHIRI", fill=(15, 23, 42), font=f_app_brand)
    d.text((88, 96), "Suuqa Muqdisho", fill=(220, 38, 38), font=f_app_sub)
    
    # Location Pill
    d.rounded_rectangle([(sw - 180, 64), (sw - 30, 100)], radius=16, fill=(241, 245, 249), outline=(226, 232, 240))
    d.text((sw - 105, 82), "📍 Muqdisho", fill=(15, 23, 42), font=f_bold, anchor="mm")
    
    # Delivery Address Bar
    d.ellipse([(30, 122), (64, 156)], fill=(254, 242, 242))
    d.text((47, 139), "📍", font=f_body, anchor="mm")
    d.text((74, 122), "GOOBTA LAGUU KEENAYO: SHR-HO-55897", fill=(100, 116, 139), font=f_tiny)
    d.text((74, 138), "Degmada Hodan, Muqdisho", fill=(15, 23, 42), font=f_h2)
    
    # Search Bar
    d.rounded_rectangle([(30, 175), (sw - 30, 225)], radius=12, fill=(248, 250, 252), outline=(226, 232, 240))
    d.text((50, 200), "🔍", font=f_body, anchor="mm")
    d.text((75, 192), "Raadi cunto, daawo, supermarket, qudaar, qalab...", fill=(148, 163, 184), font=f_small)
    
    # Active Order Card
    d.rounded_rectangle([(30, 245), (sw - 30, 325)], radius=14, fill=(220, 38, 38))
    d.text((50, 265), "DALAB TOOS U SOCDA  #SHR-28491", fill="white", font=f_small)
    d.text((50, 292), "Pizza House Mogadishu • PIN: 4792", fill=(255, 255, 255), font=f_h2)
    d.rounded_rectangle([(sw - 180, 265), (sw - 45, 305)], radius=10, fill="white")
    d.text((sw - 112, 285), "La Soco >", fill=(220, 38, 38), font=f_bold, anchor="mm")
    
    # Super Marketplace Banner
    d.rounded_rectangle([(30, 345), (sw - 30, 445)], radius=16, fill=(185, 28, 28))
    d.text((50, 365), "SUUQA GUUD EE MUQDISHO", fill=(254, 202, 202), font=f_tiny)
    d.text((50, 390), "Cunto, Daawo, Supermarkets & Gaadiid", fill="white", font=f_h1)
    d.rounded_rectangle([(50, 425), (170, 455)], radius=8, fill="white")
    d.text((110, 440), "Ka Dalbo Hadda", fill=(220, 38, 38), font=f_tiny, anchor="mm")
    d.text((sw - 90, 400), "🛍️", font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 46), anchor="mm")
    
    # Category Carousel Header
    d.text((30, 485), "QAYBAHA ADEEGYADA (12)", fill=(15, 23, 42), font=f_h2)
    d.text((sw - 30, 485), "Dhammaan", fill=(220, 38, 38), font=f_bold, anchor="rm")
    
    # Category Cards (Horizontal)
    cats = [
        ("✨", "Dhammaan", (220, 38, 38), "white"),
        ("🛺", "Dalbo Bajaaj", (248, 250, 252), (15, 23, 42)),
        ("🏍️", "Dir Baakad", (248, 250, 252), (15, 23, 42)),
        ("🍔", "Cunto", (248, 250, 252), (15, 23, 42)),
        ("🛒", "Market", (248, 250, 252), (15, 23, 42)),
        ("💊", "Daawo", (248, 250, 252), (15, 23, 42)),
    ]
    cx = 30
    for emoji, label, bg, tc in cats:
        d.rounded_rectangle([(cx, 515), (cx + 120, 605)], radius=14, fill=bg, outline=(226, 232, 240))
        d.text((cx + 60, 545), emoji, font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 26), anchor="mm")
        d.text((cx + 60, 582), label, fill=tc, font=f_tiny, anchor="mm")
        cx += 134
        
    # Stores Header
    d.text((30, 635), "DUKAAMADA & MEHERADAHA", fill=(15, 23, 42), font=f_h2)
    d.text((sw - 30, 635), "Dhammaan Muqdisho", fill=(100, 116, 139), font=f_small, anchor="rm")
    
    # Store List Cards
    stores = [
        ("🍕", "Pizza House Mogadishu", "Hodan • ⏱️ 20 min • ⭐ 4.9", "$2.50 Gaarsiin", "Cunto Talyaani & Burgero"),
        ("🛒", "Hodan Supermarket", "Taleex • ⏱️ 25 min • ⭐ 4.8", "$1.50 Gaarsiin", "Raashin, Qudaar & Cabitaanno"),
        ("💊", "Al-Xikma Pharmacy", "KM4 • ⏱️ 15 min • ⭐ 4.9", "$1.00 Gaarsiin", "Daawooyin & Qalab Caafimaad"),
        ("🥩", "Towfiiq Hilib & Kalluun", "Bakaaro • ⏱️ 30 min • ⭐ 4.7", "$2.00 Gaarsiin", "Hilib Geel, Digaag & Qudaar"),
    ]
    sy = 665
    for s_icon, s_name, s_meta, s_fee, s_desc in stores:
        d.rounded_rectangle([(30, sy), (sw - 30, sy + 130)], radius=14, fill="white", outline=(226, 232, 240))
        d.rounded_rectangle([(44, sy + 18), (130, sy + 112)], radius=10, fill=(254, 242, 242))
        d.text((87, sy + 65), s_icon, font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 34), anchor="mm")
        
        d.text((144, sy + 22), s_name, fill=(15, 23, 42), font=f_h2)
        d.text((144, sy + 48), s_meta, fill=(100, 116, 139), font=f_small)
        d.text((144, sy + 72), s_desc, fill=(71, 85, 105), font=f_small)
        
        d.rounded_rectangle([(sw - 160, sy + 20), (sw - 44, sy + 52)], radius=8, fill=(240, 253, 244))
        d.text((sw - 102, sy + 36), s_fee, fill=(22, 163, 74), font=f_tiny, anchor="mm")
        sy += 144

    # Bottom Nav Bar
    d.rectangle([(0, sh - 80), (sw, sh)], fill="white", outline=(241, 245, 249))
    tabs = [("🏠", "Hoyga", (220, 38, 38)), ("🔍", "Raadi", (148, 163, 184)), ("📋", "Dalabaadka", (148, 163, 184)), ("👤", "Koontada", (148, 163, 184))]
    for idx, (t_icon, t_name, t_col) in enumerate(tabs):
        tx = 60 + idx * 215
        d.text((tx, sh - 55), t_icon, font=f_h2, anchor="mm")
        d.text((tx, sh - 28), t_name, fill=t_col, font=f_tiny, anchor="mm")

    finish_screen(img, sc, box)
    img.save(os.path.join(OUT_DIR, "screenshot_1_home.png"), "PNG")
    print("Screenshot 1 generated!")

# ═════════════════════════════════════════════════════════════════════════════
# 2. SCREENSHOT 2: DALBO BAJAAJ DEGDEG AH ($0 RAKAABKA)
# ═════════════════════════════════════════════════════════════════════════
def gen_s2():
    img, sc, box = create_base(
        banner_color=(220, 38, 38),
        title="DALBO BAJAAJ DEGDEG AH ($0 RAKAABKA)",
        subtitle="Hal Gujin Ku Hel Bajaajta Kuugu Dhow & Khariidadda Jaamka"
    )
    d = ImageDraw.Draw(sc)
    sw, sh = sc.size
    
    # Map area background (Stylized road grid)
    d.rectangle([(0, 0), (sw, 680)], fill=(241, 245, 249))
    # Road lines
    for ry in range(100, 680, 90):
        d.line([(0, ry), (sw, ry)], fill=(226, 232, 240), width=18)
    for rx in range(80, sw, 120):
        d.line([(rx, 0), (rx, 680)], fill=(226, 232, 240), width=18)
    # Highlighted Route Path (Red route)
    d.line([(220, 480), (380, 480), (380, 260), (560, 260)], fill=(220, 38, 38), width=8)
    
    # Pickup Pin
    d.ellipse([(205, 465), (235, 495)], fill=(34, 197, 94))
    d.text((220, 440), "📍 Goobtaada (KM4)", fill=(15, 23, 42), font=f_bold, anchor="mm")
    
    # Dropoff Pin
    d.ellipse([(545, 245), (575, 275)], fill=(220, 38, 38))
    d.text((560, 225), "🎯 Tagitaanka (Suuqa Bakaaro)", fill=(15, 23, 42), font=f_bold, anchor="mm")
    
    # Bajaaj Driver Icon on map
    d.ellipse([(360, 360), (400, 400)], fill=(255, 255, 255), outline=(220, 38, 38), width=3)
    d.text((380, 380), "🛺", font=f_h1, anchor="mm")
    
    # Top floating navigation bar
    d.rounded_rectangle([(30, 60), (sw - 30, 115)], radius=14, fill="white", outline=(226, 232, 240))
    d.text((55, 87), "←", font=f_h1, anchor="mm")
    d.text((85, 78), "Raadinta Bajaajta Muqdisho", fill=(15, 23, 42), font=f_h2)
    d.text((sw - 50, 87), "🔔", font=f_body, anchor="mm")
    
    # Driver Found Bottom Card
    d.rounded_rectangle([(30, 650), (sw - 30, sh - 30)], radius=24, fill="white", outline=(226, 232, 240))
    
    # Driver Profile
    d.ellipse([(55, 680), (125, 750)], fill=(254, 242, 242))
    d.text((90, 715), "👨‍✈️", font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 32), anchor="mm")
    
    d.text((145, 690), "Maxamed Cali Jaamac", fill=(15, 23, 42), font=f_h1)
    d.text((145, 720), "⭐ 4.9 • 840+ Safarro Guul Leh", fill=(100, 116, 139), font=f_small)
    
    d.rounded_rectangle([(sw - 190, 685), (sw - 55, 740)], radius=12, fill=(241, 245, 249))
    d.text((sw - 122, 702), "TAARGO", fill=(100, 116, 139), font=f_tiny, anchor="mm")
    d.text((sw - 122, 722), "TR-4820", fill=(220, 38, 38), font=f_bold, anchor="mm")
    
    # Ride Details Row
    d.rounded_rectangle([(50, 775), (sw - 50, 865)], radius=14, fill=(248, 250, 252))
    d.text((80, 800), "Daqiiqadaha", fill=(100, 116, 139), font=f_small)
    d.text((80, 830), "⏱️ 3 Daqiiqo", fill=(15, 23, 42), font=f_bold)
    
    d.text((sw // 2, 800), "Masaafada", fill=(100, 116, 139), font=f_small, anchor="mm")
    d.text((sw // 2, 830), "📍 2.4 KM", fill=(15, 23, 42), font=f_bold, anchor="mm")
    
    d.text((sw - 80, 800), "Qiimaha Rakaabka", fill=(100, 116, 139), font=f_small, anchor="rm")
    d.text((sw - 80, 830), "$0 (BILAASH)", fill=(22, 163, 74), font=f_bold, anchor="rm")
    
    # Call Driver / Message Buttons
    d.rounded_rectangle([(50, 890), (sw // 2 - 10, 955)], radius=14, fill=(241, 245, 249))
    d.text((sw // 4 + 20, 922), "📞 Wac Darawalka", fill=(15, 23, 42), font=f_bold, anchor="mm")
    
    d.rounded_rectangle([(sw // 2 + 10, 890), (sw - 50, 955)], radius=14, fill=(241, 245, 249))
    d.text((sw * 3 // 4 - 20, 922), "💬 Fariin U Dir", fill=(15, 23, 42), font=f_bold, anchor="mm")
    
    # Ride Cancel / Emergency
    d.rounded_rectangle([(50, 975), (sw - 50, 1040)], radius=14, fill=(220, 38, 38))
    d.text((sw // 2, 1007), "Safarku Wuu Socdaa (Live GPS)", fill="white", font=f_bold, anchor="mm")

    finish_screen(img, sc, box)
    img.save(os.path.join(OUT_DIR, "screenshot_2_ride.png"), "PNG")
    print("Screenshot 2 generated!")

# ═════════════════════════════════════════════════════════════════════════════
# 3. SCREENSHOT 3: CUNTO & MAQAAYADAHA CAANKA AH (Food & Menu)
# ═════════════════════════════════════════════════════════════════════════
def gen_s3():
    img, sc, box = create_base(
        banner_color=(234, 88, 12), # Orange
        title="CUNTO & MAQAAYADAHA CAANKA AH",
        subtitle="Pizza House, Somali Traditional, Fast Food & Gaarsiin 20 Daqiiqo"
    )
    d = ImageDraw.Draw(sc)
    sw, sh = sc.size
    
    # Store Header Card
    d.rounded_rectangle([(30, 60), (sw - 30, 200)], radius=16, fill="white", outline=(226, 232, 240))
    d.rounded_rectangle([(50, 80), (140, 170)], radius=12, fill=(254, 242, 242))
    d.text((95, 125), "🍕", font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 44), anchor="mm")
    
    d.text((160, 85), "Pizza House Mogadishu", fill=(15, 23, 42), font=f_h1)
    d.text((160, 115), "Hodan • ⏱️ 20 min • ⭐ 4.9 (500+ Reviews)", fill=(100, 116, 139), font=f_small)
    d.text((160, 142), "🍕 Talyaani • Burgero • Macmacaan & Sharaab", fill=(71, 85, 105), font=f_small)
    
    # Section Title
    d.text((30, 225), "ALAABTA DUKAANKAN KA HELI KARTO (8)", fill=(220, 38, 38), font=f_h2)
    
    # Food Menu Items with + / -
    foods = [
        ("🍕", "Pepperoni Special Pizza", "$7.50", "Pizza jiis badan leh, hilib pepperoni & suugo", 2),
        ("🍔", "Somali Crispy Chicken Burger", "$4.00", "Digaag shiilan, salad cusub & baradho french fries", 1),
        ("🍝", "Baasto Saldaato & Hilib Geel", "$5.50", "Baasto dhaqameed soomaali leh hilib suugo macaan", 0),
        ("🧃", "Cabitaan Cambe & Qare Cusub", "$1.50", "Cabitaan dabiici ah oo la qaboojiyay", 1),
        ("🥐", "Sambuusi Hilib & Qudaar (5pcs)", "$2.00", "Sambuusi kulul oo xawaash fiican leh", 0),
    ]
    
    fy = 260
    for f_emoji, f_name, f_price, f_desc, qty in foods:
        d.rounded_rectangle([(30, fy), (sw - 30, fy + 125)], radius=14, fill="white", outline=(226, 232, 240))
        d.rounded_rectangle([(45, fy + 15), (135, fy + 105)], radius=10, fill=(248, 250, 252))
        d.text((90, fy + 60), f_emoji, font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 34), anchor="mm")
        
        d.text((150, fy + 18), f_name, fill=(15, 23, 42), font=f_h2)
        d.text((150, fy + 48), f"{f_price} • Cunto & Fast Food", fill=(220, 38, 38), font=f_bold)
        d.text((150, fy + 75), f_desc, fill=(100, 116, 139), font=f_small)
        
        # Quantity controls (+ / -)
        if qty > 0:
            d.ellipse([(sw - 140, fy + 42), (sw - 105, fy + 77)], fill=(220, 38, 38))
            d.text((sw - 122, fy + 59), "-", fill="white", font=f_bold, anchor="mm")
            
            d.text((sw - 85, fy + 60), str(qty), fill=(15, 23, 42), font=f_h2, anchor="mm")
            
            d.ellipse([(sw - 65, fy + 42), (sw - 30, fy + 77)], fill=(220, 38, 38))
            d.text((sw - 47, fy + 59), "+", fill="white", font=f_bold, anchor="mm")
        else:
            d.rounded_rectangle([(sw - 95, fy + 42), (sw - 30, fy + 80)], radius=8, fill=(220, 38, 38))
            d.text((sw - 62, fy + 61), "+ Kudar", fill="white", font=f_tiny, anchor="mm")
            
        fy += 140
        
    # Sticky Checkout Bar
    d.rounded_rectangle([(30, sh - 90), (sw - 30, sh - 20)], radius=16, fill=(220, 38, 38))
    d.text((60, sh - 55), "4 alaab ayaa dambiisha ku jirta", fill="white", font=f_bold, anchor="lm")
    d.text((sw - 60, sh - 55), "Dhameystir Dalabka ($14.50) →", fill="white", font=f_h2, anchor="rm")

    finish_screen(img, sc, box)
    img.save(os.path.join(OUT_DIR, "screenshot_3_eats.png"), "PNG")
    print("Screenshot 3 generated!")

# ═════════════════════════════════════════════════════════════════════════════
# 4. SCREENSHOT 4: MAAMULKA MEHERADDA (MERCHANT DASHBOARD)
# ═════════════════════════════════════════════════════════════════════════
def gen_s4():
    img, sc, box = create_base(
        banner_color=(22, 163, 74), # Green
        title="XARUNTA GANACSATADA (MERCHANT)",
        subtitle="Kudar Alaab Cusub, Maamul Dalabaadka & Xisaabi Dakhligaaga Maanta"
    )
    d = ImageDraw.Draw(sc)
    sw, sh = sc.size
    
    # Merchant Profile Banner
    d.rounded_rectangle([(30, 60), (sw - 30, 160)], radius=16, fill="white", outline=(226, 232, 240))
    d.rounded_rectangle([(50, 75), (130, 145)], radius=12, fill=(240, 253, 244))
    d.text((90, 110), "🏪", font=ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 36), anchor="mm")
    d.text((150, 85), "Pizza House Mogadishu", fill=(15, 23, 42), font=f_h1)
    d.text((150, 115), "SHR-BO-1024 • Hodan, Mogadishu", fill=(100, 116, 139), font=f_small)
    
    d.rounded_rectangle([(sw - 160, 85), (sw - 50, 125)], radius=10, fill=(220, 38, 38))
    d.text((sw - 105, 105), "Furan (Live)", fill="white", font=f_tiny, anchor="mm")
    
    # Add Product Button
    d.rounded_rectangle([(30, 180), (sw - 30, 235)], radius=12, fill=(220, 38, 38))
    d.text((sw // 2, 207), "+ Kudar Alaab Cusub (Product)", fill="white", font=f_bold, anchor="mm")
    
    # Financial Stats Cards
    stats = [
        ("Dakhliga Maanta", "$142.50", (22, 163, 74)),
        ("Dalabaadka", "18 Dalab", (15, 23, 42)),
        ("Dheelitirka", "$84.20", (2, 132, 199))
    ]
    for idx, (st_t, st_v, st_col) in enumerate(stats):
        bx = 30 + idx * 255
        d.rounded_rectangle([(bx, 255), (bx + 240, 345)], radius=12, fill="white", outline=(226, 232, 240))
        d.text((bx + 20, 275), st_t, fill=(100, 116, 139), font=f_small)
        d.text((bx + 20, 310), st_v, fill=st_col, font=f_h1)
        
    # Live Orders Section
    d.text((30, 375), "DALABAADKA TOOSKA AH (LIVE ORDERS)", fill=(15, 23, 42), font=f_h2)
    
    orders = [
        ("DALAB #SHR-28491", "2x Pepperoni Pizza, 1x Burger", "$19.00", "EVC Plus", "Diyaari"),
        ("DALAB #SHR-28492", "3x Baasto Saldaato, 2x Qare", "$18.50", "eDahab", "Mootada Sii"),
        ("DALAB #SHR-28493", "1x Crispy Burger, 1x Sharaab", "$5.50", "Caddaan", "Waddada ku jira"),
    ]
    oy = 410
    for o_num, o_items, o_total, o_pay, o_act in orders:
        d.rounded_rectangle([(30, oy), (sw - 30, oy + 120)], radius=14, fill="white", outline=(226, 232, 240))
        d.text((50, oy + 20), o_num, fill=(220, 38, 38), font=f_bold)
        d.text((50, oy + 50), o_items, fill=(15, 23, 42), font=f_body)
        d.text((50, oy + 80), f"{o_total} • {o_pay}", fill=(100, 116, 139), font=f_small)
        
        d.rounded_rectangle([(sw - 160, oy + 35), (sw - 50, oy + 85)], radius=10, fill=(241, 245, 249), outline=(203, 213, 225))
        d.text((sw - 105, oy + 60), o_act, fill=(15, 23, 42), font=f_bold, anchor="mm")
        oy += 135
        
    # Product Stock Toggles
    d.text((30, 835), "ALAABTA MEHERADDA & STOCK-GA", fill=(15, 23, 42), font=f_h2)
    stock_items = [
        ("Pepperoni Pizza", "$7.50", True),
        ("Crispy Burger", "$4.00", True),
        ("Mango Juice", "$1.50", True),
    ]
    py = 870
    for p_name, p_pr, in_stock in stock_items:
        d.rounded_rectangle([(30, py), (sw - 30, py + 70)], radius=12, fill="white", outline=(226, 232, 240))
        d.text((50, py + 24), p_name, fill=(15, 23, 42), font=f_body)
        d.text((320, py + 24), p_pr, fill=(220, 38, 38), font=f_bold)
        
        # Green Toggle
        d.rounded_rectangle([(sw - 130, py + 18), (sw - 50, py + 52)], radius=16, fill=(34, 197, 94))
        d.ellipse([(sw - 82, py + 20), (sw - 52, py + 50)], fill="white")
        py += 82

    finish_screen(img, sc, box)
    img.save(os.path.join(OUT_DIR, "screenshot_4_merchant.png"), "PNG")
    print("Screenshot 4 generated!")

# ═════════════════════════════════════════════════════════════════════════════
# 5. SCREENSHOT 5: MOOTO EXPRESS COURIER & DELIVERY
# ═════════════════════════════════════════════════════════════════════════
def gen_s5():
    img, sc, box = create_base(
        banner_color=(2, 132, 199), # Sky Blue
        title="DIR BAAKAD & MOOTO DELIVERY",
        subtitle="Koodhka Sirta ah ee Handover PIN (4792) & Dakhliga Darawalka"
    )
    d = ImageDraw.Draw(sc)
    sw, sh = sc.size
    
    # Courier Live Status Header Card
    d.rounded_rectangle([(30, 60), (sw - 30, 160)], radius=16, fill=(15, 23, 42))
    d.text((50, 85), "DALAB TOOS U SOCDA  #SHR-28491", fill=(34, 197, 94), font=f_tiny)
    d.text((50, 110), "Pizza House ➔ Hodan, Muqdisho", fill="white", font=f_h1)
    
    # Handover PIN Card (Big Bold Security PIN)
    d.rounded_rectangle([(30, 185), (sw - 30, 340)], radius=18, fill=(15, 23, 42), outline=(220, 38, 38), width=3)
    d.text((sw // 2, 220), "KOODHKA GAARSIINTA (HANDOVER PIN)", fill=(148, 163, 184), font=f_small, anchor="mm")
    d.text((sw // 2, 270), "4  7  9  2", fill=(220, 38, 38), font=ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 46), anchor="mm")
    d.text((sw // 2, 315), "Darawalka mootada sii PIN-kan marka uu xirmada kuu keeno", fill=(100, 116, 139), font=f_tiny, anchor="mm")
    
    # Delivery Timeline Steps
    d.rounded_rectangle([(30, 365), (sw - 30, 620)], radius=16, fill="white", outline=(226, 232, 240))
    d.text((50, 390), "HEERARKA GAARSIINTA DALABKA", fill=(15, 23, 42), font=f_h2)
    
    steps = [
        ("👨‍🍳", "Diyaarinta Dalabka", "Cuntada waa la dubay oo baakad baa lagu guray", True),
        ("🏍️", "Mooto ayaa loo qoondeeyay", "Darawal Axmed Xasan ayaa aqbalay xirmada", True),
        ("📍", "Waddada ayuu ku soo jiraa", "Mootadu waxay u soo socotaa gurigaaga", True),
        ("✅", "Gacantaada ayaa la gelinayaa", "Geli Handover PIN si aad u xaqiijiso", False),
    ]
    ty = 430
    for s_ic, s_t, s_sub, is_done in steps:
        bg_col = (34, 197, 94) if is_done else (241, 245, 249)
        d.ellipse([(50, ty), (90, ty + 40)], fill=bg_col)
        d.text((70, ty + 20), s_ic, font=f_body, anchor="mm")
        
        d.text((110, ty + 5), s_t, fill=(15, 23, 42), font=f_bold)
        d.text((110, ty + 26), s_sub, fill=(100, 116, 139), font=f_tiny)
        ty += 46

    # Digital Address & Voice Landmark
    d.rounded_rectangle([(30, 645), (sw - 30, 770)], radius=16, fill=(254, 242, 242), outline=(254, 202, 202))
    d.text((50, 665), "CIWAANKA DIGITAL-KA AH (SHIRI ADDRESS)", fill=(220, 38, 38), font=f_tiny)
    d.text((50, 690), "SHR-HO-55897 • Guriga Jaalaha ah, Luuqa 2-aad", fill=(15, 23, 42), font=f_h2)
    d.rounded_rectangle([(50, 725), (280, 755)], radius=10, fill="white", outline=(220, 38, 38))
    d.text((165, 740), "▶ Dhageyso Codka Tilmaanta", fill=(220, 38, 38), font=f_tiny, anchor="mm")

    # Courier Earnings Card
    d.rounded_rectangle([(30, 795), (sw - 30, 920)], radius=16, fill="white", outline=(226, 232, 240))
    d.text((50, 815), "DAKHLIGA MOOTADA (COURIER WALLET)", fill=(100, 116, 139), font=f_tiny)
    d.text((50, 845), "$49.00 USD", fill=(34, 197, 94), font=ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 36))
    d.text((50, 895), "80% Saamiga Darawalka Mootada • EVC / eDahab", fill=(100, 116, 139), font=f_small)

    finish_screen(img, sc, box)
    img.save(os.path.join(OUT_DIR, "screenshot_5_courier.png"), "PNG")
    print("Screenshot 5 generated!")

if __name__ == "__main__":
    gen_s1()
    gen_s2()
    gen_s3()
    gen_s4()
    gen_s5()
    print("ALL 5 SCREENSHOTS GENERATED AT 1080x1920!")
