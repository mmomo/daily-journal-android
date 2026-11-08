package com.mmomo.dailyjournal.data.prompts

import android.content.Context
import com.mmomo.dailyjournal.domain.entities.Prompt
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.policies.PromptLibraryValidator
import com.mmomo.dailyjournal.domain.ports.PromptLibrary
import org.json.JSONObject

/** Why the bundled prompt file can't be used. Messages contain ids and versions only. */
class PromptFileException(message: String) : Exception(message)

/**
 * The bundled `prompts.json` (same file as iOS), loaded and validated when constructed, so a broken
 * file fails app start with the startup error (DATA-P1, DATA-P2, NFR design §2).
 */
class BundledPromptLibrary(json: String) : PromptLibrary {
    private val prompts: List<Prompt> = parse(json)
    private val byId: Map<String, Prompt> = prompts.associateBy { it.id }

    constructor(context: Context) : this(context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use { it.readText() })

    override fun allPrompts(): List<Prompt> = prompts
    override fun prompt(id: String): Prompt? = byId[id]

    companion object {
        const val ASSET = "prompts.json"

        fun parse(json: String): List<Prompt> {
            val root = JSONObject(json)
            val version = root.optInt("version", -1)
            if (version != 1) throw PromptFileException("Unsupported prompt file version $version")
            val array = root.getJSONArray("prompts")
            val prompts = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                val id = item.getString("id")
                val category = PromptCategory.fromRawValue(item.getString("category"))
                    ?: throw PromptFileException("Unknown category for $id")
                Prompt(id = id, category = category, text = item.getString("text"))
            }
            PromptLibraryValidator.validate(prompts)
            return prompts
        }
    }
}
