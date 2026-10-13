package com.repp.aquaticremineztools.presentation.storage

import java.io.File

data class StorageFile(
    val file: File,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val type: FileType,
) {
    enum class FileType {
        IMAGE,
        VIDEO,
        AUDIO,
        DOCUMENT,
        OTHER,
    }
}