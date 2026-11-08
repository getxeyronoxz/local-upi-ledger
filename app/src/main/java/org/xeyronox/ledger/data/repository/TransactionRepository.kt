package org.xeyronox.ledger.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.xeyronox.ledger.data.local.dao.TransactionDao
import org.xeyronox.ledger.data.local.entity.TransactionEntity
import org.xeyronox.ledger.domain.model.Transaction

class TransactionRepository(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startTime, endTime).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getCreditTotal(): Flow<Double> = transactionDao.getCreditSum()

    fun getDebitTotal(): Flow<Double> = transactionDao.getDebitSum()

    fun getNetBalance(): Flow<Double> {
        return combine(getCreditTotal(), getDebitTotal()) { credit, debit ->
            credit - debit
        }
    }

    suspend fun insertOrUpdate(transaction: Transaction) {
        transactionDao.insertOrUpdate(TransactionEntity.fromDomain(transaction))
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions.map { TransactionEntity.fromDomain(it) })
    }

    suspend fun deleteById(id: String) {
        transactionDao.deleteById(id)
    }
}
