package com.mononote.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.MainActivity
import com.mononote.app.data.AppLock
import com.mononote.app.data.AppStore
import com.mononote.app.ui.Mono
import com.mononote.app.ui.OutlinedBox
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.lock.PinSetupDialog

@Composable
private fun MonoSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Mono.Ink,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFCFCFCF),
            uncheckedBorderColor = Color(0xFFCFCFCF),
        ),
    )
}

/** 앱 잠금 (비밀번호 4자리, 지문) */
@Composable
fun LockSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(AppLock.isEnabled(context)) }
    var bio by remember { mutableStateOf(AppLock.biometricEnabled(context)) }
    var settingPin by remember { mutableStateOf(false) }
    val canBio = (context as? MainActivity)?.canUseBiometric() == true

    OutlinedBox {
        SmallLabel("LOCK")
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("앱 잠금", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            MonoSwitch(enabled) { on ->
                if (on) settingPin = true
                else {
                    AppLock.disable(context)
                    enabled = false
                    bio = false
                    Toast.makeText(context, "앱 잠금을 껐어요", Toast.LENGTH_SHORT).show()
                }
            }
        }
        Text(
            "앱을 열 때 숫자 4자리 비밀번호를 물어봐요. 30초 넘게 다른 앱에 있다가 돌아와도 다시 잠겨요.\n" +
                "비밀번호를 잊으면 앱을 지우고 다시 설치해야 하니, 백업 파일을 꼭 만들어 두세요.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Mono.Sub,
        )
        if (enabled) {
            TextButtonMono("비밀번호 바꾸기") { settingPin = true }
            if (canBio) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("지문(생체 인증)으로 열기", fontSize = 14.sp, modifier = Modifier.weight(1f))
                    MonoSwitch(bio) { on ->
                        AppLock.setBiometric(context, on)
                        bio = on
                    }
                }
            }
        }
    }

    if (settingPin) {
        PinSetupDialog(
            onDone = { pin ->
                AppLock.setPin(context, pin)
                enabled = true
                settingPin = false
                Toast.makeText(context, "비밀번호를 설정했어요", Toast.LENGTH_SHORT).show()
            },
            onCancel = { settingPin = false },
        )
    }
}

/** 홈 화면 위젯 배경 투명도 */
@Composable
fun WidgetSection(store: AppStore) {
    var opacity by remember { mutableFloatStateOf(store.data.settings.widgetOpacity) }

    OutlinedBox {
        SmallLabel("WIDGET")
        Text("위젯 배경 투명도", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(
            "홈 화면 빈 곳을 길게 눌러 위젯 › 겹 에서 작게·보통·크게 중 골라 놓을 수 있어요.",
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Mono.Sub,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 미리보기: 체크무늬 위에 위젯 배경색
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFDADADA))) {
                Box(Modifier.size(22.dp).background(Color(0xFFF5F5F5)))
                Box(Modifier.size(44.dp).background(Color(0xFF111111).copy(alpha = opacity)))
            }
            Spacer(Modifier.size(14.dp))
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                onValueChangeFinished = {
                    store.update { d -> d.copy(settings = d.settings.copy(widgetOpacity = opacity)) }
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Mono.Ink,
                    activeTrackColor = Mono.Ink,
                    inactiveTrackColor = Mono.Box,
                ),
                modifier = Modifier.weight(1f),
            )
            Text("${(opacity * 100).toInt()}%", fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
        }
        Text(
            "0%는 완전히 투명, 100%는 검정이에요",
            fontSize = 11.sp,
            color = Mono.Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(2.dp))
    }
}
