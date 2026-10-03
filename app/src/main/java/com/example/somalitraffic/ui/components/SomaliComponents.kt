package com.example.somalitraffic.ui.components

import androidx.compose.foundation.background
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
        TrafficStatus.DHEXDHEXAAD -> Triple(Color(0xFFFEF9C3), Color(0xFFCA8A04), Icons.Default.Warning)
        TrafficStatus.MASHQUUL, TrafficStatus.SAXMAD_BADAN -> Triple(Color(0xFFFEE2E2), Color(0xFFE50914), Icons.Default.Traffic)
        TrafficStatus.XIRAN -> Triple(Color(0xFFE5E5E5), Color(0xFF000000), Icons.Default.Block)
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

@Composable
fun SomaliBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isDriver: Boolean = false
) {
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = GoBajaajBlue,
        selectedTextColor = GoBajaajBlue,
        indicatorColor = GoBajaajBlueLight,
        unselectedIconColor = TextMutedDark,
        unselectedTextColor = TextMutedDark
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        // 1. Home
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home", fontSize = 10.sp, fontWeight = if (currentRoute == "home") FontWeight.Bold else FontWeight.Medium) },
            colors = navColors
        )

        // 2. Ride (Dalbo / Dalabaadka)
        NavigationBarItem(
            selected = currentRoute == "ride",
            onClick = { onNavigate("ride") },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                    contentDescription = "Ride",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(if (isDriver) "Dalabaadka" else "Dalbo", fontSize = 10.sp, fontWeight = if (currentRoute == "ride") FontWeight.Bold else FontWeight.Medium) },
            colors = navColors
        )

        // 3. Waddooyinka (Saxmadda) - ONLY FOR DRIVERS
        if (isDriver) {
            NavigationBarItem(
                selected = currentRoute == "live_traffic",
                onClick = { onNavigate("live_traffic") },
                icon = {
                    Icon(
                        imageVector = if (currentRoute == "live_traffic") Icons.Filled.Traffic else Icons.Outlined.Traffic,
                        contentDescription = "Waddooyinka"
                    )
                },
                label = { Text("Waddooyinka", fontSize = 10.sp, fontWeight = if (currentRoute == "live_traffic") FontWeight.Bold else FontWeight.Medium) },
                colors = navColors
            )
        }

        // 4. Profile
        NavigationBarItem(
            selected = currentRoute == "profile_settings",
            onClick = { onNavigate("profile_settings") },
            icon = {
                Icon(
                    imageVector = if (currentRoute == "profile_settings") Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile", fontSize = 10.sp, fontWeight = if (currentRoute == "profile_settings") FontWeight.Bold else FontWeight.Medium) },
            colors = navColors
        )
    }
}
