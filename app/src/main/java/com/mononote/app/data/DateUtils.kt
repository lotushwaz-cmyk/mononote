package com.mononote.app.data

import android.icu.util.ChineseCalendar
import android.icu.util.TimeZone
import java.time.LocalDate
import java.time.ZoneId

data class LunarDate(val month: Int, val day: Int, val leap: Boolean) {
    /** 달력 칸용 짧은 표기: 9.15 / 윤4.1 */
    val short: String get() = (if (leap) "윤" else "") + "$month.$day"
    val long: String get() = "음력 " + (if (leap) "윤" else "") + "${month}월 ${day}일"
}

/** 안드로이드 기본 내장 음력(중국력) 계산을 사용한다. 한국 음력과 아주 드물게 하루 차이가 날 수 있다. */
object Lunar {
    private val cache = HashMap<Long, LunarDate>()
    private val zone = ZoneId.of("Asia/Seoul")

    @Synchronized
    fun of(date: LocalDate): LunarDate = cache.getOrPut(date.toEpochDay()) {
        val cal = ChineseCalendar()
        cal.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        cal.timeInMillis = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        LunarDate(
            month = cal.get(android.icu.util.Calendar.MONTH) + 1,
            day = cal.get(android.icu.util.Calendar.DAY_OF_MONTH),
            leap = cal.get(android.icu.util.Calendar.IS_LEAP_MONTH) == 1,
        )
    }
}

val KoreanWeekdays = listOf("월", "화", "수", "목", "금", "토", "일") // DayOfWeek.value - 1
val EnglishWeekdays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
val EnglishMonths = listOf(
    "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
    "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER",
)

fun LocalDate.koWeekday(): String = KoreanWeekdays[dayOfWeek.value - 1]
fun LocalDate.dotted(): String = "%d.%02d.%02d".format(year, monthValue, dayOfMonth)
fun LocalDate.koFull(): String = "${year}년 ${monthValue}월 ${dayOfMonth}일"
fun Int.hhmm(): String = "%02d:%02d".format(this / 60, this % 60)

/** 이 일정이 해당 날짜에 있는지 (반복 포함) */
fun Event.occursOn(day: LocalDate): Boolean {
    val start = LocalDate.ofEpochDay(date)
    val epoch = day.toEpochDay()
    if (epoch < date) return false
    if (repeatUntil != null && epoch > repeatUntil) return false
    if (epoch in exceptions) return false
    return when (repeat) {
        Repeat.NONE -> epoch == date
        Repeat.DAILY -> true
        Repeat.WEEKLY -> (weekDays.ifEmpty { setOf(start.dayOfWeek.value) }).contains(day.dayOfWeek.value)
        Repeat.MONTHLY -> day.dayOfMonth == start.dayOfMonth
        Repeat.YEARLY -> day.monthValue == start.monthValue && day.dayOfMonth == start.dayOfMonth
        Repeat.YEARLY_LUNAR -> {
            val a = Lunar.of(start)
            val b = Lunar.of(day)
            !b.leap && a.month == b.month && a.day == b.day
        }
    }
}

fun AppData.eventsOn(day: LocalDate): List<Event> =
    events.filter { it.occursOn(day) }
        .sortedWith(compareBy<Event>({ !it.allDay }, { it.startMin }, { it.title }))

fun AppData.category(id: String?): Category? = categories.firstOrNull { it.id == id }
