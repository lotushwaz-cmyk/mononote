package com.mononote.app.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.data.AppStore
import com.mononote.app.data.DiaryEntry
import com.mononote.app.data.Lunar
import com.mononote.app.data.koFull
import com.mononote.app.data.koWeekday
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.outlinedBox
import com.mononote.app.ui.pickDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs

/**
 * 일기: layers[0] 이 처음 쓴 글(흰 종이), 그 위로 덮어 쓴 글(검정 종이)이 쌓인다.
 * 꾹 눌러 오른쪽으로 밀면 덮이고(새 겹), 왼쪽으로 밀면 아래 글이 다시 보인다. 지워지지 않는다.
 */
@Composable
fun DiaryScreen(store: AppStore, date: LocalDate, onDateChange: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val epoch = date.toEpochDay()
    val entry = store.data.diaries.firstOrNull { it.date == epoch }
    val layers = entry?.layers?.ifEmpty { listOf("") } ?: listOf("")

    var index by remember(epoch) { mutableIntStateOf(layers.lastIndex) }
    val idx = index.coerceIn(0, layers.lastIndex)
    var editing by remember(epoch) { mutableStateOf(false) }
    var dir by remember(epoch) { mutableIntStateOf(0) } // 1: 덮는 중, -1: 걷는 중
    val prog = remember(epoch) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    fun setLayers(newLayers: List<String>) {
        store.update { d ->
            val others = d.diaries.filterNot { it.date == epoch }
            if (newLayers.size == 1 && newLayers[0].isBlank()) d.copy(diaries = others)
            else d.copy(diaries = others + DiaryEntry(epoch, newLayers))
        }
    }

    fun stopEditing() {
        editing = false
        focusManager.clearFocus()
        keyboard?.hide()
    }

    BackHandler(enabled = editing) { stopEditing() }

    LaunchedEffect(editing, idx) {
        if (editing) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(start = 22.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmallLabel("DIARY")
            Spacer(Modifier.weight(1f))
            if (editing) {
                TextButtonMono("완료", bold = true) { stopEditing() }
            } else {
                if (date != LocalDate.now()) TextButtonMono("오늘", color = Mono.Sub) { onDateChange(LocalDate.now()) }
                TextButtonMono("날짜", color = Mono.Sub) { pickDate(context, date, onDateChange) }
                IconBtn(MonoIcons.Back, "이전 날", size = 20.dp) { onDateChange(date.minusDays(1)) }
                IconBtn(MonoIcons.Forward, "다음 날", size = 20.dp) { onDateChange(date.plusDays(1)) }
            }
        }

        BoxWithConstraints(
            Modifier
                .padding(horizontal = 14.dp)
                .weight(1f)
                .fillMaxWidth()
                .outlinedBox(),
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val fullWidth = maxWidth

            val gestures = if (editing) Modifier else Modifier
                .pointerInput(epoch, idx, layers.size) {
                    detectTapGestures(onTap = { editing = true })
                }
                .pointerInput(epoch, idx, layers.size) {
                    var dragX = 0f
                    fun finish(commit: Boolean) {
                        val d = dir
                        scope.launch {
                            prog.animateTo(if (commit) 1f else 0f, tween(260))
                            if (commit && d == 1) {
                                if (idx == layers.lastIndex) setLayers(layers + "")
                                index = idx + 1
                            } else if (commit && d == -1) {
                                if (idx == layers.lastIndex && layers[idx].isBlank()) setLayers(layers.dropLast(1))
                                index = idx - 1
                            }
                            dir = 0
                            prog.snapTo(0f)
                        }
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragX = 0f
                            dir = 0
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragX += amount.x
                            if (dir == 0 && abs(dragX) > 12f) {
                                val canCover = idx < layers.lastIndex || layers[idx].isNotBlank()
                                dir = when {
                                    dragX > 0 && canCover -> 1
                                    dragX < 0 && idx > 0 -> -1
                                    else -> 0
                                }
                            }
                            val p = when (dir) {
                                1 -> dragX / widthPx
                                -1 -> -dragX / widthPx
                                else -> 0f
                            }.coerceIn(0f, 1f)
                            scope.launch { prog.snapTo(p) }
                        },
                        onDragEnd = { finish(prog.value > 0.35f) },
                        onDragCancel = { finish(false) },
                    )
                }

            Box(Modifier.fillMaxSize().then(gestures)) {
                val p = prog.value
                when (dir) {
                    1 -> {
                        DiaryLayer(date, idx, layers[idx], false, {}, focusRequester)
                        Cover(p, fullWidth) {
                            DiaryLayer(date, idx + 1, layers.getOrElse(idx + 1) { "" }, false, {}, focusRequester)
                        }
                    }
                    -1 -> {
                        DiaryLayer(date, idx - 1, layers[idx - 1], false, {}, focusRequester)
                        Cover(1f - p, fullWidth) {
                            DiaryLayer(date, idx, layers[idx], false, {}, focusRequester)
                        }
                    }
                    else -> DiaryLayer(
                        date = date,
                        index = idx,
                        text = layers[idx],
                        editable = editing,
                        onText = { t -> setLayers(layers.toMutableList().also { it[idx] = t }) },
                        focusRequester = focusRequester,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hint = when {
                editing -> "다 쓰면 ‘완료’를 눌러요"
                idx > 0 -> "꾹 누른 채 왼쪽으로 밀면 아래 글이 보여요"
                layers.size > 1 -> "꾹 누른 채 오른쪽으로 밀면 덮어 쓴 글이 보여요"
                layers[0].isBlank() -> "탭해서 오늘을 기록해 보세요"
                else -> "꾹 누른 채 오른쪽으로 밀면 검정으로 덮여요"
            }
            Text(hint, Modifier.weight(1f), fontSize = 12.sp, color = Mono.Sub)
            if (layers.size > 1) Text("${idx + 1} / ${layers.size}", fontSize = 12.sp, color = Mono.Muted)
        }
    }
}

/** 왼쪽부터 fraction 만큼 덮는 검정 종이 */
@Composable
private fun Cover(fraction: Float, fullWidth: Dp, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clipToBounds()) {
        Box(
            Modifier
                .wrapContentWidth(Alignment.Start, unbounded = true)
                .width(fullWidth)
                .fillMaxHeight(),
        ) { content() }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .size(width = 4.dp, height = 44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF5A5A5A)),
        )
    }
}

@Composable
private fun DiaryLayer(
    date: LocalDate,
    index: Int,
    text: String,
    editable: Boolean,
    onText: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val dark = index >= 1
    val bg = if (dark) Mono.Ink else Mono.Bg
    val fg = if (dark) Mono.CoverText else Mono.Ink
    val sub = if (dark) Mono.CoverSub else Mono.Sub
    val placeholder = if (dark) "덮은 자리에 새로 써 보세요" else "오늘은 어떤 하루였나요?"
    val bodyStyle = TextStyle(fontSize = 15.sp, lineHeight = 28.sp, color = fg)

    Column(
        Modifier.fillMaxSize().background(bg).padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(date.koFull(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = fg)
            Text(
                if (dark) "덮어 쓴 글 · ${index}겹" else "${date.koWeekday()}요일 · ${Lunar.of(date).long}",
                fontSize = 12.sp,
                color = sub,
            )
        }
        if (editable) {
            var value by remember(date, index) { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (value.text.isEmpty()) Text(placeholder, style = bodyStyle.copy(color = Mono.Muted))
                BasicTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        onText(it.text)
                    },
                    textStyle = bodyStyle,
                    cursorBrush = SolidColor(fg),
                    modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                )
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (text.isEmpty()) Text(placeholder, style = bodyStyle.copy(color = Mono.Muted))
                else Text(text, style = bodyStyle)
            }
        }
    }
}
