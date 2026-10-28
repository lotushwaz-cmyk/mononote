@file:OptIn(ExperimentalLayoutApi::class)

package com.mononote.app.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mononote.app.data.AppStore
import com.mononote.app.data.CategoryPalette
import com.mononote.app.data.Habit
import com.mononote.app.data.HabitGoal
import com.mononote.app.data.KoreanWeekdays
import com.mononote.app.data.newId
import com.mononote.app.ui.Chip
import com.mononote.app.ui.Mono
import com.mononote.app.ui.OutlineButton
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.UnderlineField
import com.mononote.app.ui.calendar.ColorPickerDialog
import com.mononote.app.ui.outlinedBox
import java.time.LocalDate

@Composable
fun HabitEditor(store: AppStore, original: Habit?, onDismiss: () -> Unit) {
    val used = store.data.habits.map { it.color }.toSet()
    var name by remember { mutableStateOf(original?.name ?: "") }
    var color by remember { mutableLongStateOf(original?.color ?: (CategoryPalette.firstOrNull { it !in used } ?: CategoryPalette.first())) }
    var goal by remember { mutableStateOf(original?.goal ?: HabitGoal.DAILY) }
    var weekDays by remember { mutableStateOf(original?.weekDays ?: (1..7).toSet()) }
    var times by remember { mutableIntStateOf(original?.timesPerWeek ?: 3) }
    var picking by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }

    fun save() {
        val h = Habit(
            id = original?.id ?: newId(),
            name = name.trim().ifEmpty { "새 습관" },
            color = color,
            goal = goal,
            weekDays = if (goal == HabitGoal.DAYS) weekDays.ifEmpty { (1..7).toSet() } else (1..7).toSet(),
            timesPerWeek = times,
            done = original?.done ?: emptySet(),
        )
        store.update { d ->
            d.copy(habits = if (original == null) d.habits + h else d.habits.map { if (it.id == h.id) h else it })
        }
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButtonMono("취소", color = Mono.Sub, onClick = onDismiss)
                    Text(
                        if (original == null) "습관 추가" else "습관 편집",
                        Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    TextButtonMono("저장", bold = true, onClick = ::save)
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column {
                        SmallLabel("NAME")
                        UnderlineField(name, { name = it.take(20) }, "예: 책 읽기", fontSize = 20)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallLabel("COLOR")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val all = if (color in CategoryPalette) CategoryPalette else CategoryPalette + color
                            all.forEach { c ->
                                Box(
                                    Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(c))
                                        .then(if (c == color) Modifier.border(3.dp, Color.White, CircleShape).border(1.dp, Mono.Ink, CircleShape) else Modifier)
                                        .clickable(onClickLabel = "이 색으로") { color = c },
                                )
                            }
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).border(1.dp, Color(0xFFBDBDBD), CircleShape)
                                    .clickable(onClickLabel = "색상표에서 고르기") { picking = true },
                                contentAlignment = Alignment.Center,
                            ) { Text("+", fontSize = 16.sp, color = Mono.Sub) }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallLabel("GOAL")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HabitGoal.entries.forEach { g -> Chip(g.label, goal == g) { goal = g } }
                        }
                        when (goal) {
                            HabitGoal.DAYS -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(7, 1, 2, 3, 4, 5, 6).forEach { v ->
                                    val on = v in weekDays
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape)
                                            .then(if (on) Modifier.background(Color(color)) else Modifier.border(1.dp, Mono.Box, CircleShape))
                                            .clickable { weekDays = if (on) weekDays - v else weekDays + v },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(KoreanWeekdays[v - 1], fontSize = 13.sp, color = if (on) Color.White else Mono.Ink)
                                    }
                                }
                            }
                            HabitGoal.TIMES -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("일주일에", fontSize = 14.sp, color = Mono.Sub)
                                Stepper("−", times > 1) { times-- }
                                Text("${times}회", fontSize = 22.sp, fontWeight = FontWeight.Light)
                                Stepper("+", times < 7) { times++ }
                            }
                            HabitGoal.DAILY -> Unit
                        }
                    }

                    // 미리보기
                    Column(
                        Modifier.fillMaxWidth().outlinedBox().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SmallLabel("PREVIEW")
                        Text(name.ifBlank { "새 습관" }, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        val pat = listOf(1, 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            pat.forEach { v ->
                                Box(Modifier.size(13.dp).clip(CircleShape).background(if (v == 1) Color(color) else Color(0xFFEEEEEE)))
                            }
                        }
                    }

                    if (original != null) {
                        val total = original.done.size
                        Text(
                            "지금까지 ${total}일 기록했어요 · 연속 ${original.streak(LocalDate.now())}일",
                            fontSize = 12.sp, color = Mono.Sub,
                        )
                        OutlineButton("습관 삭제", color = Mono.Sun) { askDelete = true }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    if (picking) {
        ColorPickerDialog(initial = color, onDismiss = { picking = false }, onPick = { color = it; picking = false })
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = Mono.Bg,
            title = { Text("이 습관을 삭제할까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("지금까지의 기록도 함께 지워져요.", fontSize = 14.sp, color = Mono.Sub) },
            confirmButton = {
                TextButtonMono("삭제", color = Mono.Sun, bold = true) {
                    askDelete = false
                    store.update { d -> d.copy(habits = d.habits.filterNot { it.id == original?.id }) }
                    onDismiss()
                }
            },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { askDelete = false } },
        )
    }
}

@Composable
private fun Stepper(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).border(1.dp, Mono.Box, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 18.sp, color = if (enabled) Mono.Ink else Mono.Box) }
}
