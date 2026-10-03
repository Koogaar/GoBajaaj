package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.somalitraffic.data.model.UserRole
import com.example.somalitraffic.data.repository.SomaliTrafficRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GoBajaaj", appName)
    }

    @Test
    fun `test haversine distance calculation in repository`() {
        // Distance between KM4 (2.0335, 45.3190) and Zoobe (2.0298, 45.3097) in Mogadishu is approx ~1.1 km
        val distance = SomaliTrafficRepository.calculateHaversineDistanceMeters(
            lat1 = 2.0335,
            lon1 = 45.3190,
            lat2 = 2.0298,
            lon2 = 45.3097
        )
        assertTrue(distance in 1000.0..1300.0)
    }

    @Test
    fun `test role from string parsing`() {
        assertEquals(UserRole.DRIVER, UserRole.fromString("driver"))
        assertEquals(UserRole.REPORTER, UserRole.fromString("reporter"))
        assertEquals(UserRole.SUPER_ADMIN, UserRole.fromString("super_admin"))
        assertEquals(UserRole.PASSENGER, UserRole.fromString("unknown_value"))
    }
}
