package com.example.somalitraffic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.TrafficStatus
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SomaliTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    containerColor: Color = BrandWhite,
    contentColor: Color = TextBlack,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Dib u noqo",
                        tint = contentColor
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}

@Composable
fun TrafficStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val trafficStatus = TrafficStatus.fromString(status)
    val (bgColor, textColor, icon) = when (trafficStatus) {
        TrafficStatus.FURAN -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Icons.Default.CheckCircle)
        TrafficStatus.NORMAL -> Triple(Color(0xFFFEF9C3), Color(0xFFCA8A04), Icons.Default.Warning)
        TrafficStatus.JAAM, TrafficStatus.XERAN -> Triple(Color(0xFFFEE2E2), Color(0xFFE50914), Icons.Default.Traffic)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = status,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String = "Xog wali lama hayo",
    message: String = "Marka xog cusub la helo ayaa halkan lagu soo bandhigi doonaa.",
    icon: ImageVector = Icons.Outlined.Info,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BrandLightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlack,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TextBlack,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag("btn_empty_retry"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary)
            ) {
                Text("Mar kale isku day", color = TextWhite, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LoadingStateView(message: String = "Fadlan sug... Xogta ayaa la soo dejinayaa") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = BrandRedPrimary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
        }
    }
}

// ─── Design-ka Cusub: Urban Mobility Dark Bottom Nav ────────────────────────
// 4 tabs isku mid ah labada role (Driver + Passenger):
//   Guudmar · Khariiradda · Dalbo · Akoonka
// Active tab = casaan pill indicator + casaan icon/label
// ─────────────────────────────────────────────────────────────────────────────

private val NavBgDark      = Color(0xFF0F131C)  // surface
private val NavContainerLow = Color(0xFF181C24) // surface-container-low
private val NavActiveRed   = Color(0xFFE50914)  // primary-container
private val NavActiveRedBg = Color(0x26E50914)  // primary-container/15
private val NavTextActive  = Color(0xFFDFE2EE)  // on-surface
private val NavTextInactive = Color(0x99DFE2EE) // on-surface-variant

@Composable
fun SomaliBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isDriver: Boolean = false
) {
    val tabs = listOf(
        Triple("home", Icons.Filled.Home, "Home"),
        Triple("ride", Icons.Filled.ReceiptLong, "Orders"),
        Triple(if (isDriver) "driver_pass" else "home", Icons.Filled.AccountBalanceWallet, "Wallet"),
        Triple("profile_settings", Icons.Filled.Person, "Profile")
    )

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (route, icon, label) ->
                val selected = currentRoute == route
                val iconTint = if (selected) Color(0xFFE50914) else Color(0xFF64748B)
                val labelColor = if (selected) Color(0xFFE50914) else Color(0xFF64748B)
                val labelWeight = if (selected) FontWeight.Black else FontWeight.Medium

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(route) }
                        .testTag("nav_${label.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = labelWeight,
                        color = labelColor
                    )
                }
            }
        }
    }
}

// ─── Driver Secondary Tab Bar (Shaqada · Khariiradda · Driver Pass · Dakhliga) ─
// Tan waa contextual tabs gudaha HomeDashboardScreen ee darwalka
@Composable
fun DriverSecondaryTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Pair(Icons.Filled.Work,                "Shaqada"),
        Pair(Icons.Outlined.Explore,           "Khariiradda"),
        Pair(Icons.Filled.ConfirmationNumber,  "Driver Pass"),
        Pair(Icons.Filled.AccountBalanceWallet,"Dakhliga")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF181C24)) // surface-container-low
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, (icon, label) ->
            val isSelected = selectedTab == index
            val bgColor = if (isSelected) Color(0xFF262A33) else Color.Transparent
            val iconTint = if (isSelected) Color(0xFF4EDEA3) else Color(0x99DFE2EE)
            val textColor = if (isSelected) Color(0xFFDFE2EE) else Color(0x99DFE2EE)
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 8.dp, horizontal = 2.dp)
                    .testTag("driver_tab_$index"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = fontWeight,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
