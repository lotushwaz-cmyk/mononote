package com.mononote.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.mononote.app.data.AppStore
import com.mononote.app.ui.AppRoot
import com.mononote.app.ui.MonoTheme

class MainActivity : ComponentActivity() {

    /** 위젯의 + 버튼으로 열렸는지 */
    private val newEventPending = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        if (savedInstanceState == null) handle(intent)
        val store = (application as? MonoApp)?.store ?: AppStore(applicationContext)
        setContent {
            MonoTheme {
                AppRoot(
                    store = store,
                    newEventPending = newEventPending.value,
                    onNewEventHandled = { newEventPending.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_NEW_EVENT) newEventPending.value = true
    }

    companion object {
        const val ACTION_NEW_EVENT = "com.mononote.app.NEW_EVENT"
    }
}
