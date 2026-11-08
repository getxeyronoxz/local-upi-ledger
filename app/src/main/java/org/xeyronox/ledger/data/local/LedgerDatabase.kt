package org.xeyronox.ledger.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
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
            try {
                System.loadLibrary("sqlcipher")
            } catch (e: UnsatisfiedLinkError) {
                // In local unit test environments without native libs
            }
            val keyManager = KeyManager(context.applicationContext)
            val passphrase = keyManager.getOrCreateDatabasePassphrase()
            val factory = SupportOpenHelperFactory(passphrase)

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
