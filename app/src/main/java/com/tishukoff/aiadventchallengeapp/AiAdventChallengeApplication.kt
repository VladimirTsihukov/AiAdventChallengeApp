package com.tishukoff.aiadventchallengeapp

import android.app.Application
import com.tishukoff.aiadventchallengeapp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AiAdventChallengeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AiAdventChallengeApplication)
            modules(appModule)
        }
    }
}
