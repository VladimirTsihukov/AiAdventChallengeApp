package com.tishukoff.feature.invariant.impl.di

import com.tishukoff.feature.invariant.api.InvariantProvider
import com.tishukoff.feature.invariant.impl.InvariantProviderImpl
import com.tishukoff.feature.invariant.impl.presentation.InvariantViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val invariantModule = module {
    single<InvariantProvider> { InvariantProviderImpl(get(named("invariantPrefs"))) }
    viewModel { InvariantViewModel(get()) }
}
