package com.mononote.app

import android.app.Application
import com.mononote.app.data.AppStore

class MonoApp : Application() {
    val store: AppStore by lazy { AppStore(applicationContext) }
}
