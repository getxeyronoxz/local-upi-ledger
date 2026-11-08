package org.xeyronox.ledger.export

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.ByteArrayInputStream
import java.io.File

object ZipCryptoHelper {

    fun createEncryptedZip(
        outputFile: File,
        password: String,
        entries: Map<String, ByteArray>
    ) {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        val zipFile = ZipFile(outputFile, password.toCharArray())
        val zipParameters = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        }

        for ((fileName, data) in entries) {
            zipParameters.fileNameInZip = fileName
            zipFile.addStream(ByteArrayInputStream(data), zipParameters)
        }
    }
}
