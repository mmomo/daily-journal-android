package com.mmomo.dailyjournal.app.navigation

import com.mmomo.dailyjournal.domain.entities.DeepLink
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Holds a deep link until the app is ready and onboarding is done (BR-G5), then hands it to Today once.
 * Unknown links are dropped by [DeepLink.parse] before they get here (APP-SEC-5).
 */
class DeepLinkRouter {
    private var pending: DeepLink? = null
    private var canDeliver = false
    private val channel = Channel<DeepLink>(Channel.CONFLATED)

    /** Links for the Today screen; each is delivered once. */
    val requests: Flow<DeepLink> = channel.receiveAsFlow()

    /** Returns true if Today should be shown (a link arrived). */
    fun onLink(link: DeepLink?): Boolean {
        if (link == null) return false
        if (canDeliver) channel.trySend(link) else pending = link
        return true
    }

    /** Called when the app is ready and onboarded (or stops being so). */
    fun setReady(ready: Boolean) {
        canDeliver = ready
        if (ready) pending?.let { channel.trySend(it); pending = null }
    }

    val hasPending: Boolean get() = pending != null
}
