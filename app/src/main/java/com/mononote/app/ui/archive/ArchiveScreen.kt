package com.mononote.app.ui.archive

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mononote.app.data.AppData
import com.mononote.app.data.AppStore
import com.mononote.app.data.ArchiveCategory
import com.mononote.app.data.ArchiveItem
import com.mononote.app.data.ArchiveShape
import com.mononote.app.data.dotted
import com.mononote.app.ui.AddButton
import com.mononote.app.ui.Chip
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.StarRow
import com.mononote.app.ui.outlinedBox
import java.time.LocalDate

private sealed interface ArchiveEdit {
    data object New : ArchiveEdit
    data class Existing(val item: ArchiveItem) : ArchiveEdit
}

/** 미분류를 뜻하는 필터 값 */
const val UNCATEGORIZED = ""

fun AppData.archiveCategory(id: String): ArchiveCategory? = archiveCategories.firstOrNull { it.id == id }

@Composable
fun ArchiveScreen(store: AppStore) {
    // null = 전체, "" = 미분류, 그 외 = 카테고리 id
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ArchiveEdit?>(null) }
    var managing by remember { mutableStateOf(false) }

    val data = store.data
    val categories = data.archiveCategories
    val all = data.archive.sortedWith(compareByDescending<ArchiveItem> { it.date }.thenBy { it.title })
    val validIds = categories.map { it.id }.toSet()
    fun catOf(item: ArchiveItem) = if (item.categoryId in validIds) item.categoryId else UNCATEGORIZED
    val uncategorized = all.count { catOf(it) == UNCATEGORIZED }
    val activeFilter = filter?.takeIf { it == UNCATEGORIZED && uncategorized > 0 || it in validIds }
    val shown = all
        .filter { activeFilter == null || catOf(it) == activeFilter }
        .filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 18.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallLabel("ARCHIVE")
                        Text("감상 기록", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    }
                    IconBtn(MonoIcons.Search, "검색") {
                        searching = !searching
                        if (!searching) query = ""
                    }
                }
            }
            if (searching) {
                item {
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Box(Modifier.padding(vertical = 10.dp)) {
                            if (query.isEmpty()) Text("제목으로 찾기", color = Mono.Muted, fontSize = 15.sp)
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 15.sp, color = Mono.Ink),
                                cursorBrush = SolidColor(Mono.Ink),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Ink))
                    }
                }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item { Chip("전체 ${all.size}", activeFilter == null) { filter = null } }
                    items(categories, key = { it.id }) { c ->
                        Chip("${c.name} ${all.count { catOf(it) == c.id }}", activeFilter == c.id) { filter = c.id }
                    }
                    if (uncategorized > 0) {
                        item { Chip("미분류 $uncategorized", activeFilter == UNCATEGORIZED) { filter = UNCATEGORIZED } }
                    }
                    item { Chip("+ 편집", false) { managing = true } }
                }
            }
            if (shown.isEmpty()) {
                item {
                    Text(
                        if (all.isEmpty()) "아직 기록이 없어요.\n오른쪽 아래 + 를 눌러 첫 감상을 남겨 보세요." else "해당하는 기록이 없어요",
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 40.dp),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Mono.Muted,
                    )
                }
            }
            items(shown, key = { it.id }) { item ->
                ArchiveCard(store, item, data.archiveCategory(item.categoryId)) { editing = ArchiveEdit.Existing(item) }
            }
        }
        AddButton(
            "기록 추가",
            Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 16.dp),
        ) { editing = ArchiveEdit.New }
    }

    when (val e = editing) {
        null -> Unit
        ArchiveEdit.New -> ArchiveEditor(
            store, null,
            activeFilter?.takeIf { it != UNCATEGORIZED } ?: categories.firstOrNull()?.id ?: UNCATEGORIZED,
        ) { editing = null }
        is ArchiveEdit.Existing -> ArchiveEditor(store, e.item, e.item.categoryId) { editing = null }
    }
    if (managing) ArchiveCategoryManager(store) { managing = false }
}

@Composable
private fun ArchiveCard(store: AppStore, item: ArchiveItem, category: ArchiveCategory?, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 14.dp)
            .fillMaxWidth()
            .outlinedBox()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Cover(store, item.image, category?.shape ?: ArchiveShape.POSTER, width = 88.dp)
        Column(
            Modifier.weight(1f).padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                category?.name ?: "미분류",
                Modifier
                    .border(1.dp, Mono.Box, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                fontSize = 11.sp,
                color = Mono.Sub,
            )
            Text(item.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            Text("${LocalDate.ofEpochDay(item.date).dotted()} 감상", fontSize = 12.sp, color = Mono.Sub)
            Spacer(Modifier.height(4.dp))
            StarRow(item.rating)
        }
    }
}

/** 이미지. 정사각형 카테고리(음악 등)는 정사각, 나머지는 세로 포스터 비율 */
@Composable
fun Cover(store: AppStore, image: String?, shape: ArchiveShape, width: Dp) {
    val height = if (shape == ArchiveShape.SQUARE) width else width * 1.41f
    val clip = RoundedCornerShape(10.dp)
    if (image != null) {
        AsyncImage(
            model = store.imageFile(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width, height).clip(clip).background(Mono.Soft),
        )
    } else {
        Box(
            Modifier.size(width, height).clip(clip).background(Mono.Soft),
            contentAlignment = Alignment.Center,
        ) {
            Text("이미지", fontSize = 11.sp, color = Mono.Muted)
        }
    }
}
