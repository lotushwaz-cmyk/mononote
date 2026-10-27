package com.mononote.app.ui.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.data.AppStore
import com.mononote.app.data.EnglishMonths
import com.mononote.app.data.Holidays
import com.mononote.app.data.Event
import com.mononote.app.data.KoreanWeekdays
import com.mononote.app.data.Lunar
import com.mononote.app.data.Repeat
import com.mononote.app.data.category
import com.mononote.app.data.eventsOn
import com.mononote.app.data.hhmm
import com.mononote.app.ui.AddButton
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.OutlinedBox
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.pickDate
import java.time.LocalDate
import java.time.LocalTime

private data class EditRequest(val event: Event?, val date: LocalDate, val startMin: Int)

@Composable
fun CalendarScreen(
    store: AppStore,
    newEventPending: Boolean = false,
    onNewEventHandled: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenDiary: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    var selectedEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var monthEpoch by rememberSaveable { mutableLongStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var dayMode by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EditRequest?>(null) }
    var showCategories by remember { mutableStateOf(false) }

    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val month = LocalDate.ofEpochDay(monthEpoch)

    fun select(d: LocalDate) {
        selectedEpoch = d.toEpochDay()
        monthEpoch = d.withDayOfMonth(1).toEpochDay()
    }

    fun defaultStart(d: LocalDate): Int =
        if (d == today) ((LocalTime.now().hour + 1).coerceAtMost(23)) * 60 else 9 * 60

    LaunchedEffect(newEventPending) {
        if (newEventPending) {
            onNewEventHandled()
            select(today)
            editing = EditRequest(null, today, defaultStart(today))
        }
    }

    if (dayMode) BackHandler { dayMode = false }

    Box(Modifier.fillMaxSize()) {
        if (dayMode) {
            DayView(
                store = store,
                selected = selected,
                today = today,
                onSelect = ::select,
                onToday = { select(today) },
                onMonthMode = { dayMode = false },
                onEdit = { editing = EditRequest(it, selected, it.startMin) },
                onNewAt = { editing = EditRequest(null, selected, it) },
            )
        } else {
            MonthView(
                store = store,
                month = month,
                selected = selected,
                today = today,
                onSelect = { d -> if (d == selected) dayMode = true else select(d) },
                onMonthChange = { monthEpoch = it.toEpochDay() },
                onJump = ::select,
                onToday = { select(today) },
                onOpenDay = { dayMode = true },
                onEdit = { editing = EditRequest(it, selected, it.startMin) },
                onOpenSettings = onOpenSettings,
                onManageCategories = { showCategories = true },
                onOpenDiary = { onOpenDiary(selected) },
            )
        }
        AddButton(
            "일정 추가",
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 16.dp),
        ) { editing = EditRequest(null, selected, defaultStart(selected)) }
    }

    editing?.let { req ->
        EventEditor(
            store = store,
            original = req.event,
            initialDate = req.date,
            initialStart = req.startMin,
            onDismiss = { editing = null },
        )
    }
    if (showCategories) CategoryManager(store) { showCategories = false }
}

fun Event.repeatLabel(): String? = when (repeat) {
    Repeat.NONE -> null
    Repeat.WEEKLY -> {
        val days = weekDays.ifEmpty { setOf(LocalDate.ofEpochDay(date).dayOfWeek.value) }
        "매주 " + listOf(7, 1, 2, 3, 4, 5, 6).filter { it in days }.joinToString("·") { KoreanWeekdays[it - 1] }
    }
    else -> repeat.label
}

@Composable
private fun MonthView(
    store: AppStore,
    month: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (LocalDate) -> Unit,
    onJump: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onOpenDay: () -> Unit,
    onEdit: (Event) -> Unit,
    onOpenSettings: () -> Unit,
    onManageCategories: () -> Unit,
    onOpenDiary: () -> Unit,
) {
    val data = store.data
    val showLunar = data.settings.showLunar
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 헤더: 큰 월 숫자 + 음력 스위치 + 설정
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp, top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "날짜로 이동") { pickDate(context, selected, onJump) },
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "${month.monthValue}",
                    fontSize = 68.sp,
                    fontWeight = FontWeight.ExtraLight,
                    lineHeight = 64.sp,
                    letterSpacing = (-3).sp,
                    color = Mono.Ink,
                )
                Column(Modifier.padding(bottom = 4.dp)) {
                    SmallLabel(EnglishMonths[month.monthValue - 1])
                    Text("${month.year}년", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.weight(1f))
            if (selected != today || month.year != today.year || month.month != today.month) {
                TodayButton(onToday)
            }
            Row(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(role = Role.Switch) {
                        store.update { it.copy(settings = it.settings.copy(showLunar = !showLunar)) }
                    }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("음력", fontSize = 13.sp)
                Box(
                    Modifier
                        .size(width = 40.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (showLunar) Mono.Ink else Color(0xFFCFCFCF))
                        .padding(3.dp),
                    contentAlignment = if (showLunar) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(9.dp)).background(Color.White))
                }
            }
            IconBtn(MonoIcons.Settings, "설정", onClick = onOpenSettings)
        }

        // 달력 그리드
        OutlinedBox(
            Modifier
                .padding(horizontal = 14.dp)
                .pointerInput(month) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = {
                            if (total < -80f) onMonthChange(month.plusMonths(1))
                            if (total > 80f) onMonthChange(month.minusMonths(1))
                        },
                    ) { _, dx -> total += dx }
                },
            padding = PaddingValues(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 2.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEachIndexed { i, d ->
                    Text(
                        d,
                        Modifier.weight(1f).padding(start = 6.dp),
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp,
                        color = if (i == 0) Mono.Sun else Mono.Sub,
                    )
                }
            }
            val first = month.withDayOfMonth(1)
            val offset = first.dayOfWeek.value % 7
            val weeks = (offset + month.lengthOfMonth() + 6) / 7
            val gridStart = first.minusDays(offset.toLong())
            for (w in 0 until weeks) {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
                Row(Modifier.fillMaxWidth().height(66.dp)) {
                    for (i in 0..6) {
                        val day = gridStart.plusDays((w * 7 + i).toLong())
                        DayCell(
                            day = day,
                            inMonth = day.month == month.month,
                            isToday = day == today,
                            isSelected = day == selected,
                            showLunar = showLunar,
                            barColors = data.eventsOn(day).take(3).map { categoryColor(store, it) },
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(day) },
                        )
                    }
                }
            }
        }

        // 카테고리 범례
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                data.categories.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(width = 14.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(Color(c.color)))
                        Text(c.name, fontSize = 12.sp)
                    }
                }
            }
            TextButtonMono("편집", color = Mono.Sub, onClick = onManageCategories)
        }

        // 선택한 날의 일정
        val events = data.eventsOn(selected)
        val lunar = Lunar.of(selected)
        OutlinedBox(
            Modifier.padding(horizontal = 14.dp).padding(top = 6.dp),
            padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (selected == today) "TODAY · " else ""
                SmallLabel(prefix + "%d.%02d ".format(selected.monthValue, selected.dayOfMonth) + selected.dayOfWeek.name.take(3))
                Spacer(Modifier.weight(1f))
                Holidays.nameOf(selected)?.let { Text("$it · ", fontSize = 11.sp, color = Mono.Sun) }
                Text("음 " + lunar.short, fontSize = 11.sp, color = Mono.Sub)
            }
            if (events.isEmpty()) {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Ink))
                Text("일정이 없어요", Modifier.padding(vertical = 16.dp), fontSize = 14.sp, color = Mono.Muted)
            }
            events.forEachIndexed { i, ev ->
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(if (i == 0) Mono.Ink else Mono.Line))
                EventRow(ev, categoryColor(store, ev)) { onEdit(ev) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButtonMono("타임테이블 보기 →", color = Mono.Sub, onClick = onOpenDay)
                TextButtonMono("이 날의 일기 →", color = Mono.Sub, onClick = onOpenDiary)
            }
        }
        Spacer(Modifier.height(90.dp))
    }
}

fun categoryColor(store: AppStore, ev: Event): Color =
    store.data.category(ev.categoryId)?.let { Color(it.color) } ?: Mono.Muted

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    showLunar: Boolean,
    barColors: List<Color>,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val sunday = day.dayOfWeek.value == 7
    val holiday = Holidays.nameOf(day)
    val numColor = when {
        !inMonth -> Mono.Muted
        sunday || holiday != null -> Mono.Sun
        else -> Mono.Ink
    }
    Column(
        modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(start = 4.dp, end = 4.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val shape = RoundedCornerShape(4.dp)
        Box(
            Modifier
                .height(22.dp)
                .width(24.dp)
                .clip(shape)
                .then(
                    when {
                        isToday -> Modifier.background(Mono.Ink)
                        isSelected -> Modifier.border(1.dp, Mono.Ink, shape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${day.dayOfMonth}",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontWeight = if (isToday) FontWeight.Medium else if (inMonth) FontWeight.Normal else FontWeight.Light,
                color = if (isToday) Color.White else numColor,
            )
        }
        if (holiday != null) {
            Text(
                holiday,
                Modifier.padding(start = 2.dp),
                fontSize = if (holiday.length > 4) 9.sp else 10.sp,
                lineHeight = 11.sp,
                letterSpacing = (-0.3).sp,
                color = if (inMonth) Mono.Sun else Mono.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (showLunar) {
            Text(
                Lunar.of(day).short,
                Modifier.padding(start = 2.dp),
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = if (inMonth) Mono.Sub else Mono.Muted,
                maxLines = 1,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            barColors.forEach { c ->
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(c))
            }
        }
    }
}

@Composable
private fun EventRow(ev: Event, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(50.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            if (ev.allDay) "종일" else ev.startMin.hhmm(),
            Modifier.width(44.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
        )
        Box(Modifier.size(width = 3.dp, height = 22.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(ev.title, Modifier.weight(1f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ev.repeatLabel()?.let { Text("↻ $it", fontSize = 11.sp, color = Mono.Sub) }
    }
}

/** 오늘로 돌아가기 */
@Composable
fun TodayButton(onClick: () -> Unit) {
    Box(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Mono.Ink, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "오늘로 이동", onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("오늘", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
