package org.xeyronox.ledger.parser.bhim

object BhimRegex {
    val AMOUNT_REGEX = Regex("""(?:₹|Rs\.?|INR)\s*([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    val UTR_REGEX = Regex("""(?:UPI\s*Ref(?:\s*No\.?)?|Ref\s*No\.?|UTR)[:\s]*([0-9]{12})""", RegexOption.IGNORE_CASE)
    val UPI_ID_REGEX = Regex("""([a-zA-Z0-9.\-_]{2,256}@[a-zA-Z]{2,64})""")

    val CREDIT_PREFIXES = listOf("money received from", "received from", "credited to")
    val DEBIT_PREFIXES = listOf("paid to", "sent to", "debited from", "transferred to")

    val FAILED_KEYWORDS = listOf("failed", "declined", "cancelled", "rejected", "auto-reversed", "reversed")
    val PENDING_KEYWORDS = listOf("pending", "processing", "initiated")
}
