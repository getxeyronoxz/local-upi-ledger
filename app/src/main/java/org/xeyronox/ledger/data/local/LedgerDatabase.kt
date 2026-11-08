package org.xeyronox.ledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import org.xeyronox.ledger.data.local.dao.TransactionDao
import org.xeyronox.ledger.data.local.entity.TransactionEntity
import org.xeyronox.ledger.data.security.KeyManager

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: LedgerDatabase? = null

        fun getInstance(context: Context): LedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LedgerDatabase {
            val keyManager = KeyManager(context.applicationContext)
            val passphrase = keyManager.getOrCreateDatabasePassphrase()
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                LedgerDatabase::class.java,
                "local_ledger.db"
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
