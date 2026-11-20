package com.iilmchat.chessproblem

import android.app.Application

class ChessProblemApp : Application() {
    // KI-015: контейнер создаётся до первой активности; lazy исключает lateinit
    // и гарантирует одну инициализацию при обращении из MainActivity.
    val container: AppContainer by lazy { AppContainer(this) }
}
