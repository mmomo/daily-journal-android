package com.mmomo.dailyjournal.domain.policies

import com.mmomo.dailyjournal.domain.entities.ExportDocument
import com.mmomo.dailyjournal.domain.entities.ExportFormat
import com.mmomo.dailyjournal.domain.entities.JournalEntry
import com.mmomo.dailyjournal.domain.time.CalendarDay

/** Markdown and plain-text export, identical to iOS (BR-X1–X4, business-logic-model §6). */
object JournalExporter {
    val PLAIN_TEXT_SEPARATOR = "-".repeat(40)

    fun export(entries: List<JournalEntry>, format: ExportFormat, exportDay: CalendarDay): ExportDocument {
        val ordered = entries.sortedWith(compareBy<JournalEntry>({ it.day }, { it.createdAt }))
        val countText = if (ordered.size == 1) "1 entry" else "${ordered.size} entries"
        val contents = when (format) {
            ExportFormat.MARKDOWN -> {
                val blocks = mutableListOf("# Daily Journal", "Exported ${exportDay.key} · $countText")
                for (entry in ordered) {
                    blocks += "## ${entry.day.key}"
                    blocks += "> ${entry.promptText}"
                    blocks += entry.text
                }
                blocks.joinToString("\n\n") + "\n"
            }
            ExportFormat.PLAIN_TEXT -> {
                val header = "DAILY JOURNAL\nExported ${exportDay.key} - $countText"
                val bodies = ordered.joinToString("\n\n$PLAIN_TEXT_SEPARATOR\n\n") { entry ->
                    "${entry.day.key}\nPrompt: ${entry.promptText}\n\n${entry.text}"
                }
                listOf(header, bodies).filter { it.isNotEmpty() }.joinToString("\n\n") + "\n"
            }
        }
        return ExportDocument(
            fileName = "daily-journal-${exportDay.key}.${format.fileExtension}",
            contents = contents,
            format = format,
        )
    }
}
