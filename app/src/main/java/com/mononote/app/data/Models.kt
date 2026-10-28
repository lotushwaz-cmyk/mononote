package com.mononote.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    /** ARGB 색상 (예: 0xFF3F7D5C) */
    val color: Long,
)

@Serializable
enum class Repeat(val label: String) {
    NONE("안 함"),
    DAILY("매일"),
    WEEKLY("매주"),
    MONTHLY("매월"),
    YEARLY("매년"),
    YEARLY_LUNAR("매년(음력)"),
}

@Serializable
data class Event(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    /** 시작 날짜 (LocalDate.toEpochDay) */
    val date: Long,
    val allDay: Boolean = false,
    /** 0시부터의 분 */
    val startMin: Int = 9 * 60,
    val endMin: Int = 10 * 60,
    val repeat: Repeat = Repeat.NONE,
    /** 매주 반복 요일 (1=월 … 7=일) */
    val weekDays: Set<Int> = emptySet(),
    val repeatUntil: Long? = null,
    /** 반복 중 이 날짜들만 삭제됨 */
    val exceptions: Set<Long> = emptySet(),
)

/** 하루의 일기. layers[0] 이 처음 쓴 글, 그 위로 덮어 쓴 글이 차례로 쌓인다. */
@Serializable
data class DiaryEntry(
    val date: Long,
    val layers: List<String> = listOf(""),
)

@Serializable
enum class ArchiveType(val label: String, val imageLabel: String) {
    MOVIE("영화", "포스터"),
    DRAMA("드라마", "포스터"),
    SHOW("예능", "썸네일"),
    MUSIC("음악", "앨범"),
    BOOK("책", "표지"),
}

@Serializable
enum class ArchiveShape(val label: String) {
    POSTER("세로형"),
    SQUARE("정사각형"),
}

/** 감상 기록 카테고리 (영화, 드라마 … 사용자가 추가·수정 가능) */
@Serializable
data class ArchiveCategory(
    val id: String,
    val name: String,
    val shape: ArchiveShape = ArchiveShape.POSTER,
)

@Serializable
data class ArchiveItem(
    val id: String,
    /** ArchiveCategory.id, 빈 문자열이면 미분류 */
    val categoryId: String = "",
    val title: String,
    /** 감상 날짜 (epochDay) */
    val date: Long,
    /** 0~5 */
    val rating: Int = 0,
    /** images 폴더 안 파일 이름 */
    val image: String? = null,
    /** 첫 버전 데이터 호환용 (불러올 때 categoryId 로 옮겨짐) */
    val type: ArchiveType? = null,
)

fun defaultArchiveCategories() = listOf(
    ArchiveCategory("MOVIE", "영화"),
    ArchiveCategory("DRAMA", "드라마"),
    ArchiveCategory("SHOW", "예능"),
    ArchiveCategory("MUSIC", "음악", ArchiveShape.SQUARE),
    ArchiveCategory("BOOK", "책"),
)

/** 예전 형식 데이터를 새 형식으로 정리 */
fun AppData.migrated(): AppData = copy(
    archive = archive.map {
        if (it.categoryId.isEmpty() && it.type != null) it.copy(categoryId = it.type.name, type = null) else it
    },
)

@Serializable
data class Settings(
    val showLunar: Boolean = true,
    /** 홈 화면 위젯 배경 불투명도 0~1 */
    val widgetOpacity: Float = 0.9f,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val categories: List<Category> = defaultCategories(),
    val events: List<Event> = emptyList(),
    val diaries: List<DiaryEntry> = emptyList(),
    val archive: List<ArchiveItem> = emptyList(),
    val archiveCategories: List<ArchiveCategory> = defaultArchiveCategories(),
    val habits: List<Habit> = emptyList(),
    val settings: Settings = Settings(),
)

fun defaultCategories() = listOf(
    Category("c_exercise", "운동", 0xFF3F7D5C),
    Category("c_meet", "약속", 0xFFD9822B),
    Category("c_due", "마감", 0xFF3A6EA5),
)

/** 카테고리 색 팔레트 */
val CategoryPalette: List<Long> = listOf(
    0xFF3F7D5C, 0xFFD9822B, 0xFF3A6EA5, 0xFFC0392B, 0xFF8E5BB5,
    0xFF2E8B8B, 0xFFB5874A, 0xFFD15E8C, 0xFF5E5E5E, 0xFF111111,
)

@Serializable
enum class HabitGoal(val label: String) {
    DAILY("매일"),
    DAYS("요일 선택"),
    TIMES("주 N회"),
}

/** 습관. done 에는 지킨 날짜(epochDay)가 들어간다. */
@Serializable
data class Habit(
    val id: String,
    val name: String,
    val color: Long,
    val goal: HabitGoal = HabitGoal.DAILY,
    /** 요일 선택일 때 (1=월 … 7=일) */
    val weekDays: Set<Int> = (1..7).toSet(),
    /** 주 N회일 때 N */
    val timesPerWeek: Int = 3,
    val done: Set<Long> = emptySet(),
)
