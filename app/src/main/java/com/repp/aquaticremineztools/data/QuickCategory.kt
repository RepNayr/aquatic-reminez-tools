package com.repp.aquaticremineztools.data

import androidx.annotation.DrawableRes
import com.repp.aquaticremineztools.R

data class QuickCategory(
    val id: String,
    val nameRes: Int,
    @DrawableRes val iconRes: Int,
    val count: Int,
    val accentColor: Int,
) {
    companion object {

        fun getAll(): List<QuickCategory> {
            val allTools = ToolData.ALL_TOOLS

            return listOf(
                QuickCategory(
                    id = "utility",
                    nameRes = R.string.cat_utility,
                    iconRes = R.drawable.ic_utility,
                    count = allTools.count { it.category == ToolData.Category.UTILITY },
                    accentColor = R.color.cat_utility,
                ),
                QuickCategory(
                    id = "security_gen",
                    nameRes = R.string.cat_security_gen,
                    iconRes = R.drawable.ic_security_gen,
                    count = allTools.count { it.category == ToolData.Category.SECURITY_GEN },
                    accentColor = R.color.cat_security_gen,
                ),
                QuickCategory(
                    id = "file",
                    nameRes = R.string.cat_file,
                    iconRes = R.drawable.ic_file,
                    count = allTools.count { it.category == ToolData.Category.FILE },
                    accentColor = R.color.cat_file,
                ),
                QuickCategory(
                    id = "network",
                    nameRes = R.string.cat_network,
                    iconRes = R.drawable.ic_network_cat,
                    count = allTools.count {
                        it.category == ToolData.Category.NETWORK ||
                                it.category == ToolData.Category.OFFENSIVE ||
                                it.category == ToolData.Category.DEFENSIVE
                    },
                    accentColor = R.color.cat_network,
                ),
                QuickCategory(
                    id = "media",
                    nameRes = R.string.cat_media,
                    iconRes = R.drawable.ic_media,
                    count = allTools.count { it.category == ToolData.Category.MEDIA },
                    accentColor = R.color.cat_media,
                ),
                QuickCategory(
                    id = "dev",
                    nameRes = R.string.cat_dev,
                    iconRes = R.drawable.ic_dev,
                    count = allTools.count { it.category == ToolData.Category.DEV },
                    accentColor = R.color.cat_dev,
                ),
                QuickCategory(
                    id = "forensic",
                    nameRes = R.string.cat_forensic,
                    iconRes = R.drawable.ic_forensic,
                    count = allTools.count { it.category == ToolData.Category.FORENSIC },
                    accentColor = R.color.cat_forensic,
                ),
                QuickCategory(
                    id = "security",
                    nameRes = R.string.cat_security,
                    iconRes = R.drawable.ic_security,
                    count = allTools.count { it.category == ToolData.Category.SECURITY },
                    accentColor = R.color.cat_security,
                ),
            )
        }
    }
}
