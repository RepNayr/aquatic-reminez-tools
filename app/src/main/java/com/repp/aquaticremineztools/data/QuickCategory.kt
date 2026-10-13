package com.repp.aquaticremineztools.data

import com.repp.aquaticremineztools.R

data class QuickCategory(
    val id: String,
    val nameRes: Int,
    val emoji: String,
    val count: Int,
    val accentColor: Int,
) {
    companion object {

        /**
         * Ambil semua kategori (10) — MINUS Games.
         * Games ditaruh di drawer.
         */
        fun getAll(): List<QuickCategory> {
            val allTools = ToolData.ALL_TOOLS

            return listOf(
                QuickCategory(
                    id = "utility",
                    nameRes = R.string.cat_utility,
                    emoji = "🔧",
                    count = allTools.count { it.category == ToolData.Category.UTILITY },
                    accentColor = R.color.cat_utility,
                ),
                QuickCategory(
                    id = "security_gen",
                    nameRes = R.string.cat_security_gen,
                    emoji = "🔐",
                    count = allTools.count { it.category == ToolData.Category.SECURITY_GEN },
                    accentColor = R.color.cat_security_gen,
                ),
                QuickCategory(
                    id = "file",
                    nameRes = R.string.cat_file,
                    emoji = "📁",
                    count = allTools.count { it.category == ToolData.Category.FILE },
                    accentColor = R.color.cat_file,
                ),
                QuickCategory(
                    id = "network",
                    nameRes = R.string.cat_network,
                    emoji = "🌐",
                    count = allTools.count { it.category == ToolData.Category.NETWORK },
                    accentColor = R.color.cat_network,
                ),
                QuickCategory(
                    id = "media",
                    nameRes = R.string.cat_media,
                    emoji = "🎨",
                    count = allTools.count { it.category == ToolData.Category.MEDIA },
                    accentColor = R.color.cat_media,
                ),
                QuickCategory(
                    id = "dev",
                    nameRes = R.string.cat_dev,
                    emoji = "🧰",
                    count = allTools.count { it.category == ToolData.Category.DEV },
                    accentColor = R.color.cat_dev,
                ),
                QuickCategory(
                    id = "forensic",
                    nameRes = R.string.cat_forensic,
                    emoji = "🔍",
                    count = allTools.count { it.category == ToolData.Category.FORENSIC },
                    accentColor = R.color.cat_forensic,
                ),
                QuickCategory(
                    id = "security",
                    nameRes = R.string.cat_security,
                    emoji = "🛡️",
                    count = allTools.count { it.category == ToolData.Category.SECURITY },
                    accentColor = R.color.cat_security,
                ),
                QuickCategory(
                    id = "offensive",
                    nameRes = R.string.cat_offensive,
                    emoji = "🔴",
                    count = allTools.count { it.category == ToolData.Category.OFFENSIVE },
                    accentColor = R.color.cat_offensive,
                ),
                QuickCategory(
                    id = "defensive",
                    nameRes = R.string.cat_defensive,
                    emoji = "🛡️",
                    count = allTools.count { it.category == ToolData.Category.DEFENSIVE },
                    accentColor = R.color.cat_defensive,
                ),
            )
        }
    }
}