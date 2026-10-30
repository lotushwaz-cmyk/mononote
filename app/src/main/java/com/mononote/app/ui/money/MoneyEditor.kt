package com.mononote.app.ui.money

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mononote.app.data.AppStore
import com.mononote.app.data.MoneyEntry
import com.mononote.app.data.dotted
import com.mononote.app.data.koWeekday
import com.mononote.app.data.newId
import com.mononote.app.ui.FieldRow
import com.mononote.app.ui.Mono
import com.mononote.app.ui.OutlineButton
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import com.mononote.app.ui.UnderlineField
import com.mononote.app.ui.pickDate
import java.time.LocalDate

@Composable
fun MoneyEditor(store: AppStore, original: MoneyEntry?, defaultDate: LocalDate, defaultIncome: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var income by remember { mutableStateOf(original?.income ?: defaultIncome) }
    var amountText by remember { mutableStateOf(original?.amount?.toString() ?: "") }
    var title by remember { mutableStateOf(original?.title ?: "") }
    var date by remember { mutableLongStateOf(original?.date ?: defaultDate.toEpochDay()) }
    var askDelete by remember { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val amount = amountText.toLongOrNull() ?: 0L
    val tint = if (income) IncomeBlue else Mono.Sun

    LaunchedEffect(Unit) { if (original == null) runCatching { amountFocus.requestFocus() } }

    fun save() {
        if (amount <= 0) return
        val e = MoneyEntry(
            id = original?.id ?: newId(),
            date = date,
            title = title.trim().ifEmpty { if (income) "수입" else "지출" },
            amount = amount,
            income = income,
            createdAt = original?.createdAt ?: System.currentTimeMillis(),
        )
        store.update { d -> d.copy(money = if (original == null) d.money + e else d.money.map { if (it.id == e.id) e else it }) }
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
            Column(Modifier.fillMaxSize().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButtonMono("취소", color = Mono.Sub, onClick = onDismiss)
                    Text(
                        if (original == null) "내역 추가" else "내역 수정",
                        Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    )
                    TextButtonMono("저장", color = if (amount > 0) Mono.Ink else Mono.Muted, bold = true, onClick = ::save)
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // 지출 / 수입
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Mono.Soft).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        listOf(false to "지출", true to "수입").forEach { (v, label) ->
                            val on = income == v
                            Box(
                                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(11.dp))
                                    .background(if (on) Mono.Bg else Color.Transparent)
                                    .clickable(role = Role.Tab) { income = v },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                    color = if (on) (if (v) IncomeBlue else Mono.Sun) else Mono.Sub,
                                )
                            }
                        }
                    }

                    Column {
                        SmallLabel("AMOUNT")
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                            Text(if (income) "+" else "−", fontSize = 30.sp, color = tint, fontWeight = FontWeight.Light)
                            Box(Modifier.weight(1f).padding(start = 6.dp)) {
                                if (amountText.isEmpty()) Text("0", fontSize = 30.sp, color = Mono.Box, fontWeight = FontWeight.Light)
                                BasicTextField(
                                    value = amountText,
                                    onValueChange = { v -> amountText = v.filter(Char::isDigit).take(12).trimStart('0') },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(fontSize = 30.sp, color = tint, fontWeight = FontWeight.Light),
                                    cursorBrush = SolidColor(tint),
                                    modifier = Modifier.fillMaxWidth().focusRequester(amountFocus),
                                )
                            }
                            Text("원", fontSize = 16.sp, color = Mono.Sub, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Ink))
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1000L, 5000L, 10000L, 50000L).forEach { add ->
                                Box(
                                    Modifier.height(32.dp).clip(RoundedCornerShape(16.dp))
                                        .border(1.dp, Mono.Box, RoundedCornerShape(16.dp))
                                        .clickable { amountText = (amount + add).toString() }
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text("+" + if (add >= 10000) "${add / 10000}만" else "${add / 1000}천", fontSize = 12.sp) }
                            }
                        }
                    }

                    Column {
                        SmallLabel("MEMO")
                        UnderlineField(title, { title = it.take(30) }, if (income) "예: 용돈, 아르바이트" else "예: 점심, 카페, 교통비", fontSize = 18)
                    }

                    Column {
                        SmallLabel("DATE")
                        val d = LocalDate.ofEpochDay(date)
                        FieldRow("날짜", "${d.dotted()} (${d.koWeekday()})") {
                            pickDate(context, d) { date = it.toEpochDay() }
                        }
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
            title = { Text("이 내역을 삭제할까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButtonMono("삭제", color = Mono.Sun, bold = true) {
                    askDelete = false
                    store.update { d -> d.copy(money = d.money.filterNot { it.id == original?.id }) }
                    onDismiss()
                }
            },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { askDelete = false } },
        )
    }
}
