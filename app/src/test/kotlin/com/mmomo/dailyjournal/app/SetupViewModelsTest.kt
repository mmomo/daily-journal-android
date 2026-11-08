package com.mmomo.dailyjournal.app

import androidx.lifecycle.SavedStateHandle
import com.mmomo.dailyjournal.app.navigation.DeepLinkRouter
import com.mmomo.dailyjournal.app.support.vmTest
import com.mmomo.dailyjournal.app.ui.onboarding.OnboardingViewModel
import com.mmomo.dailyjournal.app.ui.settings.SettingsViewModel
import com.mmomo.dailyjournal.domain.entities.AppSettings
import com.mmomo.dailyjournal.domain.entities.DeepLink
import com.mmomo.dailyjournal.domain.entities.PromptCategory
import com.mmomo.dailyjournal.domain.entities.ReminderAuthorization
import com.mmomo.dailyjournal.domain.support.TestEnvironment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest : FunSpec({
    fun settings(env: TestEnvironment, sdk: Int = 34, asked: MutableList<Unit> = mutableListOf()) =
        SettingsViewModel(env.useCases, { env.reminders.authorization() }, { asked += Unit }, sdkInt = sdk)

    test("the last category can't be turned off") {
        vmTest {
            val env = TestEnvironment()
            val vm = settings(env)
            advanceUntilIdle()
            listOf(PromptCategory.GRATITUDE, PromptCategory.REFLECTION, PromptCategory.GOALS).forEach { vm.toggleCategory(it); advanceUntilIdle() }
            vm.toggleCategory(PromptCategory.CREATIVITY)
            advanceUntilIdle()
            vm.state.value.settings.categories shouldBe setOf(PromptCategory.CREATIVITY)
            vm.state.value.categoryMessage shouldBe "Choose at least one category."
        }
    }

    test("Android 13+: turning the reminder on asks for permission first, then enables it") {
        vmTest {
            val env = TestEnvironment()
            env.reminders.authorization = ReminderAuthorization.NOT_DETERMINED
            val asked = mutableListOf<Unit>()
            val vm = settings(env, asked = asked)
            advanceUntilIdle()
            vm.setReminderEnabled(true)
            advanceUntilIdle()
            vm.state.value.requestPermission shouldBe true
            env.reminders.authorization = ReminderAuthorization.ALLOWED // user tapped Allow
            vm.onPermissionResult()
            advanceUntilIdle()
            asked.size shouldBe 1
            vm.state.value.requestPermission shouldBe false
            vm.state.value.settings.reminder.isEnabled shouldBe true
        }
    }

    test("Android 10-12: no permission dialog") {
        vmTest {
            val env = TestEnvironment()
            val vm = settings(env, sdk = 30)
            advanceUntilIdle()
            vm.setReminderEnabled(true)
            advanceUntilIdle()
            vm.state.value.requestPermission shouldBe false
            vm.state.value.settings.reminder.isEnabled shouldBe true
        }
    }

    test("denied notifications show the unavailable state when Settings opens and on resume") {
        vmTest {
            val env = TestEnvironment()
            env.reminders.authorization = ReminderAuthorization.DENIED
            val vm = settings(env)
            advanceUntilIdle()
            vm.state.value.reminderUnavailable shouldBe true
            vm.setReminderEnabled(true)
            advanceUntilIdle()
            vm.state.value.settings.reminder.isEnabled shouldBe false
            env.reminders.authorization = ReminderAuthorization.ALLOWED // allowed in system settings
            vm.refreshPermission()
            advanceUntilIdle()
            vm.state.value.reminderUnavailable shouldBe false
        }
    }

    test("export with no entries says so") {
        vmTest {
            val vm = settings(TestEnvironment())
            vm.export(com.mmomo.dailyjournal.domain.entities.ExportFormat.MARKDOWN)
            advanceUntilIdle()
            vm.state.value.share shouldBe null
            vm.state.value.exportMessage shouldBe "No entries to export yet."
        }
    }
})

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest : FunSpec({
    fun fresh() = TestEnvironment(settings = AppSettings())
    fun onboarding(env: TestEnvironment, sdk: Int = 34) =
        OnboardingViewModel(env.useCases, { env.reminders.authorization() }, {}, SavedStateHandle(), sdkInt = sdk)

    test("needs at least one category; Back on the reminder step returns to categories") {
        vmTest {
            val vm = onboarding(fresh())
            PromptCategory.entries.forEach(vm::toggle)
            vm.next()
            vm.state.value.step shouldBe OnboardingViewModel.Step.CATEGORIES
            vm.state.value.message shouldBe "Choose at least one category."
            vm.toggle(PromptCategory.GOALS)
            vm.next()
            vm.state.value.step shouldBe OnboardingViewModel.Step.REMINDER
            vm.back() shouldBe true
            vm.state.value.step shouldBe OnboardingViewModel.Step.CATEGORIES
            vm.back() shouldBe false
        }
    }

    test("Not now completes onboarding with the reminder off and no permission prompt") {
        vmTest {
            val env = fresh()
            env.reminders.authorization = ReminderAuthorization.NOT_DETERMINED
            val vm = onboarding(env)
            vm.next()
            vm.skipReminder()
            advanceUntilIdle()
            vm.state.value.finished shouldBe true
            vm.state.value.requestPermission shouldBe false
            env.settings.settings.onboardingCompleted shouldBe true
            env.settings.settings.reminder.isEnabled shouldBe false
        }
    }

    test("turning the reminder on asks for permission; a denial still finishes onboarding") {
        vmTest {
            val env = fresh()
            env.reminders.authorization = ReminderAuthorization.NOT_DETERMINED
            val vm = onboarding(env)
            vm.next()
            vm.setTime(7, 30)
            vm.enableReminder()
            advanceUntilIdle()
            vm.state.value.requestPermission shouldBe true
            env.reminders.authorization = ReminderAuthorization.DENIED
            vm.onPermissionResult()
            advanceUntilIdle()
            vm.state.value.finished shouldBe true
            env.settings.settings.onboardingCompleted shouldBe true
            env.settings.settings.reminder.isEnabled shouldBe false
        }
    }
})

class DeepLinkRouterTest : FunSpec({
    test("a link waits until the app is ready and onboarded, then is delivered once") {
        val router = DeepLinkRouter()
        router.onLink(DeepLink.parse("dailyjournal://backfill")) shouldBe true
        router.hasPending shouldBe true
        router.setReady(true)
        router.hasPending shouldBe false
        withTimeout(1_000) { router.requests.first() } shouldBe DeepLink.BACKFILL_EDITOR
    }

    test("unknown links are ignored") {
        val router = DeepLinkRouter()
        router.onLink(DeepLink.parse("dailyjournal://evil?x=1")) shouldBe false
        router.onLink(DeepLink.parse("https://example.com")) shouldBe false
        router.onLink(DeepLink.parse(null)) shouldBe false
        router.hasPending shouldBe false
    }

    test("when ready, a link goes straight to Today") {
        val router = DeepLinkRouter()
        router.setReady(true)
        router.onLink(DeepLink.TODAY_EDITOR)
        withTimeout(1_000) { router.requests.first() } shouldBe DeepLink.TODAY_EDITOR
    }
})
