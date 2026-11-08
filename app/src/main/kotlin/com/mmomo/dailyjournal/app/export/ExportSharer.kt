package com.mmomo.dailyjournal.app.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.mmomo.dailyjournal.domain.entities.ExportDocument
import java.io.File

/**
 * Shares an export through the share sheet (US-3.3). The file goes to the app's private cache and is
 * shared with temporary read permission only; no copy is kept in shared storage.
 */
object ExportSharer {
    fun share(context: Context, document: ExportDocument) {
        val folder = File(context.cacheDir, "exports").apply { mkdirs() }
        folder.listFiles()?.forEach { it.delete() } // keep only the latest export
        val file = File(folder, document.fileName).apply { writeText(document.contents, Charsets.UTF_8) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.exports", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(document.mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, "Export journal").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
