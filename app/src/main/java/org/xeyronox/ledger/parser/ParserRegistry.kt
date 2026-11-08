package org.xeyronox.ledger.parser

import org.xeyronox.ledger.domain.parser.UpiAppParser
import org.xeyronox.ledger.parser.bhim.BhimParser
import org.xeyronox.ledger.parser.fampay.FamPayParser
import org.xeyronox.ledger.parser.phonepe.PhonePeParser

class ParserRegistry(
    customParsers: List<UpiAppParser>? = null
) {
    private val parsers: List<UpiAppParser> = customParsers ?: listOf(
        BhimParser(),
        PhonePeParser(),
        FamPayParser()
    )

    fun getParserForPackage(packageName: String): UpiAppParser? {
        return parsers.firstOrNull { it.canParse(packageName) }
    }

    fun getAllRegisteredParsers(): List<UpiAppParser> {
        return parsers
    }
}
