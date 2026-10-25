package com.mononote.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mononote.app.data.AppLock
import com.mononote.app.data.AppStore
import com.mononote.app.ui.AppRoot
import com.mononote.app.ui.MonoTheme
import com.mononote.app.ui.lock.LockScreen

class MainActivity : FragmentActivity() {

    /** 위젯의 + 버튼으로 열렸는지 */
    private val newEventPending = mutableStateOf(false)

    /** 앱 잠금 화면을 보여줄지 */
    private val locked = mutableStateOf(false)
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
        )
        locked.value = savedInstanceState?.getBoolean("locked") ?: AppLock.isEnabled(this)
        if (savedInstanceState == null) handle(intent)
        val store = (application as? MonoApp)?.store ?: AppStore(applicationContext)
        setContent {
            MonoTheme {
                Box {
                    AppRoot(
                        store = store,
                        newEventPending = newEventPending.value && !locked.value,
                        onNewEventHandled = { newEventPending.value = false },
                    )
                    if (locked.value) {
                        LockScreen(
                            bioAvailable = AppLock.biometricEnabled(this@MainActivity) && canUseBiometric(),
                            onBio = ::promptBiometric,
                            onUnlock = { locked.value = false },
                            onExit = { moveTaskToBack(true) },
                        )
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("locked", locked.value)
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) stoppedAt = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // 사진 고르기 등으로 잠깐 나갔다 온 건 잠그지 않고, 30초 넘게 떠나 있었으면 다시 잠금
        if (stoppedAt > 0 && AppLock.isEnabled(this) && SystemClock.elapsedRealtime() - stoppedAt > 30_000) {
            locked.value = true
        }
        stoppedAt = 0
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_NEW_EVENT) newEventPending.value = true
    }

    fun canUseBiometric(): Boolean =
        BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun promptBiometric() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    locked.value = false
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("겹 잠금 해제")
            .setNegativeButtonText("비밀번호 입력")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    companion object {
        const val ACTION_NEW_EVENT = "com.mononote.app.NEW_EVENT"
    }
}
