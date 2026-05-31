package com.example.mwallet

import android.app.Application
import com.example.mwallet.domain.di.AppContainer

class MwalletApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
