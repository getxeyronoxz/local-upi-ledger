package org.xeyronox.ledger.parser.phonepe

import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType
import org.xeyronox.ledger.domain.parser.AccessibilityNodeData
import org.xeyronox.ledger.domain.parser.UpiAppParser

class PhonePeParser : UpiAppParser {
    override val targetPackage: String = "com.phonepe.app"
    override val appDisplayName: String = "PhonePe"

    override fun parse(rootNode: AccessibilityNodeData): List<Transaction> {
        // Modular parser implementation for PhonePe history screen
        val transactions = mutableListOf<Transaction>()
        val texts = rootNode.collectAllText()
        val amountRegex = Regex("""(?:₹|Rs\.?)\s*([0-9,]+(?:\.[0-9]{1,2})?)""")

        for (text in texts) {
            val match = amountRegex.find(text)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
                val isCredit = text.contains("received", ignoreCase = true) || text.contains("credited", ignoreCase = true)
                val isFailed = text.contains("failed", ignoreCase = true) || text.contains("declined", ignoreCase = true)
                val type = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT
                val status = if (isFailed) TransactionStatus.FAILED else TransactionStatus.SUCCESS
                val now = System.currentTimeMillis()
                val id = Transaction.generateId(now, amount, "PhonePe Merchant", null)

                transactions.add(
                    Transaction(
                        id = id,
                        timestamp = now,
                        amount = amount,
                        type = type,
                        status = status,
                        partyName = "PhonePe Transaction",
                        appSource = appDisplayName,
                        rawTextDump = text
                    )
                )
            }
        }
        return transactions
    }
}
