package org.xeyronox.ledger

import android.app.Application
import org.xeyronox.ledger.data.local.LedgerDatabase
import org.xeyronox.ledger.data.repository.TransactionRepository

class LocalLedgerApp : Application() {

    val database: LedgerDatabase by lazy {
        LedgerDatabase.getInstance(this)
    }

    val repository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }

    companion object {
        lateinit var instance: LocalLedgerApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
