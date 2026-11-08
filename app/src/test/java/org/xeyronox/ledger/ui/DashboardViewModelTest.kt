package org.xeyronox.ledger.ui

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.xeyronox.ledger.data.repository.TransactionRepository
import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: TransactionRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDashboardUiStateEmission() = runTest(testDispatcher) {
        val testTransactions = listOf(
            Transaction(
                id = "tx1",
                timestamp = 1700000000000L,
                amount = 1200.0,
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESS,
                partyName = "Merchant One",
                appSource = "BHIM"
            ),
            Transaction(
                id = "tx2",
                timestamp = 1700000050000L,
                amount = 2500.0,
                type = TransactionType.CREDIT,
                status = TransactionStatus.SUCCESS,
                partyName = "Friend Two",
                appSource = "BHIM"
            )
        )

        every { repository.getAllTransactions() } returns flowOf(testTransactions)
        every { repository.getCreditTotal() } returns flowOf(2500.0)
        every { repository.getDebitTotal() } returns flowOf(1200.0)
        every { repository.getNetBalance() } returns flowOf(1300.0)

        val viewModel = DashboardViewModel(repository)

        viewModel.uiState.test {
            // initial state or combined emission
            var item = awaitItem()
            if (item.transactions.isEmpty()) {
                item = awaitItem()
            }
            assertEquals(2500.0, item.creditTotal, 0.001)
            assertEquals(1200.0, item.debitTotal, 0.001)
            assertEquals(1300.0, item.netBalance, 0.001)
            assertEquals(2, item.transactions.size)
            assertEquals("Merchant One", item.transactions[0].partyName)
        }
    }
}
