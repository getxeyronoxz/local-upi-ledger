package org.xeyronox.ledger.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xeyronox.ledger.domain.parser.AccessibilityNodeData
import java.io.File

class ServiceConfigurationTest {

    @Test
    fun testAccessibilityServiceConfigXml() {
        val configFile = File("src/main/res/xml/accessibility_service_config.xml")
        assertTrue("accessibility_service_config.xml must exist", configFile.exists())
        val content = configFile.readText()
        assertTrue("canRetrieveWindowContent must be true", content.contains("android:canRetrieveWindowContent=\"true\""))
        assertTrue("must retrieve interactive windows", content.contains("flagRetrieveInteractiveWindows"))
    }

    @Test
    fun testServiceConstants() {
        assertEquals("org.xeyronox.ledger.action.START_OVERLAY", FloatingOverlayService.ACTION_START)
        assertEquals("org.xeyronox.ledger.action.STOP_OVERLAY", FloatingOverlayService.ACTION_STOP)
        assertEquals("org.xeyronox.ledger.action.TRIGGER_EXTRACTION", LedgerAccessibilityService.ACTION_TRIGGER_EXTRACTION)
    }

    @Test
    fun testExtractNodesRecursive() {
        val child1 = AccessibilityNodeData(text = "Paid to Merchant")
        val child2 = AccessibilityNodeData(text = "₹450.00")
        val parent = AccessibilityNodeData(
            text = null,
            children = listOf(child1, child2)
        )

        assertEquals(2, parent.children.size)
        assertEquals("Paid to Merchant", parent.children[0].text)
        assertEquals("₹450.00", parent.children[1].text)
    }
}
