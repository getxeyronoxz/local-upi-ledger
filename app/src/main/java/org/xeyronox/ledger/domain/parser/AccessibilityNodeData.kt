package org.xeyronox.ledger.domain.parser

data class AccessibilityNodeData(
    val text: CharSequence? = null,
    val contentDescription: CharSequence? = null,
    val viewIdResourceName: String? = null,
    val className: CharSequence? = null,
    val children: List<AccessibilityNodeData> = emptyList()
) {
    /**
     * Recursively collects all text and content descriptions from this node and its descendants.
     */
    fun collectAllText(): List<String> {
        val results = mutableListOf<String>()
        text?.toString()?.takeIf { it.isNotBlank() }?.let { results.add(it.trim()) }
        contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { results.add(it.trim()) }
        for (child in children) {
            results.addAll(child.collectAllText())
        }
        return results
    }
}
