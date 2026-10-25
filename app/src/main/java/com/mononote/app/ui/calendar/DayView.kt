package com.mononote.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.data.AppStore
import com.mononote.app.data.EnglishWeekdays
import com.mononote.app.data.Event
import com.mononote.app.data.KoreanWeekdays
import com.mononote.app.data.Lunar
import com.mononote.app.data.eventsOn
import com.mononote.app.data.hhmm
import com.mononote.app.ui.Mono
import com.mononote.app.ui.OutlinedBox
import com.mononote.app.ui.SmallLabel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

private val HourHeight = 56.dp
private val LabelWidth = 46.dp

@Composable
fun DayView(
    store: AppStore,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthMode: () -> Unit,
    onEdit: (Event) -> Unit,
    onNewAt: (Int) -> Unit,
) {
    val data = store.data
    val events = data.eventsOn(selected)
    val allDay = events.filter { it.allDay }
    val timed = events.filter { !it.allDay }
    val lunar = Lunar.of(selected)

    Column(Modifier.fillMaxSize()) {
        // 헤더
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "${selected.dayOfMonth}",
                fontSize = 68.sp,
                fontWeight = FontWeight.ExtraLight,
                lineHeight = 64.sp,
                letterSpacing = (-3).sp,
            )
            Column(Modifier.padding(start = 12.dp, bottom = 4.dp)) {
                SmallLabel(EnglishWeekdays[selected.dayOfWeek.value - 1] + " · 음 " + lunar.short)
                Text("${selected.year}년 ${selected.monthValue}월", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.border(1.dp, Mono.Box, RoundedCornerShape(22.dp)).padding(3.dp),
            ) {
                Segment("월", false, onMonthMode)
                Segment("일", true) {}
            }
        }

        // 주간 막대
        val weekStart = selected.minusDays((selected.dayOfWeek.value % 7).toLong())
        OutlinedBox(
            Modifier
                .padding(horizontal = 14.dp)
                .pointerInput(selected) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total < -60f) onSelect(selected.plusWeeks(1))
                            if (total > 60f) onSelect(selected.minusWeeks(1))
                        },
                    ) { _, dx -> total += dx }
                },
            padding = PaddingValues(vertical = 10.dp, horizontal = 6.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0..6) {
                    val d = weekStart.plusDays(i.toLong())
                    val on = d == selected
                    val sunday = i == 0
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(d) }
                            .padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(KoreanWeekdays[d.dayOfWeek.value - 1], fontSize = 11.sp, color = if (sunday) Mono.Sun else Mono.Sub)
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (on) Mono.Ink else Color.Transparent)
                                .then(if (d == today && !on) Modifier.border(1.dp, Mono.Ink, RoundedCornerShape(8.dp)) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${d.dayOfMonth}",
                                fontSize = 14.sp,
                                color = if (on) Color.White else if (sunday) Mono.Sun else Mono.Ink,
                            )
                        }
                        Row(Modifier.height(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            data.eventsOn(d).take(3).forEach {
                                Box(Modifier.size(4.dp).clip(CircleShape).background(categoryColor(store, it)))
                            }
                        }
                    }
                }
            }
        }

        // 종일 일정
        if (allDay.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                allDay.forEach { ev ->
                    val c = categoryColor(store, ev)
                    Row(
                        Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.copy(alpha = 0.14f))
                            .clickable { onEdit(ev) }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
                        Text(ev.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("종일", fontSize = 11.sp, color = Mono.Sub)
                    }
                }
            }
        }

        // 타임라인
        OutlinedBox(
            Modifier
                .padding(horizontal = 14.dp)
                .padding(top = 10.dp, bottom = 12.dp)
                .weight(1f),
            padding = PaddingValues(0.dp),
        ) {
            Timeline(
                store = store,
                timed = timed,
                isToday = selected == today,
                dayKey = selected,
                onEdit = onEdit,
                onNewAt = onNewAt,
            )
        }
    }
}

@Composable
private fun Segment(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(if (on) Mono.Ink else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = if (on) Color.White else Mono.Sub)
    }
}

private data class Placed(val ev: Event, val col: Int, val cols: Int)

/** 겹치는 일정은 나란히 칸을 나눠 배치 */
private fun layoutEvents(evs: List<Event>): List<Placed> {
    val result = mutableListOf<Placed>()
    val cluster = mutableListOf<Pair<Event, Int>>()
    val colEnds = mutableListOf<Int>()
    var clusterEnd = -1
    fun flush() {
        val n = colEnds.size
        cluster.forEach { result.add(Placed(it.first, it.second, n)) }
        cluster.clear()
        colEnds.clear()
        clusterEnd = -1
    }
    for (e in evs.sortedBy { it.startMin }) {
        if (cluster.isNotEmpty() && e.startMin >= clusterEnd) flush()
        var idx = colEnds.indexOfFirst { it <= e.startMin }
        if (idx == -1) {
            colEnds.add(e.endMin)
            idx = colEnds.size - 1
        } else {
            colEnds[idx] = e.endMin
        }
        cluster.add(e to idx)
        clusterEnd = maxOf(clusterEnd, e.endMin)
    }
    flush()
    return result
}

@Composable
private fun Timeline(
    store: AppStore,
    timed: List<Event>,
    isToday: Boolean,
    dayKey: LocalDate,
    onEdit: (Event) -> Unit,
    onNewAt: (Int) -> Unit,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val hourPx = with(density) { HourHeight.toPx() }
    var nowMin by remember { mutableIntStateOf(LocalTime.now().let { it.hour * 60 + it.minute }) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        }
    }
    LaunchedEffect(dayKey) {
        val focus = if (isToday) nowMin / 60 - 1 else (timed.minOfOrNull { it.startMin }?.div(60) ?: 8) - 1
        scroll.scrollTo((focus.coerceIn(0, 23) * hourPx).toInt())
    }

    Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(HourHeight * 24 + 80.dp)
                .padding(top = 12.dp, end = 10.dp)
                .pointerInput(dayKey) {
                    detectTapGestures { pos ->
                        val hour = (pos.y / hourPx).toInt().coerceIn(0, 23)
                        onNewAt(hour * 60)
                    }
                },
        ) {
            val areaWidth = maxWidth - LabelWidth
            for (h in 0..24) {
                Row(
                    Modifier.fillMaxWidth().offset(y = HourHeight * h - 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "%02d".format(h),
                        Modifier.width(LabelWidth - 8.dp),
                        fontSize = 11.sp,
                        color = Mono.Sub,
                        textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(8.dp))
                    Spacer(Modifier.weight(1f).height(1.dp).background(Color(0xFFEFEFEF)))
                }
            }
            layoutEvents(timed).forEach { p ->
                val ev = p.ev
                val color = categoryColor(store, ev)
                val top = HourHeight * (ev.startMin / 60f)
                val height = (HourHeight * ((ev.endMin - ev.startMin) / 60f)).coerceAtLeast(22.dp)
                val colW = areaWidth / p.cols
                Row(
                    Modifier
                        .offset(x = LabelWidth + colW * p.col, y = top + 1.dp)
                        .width(colW - 2.dp)
                        .height(height - 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.14f))
                        .clickable { onEdit(ev) }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = if (height > 40.dp) Alignment.Top else Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val tall = height > 40.dp
                    Box(
                        (if (tall) Modifier.padding(top = 14.dp) else Modifier)
                            .size(6.dp).clip(CircleShape).background(color)
                    )
                    Column((if (tall) Modifier.padding(top = 8.dp) else Modifier).weight(1f)) {
                        Text(ev.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (height > 40.dp) {
                            Text(
                                ev.startMin.hhmm() + "–" + ev.endMin.hhmm() + (ev.repeatLabel()?.let { "  ↻ $it" } ?: ""),
                                fontSize = 11.sp,
                                color = Mono.Sub,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            if (isToday) {
                Row(
                    Modifier.fillMaxWidth().offset(x = LabelWidth - 4.dp, y = HourHeight * (nowMin / 60f) - 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Mono.Sun))
                    Spacer(Modifier.weight(1f).height(1.5.dp).background(Mono.Sun))
                }
            }
        }
    }
}
