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
        assertEquals("SHIRI", appName)
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

    @Test
    fun `test marketplace order lifecycle and status flow`() {
        val order = com.example.somalitraffic.data.model.ShiriMarketOrder(
            id = "test-order-1",
            orderNumber = "#SHR-001",
            customerName = "Ahmed Hassan",
            customerPhone = "0612345678",
            merchantName = "Pizza Palace",
            itemsSummary = "Margherita Pizza x1",
            subtotal = 12.00,
            deliveryFee = 1.00,
            serviceFee = 0.50,
            total = 13.50,
            status = "pending",
            handoverPin = "4829"
        )

        assertEquals("pending", order.status)
        assertEquals(13.50, order.total, 0.01)

        // Status transition: Merchant accepts
        val preparing = order.copy(status = "preparing")
        assertEquals("preparing", preparing.status)

        // Status transition: Merchant marks ready
        val ready = preparing.copy(status = "ready_for_pickup")
        assertEquals("ready_for_pickup", ready.status)

        // Status transition: Courier assigned and picked up
        val inTransit = ready.copy(status = "picked_up", courierId = "courier-123")
        assertEquals("picked_up", inTransit.status)
        assertEquals("courier-123", inTransit.courierId)

        // Status transition: Delivered
        val delivered = inTransit.copy(status = "delivered")
        assertEquals("delivered", delivered.status)
    }

    @Test
    fun `test marketplace categories and store catalog completeness`() {
        val categories = com.example.somalitraffic.data.model.ShiriMarketRepository.getDefaultCategories()
        assertTrue("Marketplace should have at least 15 categories", categories.size >= 15)

        val stores = com.example.somalitraffic.data.model.ShiriMarketRepository.getDefaultStores()
        assertTrue("Marketplace should have stores", stores.isNotEmpty())

        val pizzaPalace = stores.find { it.name.contains("Pizza Palace", ignoreCase = true) }
        assertTrue("Pizza Palace should exist in default stores", pizzaPalace != null)
        assertTrue("Pizza Palace should have products", pizzaPalace!!.products.isNotEmpty())
    }
}
