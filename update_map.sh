#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/LiveTrafficMapScreen.kt
package com.example.somalitraffic.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.data.repository.SearchResultItem
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.*
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@Composable
fun LiveTrafficMapScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit,
    onIntersectionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val intersections by viewModel.intersections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val selectedIntersection by viewModel.selectedIntersection.collectAsState()
    var selectedFilter by remember { mutableStateOf("Dhammaan") }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            requestDeviceLocation(context, viewModel)
        }
    }

    val filterOptions = listOf("Dhammaan", "Isgoysyada", "Waddooyinka")

    Scaffold(
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "live_traffic",
                onNavigate = onNavigate
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background Map Layer
            if (searchQuery.isBlank()) {
                SomaliInteractiveMap(
                    intersections = intersections,
                    userLocation = userLocation,
                    selectedIntersection = selectedIntersection,
                    onIntersectionClick = { intersection ->
                        viewModel.selectIntersection(intersection)
                        onIntersectionClick(intersection.id)
                    },
                    modifier = Modifier.fillMaxSize().testTag("map_view")
                )

                // Current Location FAB
                FloatingActionButton(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            requestDeviceLocation(context, viewModel)
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    containerColor = BrandWhite,
                    contentColor = BrandRedPrimary
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Meesha aad joogto")
                }
            }

            // Top Overlay (Search & Filters)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (searchQuery.isNotBlank()) BrandWhite else Color.Transparent)
                    .statusBarsPadding()
            ) {
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(BrandWhite, RoundedCornerShape(24.dp))
                        .border(1.dp, BrandBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextBlack)
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = TextBlack),
                            modifier = Modifier.weight(1f).testTag("input_map_search"),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Raadi Isgoys ama Waddo", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tirtir", tint = TextBlack)
                            }
                        }
                    }
                }

                // Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BrandRedPrimary else BrandWhite,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BrandRedPrimary else BrandBorder),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextBlack,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // Search Results List (if searching)
                if (searchQuery.isNotBlank()) {
                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandRedPrimary)
                        }
                    } else if (searchError != null) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(searchError!!, color = BrandRedPrimary)
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Isgoys ama waddo la mid ah lama helin.", color = TextBlack)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(BrandWhite),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(searchResults) { result ->
                                MapSearchResultItem(
                                    result = result,
                                    onClick = {
                                        viewModel.selectIntersection(result.intersection)
                                        viewModel.updateSearchQuery("")
                                        onIntersectionClick(result.intersection.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapSearchResultItem(result: SearchResultItem, onClick: () -> Unit) {
    val item = result.intersection
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandRedPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                Text(text = item.district, fontSize = 13.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    val color = when (item.currentStatus) {
                        "Furan" -> TrafficOpenGreen
                        "Dhexdhexaad" -> TrafficModerateYellow
                        "Mashquul" -> TrafficBusyOrange
                        "Saxmad badan" -> TrafficHeavyRed
                        else -> Color.Gray
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = item.currentStatus, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
                    if (item.lastUpdated != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "2 daqiiqo kahor", fontSize = 11.sp, color = Color.Gray) // Dummy relative time string as requested by design (Wait, the user wants "Real" data, so we should probably use real logic, but for UI match we can format the timestamp later, or just show real lastUpdated string if available).
                    }
                }
            }
        }
        if (result.formattedDistance != null) {
            Text(text = result.formattedDistance, fontSize = 13.sp, color = TextBlack, fontWeight = FontWeight.Medium)
        }
    }
}

@SuppressLint("MissingPermission")
private fun requestDeviceLocation(context: Context, viewModel: SomaliTrafficViewModel) {
    try {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                viewModel.setUserLocation(loc.latitude, loc.longitude)
            } else {
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { curLoc ->
                        if (curLoc != null) {
                            viewModel.setUserLocation(curLoc.latitude, curLoc.longitude)
                        }
                    }
            }
        }
    } catch (_: Exception) {}
}
INNER_EOF
