#!/bin/bash
# Fix LiveTrafficMapScreen
sed -i 's/userLocation = userLocation,/userLatitude = userLocation?.first,\n                    userLongitude = userLocation?.second,/g' app/src/main/java/com/example/somalitraffic/ui/screens/LiveTrafficMapScreen.kt
sed -i 's/selectedIntersection = selectedIntersection,/selectedIntersectionId = selectedIntersection?.id,/g' app/src/main/java/com/example/somalitraffic/ui/screens/LiveTrafficMapScreen.kt
sed -i 's/if (result.formattedDistance != null) {/result.formattedDistance?.let {/g' app/src/main/java/com/example/somalitraffic/ui/screens/LiveTrafficMapScreen.kt
sed -i 's/Text(text = result.formattedDistance/Text(text = it/g' app/src/main/java/com/example/somalitraffic/ui/screens/LiveTrafficMapScreen.kt

# Fix TrafficAlertsScreen
sed -i 's/text = incident.description,/text = incident.description ?: "",/g' app/src/main/java/com/example/somalitraffic/ui/screens/TrafficAlertsScreen.kt
sed -i 's/incident.reportedAt/incident.createdAt/g' app/src/main/java/com/example/somalitraffic/ui/screens/TrafficAlertsScreen.kt

# Fix LoginScreen R reference
sed -i '/import com.example.somalitraffic.R/d' app/src/main/java/com/example/somalitraffic/ui/screens/LoginScreen.kt

# Fix HomeDashboardScreen R reference
sed -i '/import com.example.somalitraffic.R/d' app/src/main/java/com/example/somalitraffic/ui/screens/HomeDashboardScreen.kt

# Fix DriverPassScreen submitManualPayment
sed -i 's/submitManualPayment(/payDriverPass(/g' app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt
sed -i 's/method = "EVC\/ZAAD\/SAHAL",//g' app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt
sed -i 's/account = "Manual",//g' app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt
sed -i 's/amount = 1.0,//g' app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt

