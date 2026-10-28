package com.mononote.app.ui.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.data.AppStore
import com.mononote.app.data.Habit
import com.mononote.app.data.HabitGoal
import com.mononote.app.data.KoreanWeekdays
import com.mononote.app.data.koWeekday
import com.mononote.app.ui.Chip
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.calendar.TodayButton
import com.mononote.app.ui.outlinedBox
import java.time.LocalDate

/** 이 습관을 해야 하는 날인지 */
fun Habit.scheduledOn(d: LocalDate): Boolean =
    goal != HabitGoal.DAYS || d.dayOfWeek.value in weekDays

fun Habit.freqLabel(): String = when (goal) {
    HabitGoal.DAILY -> "매일"
    HabitGoal.TIMES -> "주 ${timesPerWeek}회"
    HabitGoal.DAYS -> if (weekDays.size == 7) "매일" else
        listOf(7, 1, 2, 3, 4, 5, 6).filter { it in weekDays }.joinToString("·") { KoreanWeekdays[it - 1] }
}

/** 오늘(또는 오늘 아직 안 했으면 어제)까지 이어진 연속 기록. 쉬는 요일은 건너뜀 */
fun Habit.streak(today: LocalDate): Int {
    var d = today
    if (d.toEpochDay() !in done) d = d.minusDays(1)
    var n = 0
    var guard = 0
    while (guard++ < 3660) {
        if (!scheduledOn(d) && goal == HabitGoal.DAYS) { d = d.minusDays(1); continue }
        if (d.toEpochDay() in done) { n++; d = d.minusDays(1) } else break
    }
    return n
}

/** 기간 안의 최장 연속 기록 */
fun Habit.bestRun(from: LocalDate, to: LocalDate): Int {
    var run = 0
    var best = 0
    var d = from
    while (!d.isAfter(to)) {
        if (d.toEpochDay() in done) { run++; best = maxOf(best, run) }
        else if (scheduledOn(d) || goal != HabitGoal.DAYS) run = 0
        d = d.plusDays(1)
    }
    return best
}

fun AppStore.toggleHabit(id: String, day: LocalDate) {
    val e = day.toEpochDay()
    update { d ->
        d.copy(habits = d.habits.map {
            if (it.id == id) it.copy(done = if (e in it.done) it.done - e else it.done + e) else it
        })
    }
}

private sealed interface HabitEdit {
    data object New : HabitEdit
    data class Existing(val id: String) : HabitEdit
}

@Composable
fun HabitScreen(store: AppStore) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    var monthEpoch by rememberSaveable { mutableLongStateOf(LocalDate.now().withDayOfMonth(1).toEpochDay()) }
    var editing by remember { mutableStateOf<HabitEdit?>(null) }
    val today = LocalDate.now()
    val habits = store.data.habits

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallLabel(
                    "HABIT · %d.%02d %s".format(
                        today.monthValue, today.dayOfMonth, today.dayOfWeek.name.take(3),
                    ),
                )
                Text("습관 기록", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
            }
            IconBtn(MonoIcons.Plus, "습관 추가") { editing = HabitEdit.New }
        }
        Row(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip("오늘", !monthly) { monthly = false }
            Chip("월간", monthly) { monthly = true }
        }

        if (habits.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).outlinedBox()
                    .clickable { editing = HabitEdit.New }.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("아직 습관이 없어요", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("오른쪽 위 + 를 눌러 첫 습관을 만들어 보세요", fontSize = 13.sp, color = Mono.Sub)
            }
        } else if (monthly) {
            MonthView(store, habits, LocalDate.ofEpochDay(monthEpoch), today,
                onMonth = { monthEpoch = it.toEpochDay() },
                onOpen = { editing = HabitEdit.Existing(it.id) })
        } else {
            TodayView(store, habits, today, onOpen = { editing = HabitEdit.Existing(it.id) })
        }
    }

    when (val e = editing) {
        null -> Unit
        HabitEdit.New -> HabitEditor(store, null) { editing = null }
        is HabitEdit.Existing -> HabitEditor(store, habits.firstOrNull { it.id == e.id }) { editing = null }
    }
}

@Composable
private fun TodayView(store: AppStore, habits: List<Habit>, today: LocalDate, onOpen: (Habit) -> Unit) {
    val te = today.toEpochDay()
    val scheduled = habits.filter { it.scheduledOn(today) }
    val doneCount = scheduled.count { te in it.done }
    val total = scheduled.size
    val sorted = habits.sortedBy { if (it.scheduledOn(today)) 0 else 1 }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().outlinedBox().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$doneCount", fontSize = 36.sp, fontWeight = FontWeight.ExtraLight, lineHeight = 36.sp)
                    Text(" / $total", fontSize = 14.sp, color = Mono.Sub, modifier = Modifier.padding(bottom = 4.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        when {
                            total == 0 -> "오늘은 쉬는 날이에요"
                            doneCount >= total -> "오늘 습관을 모두 지켰어요"
                            else -> "${total - doneCount}개 남았어요"
                        },
                        fontSize = 12.sp, color = Mono.Sub,
                    )
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Mono.Soft)) {
                        Box(
                            Modifier.fillMaxWidth(if (total == 0) 0f else doneCount / total.toFloat())
                                .height(6.dp).background(Mono.Ink),
                        )
                    }
                }
            }
        }
        items(sorted, key = { it.id }) { h ->
            val color = Color(h.color)
            val on = te in h.done
            val rest = !h.scheduledOn(today)
            Row(
                Modifier.fillMaxWidth().outlinedBox().clickable { onOpen(h) }
                    .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            h.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = if (rest) Mono.Muted else Mono.Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(if (rest) "오늘 쉬는 날" else h.freqLabel(), fontSize = 11.sp, color = Mono.Sub)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 6 downTo 0) {
                            val d = today.minusDays(i.toLong())
                            val v = d.toEpochDay() in h.done
                            val isToday = i == 0
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (v) color else if (isToday) Mono.Bg else Color(0xFFEEEEEE))
                                    .then(if (isToday && !v) Modifier.border(1.5.dp, Mono.Ink, CircleShape) else Modifier),
                            )
                        }
                        Text("연속 ${h.streak(today)}일", fontSize = 11.sp, color = Mono.Sub, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(if (on) Modifier.background(color) else Modifier.border(1.5.dp, Mono.Box, CircleShape))
                        .clickable(role = Role.Checkbox, onClickLabel = if (on) "오늘 완료 취소" else "오늘 완료") {
                            store.toggleHabit(h.id, today)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MonoIcons.Check, contentDescription = h.name + if (on) " 완료함" else " 아직 안 함",
                        tint = if (on) Color.White else Color(0xFFBDBDBD), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthView(
    store: AppStore,
    habits: List<Habit>,
    month: LocalDate,
    today: LocalDate,
    onMonth: (LocalDate) -> Unit,
    onOpen: (Habit) -> Unit,
) {
    val first = month.withDayOfMonth(1)
    val last = month.withDayOfMonth(month.lengthOfMonth())
    val isFutureMonth = first.isAfter(today)
    val pastEnd = if (last.isBefore(today)) last else today
    val pastDays = if (isFutureMonth) 0 else pastEnd.dayOfMonth
    val inMonth = { h: Habit -> h.done.count { it in first.toEpochDay()..pastEnd.toEpochDay() } }
    val totalDone = habits.sumOf { inMonth(it) }
    val overall = if (pastDays == 0) 0 else (totalDone * 100 / (pastDays * habits.size))
    val best = habits.maxByOrNull { it.bestRun(first, pastEnd) }
    val bestRun = best?.bestRun(first, pastEnd) ?: 0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${month.year}년 ${month.monthValue}월", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("전체 달성률 $overall%", fontSize = 12.sp, color = Mono.Sub)
                }
                if (month.year != today.year || month.month != today.month) {
                    TodayButton { onMonth(today.withDayOfMonth(1)) }
                }
                IconBtn(MonoIcons.Back, "지난 달", size = 20.dp) { onMonth(first.minusMonths(1)) }
                IconBtn(MonoIcons.Forward, "다음 달", size = 20.dp) { onMonth(first.plusMonths(1)) }
            }
        }
        items(habits.chunked(2), key = { it.first().id }) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { h -> MonthCard(store, h, first, today, Modifier.weight(1f)) { onOpen(h) } }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (best != null && bestRun > 0) {
            item {
                Row(
                    Modifier.fillMaxWidth().outlinedBox().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SmallLabel("BEST")
                    Text(best.name, fontSize = 13.sp)
                    Text("· 최장 ${bestRun}일 연속", fontSize = 13.sp, color = Mono.Sub)
                }
            }
        }
        item {
            Text(
                "지난 날짜의 점을 누르면 기록을 고칠 수 있어요",
                Modifier.fillMaxWidth().padding(top = 2.dp),
                fontSize = 11.sp, color = Mono.Muted, textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthCard(store: AppStore, h: Habit, first: LocalDate, today: LocalDate, modifier: Modifier, onOpen: () -> Unit) {
    val color = Color(h.color)
    val last = first.withDayOfMonth(first.lengthOfMonth())
    val pastEnd = if (last.isBefore(today)) last else today
    val pastDays = if (first.isAfter(today)) 0 else pastEnd.dayOfMonth
    val doneDays = h.done.count { it in first.toEpochDay()..pastEnd.toEpochDay() }
    val offset = first.dayOfWeek.value % 7
    val cells = offset + first.lengthOfMonth()

    Column(
        modifier.outlinedBox().clickable(onClick = onOpen).padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(h.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row {
                Text(h.freqLabel() + " · ", fontSize = 11.sp, color = Mono.Sub)
                Text("${doneDays}일", fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
                Text(" / ${pastDays}일", fontSize = 11.sp, color = Mono.Sub)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(7, 1, 2, 3, 4, 5, 6).forEach { v ->
                    Text(
                        KoreanWeekdays[v - 1], Modifier.width(14.dp), fontSize = 9.sp, textAlign = TextAlign.Center,
                        color = if (v == 7) Mono.Sun else Mono.Muted,
                    )
                }
            }
            for (w in 0 until (cells + 6) / 7) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0..6) {
                        val idx = w * 7 + i - offset
                        if (idx < 0 || idx >= first.lengthOfMonth()) {
                            Spacer(Modifier.size(14.dp))
                        } else {
                            val d = first.plusDays(idx.toLong())
                            val v = d.toEpochDay() in h.done
                            val future = d.isAfter(today)
                            val isToday = d == today
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (v) color else if (future || isToday) Mono.Bg else Color(0xFFEEEEEE))
                                    .then(
                                        when {
                                            isToday && !v -> Modifier.border(1.5.dp, Mono.Ink, CircleShape)
                                            future -> Modifier.border(1.dp, Color(0xFFEDEDED), CircleShape)
                                            else -> Modifier
                                        },
                                    )
                                    .then(
                                        if (future) Modifier
                                        else Modifier.clickable(onClickLabel = "${d.monthValue}월 ${d.dayOfMonth}일 ${d.koWeekday()} 기록 바꾸기") {
                                            store.toggleHabit(h.id, d)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}
