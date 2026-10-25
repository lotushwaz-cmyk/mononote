@file:OptIn(ExperimentalLayoutApi::class)

package com.mononote.app.ui.calendar

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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mononote.app.data.AppStore
import com.mononote.app.data.Event
import com.mononote.app.data.KoreanWeekdays
import com.mononote.app.data.Repeat
import com.mononote.app.data.dotted
import com.mononote.app.data.hhmm
import com.mononote.app.data.koWeekday
import com.mononote.app.data.newId
import com.mononote.app.ui.Chip
import com.mononote.app.ui.FieldRow
import com.mononote.app.ui.Mono
import com.mononote.app.ui.OutlineButton
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.UnderlineField
import com.mononote.app.ui.pickDate
import com.mononote.app.ui.pickTime
import java.time.LocalDate

@Composable
fun EventEditor(
    store: AppStore,
    original: Event?,
    initialDate: LocalDate,
    initialStart: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val categories = store.data.categories
    var title by remember { mutableStateOf(original?.title ?: "") }
    var categoryId by remember { mutableStateOf(original?.categoryId ?: categories.firstOrNull()?.id) }
    var date by remember { mutableLongStateOf(original?.date ?: initialDate.toEpochDay()) }
    var allDay by remember { mutableStateOf(original?.allDay ?: false) }
    var start by remember { mutableIntStateOf(original?.startMin ?: initialStart) }
    var end by remember { mutableIntStateOf(original?.endMin ?: (initialStart + 60).coerceAtMost(23 * 60 + 59)) }
    var repeat by remember { mutableStateOf(original?.repeat ?: Repeat.NONE) }
    var weekDays by remember { mutableStateOf(original?.weekDays ?: emptySet()) }
    var until by remember { mutableStateOf(original?.repeatUntil) }
    var askDelete by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }

    val startDate = LocalDate.ofEpochDay(date)

    fun save() {
        val fixedEnd = if (end <= start) (start + 60).coerceAtMost(23 * 60 + 59) else end
        val ev = Event(
            id = original?.id ?: newId(),
            title = title.trim().ifEmpty { "제목 없음" },
            categoryId = categoryId,
            date = date,
            allDay = allDay,
            startMin = start,
            endMin = fixedEnd,
            repeat = repeat,
            weekDays = if (repeat == Repeat.WEEKLY) weekDays.ifEmpty { setOf(startDate.dayOfWeek.value) } else emptySet(),
            repeatUntil = if (repeat == Repeat.NONE) null else until,
            exceptions = original?.exceptions ?: emptySet(),
        )
        store.update { d ->
            d.copy(events = if (original == null) d.events + ev else d.events.map { if (it.id == ev.id) ev else it })
        }
        onDismiss()
    }

    fun deleteAll() {
        store.update { d -> d.copy(events = d.events.filterNot { it.id == original?.id }) }
        onDismiss()
    }

    fun deleteOnce() {
        val day = initialDate.toEpochDay()
        store.update { d ->
            d.copy(events = d.events.map { if (it.id == original?.id) it.copy(exceptions = it.exceptions + day) else it })
        }
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButtonMono("취소", color = Mono.Sub, onClick = onDismiss)
                    Text(
                        if (original == null) "일정 추가" else "일정 수정",
                        Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    TextButtonMono("저장", bold = true, onClick = ::save)
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    UnderlineField(title, { title = it }, "제목", fontSize = 20)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallLabel("CATEGORY")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { c ->
                                Chip(c.name, categoryId == c.id, dot = Color(c.color)) { categoryId = c.id }
                            }
                            Chip("없음", categoryId == null) { categoryId = null }
                            Chip("+ 편집", false) { showCategories = true }
                        }
                    }

                    Column {
                        SmallLabel("DATE · TIME")
                        FieldRow("날짜", "${startDate.dotted()} (${startDate.koWeekday()})") {
                            pickDate(context, startDate) { date = it.toEpochDay() }
                        }
                        Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("종일", fontSize = 14.sp, color = Mono.Sub, modifier = Modifier.weight(1f))
                            Switch(
                                checked = allDay,
                                onCheckedChange = { allDay = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Mono.Ink,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCFCFCF),
                                    uncheckedBorderColor = Color(0xFFCFCFCF),
                                ),
                            )
                        }
                        if (!allDay) {
                            FieldRow("시작", start.hhmm()) {
                                pickTime(context, start) { m ->
                                    val dur = (end - start).coerceAtLeast(30)
                                    start = m
                                    end = (m + dur).coerceAtMost(23 * 60 + 59)
                                }
                            }
                            FieldRow("종료", end.hhmm()) { pickTime(context, end) { end = it } }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallLabel("REPEAT")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Repeat.entries.forEach { r -> Chip(r.label, repeat == r) { repeat = r } }
                        }
                        if (repeat == Repeat.WEEKLY) {
                            val active = weekDays.ifEmpty { setOf(startDate.dayOfWeek.value) }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(7, 1, 2, 3, 4, 5, 6).forEach { v ->
                                    val on = v in active
                                    Box(
                                        Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .then(if (on) Modifier.background(Mono.Ink) else Modifier.border(1.dp, Mono.Box, CircleShape))
                                            .clickable { weekDays = if (on) active - v else active + v },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            KoreanWeekdays[v - 1],
                                            fontSize = 13.sp,
                                            color = if (on) Color.White else if (v == 7) Mono.Sun else Mono.Ink,
                                        )
                                    }
                                }
                            }
                        }
                        if (repeat == Repeat.YEARLY_LUNAR) {
                            Text("시작 날짜의 음력 날짜에 맞춰 매년 반복돼요.", fontSize = 12.sp, color = Mono.Sub)
                        }
                        if (repeat != Repeat.NONE) {
                            FieldRow("반복 종료", until?.let { LocalDate.ofEpochDay(it).dotted() } ?: "없음") {
                                pickDate(context, until?.let { LocalDate.ofEpochDay(it) } ?: startDate.plusMonths(1)) {
                                    until = it.toEpochDay()
                                }
                            }
                            if (until != null) TextButtonMono("반복 종료 날짜 지우기", color = Mono.Sub) { until = null }
                        }
                    }

                    if (original != null) {
                        OutlineButton("삭제", color = Mono.Sun) {
                            if (original.repeat == Repeat.NONE) deleteAll() else askDelete = true
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = Mono.Bg,
            title = { Text("반복 일정 삭제", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    TextButtonMono("${initialDate.dotted()} 이 날만 삭제") { askDelete = false; deleteOnce() }
                    TextButtonMono("반복 일정 전체 삭제", color = Mono.Sun) { askDelete = false; deleteAll() }
                }
            },
            confirmButton = { TextButtonMono("취소", color = Mono.Sub) { askDelete = false } },
        )
    }
    if (showCategories) CategoryManager(store) { showCategories = false }
}
