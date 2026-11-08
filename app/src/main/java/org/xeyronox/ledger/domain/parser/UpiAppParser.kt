package org.xeyronox.ledger.domain.parser

import org.xeyronox.ledger.domain.model.Transaction

interface UpiAppParser {
    val targetPackage: String
    val appDisplayName: String

    fun canParse(packageName: String): Boolean = packageName == targetPackage

    /**
     * Parses the transaction history list screen.
     */
    fun parse(rootNode: AccessibilityNodeData): List<Transaction>

    /**
     * Parses an individual transaction detail receipt screen, enriching with UTR if available.
     */
    fun parseDetailReceipt(rootNode: AccessibilityNodeData): Transaction? = null
}
