package com.mononote.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.LocalSize
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mononote.app.MainActivity
import com.mononote.app.MonoApp
import com.mononote.app.R
import com.mononote.app.data.AppData
import com.mononote.app.data.AppStore
import com.mononote.app.data.Holidays
import com.mononote.app.data.category
import com.mononote.app.data.eventsOn
import com.mononote.app.data.koWeekday
import java.time.LocalDate

/** 홈 화면 일정 위젯: 오늘과 다가오는 일정이 있는 날을 날짜별로 보여준다. */
class AgendaWidget : GlanceAppWidget() {

    private data class WidgetEvent(val text: String, val color: Color)
    private data class WidgetDay(val date: LocalDate, val events: List<WidgetEvent>)

    // 위젯 크기에 맞춰 작게(오늘만)/목록 모양을 바꾼다
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = (context.applicationContext as? MonoApp)?.store ?: AppStore(context.applicationContext)
        val days = buildDays(store.data)
        val opacity = store.data.settings.widgetOpacity.coerceIn(0f, 1f)
        provideContent {
            val size = LocalSize.current
            val compact = size.width < 200.dp || size.height < 150.dp
            Content(context, days, opacity, compact)
        }
    }

    private fun timeLabel(min: Int): String {
        val h = min / 60
        val m = min % 60
        val ampm = if (h < 12) "오전" else "오후"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$ampm $h12:%02d".format(m)
    }

    private fun buildDays(data: AppData): List<WidgetDay> {
        val today = LocalDate.now()
        val result = mutableListOf<WidgetDay>()
        for (i in 0 until 45) {
            val d = today.plusDays(i.toLong())
            val evs = data.eventsOn(d).map { ev ->
                val c = data.category(ev.categoryId)?.let { Color(it.color) } ?: Color(0xFFAAAAAA)
                WidgetEvent(if (ev.allDay) ev.title else timeLabel(ev.startMin) + " " + ev.title, c)
            }
            if (i == 0 || evs.isNotEmpty()) result.add(WidgetDay(d, evs))
            if (result.size >= 8) break
        }
        return result
    }

    @Composable
    private fun Content(context: Context, days: List<WidgetDay>, opacity: Float, compact: Boolean) {
        val white = ColorProvider(Color(0xFFF2F2F2))
        val sub = ColorProvider(Color(0xFFA8A8A8))
        val red = ColorProvider(Color(0xFFFF8A80))
        val openApp = actionStartActivity(Intent(context, MainActivity::class.java))
        val newEvent = actionStartActivity(
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_NEW_EVENT)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )

        val bg = ColorProvider(Color(0xFF111111).copy(alpha = opacity))

        if (compact) {
            // 작은 위젯: 오늘 날짜와 오늘 일정만
            val today = days.first()
            val holiday = Holidays.nameOf(today.date)
            Column(
                GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(22.dp)
                    .background(bg)
                    .padding(14.dp)
                    .clickable(openApp),
            ) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            "${today.date.dayOfMonth}",
                            style = TextStyle(color = white, fontSize = 30.sp, fontWeight = FontWeight.Bold),
                        )
                        Text(
                            "${today.date.monthValue}월 " + today.date.koWeekday() + "요일",
                            style = TextStyle(color = if (holiday != null || today.date.dayOfWeek.value == 7) red else sub, fontSize = 11.sp),
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Box(
                        GlanceModifier.size(32.dp).cornerRadius(16.dp)
                            .background(ColorProvider(Color(0xFFF2F2F2))).clickable(newEvent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_plus),
                            contentDescription = "일정 추가",
                            modifier = GlanceModifier.size(16.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF111111))),
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                if (today.events.isEmpty()) {
                    Text(holiday ?: "일정 없음", style = TextStyle(color = if (holiday != null) red else sub, fontSize = 13.sp))
                } else {
                    today.events.take(3).forEach { ev ->
                        Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(GlanceModifier.width(3.dp).height(14.dp).cornerRadius(2.dp).background(ColorProvider(ev.color))) {}
                            Spacer(GlanceModifier.width(7.dp))
                            Text(ev.text, maxLines = 1, style = TextStyle(color = white, fontSize = 12.sp))
                        }
                    }
                    if (today.events.size > 3) {
                        Text("외 ${today.events.size - 3}개", style = TextStyle(color = sub, fontSize = 11.sp))
                    }
                }
            }
            return
        }

        Box(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(bg)
                .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
        ) {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(days, itemId = { it.date.toEpochDay() }) { day ->
                    val first = day.date == days.first().date
                    val holiday = Holidays.nameOf(day.date)
                    Column(GlanceModifier.fillMaxWidth().clickable(openApp)) {
                        if (!first) {
                            Box(GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(Color(0x33FFFFFF)))) {}
                        }
                        Row(
                            GlanceModifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${day.date.monthValue}월 ${day.date.dayOfMonth}일",
                                style = TextStyle(color = white, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                            )
                            Text(
                                " " + day.date.koWeekday() + (holiday?.let { " · $it" } ?: ""),
                                style = TextStyle(
                                    color = if (holiday != null || day.date.dayOfWeek.value == 7) red else sub,
                                    fontSize = 12.sp,
                                ),
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            if (first) {
                                Box(
                                    GlanceModifier
                                        .size(36.dp)
                                        .cornerRadius(18.dp)
                                        .background(ColorProvider(Color(0xFFF2F2F2)))
                                        .clickable(newEvent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Image(
                                        provider = ImageProvider(R.drawable.ic_widget_plus),
                                        contentDescription = "일정 추가",
                                        modifier = GlanceModifier.size(18.dp),
                                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFF111111))),
                                    )
                                }
                            }
                        }
                        if (day.events.isEmpty()) {
                            Text(
                                "일정 없음",
                                modifier = GlanceModifier.padding(bottom = 10.dp),
                                style = TextStyle(color = sub, fontSize = 14.sp),
                            )
                        } else {
                            Column(GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                day.events.forEach { ev ->
                                    Row(
                                        GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            GlanceModifier.width(4.dp).height(16.dp).cornerRadius(2.dp)
                                                .background(ColorProvider(ev.color)),
                                        ) {}
                                        Spacer(GlanceModifier.width(10.dp))
                                        Text(
                                            ev.text,
                                            maxLines = 1,
                                            style = TextStyle(color = white, fontSize = 14.sp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching { AgendaWidget().updateAll(context) }
        }
    }
}

/** 보통 (4x2) */
class AgendaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

/** 작게 (2x2) */
class AgendaWidgetSmallReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}

/** 크게 (4x4) */
class AgendaWidgetLargeReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AgendaWidget()
}
