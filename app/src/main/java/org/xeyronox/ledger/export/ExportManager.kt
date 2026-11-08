package org.xeyronox.ledger.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.xeyronox.ledger.domain.model.Transaction
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    fun exportToEncryptedZip(
        transactions: List<Transaction>,
        password: String,
        outputFile: File
    ) {
        val csvData = generateCsv(transactions).toByteArray(Charsets.UTF_8)
        val jsonData = generateJson(transactions).toByteArray(Charsets.UTF_8)

        val entries = mapOf(
            "transactions.csv" to csvData,
            "transactions.json" to jsonData
        )

        ZipCryptoHelper.createEncryptedZip(outputFile, password, entries)
    }

    fun exportToDownloadsFolder(
        context: Context,
        transactions: List<Transaction>,
        password: String
    ): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "LocalLedger_export_$timeStamp.zip"

        // Temporary cache file for compression
        val tempFile = File(context.cacheDir, fileName)
        try {
            exportToEncryptedZip(transactions, password, tempFile)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/LocalLedger")
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Failed to create MediaStore entry for export")

                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(tempFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                return "Downloads/LocalLedger/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "LocalLedger").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)
                FileInputStream(tempFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                return targetFile.absolutePath
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun generateCsv(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("id,timestamp,amount,type,status,partyName,upiId,referenceId,appSource\n")
        for (tx in transactions) {
            val escapedParty = escapeCsv(tx.partyName)
            val escapedUpiId = escapeCsv(tx.upiId ?: "")
            val escapedRefId = escapeCsv(tx.referenceId ?: "")
            val escapedSource = escapeCsv(tx.appSource)
            sb.append("${tx.id},${tx.timestamp},${tx.amount},${tx.type.name},${tx.status.name},$escapedParty,$escapedUpiId,$escapedRefId,$escapedSource\n")
        }
        return sb.toString()
    }

    fun generateJson(transactions: List<Transaction>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        for (i in transactions.indices) {
            val tx = transactions[i]
            sb.append("  {\n")
            sb.append("    \"id\": \"${escapeJson(tx.id)}\",\n")
            sb.append("    \"timestamp\": ${tx.timestamp},\n")
            sb.append("    \"amount\": ${tx.amount},\n")
            sb.append("    \"type\": \"${tx.type.name}\",\n")
            sb.append("    \"status\": \"${tx.status.name}\",\n")
            sb.append("    \"partyName\": \"${escapeJson(tx.partyName)}\",\n")
            sb.append("    \"upiId\": ${tx.upiId?.let { "\"${escapeJson(it)}\"" } ?: "null"},\n")
            sb.append("    \"referenceId\": ${tx.referenceId?.let { "\"${escapeJson(it)}\"" } ?: "null"},\n")
            sb.append("    \"appSource\": \"${escapeJson(tx.appSource)}\"\n")
            sb.append("  }${if (i < transactions.size - 1) "," else ""}\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
