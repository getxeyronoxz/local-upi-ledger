package org.xeyronox.ledger.parser.bhim

import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType
import org.xeyronox.ledger.domain.parser.AccessibilityNodeData
import org.xeyronox.ledger.domain.parser.UpiAppParser

class BhimParser : UpiAppParser {

    override val targetPackage: String = "in.org.npci.upiapp"
    override val appDisplayName: String = "BHIM"

    override fun parse(rootNode: AccessibilityNodeData): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        findTransactionContainers(rootNode, transactions)
        return transactions
    }

    override fun parseDetailReceipt(rootNode: AccessibilityNodeData): Transaction? {
        val texts = rootNode.collectAllText()
        if (texts.none { it.contains("Transaction", ignoreCase = true) || it.contains("Details", ignoreCase = true) }) {
            return null
        }

        var amount: Double? = null
        var type: TransactionType = TransactionType.DEBIT
        var status: TransactionStatus = TransactionStatus.SUCCESS
        var partyName = "UPI Transaction"
        var utr: String? = null
        var upiId: String? = null

        for (text in texts) {
            // Check UTR
            val utrMatch = BhimRegex.UTR_REGEX.find(text)
            if (utrMatch != null) {
                utr = utrMatch.groupValues[1]
            }

            // Check VPA
            val vpaMatch = BhimRegex.UPI_ID_REGEX.find(text)
            if (vpaMatch != null && upiId == null) {
                upiId = vpaMatch.groupValues[1]
            }

            // Check Amount
            val amountMatch = BhimRegex.AMOUNT_REGEX.find(text)
            if (amountMatch != null && amount == null) {
                amount = parseAmount(amountMatch.groupValues[1])
            }

            // Check Direction & Party
            val lower = text.lowercase()
            for (prefix in BhimRegex.CREDIT_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    type = TransactionType.CREDIT
                    partyName = text.substring(prefix.length).trim().removePrefix(":").trim()
                }
            }
            for (prefix in BhimRegex.DEBIT_PREFIXES) {
                if (lower.startsWith(prefix)) {
                    type = TransactionType.DEBIT
                    partyName = text.substring(prefix.length).trim().removePrefix(":").trim()
                }
            }

            // Check Status
            if (BhimRegex.FAILED_KEYWORDS.any { lower.contains(it) }) {
                status = TransactionStatus.FAILED
            } else if (BhimRegex.PENDING_KEYWORDS.any { lower.contains(it) }) {
                status = TransactionStatus.PENDING
            }
        }

        if (amount == null) return null

        val now = System.currentTimeMillis()
        val id = Transaction.generateId(now, amount, partyName, utr)

        return Transaction(
            id = id,
            timestamp = now,
            amount = amount,
            type = type,
            status = status,
            partyName = partyName,
            upiId = upiId,
            referenceId = utr,
            appSource = appDisplayName,
            rawTextDump = texts.joinToString(" | ")
        )
    }

    private fun findTransactionContainers(
        node: AccessibilityNodeData,
        outList: MutableList<Transaction>
    ) {
        val texts = node.collectAllText()
        val amountMatch = texts.firstNotNullOfOrNull { BhimRegex.AMOUNT_REGEX.find(it) }

        // If this node contains an amount and directional indicator, check if it's a leaf row container
        if (amountMatch != null && hasDirectionalIndicator(texts)) {
            // Check if any child also satisfies this condition (prefer deepest container for clean row parsing)
            val childHasRow = node.children.any { child ->
                val childTexts = child.collectAllText()
                childTexts.any { BhimRegex.AMOUNT_REGEX.containsMatchIn(it) } && hasDirectionalIndicator(childTexts)
            }

            if (!childHasRow) {
                val tx = parseSingleRow(texts, amountMatch)
                if (tx != null) {
                    outList.add(tx)
                    return
                }
            }
        }

        for (child in node.children) {
            findTransactionContainers(child, outList)
        }
    }

    private fun hasDirectionalIndicator(texts: List<String>): Boolean {
        val joined = texts.joinToString(" ").lowercase()
        return BhimRegex.CREDIT_PREFIXES.any { joined.contains(it) } ||
                BhimRegex.DEBIT_PREFIXES.any { joined.contains(it) } ||
                joined.contains("↓") || joined.contains("↑")
    }

    private fun parseSingleRow(texts: List<String>, amountMatch: MatchResult): Transaction? {
        val amount = parseAmount(amountMatch.groupValues[1]) ?: return null

        var type = TransactionType.DEBIT
        var status = TransactionStatus.SUCCESS
        var partyName = "Unknown"
        var utr: String? = null
        var upiId: String? = null

        for (text in texts) {
            val lower = text.lowercase()

            // Credit Check
            for (prefix in BhimRegex.CREDIT_PREFIXES) {
                if (lower.startsWith(prefix) || lower.contains(prefix)) {
                    type = TransactionType.CREDIT
                    val extracted = text.substring(lower.indexOf(prefix) + prefix.length).trim()
                    if (extracted.isNotBlank()) partyName = extracted
                }
            }

            // Debit Check
            for (prefix in BhimRegex.DEBIT_PREFIXES) {
                if (lower.startsWith(prefix) || lower.contains(prefix)) {
                    type = TransactionType.DEBIT
                    val extracted = text.substring(lower.indexOf(prefix) + prefix.length).trim()
                    if (extracted.isNotBlank()) partyName = extracted
                }
            }

            // Status Check
            if (BhimRegex.FAILED_KEYWORDS.any { lower.contains(it) }) {
                status = TransactionStatus.FAILED
            } else if (BhimRegex.PENDING_KEYWORDS.any { lower.contains(it) }) {
                status = TransactionStatus.PENDING
            }

            // Check UTR & VPA
            BhimRegex.UTR_REGEX.find(text)?.let { utr = it.groupValues[1] }
            BhimRegex.UPI_ID_REGEX.find(text)?.let { if (upiId == null) upiId = it.groupValues[1] }
        }

        val now = System.currentTimeMillis()
        val id = Transaction.generateId(now, amount, partyName, utr)

        return Transaction(
            id = id,
            timestamp = now,
            amount = amount,
            type = type,
            status = status,
            partyName = partyName,
            upiId = upiId,
            referenceId = utr,
            appSource = appDisplayName,
            rawTextDump = texts.joinToString(" | ")
        )
    }

    private fun parseAmount(raw: String): Double? {
        return try {
            raw.replace(",", "").trim().toDouble()
        } catch (_: Exception) {
            null
        }
    }
}
