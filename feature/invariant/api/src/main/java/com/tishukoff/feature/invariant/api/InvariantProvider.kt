package com.tishukoff.feature.invariant.api

import kotlinx.coroutines.flow.Flow

/**
 * Provides access to invariants — hard constraints the assistant must never violate.
 */
interface InvariantProvider {

    /** All invariants as a reactive stream. */
    val invariants: Flow<List<Invariant>>

    /** Builds a system prompt section from active invariants. Returns empty string if none. */
    fun buildInvariantPrompt(): String

    /** Adds a new invariant. */
    fun addInvariant(category: InvariantCategory, text: String)

    /** Removes an invariant by ID. */
    fun removeInvariant(id: String)

    /** Toggles an invariant on/off. */
    fun setEnabled(id: String, enabled: Boolean)
}
