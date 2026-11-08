package com.mmomo.dailyjournal.domain.entities

/** Daily reminder; hour and minute are clamped to valid ranges, like iOS. Default: off at 20:00. */
class ReminderSettings(isEnabled: Boolean = false, hour: Int = 20, minute: Int = 0) {
    val isEnabled: Boolean = isEnabled
    val hour: Int = hour.coerceIn(0, 23)
    val minute: Int = minute.coerceIn(0, 59)

    fun copy(isEnabled: Boolean = this.isEnabled, hour: Int = this.hour, minute: Int = this.minute) =
        ReminderSettings(isEnabled, hour, minute)

    override fun equals(other: Any?): Boolean =
        other is ReminderSettings && isEnabled == other.isEnabled && hour == other.hour && minute == other.minute

    override fun hashCode(): Int = (if (isEnabled) 1 else 0) * 10_000 + hour * 100 + minute

    override fun toString(): String = "ReminderSettings(isEnabled=$isEnabled, hour=$hour, minute=$minute)"

    companion object {
        val DEFAULT = ReminderSettings()
    }
}

/** App settings. An empty category set falls back to all four, like iOS (BR-G1, BR-G2). */
class AppSettings(
    categories: Set<PromptCategory> = PromptCategory.entries.toSet(),
    val reminder: ReminderSettings = ReminderSettings.DEFAULT,
    val onboardingCompleted: Boolean = false,
) {
    val categories: Set<PromptCategory> = categories.ifEmpty { PromptCategory.entries.toSet() }

    fun copy(
        categories: Set<PromptCategory> = this.categories,
        reminder: ReminderSettings = this.reminder,
        onboardingCompleted: Boolean = this.onboardingCompleted,
    ) = AppSettings(categories, reminder, onboardingCompleted)

    override fun equals(other: Any?): Boolean =
        other is AppSettings && categories == other.categories && reminder == other.reminder &&
            onboardingCompleted == other.onboardingCompleted

    override fun hashCode(): Int = categories.hashCode() * 31 + reminder.hashCode() * 7 + if (onboardingCompleted) 1 else 0

    override fun toString(): String =
        "AppSettings(categories=$categories, reminder=$reminder, onboardingCompleted=$onboardingCompleted)"

    companion object {
        val DEFAULT = AppSettings()
    }
}
