package com.tishukoff.feature.invariant.impl

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tishukoff.feature.invariant.api.Invariant
import com.tishukoff.feature.invariant.api.InvariantCategory
import com.tishukoff.feature.invariant.api.InvariantProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal class InvariantProviderImpl(
    private val prefs: SharedPreferences,
) : InvariantProvider {

    private val _invariants = MutableStateFlow(loadInvariants())
    override val invariants: Flow<List<Invariant>> = _invariants.asStateFlow()

    override fun buildInvariantPrompt(): String {
        val active = _invariants.value.filter { it.enabled }
        if (active.isEmpty()) return ""

        val grouped = active.groupBy { it.category }

        return buildString {
            appendLine("=== INVARIANTS (HARD CONSTRAINTS) ===")
            appendLine("The following invariants are absolute constraints that you MUST NOT violate under any circumstances.")
            appendLine("If a user request conflicts with any invariant, you MUST:")
            appendLine("1. Refuse to provide a solution that violates the invariant")
            appendLine("2. Explicitly name the invariant being violated")
            appendLine("3. Explain why the request conflicts with it")
            appendLine("4. Suggest an alternative that respects all invariants")
            appendLine()

            for ((category, items) in grouped) {
                appendLine("[${category.displayName}]")
                for (item in items) {
                    appendLine("- ${item.text}")
                }
                appendLine()
            }

            appendLine("Always consider these invariants in your reasoning before answering.")
            append("=== END INVARIANTS ===")
        }
    }

    override fun addInvariant(category: InvariantCategory, text: String) {
        val invariant = Invariant(
            id = UUID.randomUUID().toString(),
            category = category,
            text = text,
        )
        val updated = _invariants.value + invariant
        _invariants.value = updated
        saveInvariants(updated)
    }

    override fun removeInvariant(id: String) {
        val updated = _invariants.value.filter { it.id != id }
        _invariants.value = updated
        saveInvariants(updated)
    }

    override fun setEnabled(id: String, enabled: Boolean) {
        val updated = _invariants.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        _invariants.value = updated
        saveInvariants(updated)
    }

    private fun loadInvariants(): List<Invariant> {
        val json = prefs.getString(KEY_INVARIANTS, null)
        if (json == null) {
            val defaults = defaultInvariants()
            saveInvariants(defaults)
            return defaults
        }
        return parseInvariants(json)
    }

    private fun saveInvariants(invariants: List<Invariant>) {
        val array = JSONArray()
        for (inv in invariants) {
            array.put(JSONObject().apply {
                put("id", inv.id)
                put("category", inv.category.name)
                put("text", inv.text)
                put("enabled", inv.enabled)
            })
        }
        prefs.edit { putString(KEY_INVARIANTS, array.toString()) }
    }

    private fun parseInvariants(json: String): List<Invariant> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Invariant(
                id = obj.getString("id"),
                category = InvariantCategory.entries.find { it.name == obj.getString("category") }
                    ?: InvariantCategory.TECH_DECISIONS,
                text = obj.getString("text"),
                enabled = obj.optBoolean("enabled", true),
            )
        }
    }

    private companion object {
        const val KEY_INVARIANTS = "invariants_json"

        fun defaultInvariants(): List<Invariant> = listOf(
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.ARCHITECTURE,
                text = "Always follow Clean Architecture with layers: domain, data, presentation",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.ARCHITECTURE,
                text = "Domain layer (UseCases, Repository interfaces, models) must have no Android dependencies",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.ARCHITECTURE,
                text = "Split every feature into :feature:api and :feature:impl modules. Other modules depend only on :api, never on :impl directly",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.ARCHITECTURE,
                text = "Presentation layer must follow MVI pattern: ViewModel, UI State, Compose screens",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_STACK,
                text = "Use Kotlin as the only programming language",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_STACK,
                text = "Use Jetpack Compose for all UI — no XML layouts",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_STACK,
                text = "Use Koin for dependency injection — no Dagger/Hilt",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_STACK,
                text = "Use Retrofit for networking",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_STACK,
                text = "Use Kotlin Coroutines for asynchronous operations — no RxJava",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_DECISIONS,
                text = "Use sealed interface for UI states",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_DECISIONS,
                text = "Prefer val over var — immutability first",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_DECISIONS,
                text = "Never use !! operator — always handle nullability properly",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.TECH_DECISIONS,
                text = "Prefer extension functions over utility classes",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.BUSINESS_RULES,
                text = "Never add dependencies without explicit approval",
            ),
            Invariant(
                id = UUID.randomUUID().toString(),
                category = InvariantCategory.BUSINESS_RULES,
                text = "Never use deprecated APIs",
            ),
        )
    }
}
