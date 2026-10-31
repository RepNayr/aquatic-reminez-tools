package com.repp.aquaticremineztools.data

/**
 * Helper to determine whether a tool is fully implemented and functional.
 * Temporary, placeholder, and mock tools return false.
 */
object ToolImplementationHelper {
    fun isToolImplemented(toolId: String): Boolean {
        // As tools transition toward real implementations,
        // currently no placeholder tools in the catalog are fully implemented.
        return false
    }
}
