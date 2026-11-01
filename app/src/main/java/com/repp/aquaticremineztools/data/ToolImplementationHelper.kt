package com.repp.aquaticremineztools.data

/**
 * Helper to determine whether a tool is fully implemented and functional.
 * Temporary, placeholder, and mock tools return false.
 */
object ToolImplementationHelper {
    fun isToolImplemented(toolId: String): Boolean {
        return when (toolId) {
            "media_image_compressor" -> true
            else -> false
        }
    }
}
