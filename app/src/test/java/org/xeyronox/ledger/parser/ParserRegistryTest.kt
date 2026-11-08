package org.xeyronox.ledger.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.xeyronox.ledger.parser.bhim.BhimParser
import org.xeyronox.ledger.parser.fampay.FamPayParser
import org.xeyronox.ledger.parser.phonepe.PhonePeParser

class ParserRegistryTest {

    private lateinit var registry: ParserRegistry

    @Before
    fun setUp() {
        registry = ParserRegistry()
    }

    @Test
    fun `resolves BhimParser for BHIM package`() {
        val parser = registry.getParserForPackage("in.org.npci.upiapp")
        assertNotNull(parser)
        assertTrue(parser is BhimParser)
        assertEquals("BHIM", parser?.appDisplayName)
    }

    @Test
    fun `resolves PhonePeParser for PhonePe package`() {
        val parser = registry.getParserForPackage("com.phonepe.app")
        assertNotNull(parser)
        assertTrue(parser is PhonePeParser)
        assertEquals("PhonePe", parser?.appDisplayName)
    }

    @Test
    fun `resolves FamPayParser for FamPay package`() {
        val parser = registry.getParserForPackage("com.famapp")
        assertNotNull(parser)
        assertTrue(parser is FamPayParser)
        assertEquals("FamPay", parser?.appDisplayName)
    }

    @Test
    fun `returns null for unsupported banking app package`() {
        val parser = registry.getParserForPackage("com.unsupported.randombank")
        assertNull(parser)
    }

    @Test
    fun `getAllRegisteredParsers contains all configured parsers`() {
        val parsers = registry.getAllRegisteredParsers()
        assertEquals(3, parsers.size)
        assertTrue(parsers.any { it is BhimParser })
        assertTrue(parsers.any { it is PhonePeParser })
        assertTrue(parsers.any { it is FamPayParser })
    }
}
