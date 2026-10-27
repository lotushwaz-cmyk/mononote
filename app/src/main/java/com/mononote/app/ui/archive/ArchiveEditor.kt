@file:OptIn(ExperimentalLayoutApi::class)

package com.mononote.app.ui.archive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mononote.app.data.AppStore
import com.mononote.app.data.ArchiveItem
import com.mononote.app.data.ArchiveShape
import com.mononote.app.data.dotted
import com.mononote.app.data.newId
import com.mononote.app.ui.Chip
import com.mononote.app.ui.FieldRow
import com.mononote.app.ui.Mono
import com.mononote.app.ui.OutlineButton
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.StarRow
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.UnderlineField
import com.mononote.app.ui.pickDate
import java.time.LocalDate

@Composable
fun ArchiveEditor(store: AppStore, original: ArchiveItem?, defaultCategoryId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var categoryId by remember { mutableStateOf(original?.categoryId ?: defaultCategoryId) }
    var title by remember { mutableStateOf(original?.title ?: "") }
    var date by remember { mutableLongStateOf(original?.date ?: LocalDate.now().toEpochDay()) }
    var rating by remember { mutableIntStateOf(original?.rating ?: 0) }
    var image by remember { mutableStateOf(original?.image) }
    val addedImages = remember { mutableStateListOf<String>() }
    var askDelete by remember { mutableStateOf(false) }
    var managing by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    val categories = store.data.archiveCategories
    val shape = store.data.archiveCategory(categoryId)?.shape ?: ArchiveShape.POSTER

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            store.saveImage(uri)?.let {
                addedImages.add(it)
                image = it
            }
        }
    }

    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    fun cancel() {
        addedImages.forEach(store::deleteImage)
        onDismiss()
    }

    fun save() {
        val item = ArchiveItem(
            id = original?.id ?: newId(),
            categoryId = if (categories.any { it.id == categoryId }) categoryId else UNCATEGORIZED,
            title = title.trim().ifEmpty { "제목 없음" },
            date = date,
            rating = rating,
            image = image,
        )
        addedImages.filter { it != image }.forEach(store::deleteImage)
        if (original?.image != null && original.image != image) store.deleteImage(original.image)
        store.update { d ->
            d.copy(archive = if (original == null) d.archive + item else d.archive.map { if (it.id == item.id) item else it })
        }
        onDismiss()
    }

    fun delete() {
        addedImages.forEach(store::deleteImage)
        store.deleteImage(original?.image)
        store.update { d -> d.copy(archive = d.archive.filterNot { it.id == original?.id }) }
        onDismiss()
    }

    Dialog(onDismissRequest = ::cancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButtonMono("취소", color = Mono.Sub, onClick = ::cancel)
                    Text(
                        if (original == null) "감상 기록" else "기록 수정",
                        Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    TextButtonMono("저장", bold = true, onClick = ::save)
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SmallLabel("CATEGORY")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            categories.forEach { c -> Chip(c.name, categoryId == c.id) { categoryId = c.id } }
                            Chip("+ 편집", false) { managing = true }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClickLabel = "구글에서 이미지 찾기") { searching = true },
                        ) {
                            Cover(store, image, shape, width = 120.dp)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButtonMono(if (image == null) "이미지 넣기" else "이미지 바꾸기") { searching = true }
                            TextButtonMono("갤러리에서", color = Mono.Sub) { pick() }
                            if (image != null) TextButtonMono("이미지 빼기", color = Mono.Sub) { image = null }
                        }
                    }

                    UnderlineField(title, { title = it }, "제목", fontSize = 20)

                    Column {
                        SmallLabel("DATE")
                        FieldRow("감상 날짜", LocalDate.ofEpochDay(date).dotted()) {
                            pickDate(context, LocalDate.ofEpochDay(date)) { date = it.toEpochDay() }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallLabel("RATING")
                        StarRow(rating, size = 30.dp, gap = 0.dp) { rating = it }
                    }

                    if (original != null) OutlineButton("삭제", color = Mono.Sun) { askDelete = true }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = Mono.Bg,
            title = { Text("이 기록을 삭제할까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("삭제하면 되돌릴 수 없어요.", fontSize = 14.sp, color = Mono.Sub) },
            confirmButton = { TextButtonMono("삭제", color = Mono.Sun, bold = true) { askDelete = false; delete() } },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { askDelete = false } },
        )
    }
    if (managing) ArchiveCategoryManager(store) { managing = false }
    if (searching) {
        val catName = store.data.archiveCategory(categoryId)?.name ?: ""
        val word = when (shape) {
            ArchiveShape.SQUARE -> "앨범 커버"
            ArchiveShape.POSTER -> if (catName == "책") "표지" else "포스터"
        }
        ImageSearchDialog(
            store = store,
            initialQuery = if (title.isBlank()) "" else "${title.trim()} $word",
            onPicked = { name ->
                addedImages.add(name)
                image = name
                searching = false
            },
            onDismiss = { searching = false },
        )
    }
}
