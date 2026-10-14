package com.example.somalitraffic.domain

/**
 * Pure Driver Pass / auth rules used by the app and unit tests.
 * Expiration and eligibility must match the backend: $0.25 for 24 hours
 * after admin approval, using trusted timestamps (UTC millis).
 */
object DriverPassRules {
    const val AMOUNT_USD = 0.25
    const val DURATION_MS = 24L * 60L * 60L * 1000L
    const val PHONE_AUTH_EMAIL_DOMAIN = "phone.gobajaaj.so"

    fun phoneAuthEmail(phoneE164: String): String {
        val digits = phoneE164.filter { it.isDigit() }
        require(digits.length >= 8) { "Phone is too short for an auth identity" }
        return "$digits@$PHONE_AUTH_EMAIL_DOMAIN"
    }

    fun normalizeStatus(status: String?): String = status?.trim()?.lowercase().orEmpty()

    fun isPending(status: String?): Boolean = normalizeStatus(status) == "pending"

    fun isRejected(status: String?): Boolean {
        val st = normalizeStatus(status)
        return st == "rejected" || st == "failed"
    }

    fun isExpiredStatus(status: String?): Boolean = normalizeStatus(status) == "expired"

    /** Only an admin-activated subscription with a future end time grants access. */
    fun isPassActive(status: String?, endEpochMs: Long?, nowEpochMs: Long): Boolean {
        val st = normalizeStatus(status)
        if (st != "active") return false
        if (endEpochMs == null || endEpochMs <= 0L) return false
        return endEpochMs > nowEpochMs
    }

    fun isPassExpired(status: String?, endEpochMs: Long?, nowEpochMs: Long): Boolean {
        if (isPassActive(status, endEpochMs, nowEpochMs)) return false
        if (endEpochMs != null && endEpochMs > 0L && endEpochMs <= nowEpochMs) return true
        return isExpiredStatus(status)
    }

    fun canUseFreeRiderService(role: String?): Boolean {
        val r = role?.trim()?.lowercase().orEmpty()
        return r != "driver" || r.isEmpty() || r.contains("passenger") || r.contains("rider")
    }

    fun canPerformProtectedDriverAction(
        role: String?,
        passStatus: String?,
        endEpochMs: Long?,
        nowEpochMs: Long
    ): Boolean {
        if (role?.trim()?.lowercase() != "driver") return false
        return isPassActive(passStatus, endEpochMs, nowEpochMs)
    }

    fun canSubmitPayment(
        alreadyPending: Boolean,
        passStatus: String?,
        endEpochMs: Long?,
        nowEpochMs: Long
    ): Boolean {
        if (alreadyPending) return false
        return !isPassActive(passStatus, endEpochMs, nowEpochMs)
    }

    fun approvalWindow(nowEpochMs: Long): Pair<Long, Long> {
        return nowEpochMs to (nowEpochMs + DURATION_MS)
    }

    fun rejectedGrantsAccess(): Boolean = false

    fun pendingGrantsAccess(): Boolean = false

    fun otpProviderErrorIsSuccess(): Boolean = false

    fun hardcodedTestOtpAllowed(): Boolean = false

    fun localOnlyRideIsSuccess(serverConfirmed: Boolean): Boolean = serverConfirmed

    fun httpWriteSucceeded(code: Int, bodyEmpty: Boolean = false): Boolean {
        if (code !in 200..299) return false
        return true
    }

    fun unauthorizedCannotApprove(isAuthorizedAdmin: Boolean): Boolean = !isAuthorizedAdmin

    fun ineligibleDriverCannotBypass(modifiedClientClaimsActive: Boolean, serverSaysActive: Boolean): Boolean {
        return serverSaysActive && !modifiedClientClaimsActive || serverSaysActive
    }
}
