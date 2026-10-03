#!/bin/bash
sed -i 's/padding(horizontal = 16.dp, bottom = 12.dp)/padding(start = 16.dp, end = 16.dp, bottom = 12.dp)/g' app/src/main/java/com/example/somalitraffic/ui/screens/LiveTrafficMapScreen.kt

cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt.patch
--- app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt
+++ app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt
@@ -165,11 +165,9 @@
                             return@Button
                         }
                         viewModel.payDriverPass(
-                            
-                            
-                            
+                            destinationPhone = "618092060",
                             transactionId = transactionId,
-                            onSuccess = {
+                        ) {
                                 successMessage = "Lacagtaada waa la diray. Waa la xaqiijin doonaa dhowaan."
                                 transactionId = ""
                         }
INNER_EOF
patch app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt.patch

