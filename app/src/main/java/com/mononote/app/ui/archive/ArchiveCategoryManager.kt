package com.mononote.app.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mononote.app.data.AppStore
import com.mononote.app.data.ArchiveCategory
import com.mononote.app.data.ArchiveShape
import com.mononote.app.data.newId
import com.mononote.app.ui.Mono
import com.mononote.app.ui.PrimaryButton
import com.mononote.app.ui.TextButtonMono

/** 감상 기록 카테고리 추가·이름 수정·이미지 모양·삭제. 바꾸는 즉시 저장된다. */
@Composable
fun ArchiveCategoryManager(store: AppStore, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf<ArchiveCategory?>(null) }

    fun edit(id: String, f: (ArchiveCategory) -> ArchiveCategory) =
        store.update { d -> d.copy(archiveCategories = d.archiveCategories.map { if (it.id == id) f(it) else it }) }

    fun remove(c: ArchiveCategory) = store.update { d ->
        d.copy(
            archiveCategories = d.archiveCategories.filterNot { it.id == c.id },
            archive = d.archive.map { if (it.categoryId == c.id) it.copy(categoryId = UNCATEGORIZED) else it },
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Mono.Bg) {
            Column(Modifier.padding(20.dp)) {
                Text("감상 카테고리", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "이름을 눌러 고치고, 오른쪽 모양을 눌러 이미지 비율(세로형/정사각형)을 바꿔요",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Mono.Sub,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    store.data.archiveCategories.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                BasicTextField(
                                    value = c.name,
                                    onValueChange = { v -> edit(c.id) { it.copy(name = v.take(12)) } },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 15.sp, color = Mono.Ink),
                                    cursorBrush = SolidColor(Mono.Ink),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                )
                                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
                            }
                            // 이미지 모양 전환
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClickLabel = "이미지 모양 바꾸기, 지금 ${c.shape.label}") {
                                        edit(c.id) {
                                            it.copy(shape = if (it.shape == ArchiveShape.POSTER) ArchiveShape.SQUARE else ArchiveShape.POSTER)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                val (w, h) = if (c.shape == ArchiveShape.SQUARE) 20.dp to 20.dp else 16.dp to 24.dp
                                Box(Modifier.size(w, h).border(1.5.dp, Mono.Ink, RoundedCornerShape(3.dp)))
                            }
                            TextButtonMono("삭제", color = Mono.Sun) {
                                if (store.data.archive.any { it.categoryId == c.id }) confirmDelete = c else remove(c)
                            }
                        }
                    }
                }
                TextButtonMono("+ 카테고리 추가") {
                    store.update { d -> d.copy(archiveCategories = d.archiveCategories + ArchiveCategory(newId(), "새 카테고리")) }
                }
                Spacer(Modifier.size(8.dp))
                PrimaryButton("완료", onClick = onDismiss)
            }
        }
    }

    confirmDelete?.let { c ->
        val count = store.data.archive.count { it.categoryId == c.id }
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            containerColor = Mono.Bg,
            title = { Text("‘${c.name}’ 카테고리를 삭제할까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = { Text("이 카테고리의 기록 ${count}개는 지워지지 않고 ‘미분류’로 옮겨져요.", fontSize = 14.sp, lineHeight = 20.sp, color = Mono.Sub) },
            confirmButton = { TextButtonMono("삭제", color = Mono.Sun, bold = true) { remove(c); confirmDelete = null } },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { confirmDelete = null } },
        )
    }
}
