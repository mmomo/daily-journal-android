package com.mmomo.dailyjournal.data

import com.mmomo.dailyjournal.data.prompts.BundledPromptLibrary
import com.mmomo.dailyjournal.data.prompts.PromptFileException
import com.mmomo.dailyjournal.data.support.IosDataFixtures
import com.mmomo.dailyjournal.data.support.appContext
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.policies.PromptLibraryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.MessageDigest

/** US-1.1, FR-1.2, DATA-P1–P3. */
@RunWith(RobolectricTestRunner::class)
class PromptFileTest {
    @Test
    fun `the shipped file is the iOS file byte for byte`() {
        val bytes = appContext().assets.open(BundledPromptLibrary.ASSET).use { it.readBytes() }
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        sha shouldBe IosDataFixtures.PROMPTS_SHA256
    }

    @Test
    fun `600 valid prompts, 150 per category, ids 001 to 150`() {
        val library = BundledPromptLibrary(appContext())
        library.allPrompts().size shouldBe 600
        PromptCategory.entries.forEach { category ->
            val ids = library.allPrompts().filter { it.category == category }.map { it.id }.sorted()
            ids shouldBe (1..150).map { "${category.rawValue}-${it.toString().padStart(3, '0')}" }
        }
        library.prompt("creativity-125")?.text shouldBe "Describe a lighthouse keeper's ordinary day."
        library.prompt("missing-001") shouldBe null
    }

    @Test
    fun `an unsupported version or invalid prompts are rejected (DATA-P2)`() {
        shouldThrow<PromptFileException> { BundledPromptLibrary("""{"version": 2, "prompts": []}""") }
        shouldThrow<PromptFileException> {
            BundledPromptLibrary("""{"version": 1, "prompts": [{"id": "x-001", "category": "unknown", "text": "Some prompt text"}]}""")
        }
        shouldThrow<PromptLibraryException.TooFewPrompts> { BundledPromptLibrary("""{"version": 1, "prompts": []}""") }
    }
}
