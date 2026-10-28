package com.iilmchat.chessproblem

import android.app.Application

class ChessProblemApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
