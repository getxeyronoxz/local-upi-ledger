package org.xeyronox.ledger.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType
import org.xeyronox.ledger.ui.theme.AmberPending
import org.xeyronox.ledger.ui.theme.GreenEmerald
import org.xeyronox.ledger.ui.theme.RedRose
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransactionFilter {
    ALL, CREDITS, DEBITS
}

@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }

    val filteredList = remember(transactions, selectedFilter) {
        when (selectedFilter) {
            TransactionFilter.ALL -> transactions
            TransactionFilter.CREDITS -> transactions.filter { it.type == TransactionType.CREDIT }
            TransactionFilter.DEBITS -> transactions.filter { it.type == TransactionType.DEBIT }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Filter row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == TransactionFilter.ALL,
                onClick = { selectedFilter = TransactionFilter.ALL },
                label = { Text("All (${transactions.size})") }
            )
            FilterChip(
                selected = selectedFilter == TransactionFilter.CREDITS,
                onClick = { selectedFilter = TransactionFilter.CREDITS },
                label = { Text("Received (+)") }
            )
            FilterChip(
                selected = selectedFilter == TransactionFilter.DEBITS,
                onClick = { selectedFilter = TransactionFilter.DEBITS },
                label = { Text("Spent (-)") }
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions found.\nTap floating bubble on UPI app to scan.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        onDelete = { onDeleteTransaction(tx.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    val isCredit = transaction.type == TransactionType.CREDIT
    val amountColor = if (isCredit) GreenEmerald else RedRose
    val prefix = if (isCredit) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.partyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Source tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = transaction.appSource,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!transaction.referenceId.isNullOrBlank()) {
                    Text(
                        text = "UTR: ${transaction.referenceId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix₹%.2f".format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Status chip
                val (statusBg, statusFg) = when (transaction.status) {
                    TransactionStatus.SUCCESS -> GreenEmerald.copy(alpha = 0.15f) to GreenEmerald
                    TransactionStatus.PENDING -> AmberPending.copy(alpha = 0.15f) to AmberPending
                    TransactionStatus.FAILED -> RedRose.copy(alpha = 0.15f) to RedRose
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = transaction.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
