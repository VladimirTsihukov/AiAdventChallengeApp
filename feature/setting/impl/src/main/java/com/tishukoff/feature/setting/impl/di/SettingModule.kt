package com.tishukoff.feature.setting.impl.di

import com.tishukoff.feature.setting.impl.presentation.SettingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingModule = module {
    viewModel { SettingViewModel(get()) }
}
