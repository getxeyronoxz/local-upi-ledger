package org.xeyronox.ledger.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTest {

    @Test
    fun `computeId generates identical deterministic hash for same transaction properties`() {
        val timestamp = 1731070800000L // 08 Nov 2026 13:00:00 UTC
        val id1 = Transaction.generateId(
            timestamp = timestamp,
            amount = 1500.50,
            partyName = "Electricity Board",
            referenceId = "123456789012"
        )
        val id2 = Transaction.generateId(
            timestamp = timestamp,
            amount = 1500.50,
            partyName = "Electricity Board",
            referenceId = "123456789012"
        )

        assertEquals("Hashes must match for identical transaction attributes", id1, id2)
        assertEquals(64, id1.length) // SHA-256 hex string length
    }

    @Test
    fun `computeId generates distinct hash when amount or party differs`() {
        val timestamp = 1731070800000L
        val id1 = Transaction.generateId(
            timestamp = timestamp,
            amount = 500.00,
            partyName = "Store A",
            referenceId = null
        )
        val id2 = Transaction.generateId(
            timestamp = timestamp,
            amount = 500.01,
            partyName = "Store A",
            referenceId = null
        )
        val id3 = Transaction.generateId(
            timestamp = timestamp,
            amount = 500.00,
            partyName = "Store B",
            referenceId = null
        )

        assertNotEquals(id1, id2)
        assertNotEquals(id1, id3)
    }

    @Test
    fun `transaction creation stores all attributes correctly`() {
        val tx = Transaction(
            id = "test-hash-123",
            timestamp = 1731070800000L,
            amount = 250.00,
            type = TransactionType.DEBIT,
            status = TransactionStatus.SUCCESS,
            partyName = "Coffee Shop",
            upiId = "coffeeshop@upi",
            referenceId = "987654321098",
            appSource = "BHIM",
            rawTextDump = "Paid to Coffee Shop ₹250.00"
        )

        assertEquals("test-hash-123", tx.id)
        assertEquals(250.00, tx.amount, 0.001)
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals(TransactionStatus.SUCCESS, tx.status)
        assertEquals("Coffee Shop", tx.partyName)
        assertEquals("coffeeshop@upi", tx.upiId)
        assertEquals("987654321098", tx.referenceId)
        assertEquals("BHIM", tx.appSource)
    }
}
