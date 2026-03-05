package com.tishukoff.feature.invariant.impl.presentation

import com.tishukoff.feature.invariant.api.Invariant
import com.tishukoff.feature.invariant.api.InvariantCategory

data class InvariantUiState(
    val invariants: List<Invariant> = emptyList(),
    val selectedCategory: InvariantCategory = InvariantCategory.ARCHITECTURE,
)
