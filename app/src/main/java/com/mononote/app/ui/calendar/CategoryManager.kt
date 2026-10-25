@file:OptIn(ExperimentalLayoutApi::class)

package com.mononote.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mononote.app.data.AppStore
import com.mononote.app.data.Category
import com.mononote.app.data.CategoryPalette
import com.mononote.app.data.newId
import com.mononote.app.ui.Mono
import com.mononote.app.ui.PrimaryButton
import com.mononote.app.ui.TextButtonMono

/** 카테고리 이름·색 편집. 바꾸는 즉시 저장된다. */
@Composable
fun CategoryManager(store: AppStore, onDismiss: () -> Unit) {
    var picking by remember { mutableStateOf<Category?>(null) }

    fun edit(id: String, f: (Category) -> Category) =
        store.update { d -> d.copy(categories = d.categories.map { if (it.id == id) f(it) else it }) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = Mono.Bg) {
            Column(Modifier.padding(20.dp)) {
                Text("카테고리", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("색 동그라미를 누르면 색상표에서 원하는 색을 고를 수 있어요", fontSize = 12.sp, color = Mono.Sub, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    store.data.categories.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.color))
                                    .clickable(onClickLabel = "색 바꾸기") { picking = c },
                            )
                            Column(Modifier.weight(1f)) {
                                BasicTextField(
                                    value = c.name,
                                    onValueChange = { v -> edit(c.id) { it.copy(name = v.take(12)) } },
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 15.sp, color = Mono.Ink),
                                    cursorBrush = SolidColor(Mono.Ink),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                )
                                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
                            }
                            TextButtonMono("삭제", color = Mono.Sun) {
                                store.update { d ->
                                    d.copy(
                                        categories = d.categories.filterNot { it.id == c.id },
                                        events = d.events.map { if (it.categoryId == c.id) it.copy(categoryId = null) else it },
                                    )
                                }
                            }
                        }
                    }
                }
                TextButtonMono("+ 카테고리 추가") {
                    val used = store.data.categories.map { it.color }.toSet()
                    val color = CategoryPalette.firstOrNull { it !in used } ?: CategoryPalette.first()
                    store.update { d -> d.copy(categories = d.categories + Category(newId(), "새 카테고리", color)) }
                }
                Spacer(Modifier.size(8.dp))
                PrimaryButton("완료", onClick = onDismiss)
            }
        }
    }

    picking?.let { c ->
        ColorPickerDialog(
            initial = c.color,
            onDismiss = { picking = null },
            onPick = { col ->
                edit(c.id) { it.copy(color = col) }
                picking = null
            },
        )
    }
}
