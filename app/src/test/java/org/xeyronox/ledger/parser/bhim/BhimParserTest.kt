package org.xeyronox.ledger.parser.bhim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType
import org.xeyronox.ledger.domain.parser.AccessibilityNodeData

class BhimParserTest {

    private lateinit var parser: BhimParser

    @Before
    fun setUp() {
        parser = BhimParser()
    }

    @Test
    fun `canParse accepts BHIM package and rejects others`() {
        assertTrue(parser.canParse("in.org.npci.upiapp"))
        assertFalse(parser.canParse("com.phonepe.app"))
        assertFalse(parser.canParse("com.google.android.apps.nbu.paisa.user"))
    }

    @Test
    fun `parses incoming credit transaction from history list row`() {
        val rowNode = AccessibilityNodeData(
            className = "android.widget.ViewGroup",
            children = listOf(
                AccessibilityNodeData(text = "Money received from Rahul Sharma"),
                AccessibilityNodeData(text = "₹ 2,450.00"),
                AccessibilityNodeData(text = "08 Nov 2026, 02:45 PM"),
                AccessibilityNodeData(text = "Successful")
            )
        )
        val rootNode = AccessibilityNodeData(
            className = "android.widget.FrameLayout",
            children = listOf(rowNode)
        )

        val transactions = parser.parse(rootNode)

        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(TransactionType.CREDIT, tx.type)
        assertEquals(2450.0, tx.amount, 0.001)
        assertEquals(TransactionStatus.SUCCESS, tx.status)
        assertEquals("Rahul Sharma", tx.partyName)
        assertEquals("BHIM", tx.appSource)
    }

    @Test
    fun `parses outgoing debit transaction from history list row`() {
        val rowNode = AccessibilityNodeData(
            className = "android.widget.ViewGroup",
            children = listOf(
                AccessibilityNodeData(text = "Paid to Supermarket Groceries"),
                AccessibilityNodeData(text = "₹ 820.50"),
                AccessibilityNodeData(text = "08 Nov 2026, 11:20 AM"),
                AccessibilityNodeData(text = "Completed")
            )
        )
        val rootNode = AccessibilityNodeData(
            className = "android.widget.FrameLayout",
            children = listOf(rowNode)
        )

        val transactions = parser.parse(rootNode)

        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals(820.50, tx.amount, 0.001)
        assertEquals(TransactionStatus.SUCCESS, tx.status)
        assertEquals("Supermarket Groceries", tx.partyName)
    }

    @Test
    fun `parses failed or declined transaction correctly`() {
        val rowNode = AccessibilityNodeData(
            className = "android.widget.ViewGroup",
            children = listOf(
                AccessibilityNodeData(text = "Paid to Pharmacy"),
                AccessibilityNodeData(text = "₹ 120.00"),
                AccessibilityNodeData(text = "08 Nov 2026, 09:00 AM"),
                AccessibilityNodeData(text = "Declined by Bank")
            )
        )
        val rootNode = AccessibilityNodeData(
            className = "android.widget.FrameLayout",
            children = listOf(rowNode)
        )

        val transactions = parser.parse(rootNode)

        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals(120.0, tx.amount, 0.001)
        assertEquals(TransactionStatus.FAILED, tx.status)
        assertEquals("Pharmacy", tx.partyName)
    }

    @Test
    fun `parseDetailReceipt extracts UTR reference id and exact details`() {
        val detailRoot = AccessibilityNodeData(
            className = "android.widget.FrameLayout",
            children = listOf(
                AccessibilityNodeData(text = "Transaction Details"),
                AccessibilityNodeData(text = "Paid to Electric Supply Co"),
                AccessibilityNodeData(text = "₹ 1,500.00"),
                AccessibilityNodeData(text = "UPI Ref No: 432198765432"),
                AccessibilityNodeData(text = "08 Nov 2026, 10:15 AM"),
                AccessibilityNodeData(text = "Payment Successful")
            )
        )

        val tx = parser.parseDetailReceipt(detailRoot)

        assertNotNull(tx)
        assertEquals("432198765432", tx?.referenceId)
        assertEquals("Electric Supply Co", tx?.partyName)
        assertEquals(1500.0, tx?.amount ?: 0.0, 0.001)
        assertEquals(TransactionType.DEBIT, tx?.type)
        assertEquals(TransactionStatus.SUCCESS, tx?.status)
    }
}
