package com.mononote.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mononote.app.data.AppStore
import com.mononote.app.ui.AppRoot
import com.mononote.app.ui.MonoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        val store = (application as? MonoApp)?.store ?: AppStore(applicationContext)
        setContent {
            MonoTheme { AppRoot(store) }
        }
    }
}
