package com.mononote.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mononote.app.data.AppStore
import com.mononote.app.ui.archive.ArchiveScreen
import com.mononote.app.ui.calendar.CalendarScreen
import com.mononote.app.ui.diary.DiaryScreen
import com.mononote.app.ui.settings.SettingsScreen
import java.time.LocalDate

enum class Tab { CALENDAR, DIARY, HABIT, MONEY, ARCHIVE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot(store: AppStore, newEventPending: Boolean = false, onNewEventHandled: () -> Unit = {}) {
    var tab by rememberSaveable { mutableStateOf(Tab.CALENDAR) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var diaryDate by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }

    if (showSettings) {
        BackHandler { showSettings = false }
        Scaffold(containerColor = Mono.Bg) { pad ->
            Box(Modifier.padding(pad)) { SettingsScreen(store, onBack = { showSettings = false }) }
        }
        return
    }

    // 위젯의 + 버튼: 캘린더 탭으로 이동해 새 일정 열기
    androidx.compose.runtime.LaunchedEffect(newEventPending) {
        if (newEventPending) {
            showSettings = false
            tab = Tab.CALENDAR
        }
    }

    if (tab != Tab.CALENDAR) BackHandler { tab = Tab.CALENDAR }

    // 키보드가 올라오면 하단 탭을 숨겨 글 쓰는 칸을 넓게
    val imeVisible = WindowInsets.isImeVisible
    Scaffold(
        containerColor = Mono.Bg,
        bottomBar = { if (!imeVisible) BottomBar(tab) { tab = it } },
    ) { pad ->
        Box(Modifier.padding(pad).consumeWindowInsets(pad).fillMaxSize()) {
            when (tab) {
                Tab.CALENDAR -> CalendarScreen(
                    store = store,
                    newEventPending = newEventPending,
                    onNewEventHandled = onNewEventHandled,
                    onOpenSettings = { showSettings = true },
                    onOpenDiary = { day ->
                        diaryDate = day.toEpochDay()
                        tab = Tab.DIARY
                    },
                )
                Tab.DIARY -> DiaryScreen(
                    store = store,
                    date = LocalDate.ofEpochDay(diaryDate),
                    onDateChange = { diaryDate = it.toEpochDay() },
                )
                Tab.HABIT -> com.mononote.app.ui.habit.HabitScreen(store)
                Tab.MONEY -> com.mononote.app.ui.money.MoneyScreen(store)
                Tab.ARCHIVE -> ArchiveScreen(store)
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.background(Mono.Bg).windowInsetsPadding(WindowInsets.navigationBars)) {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
        Row(Modifier.fillMaxWidth().height(72.dp)) {
            listOf(
                Triple(Tab.CALENDAR, MonoIcons.Calendar, "캘린더"),
                Triple(Tab.DIARY, MonoIcons.Diary, "일기"),
                Triple(Tab.HABIT, MonoIcons.Habit, "습관"),
                Triple(Tab.MONEY, MonoIcons.Money, "가계부"),
                Triple(Tab.ARCHIVE, MonoIcons.Archive, "아카이브"),
            ).forEach { (t, icon, label) ->
                val on = t == current
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(role = Role.Tab, onClickLabel = label) { onSelect(t) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    Icon(icon, contentDescription = label, tint = if (on) Mono.Ink else Mono.Muted, modifier = Modifier.size(24.dp))
                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (on) Mono.Ink else Mono.Bg))
                }
            }
        }
    }
}
