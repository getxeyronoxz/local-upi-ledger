package org.xeyronox.ledger.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.xeyronox.ledger.data.local.dao.TransactionDao
import org.xeyronox.ledger.data.local.entity.TransactionEntity
import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType

class TransactionRepositoryTest {

    private val transactionDao: TransactionDao = mockk(relaxed = true)
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        repository = TransactionRepository(transactionDao)
    }

    @Test
    fun `getCreditTotal returns sum of successful credit transactions`() = runTest {
        coEvery { transactionDao.getCreditSum() } returns flowOf(5400.0)

        val total = repository.getCreditTotal().first()
        assertEquals(5400.0, total, 0.001)
    }

    @Test
    fun `getDebitTotal returns sum of successful debit transactions`() = runTest {
        coEvery { transactionDao.getDebitSum() } returns flowOf(1250.50)

        val total = repository.getDebitTotal().first()
        assertEquals(1250.50, total, 0.001)
    }

    @Test
    fun `getNetBalance correctly calculates credit minus debit`() = runTest {
        coEvery { transactionDao.getCreditSum() } returns flowOf(5000.0)
        coEvery { transactionDao.getDebitSum() } returns flowOf(2000.0)

        val net = repository.getNetBalance().first()
        assertEquals(3000.0, net, 0.001)
    }

    @Test
    fun `insertOrUpdate delegates entity conversion and dao insert`() = runTest {
        val tx = Transaction(
            id = "tx-123",
            timestamp = 1731070800000L,
            amount = 100.0,
            type = TransactionType.DEBIT,
            status = TransactionStatus.SUCCESS,
            partyName = "Store",
            upiId = "store@upi",
            referenceId = "112233445566",
            appSource = "BHIM",
            rawTextDump = "raw text"
        )

        repository.insertOrUpdate(tx)

        coVerify {
            transactionDao.insertOrUpdate(match { entity ->
                entity.id == "tx-123" &&
                        entity.amount == 100.0 &&
                        entity.type == "DEBIT" &&
                        entity.status == "SUCCESS" &&
                        entity.partyName == "Store" &&
                        entity.referenceId == "112233445566"
            })
        }
    }
}
