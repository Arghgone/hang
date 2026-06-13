package com.argh.hang

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tracks whether a verification prompt is currently on screen.
 * Prevents the accessibility service from launching duplicate prompts.
 */
object PromptState {
    private val active = AtomicBoolean(false)

    fun isPromptActive(): Boolean = active.get()

    fun onPromptShown() {
        active.set(true)
    }

    fun onPromptDismissed() {
        active.set(false)
    }
}
