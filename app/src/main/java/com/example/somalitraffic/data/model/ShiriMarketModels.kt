package com.example.somalitraffic.data.model

import java.util.UUID

data class ShiriBusinessSubcategory(
    val id: String,
    val nameSo: String,
    val nameEn: String,
    val emoji: String = "✨"
)

data class ShiriBusinessCategory(
    val id: String,
    val nameSo: String,
    val nameEn: String,
    val emoji: String,
    val bannerImageUrl: String,
    val subcategories: List<ShiriBusinessSubcategory> = emptyList()
)

data class ShiriProductItem(
    val id: String = UUID.randomUUID().toString(),
    val merchantId: String = "",
    val name: String,
    val nameEn: String = name,
    val price: Double,
    val category: String,
    val subCategory: String = "",
    val imageUrl: String,
    val inStock: Boolean = true,
    val emoji: String = "📦",
    val description: String = ""
)

data class ShiriPricingConfig(
    var driverDailyPassUsd: Double = 0.25,
    var driverWeeklyPassUsd: Double = 1.50,
    var driverMonthlyPassUsd: Double = 5.00,
    var courierBaseDeliveryFeeUsd: Double = 2.00,
    var courierPerKmFeeUsd: Double = 0.30,
    var courierSharePercentage: Double = 80.0,
    var serviceFeeUsd: Double = 0.50,
    var merchantDefaultCommissionRate: Double = 15.0,
    var merchantMinOrderUsd: Double = 3.00
)

data class ShiriMerchantStore(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameEn: String = name,
    val shiriCode: String,
    val district: String,
    val categoryId: String, // "food", "supermarket", "pharmacy", "meat_veg", "electronics", "fashion", "stationery", "home_services", "hardware", "perfume"
    val subcategoryId: String = "",
    val extraCategories: List<String> = emptyList(), // Multi-category support
    val rating: Double,
    val deliveryTimeMin: Int,
    val deliveryFee: Double,
    val imageUrl: String,
    val emoji: String = "🏪",
    val phone: String = "0612345678",
    val products: List<ShiriProductItem> = emptyList()
)

object ShiriMarketRepository {
    var currentPricing: ShiriPricingConfig = ShiriPricingConfig()

    fun getDefaultCategories(): List<ShiriBusinessCategory> {
        return listOf(
            ShiriBusinessCategory(
                id = "cunto",
                nameSo = "Cunto & Maqaayado",
                nameEn = "Food & Restaurants",
                emoji = "🍔",
                bannerImageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_cunto", "Dhammaan Cuntada", "All Food", "🍽️"),
                    ShiriBusinessSubcategory("pizza", "Pizza & Italian", "Pizza & Italian", "🍕"),
                    ShiriBusinessSubcategory("fast_food", "Fast Food & Burgers", "Fast Food & Burgers", "🍔"),
                    ShiriBusinessSubcategory("traditional", "Cunto Dhaqameed", "Traditional Somali", "🍛"),
                    ShiriBusinessSubcategory("seafood", "Kalluun & Badda", "Seafood", "🐟"),
                    ShiriBusinessSubcategory("bakery", "Rooti & Macmacaan", "Bakeries & Sweets", "🥐"),
                    ShiriBusinessSubcategory("cafe", "Bun & Shaah", "Cafes & Coffee", "☕"),
                    ShiriBusinessSubcategory("juice", "Cabitaan & Smoothies", "Fresh Juice & Smoothies", "🧃")
                )
            ),
            ShiriBusinessCategory(
                id = "supermarket",
                nameSo = "Supermarket & Dukaamo",
                nameEn = "Supermarket & Grocery",
                emoji = "🛒",
                bannerImageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_market", "Dhammaan Dukaamada", "All Stores", "🛒"),
                    ShiriBusinessSubcategory("supermarkets", "Supermarkets Waaweyn", "Hypermarkets", "🏪"),
                    ShiriBusinessSubcategory("groceries", "Raashinka Guud", "Pantry & Groceries", "🍚"),
                    ShiriBusinessSubcategory("mini_market", "Mini Markets", "Convenience Stores", "🏬"),
                    ShiriBusinessSubcategory("wholesale", "Jumlo & Bakaaro", "Wholesale Markets", "📦")
                )
            ),
            ShiriBusinessCategory(
                id = "daawo",
                nameSo = "Daawo & Farmashiyo",
                nameEn = "Pharmacy & Healthcare",
                emoji = "💊",
                bannerImageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_pharma", "Dhammaan Farmashiyeyaasha", "All Pharmacies", "💊"),
                    ShiriBusinessSubcategory("medicine", "Daawooyinka Guud", "Prescription & OTC", "🩹"),
                    ShiriBusinessSubcategory("skincare", "Daryeelka Maqaarka", "Skincare & Body", "🧴"),
                    ShiriBusinessSubcategory("medical_devices", "Qalabka Caafimaadka", "Medical Devices", "🩺")
                )
            ),
            ShiriBusinessCategory(
                id = "hilib_qudaar",
                nameSo = "Hilib, Qudaar & Kalluun",
                nameEn = "Meat, Veggies & Fish",
                emoji = "🥩",
                bannerImageUrl = "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_fresh", "Dhammaan Cusub", "All Fresh", "🥩"),
                    ShiriBusinessSubcategory("meat", "Hilib Geel, Ari & Lo'", "Fresh Camel & Goat Meat", "🥩"),
                    ShiriBusinessSubcategory("poultry", "Digaag & Beed Cusub", "Fresh Chicken & Eggs", "🍗"),
                    ShiriBusinessSubcategory("veggies", "Qudaar & Khudaarta Beerta", "Farm Vegetables & Fruits", "🥦"),
                    ShiriBusinessSubcategory("milk_butter", "Caano Geel & Subag", "Fresh Milk & Pure Ghee", "🥛")
                )
            ),
            ShiriBusinessCategory(
                id = "electronics",
                nameSo = "Taleefanno & Electronics",
                nameEn = "Phones & Electronics",
                emoji = "📱",
                bannerImageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_tech", "Dhammaan Qalabka", "All Electronics", "📱"),
                    ShiriBusinessSubcategory("smartphones", "Taleefannada Casriga ah", "Smartphones", "📲"),
                    ShiriBusinessSubcategory("accessories", "Cables, Chargers & Covers", "Accessories", "🔌"),
                    ShiriBusinessSubcategory("computers", "Laptops & Computers", "Computers & IT", "💻"),
                    ShiriBusinessSubcategory("audio", "Airpods & Speakers", "Headphones & Audio", "🎧")
                )
            ),
            ShiriBusinessCategory(
                id = "fashion",
                nameSo = "Dharka, Fashion & Tolista",
                nameEn = "Clothing, Fashion & Tailoring",
                emoji = "👗",
                bannerImageUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_fashion", "Dhammaan Fashion-ka", "All Fashion", "👗"),
                    ShiriBusinessSubcategory("tailoring", "Harqaan & Tolista", "Tailoring Services", "🪡"),
                    ShiriBusinessSubcategory("women", "Dirac, Googarad & Shaash", "Women's Clothing", "💃"),
                    ShiriBusinessSubcategory("men", "Khamiisyo & Macawiis", "Men's Clothing", "👔"),
                    ShiriBusinessSubcategory("abaya", "Cabayado & Xijaab", "Abayas & Hijabs", "🧕"),
                    ShiriBusinessSubcategory("kids", "Dharka Carruurta", "Kids Wear", "👶")
                )
            ),
            ShiriBusinessCategory(
                id = "stationery",
                nameSo = "Stationery & Buugaag",
                nameEn = "Stationery & Books",
                emoji = "📚",
                bannerImageUrl = "https://images.unsplash.com/photo-1507842229451-79b1be886a20?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_books", "Dhammaan Buugaagta", "All Stationery", "📚"),
                    ShiriBusinessSubcategory("school", "Daftarro & Qalabka Waxbarashada", "School Supplies", "✏️"),
                    ShiriBusinessSubcategory("printing", "Daabacaad & Koobi", "Printing & Scanning", "🖨️"),
                    ShiriBusinessSubcategory("office", "Qalabka Xafiisyada", "Office Equipment", "📁")
                )
            ),
            ShiriBusinessCategory(
                id = "home_services",
                nameSo = "Adeegyada Guriga & Farsamada",
                nameEn = "Home Services & Repairs",
                emoji = "🔧",
                bannerImageUrl = "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_repairs", "Dhammaan Farsamada", "All Home Services", "🔧"),
                    ShiriBusinessSubcategory("electrician", "Koronto Yaqaan", "Electricians", "💡"),
                    ShiriBusinessSubcategory("plumbing", "Tuubiste & Biyo", "Plumbers", "🚰"),
                    ShiriBusinessSubcategory("ac_repair", "Hagaajinta AC & Qaboojiye", "AC & Fridge Repair", "❄️"),
                    ShiriBusinessSubcategory("cleaning", "Nadaafadda Guryaha", "House Cleaning", "🧹")
                )
            ),
            ShiriBusinessCategory(
                id = "perfume",
                nameSo = "Barafuun, Uunsi & Qurxin",
                nameEn = "Perfume, Oud & Cosmetics",
                emoji = "✨",
                bannerImageUrl = "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_perfume", "Dhammaan Uunsiga", "All Fragrances", "✨"),
                    ShiriBusinessSubcategory("oud", "Uunsi & Cuud Carfoon", "Oud & Bukhoor", "🪵"),
                    ShiriBusinessSubcategory("fragrance", "Barafuunno Caalami ah", "Perfumes & Colognes", "🧴"),
                    ShiriBusinessSubcategory("cosmetics", "Isqurxinta & Makeup", "Cosmetics & Makeup", "💄")
                )
            ),
            ShiriBusinessCategory(
                id = "hardware",
                nameSo = "Qalabka Guryaha & Dhismaha",
                nameEn = "Building & Hardware",
                emoji = "🏠",
                bannerImageUrl = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=500&auto=format&fit=crop&q=80",
                subcategories = listOf(
                    ShiriBusinessSubcategory("all_hardware", "Dhammaan Dhismaha", "All Hardware", "🏠"),
                    ShiriBusinessSubcategory("building", "Qalabka Dhismaha", "Building Materials", "🧱"),
                    ShiriBusinessSubcategory("tools", "Qalabka Farsamada", "Tools & Hardware", "🔨"),
                    ShiriBusinessSubcategory("paint", "Rinji & Qurxinta Guriga", "Paint & Decoration", "🎨")
                )
            )
        )
    }

    fun getDefaultStores(): List<ShiriMerchantStore> {
        return listOf(
            // ─── 1. FOOD & RESTAURANTS ───
            ShiriMerchantStore(
                name = "Pizza House Mogadishu",
                nameEn = "Pizza House Mogadishu",
                shiriCode = "SHR-BO-1024",
                district = "Hodan",
                categoryId = "cunto",
                subcategoryId = "pizza",
                extraCategories = listOf("cunto", "fast_food"),
                rating = 4.9,
                deliveryTimeMin = 25,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
                emoji = "🍕",
                phone = "0615551024",
                products = listOf(
                    ShiriProductItem(
                        name = "Margherita Pizza (Large)",
                        nameEn = "Margherita Pizza (Large)",
                        price = 8.50,
                        category = "Pizza",
                        imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍕",
                        description = "Pizza jiis iyo suugo yaanyo lagu dhex dubay"
                    ),
                    ShiriProductItem(
                        name = "Chicken Shawarma Wrap",
                        nameEn = "Chicken Shawarma Wrap",
                        price = 2.00,
                        category = "Shuwarma",
                        imageUrl = "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?w=400&auto=format&fit=crop&q=80",
                        emoji = "🌯",
                        description = "Shuwarma digaag kulul oo leh suugo toon ah"
                    ),
                    ShiriProductItem(
                        name = "Crispy Chicken Burger",
                        nameEn = "Crispy Chicken Burger",
                        price = 6.50,
                        category = "Burgers",
                        imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍔",
                        description = "Burger digaag shiilan oo jajaban oo leh chips"
                    ),
                    ShiriProductItem(
                        name = "French Fries (Baradho)",
                        nameEn = "French Fries",
                        price = 3.00,
                        category = "Sides",
                        imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍟",
                        description = "Baradho shiilan oo cusbo iyo suugo leh"
                    ),
                    ShiriProductItem(
                        name = "Coca Cola 330ml Can",
                        nameEn = "Coca Cola 330ml Can",
                        price = 1.50,
                        category = "Drinks",
                        imageUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥤",
                        description = "Cabitaan qabow"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Sahra Somali Restaurant",
                nameEn = "Sahra Somali Restaurant",
                shiriCode = "SHR-BO-2041",
                district = "Waberi",
                categoryId = "cunto",
                subcategoryId = "traditional",
                extraCategories = listOf("cunto"),
                rating = 4.8,
                deliveryTimeMin = 20,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&auto=format&fit=crop&q=80",
                emoji = "🍲",
                phone = "0615552041",
                products = listOf(
                    ShiriProductItem(
                        name = "Bariis & Hilib Geel Cusub",
                        nameEn = "Somali Rice with Camel Meat",
                        price = 6.00,
                        category = "Cunto Dhaqameed",
                        imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍛",
                        description = "Bariis udgoon oo xawaash leh iyo hilib geel la kariyay"
                    ),
                    ShiriProductItem(
                        name = "Baasto Saldaato oo Suqaar leh",
                        nameEn = "Pasta with Meat Sauce",
                        price = 4.50,
                        category = "Baasto",
                        imageUrl = "https://images.unsplash.com/photo-1621996346565-e3d5d6281699?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍝",
                        description = "Baasto Suugo yaanyo iyo suqaar hilib ari leh"
                    ),
                    ShiriProductItem(
                        name = "Canjeero & Beer Shiilan",
                        nameEn = "Canjeero with Fried Liver",
                        price = 3.50,
                        category = "Quraac",
                        imageUrl = "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥞",
                        description = "Canjeero quraac iyo beer geel oo basasha la shiilay"
                    ),
                    ShiriProductItem(
                        name = "Shaah Caleen Somali ah",
                        nameEn = "Spiced Somali Tea",
                        price = 0.50,
                        category = "Cabitaan",
                        imageUrl = "https://images.unsplash.com/photo-1544787219-7f47ccb76574?w=400&auto=format&fit=crop&q=80",
                        emoji = "☕",
                        description = "Shaah hayl, qorfe iyo xabo-liin lagu kariyay"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Liido Beach Seafood Cafe",
                nameEn = "Liido Beach Seafood Cafe",
                shiriCode = "SHR-BO-5092",
                district = "Cabdicasiis / Liido",
                categoryId = "cunto",
                subcategoryId = "seafood",
                extraCategories = listOf("cunto"),
                rating = 4.9,
                deliveryTimeMin = 30,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400&auto=format&fit=crop&q=80",
                emoji = "🐟",
                phone = "0615555092",
                products = listOf(
                    ShiriProductItem(
                        name = "Kalluun Yuumbi Shiilan (Fresh Fish)",
                        nameEn = "Fried Red Snapper Fish",
                        price = 8.00,
                        category = "Kalluun",
                        imageUrl = "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400&auto=format&fit=crop&q=80",
                        emoji = "🐟",
                        description = "Kalluun badda laga soo saaray maanta oo la shiilay"
                    ),
                    ShiriProductItem(
                        name = "Aargoosto (Fresh Lobster)",
                        nameEn = "Grilled Lobster",
                        price = 16.00,
                        category = "Seafood",
                        imageUrl = "https://images.unsplash.com/photo-1559737558-2f5a35f4523b?w=400&auto=format&fit=crop&q=80",
                        emoji = "🦞",
                        description = "Aargoosto dabka lagu dubay oo subag iyo toon leh"
                    ),
                    ShiriProductItem(
                        name = "Maraq Kalluun Cadmeed",
                        nameEn = "Spicy Fish Soup",
                        price = 3.50,
                        category = "Maraq",
                        imageUrl = "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥣",
                        description = "Maraq kalluun kulul oo liin dhanaan leh"
                    )
                )
            ),

            // ─── 2. PHARMACIES & HEALTHCARE ───
            ShiriMerchantStore(
                name = "Al-Najax Pharmacy",
                nameEn = "Al-Najax Central Pharmacy",
                shiriCode = "SHR-BO-4911",
                district = "Taleex",
                categoryId = "daawo",
                subcategoryId = "medicine",
                extraCategories = listOf("daawo"),
                rating = 4.9,
                deliveryTimeMin = 15,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
                emoji = "💊",
                phone = "0615554911",
                products = listOf(
                    ShiriProductItem(
                        name = "Panadol Extra (24 Tablets)",
                        nameEn = "Panadol Extra 24x",
                        price = 2.50,
                        category = "Xanuun Baabi'iye",
                        imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
                        emoji = "💊",
                        description = "Kiniinka qandhada iyo madax xanuunka"
                    ),
                    ShiriProductItem(
                        name = "Vitamin C 1000mg Effervescent",
                        nameEn = "Vitamin C 1000mg Effervescent",
                        price = 4.00,
                        category = "Vitamins",
                        imageUrl = "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍊",
                        description = "Faytamiin C biyaha lagu milo oo difaaca jirka xoojiya"
                    ),
                    ShiriProductItem(
                        name = "Amoxicillin 500mg (Antibiotic)",
                        nameEn = "Amoxicillin 500mg",
                        price = 3.50,
                        category = "Daawo",
                        imageUrl = "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=400&auto=format&fit=crop&q=80",
                        emoji = "💊",
                        description = "Daawo antibiyootik ah"
                    ),
                    ShiriProductItem(
                        name = "First Aid Medical Box (Sanduuqa Gurmadka)",
                        nameEn = "First Aid Emergency Kit",
                        price = 12.00,
                        category = "Qalabka Caafimaadka",
                        imageUrl = "https://images.unsplash.com/photo-1603398938378-e54eab446dde?w=400&auto=format&fit=crop&q=80",
                        emoji = "🩹",
                        description = "Sanduuq dhammaystiran oo wata faashad, iodine iyo aalado"
                    ),
                    ShiriProductItem(
                        name = "Digital Thermometer (Cabirka Qandhada)",
                        nameEn = "Digital Thermometer",
                        price = 5.00,
                        category = "Qalab",
                        imageUrl = "https://images.unsplash.com/photo-1584515979956-d9f6e5d09982?w=400&auto=format&fit=crop&q=80",
                        emoji = "🌡️",
                        description = "Heerkulbeeg degdeg ah"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Digfeer Central Pharmacy",
                nameEn = "Digfeer Central Pharmacy",
                shiriCode = "SHR-BO-4920",
                district = "Digfeer",
                categoryId = "daawo",
                subcategoryId = "medical_devices",
                extraCategories = listOf("daawo"),
                rating = 4.8,
                deliveryTimeMin = 20,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1576602976047-174e57a47881?w=400&auto=format&fit=crop&q=80",
                emoji = "🩺",
                phone = "0615554920",
                products = listOf(
                    ShiriProductItem(
                        name = "Omron Blood Pressure Monitor (Cabirka Dhiigga)",
                        nameEn = "Omron Blood Pressure Monitor",
                        price = 28.00,
                        category = "Qalabka Caafimaadka",
                        imageUrl = "https://images.unsplash.com/photo-1576602976047-174e57a47881?w=400&auto=format&fit=crop&q=80",
                        emoji = "🩺",
                        description = "Qalabka dhiig-karka ee gacanta lagu cabiro"
                    ),
                    ShiriProductItem(
                        name = "Accu-Chek Glucometer (Qalabka Sonkorta)",
                        nameEn = "Accu-Chek Blood Glucose Meter",
                        price = 18.00,
                        category = "Qalabka Caafimaadka",
                        imageUrl = "https://images.unsplash.com/photo-1603398938378-e54eab446dde?w=400&auto=format&fit=crop&q=80",
                        emoji = "🩸",
                        description = "Qalabka sonkorta dhiigga lagu baaro oo wata strips"
                    ),
                    ShiriProductItem(
                        name = "Dettol Antiseptic Liquid 500ml",
                        nameEn = "Dettol Antiseptic Liquid 500ml",
                        price = 4.50,
                        category = "Nadiifin",
                        imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧴",
                        description = "Daawada jeermiska dileyso ee Dettol"
                    )
                )
            ),

            // ─── 3. SUPERMARKETS & GROCERIES ───
            ShiriMerchantStore(
                name = "Bakaaro Fresh Supermarket",
                nameEn = "Bakaaro Fresh Supermarket",
                shiriCode = "SHR-BO-3392",
                district = "Bakaaro",
                categoryId = "supermarket",
                subcategoryId = "groceries",
                extraCategories = listOf("supermarket"),
                rating = 4.7,
                deliveryTimeMin = 30,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&auto=format&fit=crop&q=80",
                emoji = "🛒",
                phone = "0615553392",
                products = listOf(
                    ShiriProductItem(
                        name = "Bariis Basmati Tayo Sare 5kg",
                        nameEn = "Basmati Rice 5kg Bag",
                        price = 7.50,
                        category = "Raashin",
                        imageUrl = "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍚",
                        description = "Kiish bariis ah oo dheer oo udgoon"
                    ),
                    ShiriProductItem(
                        name = "Saliid Macsar / Cooking Oil 2L",
                        nameEn = "Pure Cooking Oil 2 Liters",
                        price = 4.50,
                        category = "Raashin",
                        imageUrl = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=400&auto=format&fit=crop&q=80",
                        emoji = "🫒",
                        description = "Saliid saafi ah oo cuntada lagu karsado"
                    ),
                    ShiriProductItem(
                        name = "Sonkor Cad 5kg (Sugar)",
                        nameEn = "White Sugar 5kg",
                        price = 5.00,
                        category = "Raashin",
                        imageUrl = "https://images.unsplash.com/photo-1581441363689-1f3c3c414635?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧂",
                        description = "Sonkor cad oo tayo fiican"
                    ),
                    ShiriProductItem(
                        name = "Anchor Caano Boore / Milk Powder 900g",
                        nameEn = "Anchor Milk Powder 900g",
                        price = 8.00,
                        category = "Caano",
                        imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥛",
                        description = "Caano boore Anchor qasac ah"
                    ),
                    ShiriProductItem(
                        name = "Baasto Santa Lucia (5x Pack)",
                        nameEn = "Pasta Santa Lucia 5x Pack",
                        price = 3.50,
                        category = "Raashin",
                        imageUrl = "https://images.unsplash.com/photo-1621996346565-e3d5d6281699?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍝",
                        description = "Xirmo 5 xabbo oo baasto Talyaani ah"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Al-Nour Hypermarket",
                nameEn = "Al-Nour Hypermarket KM4",
                shiriCode = "SHR-BO-3401",
                district = "KM4 / Hodan",
                categoryId = "supermarket",
                subcategoryId = "supermarkets",
                extraCategories = listOf("supermarket"),
                rating = 4.9,
                deliveryTimeMin = 25,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1578916171728-46686eac8d58?w=400&auto=format&fit=crop&q=80",
                emoji = "🏬",
                phone = "0615553401",
                products = listOf(
                    ShiriProductItem(
                        name = "Biyo Saafi Xirmo (12x 500ml Bottles)",
                        nameEn = "Saafi Mineral Water 12-pack",
                        price = 3.00,
                        category = "Biyo",
                        imageUrl = "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=400&auto=format&fit=crop&q=80",
                        emoji = "💧",
                        description = "Xirmo biyo macdan ah oo qabow"
                    ),
                    ShiriProductItem(
                        name = "Ariel Sabuun Dharka 2kg Bag",
                        nameEn = "Ariel Washing Powder 2kg",
                        price = 4.50,
                        category = "Nadiifin",
                        imageUrl = "https://images.unsplash.com/photo-1585837575652-267c041d77d4?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧼",
                        description = "Sabuun budo ah oo dharka nadiifisa"
                    ),
                    ShiriProductItem(
                        name = "Lipton Tea Bags (100 Bags Box)",
                        nameEn = "Lipton Yellow Label 100x",
                        price = 2.50,
                        category = "Shaah",
                        imageUrl = "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=400&auto=format&fit=crop&q=80",
                        emoji = "☕",
                        description = "Sanduuq shaaha Lipton ah"
                    )
                )
            ),

            // ─── 4. FRESH MEAT, VEGETABLES & FISH ───
            ShiriMerchantStore(
                name = "Hilibka Geela & Ariga Hodan",
                nameEn = "Hodan Fresh Camel & Goat Butchery",
                shiriCode = "SHR-BO-6012",
                district = "Hodan",
                categoryId = "hilib_qudaar",
                subcategoryId = "meat",
                extraCategories = listOf("hilib_qudaar"),
                rating = 4.9,
                deliveryTimeMin = 20,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=400&auto=format&fit=crop&q=80",
                emoji = "🥩",
                phone = "0615556012",
                products = listOf(
                    ShiriProductItem(
                        name = "Hilib Geel Qoob ah 1kg Cusub",
                        nameEn = "Fresh Camel Meat 1kg",
                        price = 8.00,
                        category = "Hilib",
                        imageUrl = "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥩",
                        description = "Hilib geel maanta la qalay oo aad u jilicsan"
                    ),
                    ShiriProductItem(
                        name = "Hilib Ari Cusub 1kg",
                        nameEn = "Fresh Goat Meat 1kg",
                        price = 7.50,
                        category = "Hilib",
                        imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥩",
                        description = "Hilib ari yar oo macaan oo dhuux leh"
                    ),
                    ShiriProductItem(
                        name = "Beer & Kilyo Geel Cusub 1kg",
                        nameEn = "Fresh Camel Liver 1kg",
                        price = 6.00,
                        category = "Hilib",
                        imageUrl = "https://images.unsplash.com/photo-1603048588665-791ca8aea617?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥩",
                        description = "Beer cusub oo quraacda ku fiican"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Beerta Afgooye Qudaar Cusub",
                nameEn = "Afgooye Farm Fresh Vegetables",
                shiriCode = "SHR-BO-6020",
                district = "Bakaaro / Waberi",
                categoryId = "hilib_qudaar",
                subcategoryId = "veggies",
                extraCategories = listOf("hilib_qudaar"),
                rating = 4.8,
                deliveryTimeMin = 25,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&auto=format&fit=crop&q=80",
                emoji = "🥦",
                phone = "0615556020",
                products = listOf(
                    ShiriProductItem(
                        name = "Yaanyo Cusub 2kg (Fresh Tomatoes)",
                        nameEn = "Fresh Tomatoes 2kg",
                        price = 2.00,
                        category = "Qudaar",
                        imageUrl = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍅",
                        description = "Yaanyo guduudan oo beerta Afgooye laga keenay"
                    ),
                    ShiriProductItem(
                        name = "Baradho & Basal Xirmo 4kg",
                        nameEn = "Potatoes & Onions 4kg Combo",
                        price = 3.50,
                        category = "Qudaar",
                        imageUrl = "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥔",
                        description = "2kg Baradho iyo 2kg Basal"
                    ),
                    ShiriProductItem(
                        name = "Moos Shabeelle Cusub 2kg",
                        nameEn = "Fresh Shabelle Bananas 2kg",
                        price = 1.80,
                        category = "Miraha",
                        imageUrl = "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400&auto=format&fit=crop&q=80",
                        emoji = "🍌",
                        description = "Moos macaan oo wabiga Shabeelle ka yimid"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Caano Geel & Subag Taleex",
                nameEn = "Taleex Fresh Camel Milk & Ghee",
                shiriCode = "SHR-BO-6031",
                district = "Taleex",
                categoryId = "hilib_qudaar",
                subcategoryId = "milk_butter",
                extraCategories = listOf("hilib_qudaar"),
                rating = 4.9,
                deliveryTimeMin = 15,
                deliveryFee = 1.00,
                imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400&auto=format&fit=crop&q=80",
                emoji = "🥛",
                phone = "0615556031",
                products = listOf(
                    ShiriProductItem(
                        name = "Caano Geel Dhay ah 1 Liter",
                        nameEn = "Fresh Pure Camel Milk 1L",
                        price = 2.50,
                        category = "Caano",
                        imageUrl = "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400&auto=format&fit=crop&q=80",
                        emoji = "🥛",
                        description = "Caano geel saafi ah oo dhay ah"
                    ),
                    ShiriProductItem(
                        name = "Subag Saafi ah 500g (Somali Pure Ghee)",
                        nameEn = "Somali Pure Ghee 500g",
                        price = 6.00,
                        category = "Subag",
                        imageUrl = "https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧈",
                        description = "Subag asal ah oo geel ama lo'aad"
                    )
                )
            ),

            // ─── 5. ELECTRONICS & MOBILE PHONES ───
            ShiriMerchantStore(
                name = "Hormuud Telecom Store",
                nameEn = "Hormuud Telecom Flagship Store",
                shiriCode = "SHR-BO-7011",
                district = "Hodan / KM4",
                categoryId = "electronics",
                subcategoryId = "smartphones",
                extraCategories = listOf("electronics"),
                rating = 4.9,
                deliveryTimeMin = 30,
                deliveryFee = 2.50,
                imageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&auto=format&fit=crop&q=80",
                emoji = "📱",
                phone = "0615557011",
                products = listOf(
                    ShiriProductItem(
                        name = "Samsung Galaxy A15 (128GB)",
                        nameEn = "Samsung Galaxy A15 128GB",
                        price = 145.00,
                        category = "Taleefanno",
                        imageUrl = "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&auto=format&fit=crop&q=80",
                        emoji = "📱",
                        description = "Taleefan casri ah oo batari weyn iyo camera fiican leh"
                    ),
                    ShiriProductItem(
                        name = "Anker 25W Fast Charger Type-C",
                        nameEn = "Anker 25W Fast Charger",
                        price = 12.00,
                        category = "Accessories",
                        imageUrl = "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=400&auto=format&fit=crop&q=80",
                        emoji = "🔌",
                        description = "Dabeysada degdegga ah ee taleefanka"
                    ),
                    ShiriProductItem(
                        name = "Anker Power Bank 20,000mAh",
                        nameEn = "Anker Power Bank 20,000mAh",
                        price = 28.00,
                        category = "Power Bank",
                        imageUrl = "https://images.unsplash.com/photo-1609592424364-c466fb10bce1?w=400&auto=format&fit=crop&q=80",
                        emoji = "🔋",
                        description = "Batari meel kasta lagu qaato oo taleefanka 5 jeer buuxiya"
                    ),
                    ShiriProductItem(
                        name = "Wireless Bluetooth Earbuds",
                        nameEn = "Wireless Bluetooth Earbuds",
                        price = 18.00,
                        category = "Audio",
                        imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=400&auto=format&fit=crop&q=80",
                        emoji = "🎧",
                        description = "Dhegaha bilaa siligga ah oo cod cad leh"
                    )
                )
            ),

            // ─── 6. CLOTHING, FASHION & TAILORING ───
            ShiriMerchantStore(
                name = "Muqdisho Modern Tailoring (Harqaan)",
                nameEn = "Mogadishu Modern Tailoring & Men's Wear",
                shiriCode = "SHR-BO-8015",
                district = "Waberi",
                categoryId = "fashion",
                subcategoryId = "tailoring",
                extraCategories = listOf("fashion"),
                rating = 4.8,
                deliveryTimeMin = 35,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=400&auto=format&fit=crop&q=80",
                emoji = "🪡",
                phone = "0615558015",
                products = listOf(
                    ShiriProductItem(
                        name = "Tolitaanka Khamiis Rag oo Tayo Sare ah",
                        nameEn = "Custom Men's Thobe Tailoring",
                        price = 15.00,
                        category = "Tolista",
                        imageUrl = "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=400&auto=format&fit=crop&q=80",
                        emoji = "👔",
                        description = "Harqaan tolaya khamiisyo casri ah oo cabir sax ah leh"
                    ),
                    ShiriProductItem(
                        name = "Tolitaanka Dirac & Googarad Aroos",
                        nameEn = "Somali Dirac & Bridal Tailoring",
                        price = 18.00,
                        category = "Tolista",
                        imageUrl = "https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=400&auto=format&fit=crop&q=80",
                        emoji = "👗",
                        description = "Tolista diraca iyo googarada dumarka"
                    ),
                    ShiriProductItem(
                        name = "Khamiis Cadaan ah oo Diyaar ah",
                        nameEn = "Ready-made White Thobe",
                        price = 20.00,
                        category = "Dharka Ragga",
                        imageUrl = "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400&auto=format&fit=crop&q=80",
                        emoji = "👔",
                        description = "Khamiis tolmoon oo diyaar ah oo cabirro kala duwan leh"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Hooyo Fashion & Abaya",
                nameEn = "Hooyo Fashion & Dubai Abayas",
                shiriCode = "SHR-BO-8022",
                district = "Hamarweyne",
                categoryId = "fashion",
                subcategoryId = "abaya",
                extraCategories = listOf("fashion"),
                rating = 4.9,
                deliveryTimeMin = 25,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=400&auto=format&fit=crop&q=80",
                emoji = "🧕",
                phone = "0615558022",
                products = listOf(
                    ShiriProductItem(
                        name = "Abaya Dubai Luxury (Madow & Cadaan)",
                        nameEn = "Dubai Luxury Abaya",
                        price = 35.00,
                        category = "Abaya",
                        imageUrl = "https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧕",
                        description = "Cabayad tayo sare leh oo Dubay laga keenay"
                    ),
                    ShiriProductItem(
                        name = "Xijaab Xariir ah (Silk Scarf)",
                        nameEn = "Premium Silk Hijab",
                        price = 6.00,
                        category = "Xijaab",
                        imageUrl = "https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧣",
                        description = "Xijaab jilicsan oo midabbo qurux badan leh"
                    )
                )
            ),

            // ─── 7. STATIONERY & BOOKS ───
            ShiriMerchantStore(
                name = "Iqra Bookshop & Stationery",
                nameEn = "Iqra Bookshop & School Supplies",
                shiriCode = "SHR-BO-9011",
                district = "KM4",
                categoryId = "stationery",
                subcategoryId = "school",
                extraCategories = listOf("stationery"),
                rating = 4.8,
                deliveryTimeMin = 20,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1507842229451-79b1be886a20?w=400&auto=format&fit=crop&q=80",
                emoji = "📚",
                phone = "0615559011",
                products = listOf(
                    ShiriProductItem(
                        name = "Xirmo Daftarrada Iskuulka (10x Pack)",
                        nameEn = "School Exercise Books (10-pack)",
                        price = 5.00,
                        category = "Buugaag",
                        imageUrl = "https://images.unsplash.com/photo-1507842229451-79b1be886a20?w=400&auto=format&fit=crop&q=80",
                        emoji = "📓",
                        description = "10 xabo oo daftarrada casharrada ah"
                    ),
                    ShiriProductItem(
                        name = "Qalimaan Box (20x Blue & Black)",
                        nameEn = "Ballpoint Pens Box (20-pack)",
                        price = 4.00,
                        category = "Qalimaan",
                        imageUrl = "https://images.unsplash.com/photo-1585336261026-6218ea1b0b5e?w=400&auto=format&fit=crop&q=80",
                        emoji = "🖊️",
                        description = "Sanduuq qalimaan qorista ah"
                    ),
                    ShiriProductItem(
                        name = "A4 Printing Paper Box (Ream 500 Sheets)",
                        nameEn = "A4 Copy Paper Ream 500s",
                        price = 6.00,
                        category = "Waraaqo",
                        imageUrl = "https://images.unsplash.com/photo-1589829085413-56de8ae18c73?w=400&auto=format&fit=crop&q=80",
                        emoji = "📄",
                        description = "Waraaqaha daabacaadda iyo printer-ka"
                    )
                )
            ),

            // ─── 8. HOME SERVICES & REPAIRS ───
            ShiriMerchantStore(
                name = "SomFix Koronto & Tuubiste",
                nameEn = "SomFix Home Maintenance & Plumbing",
                shiriCode = "SHR-BO-9520",
                district = "Mogadishu All",
                categoryId = "home_services",
                subcategoryId = "electrician",
                extraCategories = listOf("home_services"),
                rating = 4.9,
                deliveryTimeMin = 40,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=400&auto=format&fit=crop&q=80",
                emoji = "🔧",
                phone = "0615559520",
                products = listOf(
                    ShiriProductItem(
                        name = "Hagaajinta Ciladaha Korontada Guriga",
                        nameEn = "Home Electrical Repair Service",
                        price = 15.00,
                        category = "Farsamo",
                        imageUrl = "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=400&auto=format&fit=crop&q=80",
                        emoji = "💡",
                        description = "Cilad bixinta layrarka, furayaasha iyo korontada"
                    ),
                    ShiriProductItem(
                        name = "Hagaajinta & Furista Tuubooyinka Biyaha",
                        nameEn = "Plumbing Leak & Drain Service",
                        price = 12.00,
                        category = "Farsamo",
                        imageUrl = "https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=400&auto=format&fit=crop&q=80",
                        emoji = "🚰",
                        description = "Dayactirka tubbada, musqusha iyo biyo-mareennada"
                    )
                )
            ),
            ShiriMerchantStore(
                name = "Baraf AC & Qaboojiye Repair",
                nameEn = "Baraf AC & Refrigerator Specialists",
                shiriCode = "SHR-BO-9531",
                district = "Hodan / Taleex",
                categoryId = "home_services",
                subcategoryId = "ac_repair",
                extraCategories = listOf("home_services"),
                rating = 4.8,
                deliveryTimeMin = 45,
                deliveryFee = 2.50,
                imageUrl = "https://images.unsplash.com/photo-1621905251918-48416bd8575a?w=400&auto=format&fit=crop&q=80",
                emoji = "❄️",
                phone = "0615559531",
                products = listOf(
                    ShiriProductItem(
                        name = "Adeegga & Gaas-galinta AC-ga (AC Gas Fill)",
                        nameEn = "AC Service & Gas Refill",
                        price = 25.00,
                        category = "AC Repair",
                        imageUrl = "https://images.unsplash.com/photo-1621905251918-48416bd8575a?w=400&auto=format&fit=crop&q=80",
                        emoji = "❄️",
                        description = "Dhaqida, sifeynta iyo ku shubidda gaaska AC-ga"
                    ),
                    ShiriProductItem(
                        name = "Cilad Bixinta Qaboojiyaha (Fridge Repair)",
                        nameEn = "Refrigerator Maintenance",
                        price = 20.00,
                        category = "Fridge",
                        imageUrl = "https://images.unsplash.com/photo-1571175443880-49e1d25b2bc5?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧊",
                        description = "Dayactirka iyo hagaajinta fariinjiga iyo qaboojiyaha"
                    )
                )
            ),

            // ─── 9. PERFUME, OUD & COSMETICS ───
            ShiriMerchantStore(
                name = "Uunsiga & Barafuunka Xamarweyne",
                nameEn = "Xamarweyne Luxury Oud & Fragrance",
                shiriCode = "SHR-BO-9810",
                district = "Hamarweyne",
                categoryId = "perfume",
                subcategoryId = "oud",
                extraCategories = listOf("perfume"),
                rating = 4.9,
                deliveryTimeMin = 20,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=400&auto=format&fit=crop&q=80",
                emoji = "✨",
                phone = "0615559810",
                products = listOf(
                    ShiriProductItem(
                        name = "Cuud Carfoon & Uunsi Asal ah (Somali Bukhoor)",
                        nameEn = "Premium Somali Bukhoor & Incense",
                        price = 10.00,
                        category = "Uunsi",
                        imageUrl = "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=400&auto=format&fit=crop&q=80",
                        emoji = "🪵",
                        description = "Uunsi guriga udgoon ka dhiga oo gacanta lagu sameeyay"
                    ),
                    ShiriProductItem(
                        name = "Barafuunka Oud Royal (100ml)",
                        nameEn = "Royal Oud Perfume 100ml",
                        price = 25.00,
                        category = "Barafuun",
                        imageUrl = "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=400&auto=format&fit=crop&q=80",
                        emoji = "🧴",
                        description = "Barafuun caraf dheer oo cuud iyo ubax leh"
                    )
                )
            )
        )
    }

    // Helper: auto-detect image & emoji based on product name
    fun getAutoImageForProduct(name: String): Pair<String, String> {
        val q = name.lowercase().trim()
        return when {
            q.contains("pizza") -> Pair(
                "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
                "🍕"
            )
            q.contains("shuwarma") || q.contains("shawarma") || q.contains("shawerma") -> Pair(
                "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?w=400&auto=format&fit=crop&q=80",
                "🌯"
            )
            q.contains("burger") -> Pair(
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&auto=format&fit=crop&q=80",
                "🍔"
            )
            q.contains("fries") || q.contains("chips") || q.contains("baradho") -> Pair(
                "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&auto=format&fit=crop&q=80",
                "🍟"
            )
            q.contains("cola") || q.contains("coca") || q.contains("pepsi") || q.contains("soda") || q.contains("drink") -> Pair(
                "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80",
                "🥤"
            )
            q.contains("chicken") || q.contains("digaag") -> Pair(
                "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=400&auto=format&fit=crop&q=80",
                "🍗"
            )
            q.contains("rice") || q.contains("bariis") -> Pair(
                "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&auto=format&fit=crop&q=80",
                "🍚"
            )
            q.contains("sambusa") || q.contains("samosa") -> Pair(
                "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=400&auto=format&fit=crop&q=80",
                "🥟"
            )
            q.contains("cake") || q.contains("keeg") || q.contains("macmacaan") -> Pair(
                "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400&auto=format&fit=crop&q=80",
                "🍰"
            )
            q.contains("fish") || q.contains("kalluun") -> Pair(
                "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400&auto=format&fit=crop&q=80",
                "🐟"
            )
            q.contains("meat") || q.contains("hilib") -> Pair(
                "https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=400&auto=format&fit=crop&q=80",
                "🥩"
            )
            q.contains("milk") || q.contains("caano") -> Pair(
                "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400&auto=format&fit=crop&q=80",
                "🥛"
            )
            q.contains("water") || q.contains("biyo") -> Pair(
                "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=400&auto=format&fit=crop&q=80",
                "💧"
            )
            q.contains("medicine") || q.contains("daawo") || q.contains("panadol") || q.contains("kiniin") -> Pair(
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
                "💊"
            )
            q.contains("phone") || q.contains("taleefan") || q.contains("samsung") || q.contains("iphone") -> Pair(
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&auto=format&fit=crop&q=80",
                "📱"
            )
            q.contains("dhar") || q.contains("khamiis") || q.contains("dirac") || q.contains("abaya") -> Pair(
                "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=400&auto=format&fit=crop&q=80",
                "👗"
            )
            q.contains("buug") || q.contains("book") || q.contains("qalin") || q.contains("stationery") -> Pair(
                "https://images.unsplash.com/photo-1507842229451-79b1be886a20?w=400&auto=format&fit=crop&q=80",
                "📚"
            )
            q.contains("uunsi") || q.contains("barafuun") || q.contains("perfume") || q.contains("oud") -> Pair(
                "https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=400&auto=format&fit=crop&q=80",
                "✨"
            )
            q.contains("koronto") || q.contains("tuubo") || q.contains("repair") || q.contains("farsamo") -> Pair(
                "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=400&auto=format&fit=crop&q=80",
                "🔧"
            )
            else -> Pair(
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&auto=format&fit=crop&q=80",
                "📦"
            )
        }
    }
}

