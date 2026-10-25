package com.mononote.app.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mononote.app.data.AppLock
import com.mononote.app.ui.Mono
import com.mononote.app.ui.TextButtonMono

private const val PIN_LENGTH = 4

/** 점 4개 + 숫자 키패드 */
@Composable
fun PinPad(
    title: String,
    message: String?,
    messageIsError: Boolean,
    resetKey: Int,
    onComplete: (String) -> Unit,
    bioLabel: String? = null,
    onBio: () -> Unit = {},
) {
    var pin by remember(resetKey) { mutableStateOf("") }

    fun press(d: String) {
        if (pin.length >= PIN_LENGTH) return
        pin += d
        if (pin.length == PIN_LENGTH) onComplete(pin)
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Mono.Ink)
        Text(
            message ?: " ",
            Modifier.padding(top = 10.dp),
            fontSize = 13.sp,
            color = if (messageIsError) Mono.Sun else Mono.Sub,
        )
        Row(Modifier.padding(vertical = 32.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            for (i in 0 until PIN_LENGTH) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .then(if (i < pin.length) Modifier.background(Mono.Ink) else Modifier.border(1.5.dp, Mono.Ink, CircleShape)),
                )
            }
        }
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (bioLabel != null) "bio" else "", "0", "del"),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                    row.forEach { k ->
                        Key(
                            label = when (k) { "del" -> "⌫"; "bio" -> bioLabel ?: ""; else -> k },
                            small = k == "bio",
                            enabled = k.isNotEmpty(),
                        ) {
                            when (k) {
                                "del" -> pin = pin.dropLast(1)
                                "bio" -> onBio()
                                else -> press(k)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, small: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = if (small) 13.sp else 26.sp, fontWeight = FontWeight.Light, color = Mono.Ink)
    }
}

/** 앱을 열 때 나오는 잠금 화면 */
@Composable
fun LockScreen(bioAvailable: Boolean, onBio: () -> Unit, onUnlock: () -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    var resetKey by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler { onExit() }
    LaunchedEffect(Unit) { if (bioAvailable) onBio() }

    Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
        PinPad(
            title = "겹",
            message = error ?: "비밀번호를 입력하세요",
            messageIsError = error != null,
            resetKey = resetKey,
            onComplete = { pin ->
                if (AppLock.verify(context, pin)) onUnlock()
                else {
                    error = "비밀번호가 맞지 않아요"
                    resetKey++
                }
            },
            bioLabel = if (bioAvailable) "지문" else null,
            onBio = onBio,
        )
    }
}

/** 새 비밀번호 정하기 (두 번 입력) */
@Composable
fun PinSetupDialog(onDone: (String) -> Unit, onCancel: () -> Unit) {
    var first by remember { mutableStateOf<String?>(null) }
    var resetKey by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
            Box(Modifier.fillMaxSize()) {
                PinPad(
                    title = if (first == null) "새 비밀번호" else "한 번 더 입력",
                    message = error ?: if (first == null) "숫자 4자리를 정해 주세요" else "확인을 위해 다시 입력하세요",
                    messageIsError = error != null,
                    resetKey = resetKey,
                    onComplete = { pin ->
                        val f = first
                        if (f == null) {
                            first = pin
                            error = null
                        } else if (f == pin) {
                            onDone(pin)
                        } else {
                            first = null
                            error = "두 번 입력한 비밀번호가 달라요. 처음부터 다시 입력하세요"
                        }
                        resetKey++
                    },
                )
                Box(Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp)) {
                    TextButtonMono("취소", color = Mono.Sub, onClick = onCancel)
                }
            }
        }
    }
}
