package org.xeyronox.ledger.export

import net.lingala.zip4j.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.domain.model.TransactionStatus
import org.xeyronox.ledger.domain.model.TransactionType
import java.io.File

class ExportManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `exportToEncryptedZip produces AES-256 password-protected ZIP with CSV and JSON`() {
        val transactions = listOf(
            Transaction(
                id = "tx-1",
                timestamp = 1731070800000L,
                amount = 1500.0,
                type = TransactionType.CREDIT,
                status = TransactionStatus.SUCCESS,
                partyName = "Alice",
                upiId = "alice@upi",
                referenceId = "123456789012",
                appSource = "BHIM"
            ),
            Transaction(
                id = "tx-2",
                timestamp = 1731074400000L,
                amount = 250.50,
                type = TransactionType.DEBIT,
                status = TransactionStatus.SUCCESS,
                partyName = "Bakery",
                upiId = "bakery@upi",
                referenceId = "987654321098",
                appSource = "BHIM"
            )
        )

        val outputFile = File(tempFolder.root, "test_export.zip")
        val password = "SuperSecretPassword123!"

        ExportManager.exportToEncryptedZip(transactions, password, outputFile)

        assertTrue("Output zip must exist", outputFile.exists())
        assertTrue("Output zip must not be empty", outputFile.length() > 0)

        // Read back with correct password using Zip4j
        val zipFile = ZipFile(outputFile, password.toCharArray())
        assertTrue("Archive must be encrypted", zipFile.isEncrypted)

        val fileHeaders = zipFile.fileHeaders
        assertEquals(2, fileHeaders.size)

        val csvHeader = fileHeaders.firstOrNull { it.fileName == "transactions.csv" }
        val jsonHeader = fileHeaders.firstOrNull { it.fileName == "transactions.json" }

        assertNotNull("transactions.csv must exist in archive", csvHeader)
        assertNotNull("transactions.json must exist in archive", jsonHeader)

        // Extract and verify CSV content
        val destDir = tempFolder.newFolder("unzipped")
        zipFile.extractAll(destDir.absolutePath)

        val extractedCsv = File(destDir, "transactions.csv").readText()
        assertTrue(extractedCsv.contains("id,timestamp,amount,type,status,partyName,upiId,referenceId,appSource"))
        assertTrue(extractedCsv.contains("Alice"))
        assertTrue(extractedCsv.contains("1500.0"))
        assertTrue(extractedCsv.contains("Bakery"))
        assertTrue(extractedCsv.contains("250.5"))

        val extractedJson = File(destDir, "transactions.json").readText()
        assertTrue(extractedJson.contains("\"partyName\": \"Alice\""))
        assertTrue(extractedJson.contains("\"amount\": 1500.0"))
    }
}
