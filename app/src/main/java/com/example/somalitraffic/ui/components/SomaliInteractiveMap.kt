package com.example.somalitraffic.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.data.model.TrafficStatus
import com.example.ui.theme.*
import kotlin.math.hypot

/**
 * Native Jetpack Compose Interactive Map for Mogadishu Road Network & GoBajaaj Live Tracking.
 * 100% Native Vector & Canvas implementation:
 * - High density Mogadishu street grid, avenues, and intersections
 * - Interactive intersection traffic lights (Green/Yellow/Red) with animated blinking beacons
 * - Live user GPS location tracking with animated pulse & radar
 * - Realistic coastline, Aden Adde Airport, Lido Beach, and City landmarks
 * - Animated roaming Bajaaj tuk-tuks on major corridors
 */
@Composable
fun SomaliInteractiveMap(
    intersections: List<Intersection>,
    selectedIntersectionId: String? = null,
    blinkingIntersectionId: String? = null,
    blinkingIntersectionIds: Set<String> = emptySet(),
    onIntersectionClick: (Intersection) -> Unit = {},
    userLatitude: Double? = null,
    userLongitude: Double? = null,
    routePointsJson: String? = null,
    centerOnUserLocation: Boolean = false,
    customUserPinLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val effectiveBlinkingSet = remember(blinkingIntersectionId, blinkingIntersectionIds) {
        val set = blinkingIntersectionIds.toMutableSet()
        if (blinkingIntersectionId != null) {
            set.add(blinkingIntersectionId)
        }
        set
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2E8F0))
            .testTag("real_production_map")
    ) {
        NativeMogadishuVectorMap(
            intersections = intersections,
            selectedIntersectionId = selectedIntersectionId,
            blinkingIntersectionIds = effectiveBlinkingSet,
            onIntersectionClick = onIntersectionClick,
            userLatitude = userLatitude ?: 2.0371,
            userLongitude = userLongitude ?: 45.3283,
            centerOnUserLocation = centerOnUserLocation,
            customUserPinLabel = customUserPinLabel,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun NativeMogadishuVectorMap(
    intersections: List<Intersection>,
    selectedIntersectionId: String?,
    blinkingIntersectionIds: Set<String>,
    onIntersectionClick: (Intersection) -> Unit,
    userLatitude: Double?,
    userLongitude: Double?,
    centerOnUserLocation: Boolean = false,
    customUserPinLabel: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.15f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Fallback Mogadishu Intersections if the provided database list is empty or minimal
    val displayIntersections = remember(intersections) {
        if (intersections.isNotEmpty()) {
            intersections
        } else {
            listOf(
                Intersection(id = "1", name = "Isgoyska KM4", latitude = 2.0290, longitude = 45.3180, currentStatus = "FURAN", roadNames = "Wadada Maka Al-Mukarama & Airport"),
                Intersection(id = "2", name = "Isgoyska Zoobe (KM5)", latitude = 2.0315, longitude = 45.3130, currentStatus = "DHEXDHEXAAD", roadNames = "Wadada Zoobe - Hodan"),
                Intersection(id = "3", name = "Isgoyska Banaadir", latitude = 2.0340, longitude = 45.3080, currentStatus = "MASHQUUL", roadNames = "Wadada Wadnaha & Digfeer"),
                Intersection(id = "4", name = "Isgoyska Taleex", latitude = 2.0335, longitude = 45.3150, currentStatus = "FURAN", roadNames = "Wadada Taleex - Hodan"),
                Intersection(id = "5", name = "Isgoyska Shaqaalaha", latitude = 2.0350, longitude = 45.3235, currentStatus = "FURAN", roadNames = "Maka Al-Mukarama"),
                Intersection(id = "6", name = "Isgoyska Dabka", latitude = 2.0390, longitude = 45.3280, currentStatus = "DHEXDHEXAAD", roadNames = "Waberi & Hodan"),
                Intersection(id = "7", name = "Taallada Sayidka", latitude = 2.0430, longitude = 45.3330, currentStatus = "SAXMAD_BADAN", roadNames = "Wadada Madaxtooyada"),
                Intersection(id = "8", name = "Suuqa Bakaaraha", latitude = 2.0450, longitude = 45.3200, currentStatus = "MASHQUUL", roadNames = "Wadada Wadnaha"),
                Intersection(id = "9", name = "Isgoyska Hawlwadaag", latitude = 2.0410, longitude = 45.3240, currentStatus = "FURAN", roadNames = "Hawlwadaag Road"),
                Intersection(id = "10", name = "Isgoyska Tarbuunka", latitude = 2.0400, longitude = 45.3050, currentStatus = "FURAN", roadNames = "Hodan Tarbuunka"),
                Intersection(id = "11", name = "Garoonka Aden Adde", latitude = 2.0160, longitude = 45.3060, currentStatus = "FURAN", roadNames = "Wadada Garoonka"),
                Intersection(id = "12", name = "Xeebta Liido", latitude = 2.0420, longitude = 45.3620, currentStatus = "FURAN", roadNames = "Wadada Liido - Cabdicasiis")
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Animated traffic motion on roads (Bajaaj vehicles moving along streets)
    val carMotionPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "carMotion"
    )

    // Geographic bounding box for Mogadishu core road network
    val minLat = 2.010
    val maxLat = 2.065
    val minLng = 45.285
    val maxLng = 45.370

    // Automatically focus when selected intersection or user location changes
    LaunchedEffect(selectedIntersectionId) {
        if (selectedIntersectionId != null) {
            val target = displayIntersections.find { it.id == selectedIntersectionId }
            if (target != null) {
                scale = 1.4f
            }
        }
    }

    LaunchedEffect(centerOnUserLocation, userLatitude, userLongitude) {
        if (centerOnUserLocation && userLatitude != null && userLongitude != null) {
            scale = 1.3f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.8f, 4.5f)
                    offset += pan
                }
            }
            .pointerInput(displayIntersections, scale, offset) {
                detectTapGestures { tapOffset ->
                    val w = size.width
                    val h = size.height
                    val pad = 36f
                    var closest: Intersection? = null
                    var minDist = Float.MAX_VALUE
                    displayIntersections.forEach { item ->
                        val normX = ((item.longitude - minLng) / (maxLng - minLng)).coerceIn(0.0, 1.0).toFloat()
                        val normY = (1.0f - ((item.latitude - minLat) / (maxLat - minLat)).coerceIn(0.0, 1.0).toFloat())
                        val cx = (pad + normX * (w - 2 * pad) - w / 2) * scale + w / 2 + offset.x
                        val cy = (pad + normY * (h - 2 * pad) - h / 2) * scale + h / 2 + offset.y
                        val dist = hypot(tapOffset.x - cx, tapOffset.y - cy)
                        if (dist < minDist && dist < 65f) {
                            minDist = dist
                            closest = item
                        }
                    }
                    closest?.let { onIntersectionClick(it) }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = 36f

            fun mapX(lng: Double): Float {
                val normX = ((lng - minLng) / (maxLng - minLng)).coerceIn(0.0, 1.0).toFloat()
                return (pad + normX * (w - 2 * pad) - w / 2) * scale + w / 2 + offset.x
            }

            fun mapY(lat: Double): Float {
                val normY = (1.0f - ((lat - minLat) / (maxLat - minLat)).coerceIn(0.0, 1.0).toFloat())
                return (pad + normY * (h - 2 * pad) - h / 2) * scale + h / 2 + offset.y
            }

            // 1. City Land Area Base Color (warm map cream)
            drawRect(color = Color(0xFFF8FAFC), size = size)

            // 2. District Zones (Shaded city blocks)
            // Hodan District
            val hodanRect = Path().apply {
                moveTo(mapX(45.295), mapY(2.050))
                lineTo(mapX(45.325), mapY(2.045))
                lineTo(mapX(45.320), mapY(2.025))
                lineTo(mapX(45.295), mapY(2.030))
                close()
            }
            drawPath(hodanRect, color = Color(0xFFF1F5F9))

            // Green Parks & Stadium
            val stadiumX = mapX(45.318)
            val stadiumY = mapY(2.055)
            drawCircle(color = Color(0xFFDCFCE7), radius = 32f * scale, center = Offset(stadiumX, stadiumY))

            val peaceParkX = mapX(45.330)
            val peaceParkY = mapY(2.035)
            drawCircle(color = Color(0xFFDCFCE7), radius = 24f * scale, center = Offset(peaceParkX, peaceParkY))

            // 3. Indian Ocean & Coastline along Mogadishu south-east
            val oceanPath = Path().apply {
                moveTo(mapX(45.305), size.height + 400)
                lineTo(mapX(45.312), mapY(2.012))
                lineTo(mapX(45.328), mapY(2.022))
                lineTo(mapX(45.348), mapY(2.032))
                lineTo(mapX(45.370), mapY(2.042))
                lineTo(size.width + 400, mapY(2.045))
                lineTo(size.width + 400, size.height + 400)
                close()
            }
            // Coast Beach Sand Strip
            drawPath(oceanPath, color = Color(0xFFFEF3C7))
            // Deep Ocean Blue
            val deepOceanPath = Path().apply {
                moveTo(mapX(45.308), size.height + 400)
                lineTo(mapX(45.315), mapY(2.010))
                lineTo(mapX(45.332), mapY(2.020))
                lineTo(mapX(45.352), mapY(2.030))
                lineTo(mapX(45.372), mapY(2.040))
                lineTo(size.width + 400, mapY(2.043))
                lineTo(size.width + 400, size.height + 400)
                close()
            }
            drawPath(deepOceanPath, color = Color(0xFF38BDF8).copy(alpha = 0.85f))

            // 4. Aden Adde International Airport Runway
            val airportStartX = mapX(45.302)
            val airportStartY = mapY(2.018)
            val airportEndX = mapX(45.312)
            val airportEndY = mapY(2.012)
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(airportStartX, airportStartY),
                end = Offset(airportEndX, airportEndY),
                strokeWidth = 14f * scale,
                cap = StrokeCap.Square
            )
            drawLine(
                color = Color.White,
                start = Offset(airportStartX, airportStartY),
                end = Offset(airportEndX, airportEndY),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Square
            )

            // 5. Mogadishu Road Network (Comprehensive Grid)
            val makaMukarama = listOf(
                Pair(2.0160, 45.3060), // Airport
                Pair(2.0290, 45.3180), // KM4
                Pair(2.0350, 45.3235), // Shaqaalaha
                Pair(2.0390, 45.3280), // Dabka
                Pair(2.0430, 45.3330)  // Sayidka
            )
            val wadadaWadnaha = listOf(
                Pair(2.0520, 45.3100),
                Pair(2.0450, 45.3200), // Bakaaro
                Pair(2.0410, 45.3240), // Hawlwadaag
                Pair(2.0340, 45.3080)  // Banaadir
            )
            val wadadaSoddonka = listOf(
                Pair(2.0420, 45.2980),
                Pair(2.0370, 45.3040), // Digfeer
                Pair(2.0340, 45.3080), // Banaadir
                Pair(2.0315, 45.3130), // Zoobe
                Pair(2.0290, 45.3180)  // KM4
            )
            val airportRoad = listOf(
                Pair(2.0290, 45.3180), // KM4
                Pair(2.0190, 45.3110),
                Pair(2.0160, 45.3060)  // Airport
            )
            val taleexBanadir = listOf(
                Pair(2.0340, 45.3080), // Banadir
                Pair(2.0335, 45.3150), // Taleex
                Pair(2.0290, 45.3180)  // KM4
            )
            val tarabuunkaRoad = listOf(
                Pair(2.0400, 45.3050), // Tarabuunka
                Pair(2.0340, 45.3080)  // Banadir
            )
            val lidoRoad = listOf(
                Pair(2.0430, 45.3330), // Sayidka
                Pair(2.0400, 45.3480),
                Pair(2.0420, 45.3620)  // Lido Beach
            )
            val crossStreets = listOf(
                listOf(Pair(2.0390, 45.3280), Pair(2.0410, 45.3240)), // Dabka to Hawlwadaag
                listOf(Pair(2.0350, 45.3235), Pair(2.0335, 45.3150)), // Shaqaalaha to Taleex
                listOf(Pair(2.0450, 45.3200), Pair(2.0430, 45.3330))  // Bakaaro to Sayidka
            )

            // Draw Secondary Cross Streets
            crossStreets.forEach { pts ->
                if (pts.size >= 2) {
                    val p = Path().apply {
                        moveTo(mapX(pts[0].second), mapY(pts[0].first))
                        for (i in 1 until pts.size) {
                            lineTo(mapX(pts[i].second), mapY(pts[i].first))
                        }
                    }
                    drawPath(p, color = Color(0xFFCBD5E1), style = Stroke(width = 8f * scale, cap = StrokeCap.Round))
                    drawPath(p, color = Color.White, style = Stroke(width = 5f * scale, cap = StrokeCap.Round))
                }
            }

            // Draw Major Highways & Avenues
            val majorRoads = listOf(makaMukarama, wadadaWadnaha, wadadaSoddonka, airportRoad, taleexBanadir, tarabuunkaRoad, lidoRoad)
            majorRoads.forEach { pts ->
                if (pts.size >= 2) {
                    val p = Path().apply {
                        moveTo(mapX(pts[0].second), mapY(pts[0].first))
                        for (i in 1 until pts.size) {
                            lineTo(mapX(pts[i].second), mapY(pts[i].first))
                        }
                    }
                    // Outer border
                    drawPath(p, color = Color(0xFF94A3B8), style = Stroke(width = 13f * scale, cap = StrokeCap.Round))
                    // Road Surface
                    drawPath(p, color = Color(0xFFFFFFFF), style = Stroke(width = 9f * scale, cap = StrokeCap.Round))
                    // Center Dash Line
                    drawPath(p, color = Color(0xFFE2E8F0), style = Stroke(width = 2f * scale, cap = StrokeCap.Round))
                }
            }

            // 6. Animated Roaming Bajaaj Vehicles on Roads (To show dynamic live city traffic)
            val carPositions = listOf(
                // Vehicle 1 on Maka Al-Mukarama
                Offset(
                    mapX(45.3180 + (45.3330 - 45.3180) * carMotionPhase),
                    mapY(2.0290 + (2.0430 - 2.0290) * carMotionPhase)
                ),
                // Vehicle 2 on Wadada Soddonka
                Offset(
                    mapX(45.3080 + (45.3180 - 45.3080) * ((carMotionPhase + 0.5f) % 1f)),
                    mapY(2.0340 + (2.0290 - 2.0340) * ((carMotionPhase + 0.5f) % 1f))
                ),
                // Vehicle 3 on Wadada Wadnaha
                Offset(
                    mapX(45.3240 - (45.3240 - 45.3080) * carMotionPhase),
                    mapY(2.0410 - (2.0410 - 2.0340) * carMotionPhase)
                )
            )

            carPositions.forEach { carOffset ->
                // Draw small animated Bajaaj tuk-tuk icon on the map
                drawCircle(color = GoBajaajYellow, radius = 5.5f * scale, center = carOffset)
                drawCircle(color = GoBajaajNavy, radius = 2.5f * scale, center = carOffset)
            }

            // 7. Landmark & District Name Texts
            val districtPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = (11f * scale).coerceIn(10f, 18f) * density
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText("HODAN", mapX(45.310), mapY(2.042), districtPaint)
                canvas.nativeCanvas.drawText("WAABERI", mapX(45.325), mapY(2.025), districtPaint)
                canvas.nativeCanvas.drawText("BADWEYNTA HINDIYA", mapX(45.345), mapY(2.015), districtPaint)
                canvas.nativeCanvas.drawText("✈️ Garoonka Aadan Cadde", mapX(45.306), mapY(2.010), districtPaint)
                canvas.nativeCanvas.drawText("🏖️ Xeebta Liido", mapX(45.362), mapY(2.048), districtPaint)
            }

            // 8. Draw Intersections with traffic light status
            val textPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#0F172A")
                textSize = (11.5f * scale).coerceIn(10f, 20f) * density
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val badgeBgPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#FFFFFF")
                isAntiAlias = true
                setShadowLayer(4f, 0f, 2f, android.graphics.Color.parseColor("#30000000"))
            }

            displayIntersections.forEach { item ->
                val cx = mapX(item.longitude)
                val cy = mapY(item.latitude)
                val isSelected = item.id == selectedIntersectionId
                val isBlinking = blinkingIntersectionIds.contains(item.id) || isSelected
                val status = TrafficStatus.fromString(item.currentStatus)
                val baseColor = when (status) {
                    TrafficStatus.FURAN -> Color(0xFF16A34A)
                    TrafficStatus.DHEXDHEXAAD -> Color(0xFFEAB308)
                    TrafficStatus.MASHQUUL -> Color(0xFFF97316)
                    TrafficStatus.SAXMAD_BADAN -> Color(0xFFDC2626)
                    TrafficStatus.XIRAN -> Color(0xFF000000)
                    else -> Color(0xFF16A34A)
                }

                if (isBlinking) {
                    drawCircle(
                        color = baseColor.copy(alpha = pulseAlpha),
                        radius = (20f * pulseAnim) * scale,
                        center = Offset(cx, cy)
                    )
                }

                drawCircle(
                    color = Color.White,
                    radius = (if (isSelected) 13f else 9.5f) * scale,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = baseColor,
                    radius = (if (isSelected) 10f else 7f) * scale,
                    center = Offset(cx, cy)
                )

                // High contrast label card for intersection name
                val labelText = item.name.replace("Isgoyska ", "")
                val textWidth = textPaint.measureText(labelText)
                drawIntoCanvas { canvas ->
                    val bgRect = android.graphics.RectF(
                        cx - textWidth / 2 - 8f,
                        cy + (10f * scale),
                        cx + textWidth / 2 + 8f,
                        cy + (24f * scale)
                    )
                    canvas.nativeCanvas.drawRoundRect(bgRect, 6f, 6f, badgeBgPaint)
                    canvas.nativeCanvas.drawText(labelText, cx, cy + (20f * scale), textPaint)
                }
            }

            // 9. User GPS Location Pin (Pulsing Radar + Pin)
            if (userLatitude != null && userLongitude != null) {
                val ux = mapX(userLongitude)
                val uy = mapY(userLatitude)

                // Radar ring
                drawCircle(
                    color = Color(0xFF2563EB).copy(alpha = pulseAlpha),
                    radius = (26f * pulseAnim) * scale,
                    center = Offset(ux, uy)
                )
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = 0.35f),
                    radius = 16f * scale,
                    center = Offset(ux, uy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 11f * scale,
                    center = Offset(ux, uy)
                )
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = 8f * scale,
                    center = Offset(ux, uy)
                )

                val userLabel = customUserPinLabel ?: "Goobtaada Hadda"
                val userTextPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = (11f * scale).coerceIn(10f, 18f) * density
                    typeface = Typeface.DEFAULT_BOLD
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#0F172A")
                    isAntiAlias = true
                    setShadowLayer(6f, 0f, 3f, android.graphics.Color.parseColor("#50000000"))
                }
                val textWidth = userTextPaint.measureText(userLabel)
                drawIntoCanvas { canvas ->
                    val bgRect = android.graphics.RectF(
                        ux - textWidth / 2 - 14f,
                        uy - (32f * scale),
                        ux + textWidth / 2 + 14f,
                        uy - (10f * scale)
                    )
                    canvas.nativeCanvas.drawRoundRect(bgRect, 10f, 10f, bgPaint)
                    canvas.nativeCanvas.drawText(userLabel, ux, uy - (18f * scale), userTextPaint)
                }
            }
        }

        // Zoom & Reset controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(4.5f) },
                containerColor = Color.White,
                contentColor = BrandBlack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
            SmallFloatingActionButton(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.8f) },
                containerColor = Color.White,
                contentColor = BrandBlack,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
            SmallFloatingActionButton(
                onClick = {
                    scale = 1.15f
                    offset = Offset.Zero
                },
                containerColor = Color.White,
                contentColor = GoBajaajBlue,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Dib u eeg", modifier = Modifier.size(20.dp))
            }
        }

        // Traffic status legend (Bottom Start)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.98f),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Xaaladda Waddooyinka",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Furan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlack)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEAB308)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dhex", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlack)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFDC2626)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xiran", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlack)
                    }
                }
            }
        }
    }
}
