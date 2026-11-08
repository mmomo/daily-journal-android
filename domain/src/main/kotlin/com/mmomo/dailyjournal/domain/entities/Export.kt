package com.mmomo.dailyjournal.domain.entities

enum class ExportFormat(val fileExtension: String, val mimeType: String) {
    MARKDOWN("md", "text/markdown"),
    PLAIN_TEXT("txt", "text/plain"),
}

data class ExportDocument(val fileName: String, val contents: String, val format: ExportFormat) {
    val mimeType: String get() = format.mimeType
}
