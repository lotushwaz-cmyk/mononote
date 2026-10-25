@file:OptIn(ExperimentalLayoutApi::class)

package com.mononote.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mononote.app.data.CategoryPalette
import com.mononote.app.ui.Mono
import com.mononote.app.ui.PrimaryButton
import com.mononote.app.ui.TextButtonMono

private fun hsvToArgb(h: Float, s: Float, v: Float): Long =
    android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)).toLong() and 0xFFFFFFFFL

/** 색상 막대(색조) + 진하기·밝기 판 + 기본 색으로 고르는 색상표 */
@Composable
fun ColorPickerDialog(initial: Long, onDismiss: () -> Unit, onPick: (Long) -> Unit) {
    val start = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toInt(), it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    val current = hsvToArgb(hue, sat, value)
    val density = LocalDensity.current

    fun setFrom(argb: Long) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb.toInt(), hsv)
        hue = hsv[0]; sat = hsv[1]; value = hsv[2]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Mono.Bg) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(Color(current)))
                    Column {
                        Text("색 고르기", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("#%06X".format(current and 0xFFFFFF), fontSize = 12.sp, color = Mono.Sub)
                    }
                }

                // 진하기(가로) · 밝기(세로) 판
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color.White, Color(hsvToArgb(hue, 1f, 1f)))))
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        .pointerInput(Unit) {
                            fun set(x: Float, y: Float) {
                                sat = (x / size.width).coerceIn(0f, 1f)
                                value = 1f - (y / size.height).coerceIn(0f, 1f)
                            }
                            detectTapGestures { set(it.x, it.y) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(onDragStart = {
                                sat = (it.x / size.width).coerceIn(0f, 1f)
                                value = 1f - (it.y / size.height).coerceIn(0f, 1f)
                            }) { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        },
                ) {
                    val x = maxWidth * sat - 11.dp
                    val y = maxHeight * (1f - value) - 11.dp
                    Box(
                        Modifier
                            .offset(x = x, y = y)
                            .size(22.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .border(3.dp, Color(0x55000000), CircleShape),
                    )
                }

                // 색조 막대
                val hues = remember { (0..6).map { Color(hsvToArgb(it * 60f % 360f, 1f, 1f)) } }
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(hues))
                        .pointerInput(Unit) {
                            detectTapGestures { hue = (it.x / size.width).coerceIn(0f, 0.999f) * 360f }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                hue = (change.position.x / size.width).coerceIn(0f, 0.999f) * 360f
                            }
                        },
                ) {
                    Box(
                        Modifier
                            .offset(x = maxWidth * (hue / 360f) - 6.dp)
                            .size(width = 12.dp, height = 28.dp)
                            .border(2.dp, Color.White, RoundedCornerShape(6.dp)),
                    )
                }

                // 기본 색
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPalette.forEach { col ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .then(if (col == current) Modifier.border(2.dp, Mono.Ink, CircleShape) else Modifier)
                                .clickable { setFrom(col) },
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButtonMono("취소", color = Mono.Sub, onClick = onDismiss)
                    PrimaryButton("이 색으로", modifier = Modifier.weight(1f)) { onPick(current) }
                }
            }
        }
    }
}
