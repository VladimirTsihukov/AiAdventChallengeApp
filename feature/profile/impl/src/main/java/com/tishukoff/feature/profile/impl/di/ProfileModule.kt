package com.tishukoff.feature.profile.impl.di

import com.tishukoff.feature.profile.api.ProfileProvider
import com.tishukoff.feature.profile.impl.ProfileProviderImpl
import com.tishukoff.feature.profile.impl.presentation.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val profileModule = module {
    single<ProfileProvider> { ProfileProviderImpl(get(named("profilePrefs"))) }
    viewModel { ProfileViewModel(get()) }
}
