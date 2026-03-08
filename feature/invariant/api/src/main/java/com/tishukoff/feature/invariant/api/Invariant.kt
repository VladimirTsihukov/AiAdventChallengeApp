package com.tishukoff.feature.invariant.api

data class Invariant(
    val id: String,
    val category: InvariantCategory,
    val text: String,
    val enabled: Boolean = true,
)
