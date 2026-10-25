package com.mononote.app.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 앱 잠금 비밀번호(숫자 4자리). 비밀번호 자체는 저장하지 않고 소금값을 섞은 해시만 저장한다.
 * 이 설정은 백업 파일·자동 백업에 포함되지 않는다.
 */
object AppLock {
    private fun prefs(c: Context) = c.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun isEnabled(c: Context): Boolean = prefs(c).getString("hash", null) != null

    fun setPin(c: Context, pin: String) {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        prefs(c).edit().putString("salt", salt).putString("hash", hash(salt, pin)).apply()
    }

    fun verify(c: Context, pin: String): Boolean {
        val p = prefs(c)
        val salt = p.getString("salt", null) ?: return false
        return p.getString("hash", null) == hash(salt, pin)
    }

    fun disable(c: Context) {
        prefs(c).edit().clear().apply()
    }

    fun biometricEnabled(c: Context): Boolean = isEnabled(c) && prefs(c).getBoolean("bio", false)

    fun setBiometric(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("bio", on).apply()
    }
}
