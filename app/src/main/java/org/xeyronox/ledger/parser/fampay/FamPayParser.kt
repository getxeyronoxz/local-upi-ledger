package org.xeyronox.ledger.parser.fampay

import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType
import org.xeyronox.ledger.domain.parser.AccessibilityNodeData
import org.xeyronox.ledger.domain.parser.UpiAppParser

class FamPayParser : UpiAppParser {
    override val targetPackage: String = "com.famapp"
    override val appDisplayName: String = "FamPay"

    override fun parse(rootNode: AccessibilityNodeData): List<Transaction> {
        // Modular parser implementation for FamPay transactions
        val transactions = mutableListOf<Transaction>()
        val texts = rootNode.collectAllText()
        val amountRegex = Regex("""(?:₹|Rs\.?)\s*([0-9,]+(?:\.[0-9]{1,2})?)""")

        for (text in texts) {
            val match = amountRegex.find(text)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
                val isCredit = text.contains("added", ignoreCase = true) || text.contains("received", ignoreCase = true)
                val isFailed = text.contains("failed", ignoreCase = true) || text.contains("declined", ignoreCase = true)
                val type = if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT
                val status = if (isFailed) TransactionStatus.FAILED else TransactionStatus.SUCCESS
                val now = System.currentTimeMillis()
                val id = Transaction.generateId(now, amount, "FamPay Transaction", null)

                transactions.add(
                    Transaction(
                        id = id,
                        timestamp = now,
                        amount = amount,
                        type = type,
                        status = status,
                        partyName = "FamPay Transaction",
                        appSource = appDisplayName,
                        rawTextDump = text
                    )
                )
            }
        }
        return transactions
    }
}
