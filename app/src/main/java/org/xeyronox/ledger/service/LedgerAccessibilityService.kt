package org.xeyronox.ledger.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.xeyronox.ledger.LocalLedgerApp
import org.xeyronox.ledger.domain.parser.AccessibilityNodeData
import org.xeyronox.ledger.parser.ParserRegistry

class LedgerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val parserRegistry = ParserRegistry()
    private var triggerReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_TRIGGER_EXTRACTION = "org.xeyronox.ledger.action.TRIGGER_EXTRACTION"

        fun convertToNodeData(node: AccessibilityNodeInfo?): AccessibilityNodeData? {
            if (node == null) return null
            val childCount = node.childCount
            val children = ArrayList<AccessibilityNodeData>(childCount)
            for (i in 0 until childCount) {
                val child = node.getChild(i) ?: continue
                val childData = convertToNodeData(child)
                if (childData != null) {
                    children.add(childData)
                }
            }

            return AccessibilityNodeData(
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewIdResourceName = node.viewIdResourceName,
                className = node.className?.toString(),
                children = children
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        registerTriggerReceiver()
    }

    private fun registerTriggerReceiver() {
        triggerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_TRIGGER_EXTRACTION) {
                    performExtraction()
                }
            }
        }
        val filter = IntentFilter(ACTION_TRIGGER_EXTRACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(triggerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(triggerReceiver, filter)
        }
    }

    fun performExtraction() {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            showToast("No active window found to scan")
            return
        }

        val packageName = rootNode.packageName?.toString() ?: ""
        val parser = parserRegistry.getParserForPackage(packageName)
        if (parser == null) {
            showToast("No UPI parser registered for $packageName")
            return
        }

        val nodeData = convertToNodeData(rootNode)
        if (nodeData == null) {
            showToast("Failed to inspect screen nodes")
            return
        }

        serviceScope.launch {
            try {
                val transactions = parser.parse(nodeData).ifEmpty {
                    listOfNotNull(parser.parseDetailReceipt(nodeData))
                }
                if (transactions.isEmpty()) {
                    showToastOnMain("No transactions found on screen")
                } else {
                    LocalLedgerApp.instance.repository.insertAll(transactions)
                    showToastOnMain("Extracted ${transactions.size} transactions (${parser.appDisplayName})")
                }
            } catch (e: Exception) {
                showToastOnMain("Extraction error: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun showToastOnMain(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Reserved for passive enrichment or auto-detection
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        triggerReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            triggerReceiver = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
