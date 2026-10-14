package com.example.somalitraffic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverPassRulesTest {

    private val now = 1_700_000_000_000L

    @Test
    fun amountAndDurationMatchBusinessRules() {
        assertEquals(0.25, DriverPassRules.AMOUNT_USD, 0.0)
        assertEquals(24L * 60L * 60L * 1000L, DriverPassRules.DURATION_MS)
    }

    @Test
    fun phoneAuthEmailIsDerivedFromDigits() {
        assertEquals("252615123456@phone.gobajaaj.so", DriverPassRules.phoneAuthEmail("+252615123456"))
        assertEquals("252615123456@phone.gobajaaj.so", DriverPassRules.phoneAuthEmail("252 615 123456"))
        assertEquals("00252615123456@phone.gobajaaj.so", DriverPassRules.phoneAuthEmail("00252615123456"))
    }

    @Test
    fun onlyActiveWithFutureEndGrantsAccess() {
        assertTrue(DriverPassRules.isPassActive("active", now + 1_000, now))
        assertFalse(DriverPassRules.isPassActive("active", now - 1_000, now))
        assertFalse(DriverPassRules.isPassActive("active", now, now))
        assertFalse(DriverPassRules.isPassActive("active", null, now))
        assertFalse(DriverPassRules.isPassActive("active", 0L, now))
    }

    @Test
    fun nonActiveStatusesNeverGrantAccess() {
        val future = now + DriverPassRules.DURATION_MS
        for (status in listOf("pending", "rejected", "failed", "expired", "approved", "completed", null)) {
            assertFalse("status=$status must not be active", DriverPassRules.isPassActive(status, future, now))
        }
    }

    @Test
    fun statusHelpersNormalizeCaseAndWhitespace() {
        assertTrue(DriverPassRules.isPassActive("  ACTIVE ", now + 5_000, now))
        assertTrue(DriverPassRules.isPending(" Pending "))
        assertTrue(DriverPassRules.isRejected("Rejected"))
        assertTrue(DriverPassRules.isRejected("FAILED"))
        assertTrue(DriverPassRules.isExpiredStatus("EXPIRED"))
        assertFalse(DriverPassRules.isPending("active"))
        assertFalse(DriverPassRules.isRejected("pending"))
    }

    @Test
    fun expiredStatusReflectsPastEndOrExplicitExpiry() {
        assertTrue(DriverPassRules.isPassExpired("active", now - 1, now))
        assertTrue(DriverPassRules.isPassExpired("expired", null, now))
        assertFalse(DriverPassRules.isPassExpired("active", now + 1, now))
        assertFalse(DriverPassRules.isPassExpired("pending", null, now))
    }

    @Test
    fun protectedDriverActionRequiresDriverRoleAndActivePass() {
        assertTrue(DriverPassRules.canPerformProtectedDriverAction("driver", "active", now + 1_000, now))
        assertFalse(DriverPassRules.canPerformProtectedDriverAction("passenger", "active", now + 1_000, now))
        assertFalse(DriverPassRules.canPerformProtectedDriverAction("driver", "pending", now + 1_000, now))
        assertFalse(DriverPassRules.canPerformProtectedDriverAction("driver", "active", now - 1, now))
    }

    @Test
    fun canSubmitPaymentOnlyWhenNoActivePassAndNotPending() {
        assertTrue(DriverPassRules.canSubmitPayment(false, "expired", now - 1, now))
        assertFalse(DriverPassRules.canSubmitPayment(true, "expired", now - 1, now))
        assertFalse(DriverPassRules.canSubmitPayment(false, "active", now + 1_000_000, now))
    }

    @Test
    fun ridersUseFreeServiceAndDriversDoNot() {
        assertTrue(DriverPassRules.canUseFreeRiderService("passenger"))
        assertTrue(DriverPassRules.canUseFreeRiderService("rider"))
        assertTrue(DriverPassRules.canUseFreeRiderService(null))
        assertFalse(DriverPassRules.canUseFreeRiderService("driver"))
    }

    @Test
    fun antiBypassGuardsHold() {
        assertFalse(DriverPassRules.rejectedGrantsAccess())
        assertFalse(DriverPassRules.pendingGrantsAccess())
        assertFalse(DriverPassRules.otpProviderErrorIsSuccess())
        assertFalse(DriverPassRules.hardcodedTestOtpAllowed())
        assertFalse(DriverPassRules.unauthorizedCannotApprove(true))
        assertTrue(DriverPassRules.unauthorizedCannotApprove(false))
    }

    @Test
    fun writesRequireServerConfirmation() {
        assertTrue(DriverPassRules.localOnlyRideIsSuccess(true))
        assertFalse(DriverPassRules.localOnlyRideIsSuccess(false))
        assertTrue(DriverPassRules.httpWriteSucceeded(200))
        assertTrue(DriverPassRules.httpWriteSucceeded(201))
        assertFalse(DriverPassRules.httpWriteSucceeded(409))
        assertFalse(DriverPassRules.httpWriteSucceeded(500))
    }

    @Test
    fun approvalWindowIsExactly24Hours() {
        val (start, end) = DriverPassRules.approvalWindow(now)
        assertEquals(now, start)
        assertEquals(now + DriverPassRules.DURATION_MS, end)
    }
}
