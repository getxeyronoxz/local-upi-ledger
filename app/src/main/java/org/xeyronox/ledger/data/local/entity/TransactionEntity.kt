package org.xeyronox.ledger.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["status"]),
        Index(value = ["referenceId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val amount: Double,
    val type: String, // "CREDIT" or "DEBIT"
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val partyName: String,
    val upiId: String?,
    val referenceId: String?,
    val appSource: String,
    val rawTextDump: String
) {
    fun toDomain(): Transaction {
        return Transaction(
            id = id,
            timestamp = timestamp,
            amount = amount,
            type = if (type == "CREDIT") TransactionType.CREDIT else TransactionType.DEBIT,
            status = try {
                TransactionStatus.valueOf(status)
            } catch (_: Exception) {
                TransactionStatus.SUCCESS
            },
            partyName = partyName,
            upiId = upiId,
            referenceId = referenceId,
            appSource = appSource,
            rawTextDump = rawTextDump
        )
    }

    companion object {
        fun fromDomain(tx: Transaction): TransactionEntity {
            return TransactionEntity(
                id = tx.id,
                timestamp = tx.timestamp,
                amount = tx.amount,
                type = tx.type.name,
                status = tx.status.name,
                partyName = tx.partyName,
                upiId = tx.upiId,
                referenceId = tx.referenceId,
                appSource = tx.appSource,
                rawTextDump = tx.rawTextDump
            )
        }
    }
}
