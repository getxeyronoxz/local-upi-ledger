package org.xeyronox.ledger.domain.model

import java.security.MessageDigest

data class Transaction(
    val id: String,
    val timestamp: Long,
    val amount: Double,
    val type: TransactionType,
    val status: TransactionStatus,
    val partyName: String,
    val upiId: String? = null,
    val referenceId: String? = null,
    val appSource: String,
    val rawTextDump: String = ""
) {
    companion object {
        fun generateId(
            timestamp: Long,
            amount: Double,
            partyName: String,
            referenceId: String? = null
        ): String {
            // Normalized to 5-minute time window if referenceId is null, to prevent duplicate jitter on repeated taps
            val timeKey = if (referenceId != null) timestamp.toString() else (timestamp / (5 * 60 * 1000)).toString()
            val raw = "$timeKey|$amount|${partyName.trim().lowercase()}|${referenceId?.trim() ?: ""}"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
