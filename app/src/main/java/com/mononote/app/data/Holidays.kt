package com.mononote.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 한국 공휴일 (관공서의 공휴일에 관한 규정 기준, 2026년 개정 반영).
 * 음력 공휴일(설·부처님오신날·추석)과 대체공휴일은 해마다 계산한다.
 * 선거일·임시공휴일은 미리 알 수 없어 [extra] 에 직접 적는다.
 */
object Holidays {
    private enum class Sub { NONE, SUNDAY, WEEKEND }
    private data class Group(val name: String, val days: List<LocalDate>, val sub: Sub)

    /** 선거일 등 정해진 날만 쉬는 공휴일 */
    private val extra = mapOf(
        LocalDate.of(2026, 6, 3) to "지방선거",
        LocalDate.of(2028, 4, 12) to "총선",
    )

    private val cache = HashMap<Int, Map<LocalDate, String>>()

    fun nameOf(date: LocalDate): String? = forYear(date.year)[date]

    fun isHoliday(date: LocalDate): Boolean = nameOf(date) != null

    @Synchronized
    fun forYear(year: Int): Map<LocalDate, String> = cache.getOrPut(year) { compute(year) }

    private fun lunarToSolar(year: Int, month: Int, day: Int): LocalDate? {
        var d = LocalDate.of(year, 1, 1)
        while (d.year == year) {
            val l = Lunar.of(d)
            if (!l.leap && l.month == month && l.day == day) return d
            d = d.plusDays(1)
        }
        return null
    }

    private fun compute(y: Int): Map<LocalDate, String> {
        val groups = mutableListOf<Group>()
        fun fixed(m: Int, d: Int, name: String, sub: Sub) = groups.add(Group(name, listOf(LocalDate.of(y, m, d)), sub))

        fixed(1, 1, "신정", Sub.NONE)
        fixed(3, 1, "삼일절", Sub.WEEKEND)
        if (y >= 2026) fixed(5, 1, "노동절", Sub.WEEKEND)
        fixed(5, 5, "어린이날", Sub.WEEKEND)
        fixed(6, 6, "현충일", Sub.NONE)
        if (y >= 2026) fixed(7, 17, "제헌절", Sub.WEEKEND)
        fixed(8, 15, "광복절", Sub.WEEKEND)
        fixed(10, 3, "개천절", Sub.WEEKEND)
        fixed(10, 9, "한글날", Sub.WEEKEND)
        fixed(12, 25, "성탄절", Sub.WEEKEND)

        lunarToSolar(y, 1, 1)?.let { groups.add(Group("설날", listOf(it.minusDays(1), it, it.plusDays(1)), Sub.SUNDAY)) }
        lunarToSolar(y, 4, 8)?.let { groups.add(Group("부처님오신날", listOf(it), Sub.WEEKEND)) }
        lunarToSolar(y, 8, 15)?.let { groups.add(Group("추석", listOf(it.minusDays(1), it, it.plusDays(1)), Sub.SUNDAY)) }
        extra.filterKeys { it.year == y }.forEach { (d, n) -> groups.add(Group(n, listOf(d), Sub.NONE)) }

        val result = HashMap<LocalDate, String>()
        val count = HashMap<LocalDate, Int>()
        groups.forEach { g ->
            g.days.forEach { d ->
                result[d] = result[d]?.let { "$it·${g.name}" } ?: g.name
                count[d] = (count[d] ?: 0) + 1
            }
        }

        // 대체공휴일: 주말(또는 설·추석은 일요일)이나 다른 공휴일과 겹치면 그 뒤 첫 평일
        val compensated = HashSet<LocalDate>()
        groups.sortedBy { it.days.first() }.forEach { g ->
            if (g.sub == Sub.NONE) return@forEach
            val weekendHit = g.days.any { d ->
                when (g.sub) {
                    Sub.SUNDAY -> d.dayOfWeek == DayOfWeek.SUNDAY
                    Sub.WEEKEND -> d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
                    Sub.NONE -> false
                }
            }
            val overlapHit = g.days.any { (count[it] ?: 0) > 1 && it !in compensated }
            if (weekendHit || overlapHit) {
                var n = g.days.last().plusDays(1)
                while (n.dayOfWeek == DayOfWeek.SATURDAY || n.dayOfWeek == DayOfWeek.SUNDAY || result.containsKey(n)) {
                    n = n.plusDays(1)
                }
                result[n] = "대체휴일"
            }
            compensated.addAll(g.days)
        }
        return result
    }
}
