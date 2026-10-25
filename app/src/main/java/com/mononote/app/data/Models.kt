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
data class ArchiveItem(
    val id: String,
    val type: ArchiveType,
    val title: String,
    /** 감상 날짜 (epochDay) */
    val date: Long,
    /** 0~5 */
    val rating: Int = 0,
    /** images 폴더 안 파일 이름 */
    val image: String? = null,
)

@Serializable
data class Settings(
    val showLunar: Boolean = true,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val categories: List<Category> = defaultCategories(),
    val events: List<Event> = emptyList(),
    val diaries: List<DiaryEntry> = emptyList(),
    val archive: List<ArchiveItem> = emptyList(),
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
