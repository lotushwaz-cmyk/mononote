package com.mononote.app.ui.money

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.data.AppStore
import com.mononote.app.data.MoneyEntry
import com.mononote.app.data.koWeekday
import com.mononote.app.ui.AddButton
import com.mononote.app.ui.Chip
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.calendar.TodayButton
import com.mononote.app.ui.outlinedBox
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

val IncomeBlue = Color(0xFF2F6FB0)

private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
fun won(n: Long): String = nf.format(kotlin.math.abs(n))

private sealed interface MoneyEdit {
    data object New : MoneyEdit
    data class Existing(val entry: MoneyEntry) : MoneyEdit
}

private val DateW = 62.dp
private val AmountW = 80.dp
private val BalanceW = 72.dp

@Composable
fun MoneyScreen(store: AppStore) {
    val today = LocalDate.now()
    var monthEpoch by rememberSaveable { mutableLongStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 전체, 1 수입, 2 지출
    var editing by remember { mutableStateOf<MoneyEdit?>(null) }

    val month = LocalDate.ofEpochDay(monthEpoch)
    val from = month.toEpochDay()
    val to = month.withDayOfMonth(month.lengthOfMonth()).toEpochDay()
    val entries = store.data.money
        .filter { it.date in from..to }
        .sortedWith(compareBy<MoneyEntry>({ it.date }, { it.createdAt }))
    val income = entries.filter { it.income }.sumOf { it.amount }
    val expense = entries.filter { !it.income }.sumOf { it.amount }

    // 잔액: 이 달 안에서 차례로 더하고 뺀 값
    var running = 0L
    val withBalance = entries.map { e ->
        running += if (e.income) e.amount else -e.amount
        e to running
    }
    val rows = withBalance
        .filter { (e, _) -> filter == 0 || (filter == 1) == e.income }
        .reversed()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 6.dp, top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallLabel("MONEY · ${month.year}")
                        Text("${month.monthValue}월 용돈기입장", fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    }
                    if (month.year != today.year || month.month != today.month) {
                        Box(Modifier.padding(bottom = 6.dp)) { TodayButton { monthEpoch = today.withDayOfMonth(1).toEpochDay() } }
                    }
                    IconBtn(MonoIcons.Back, "지난 달", size = 20.dp) { monthEpoch = month.minusMonths(1).toEpochDay() }
                    IconBtn(MonoIcons.Forward, "다음 달", size = 20.dp) { monthEpoch = month.plusMonths(1).toEpochDay() }
                }
            }
            item {
                Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth().outlinedBox()) {
                    Summary("들어온 돈", "+" + won(income), IncomeBlue, Modifier.weight(1f))
                    Spacer(Modifier.width(1.dp).height(64.dp).background(Mono.Line))
                    Summary("나간 돈", "−" + won(expense), Mono.Sun, Modifier.weight(1f))
                    Spacer(Modifier.width(1.dp).height(64.dp).background(Mono.Line))
                    val left = income - expense
                    Summary("남은 돈", (if (left < 0) "−" else "") + won(left), Mono.Ink, Modifier.weight(1f), bold = true)
                }
            }
            item {
                Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("전체", "수입", "지출").forEachIndexed { i, label -> Chip(label, filter == i) { filter = i } }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 14.dp).fillMaxWidth().outlinedBox()) {
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFFF7F7F7)).padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("날짜", Modifier.width(DateW), fontSize = 11.sp, color = Mono.Sub)
                        Text("내용", Modifier.weight(1f), fontSize = 11.sp, color = Mono.Sub)
                        Text("금액", Modifier.width(AmountW), fontSize = 11.sp, color = Mono.Sub, textAlign = TextAlign.End)
                        Text("잔액", Modifier.width(BalanceW), fontSize = 11.sp, color = Mono.Sub, textAlign = TextAlign.End)
                    }
                    if (rows.isEmpty()) {
                        Text(
                            if (entries.isEmpty()) "이 달 기록이 없어요.\n오른쪽 아래 + 를 눌러 적어 보세요." else "해당하는 기록이 없어요",
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 24.dp),
                            fontSize = 13.sp, lineHeight = 20.sp, color = Mono.Muted,
                        )
                    }
                    rows.forEach { (e, bal) -> EntryRow(e, bal) { editing = MoneyEdit.Existing(e) } }
                }
            }
        }
        AddButton("내역 추가", Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 16.dp)) {
            editing = MoneyEdit.New
        }
    }

    when (val e = editing) {
        null -> Unit
        MoneyEdit.New -> MoneyEditor(
            store, null,
            defaultDate = if (month.year == today.year && month.month == today.month) today else month,
            defaultIncome = filter == 1,
        ) { editing = null }
        is MoneyEdit.Existing -> MoneyEditor(store, e.entry, LocalDate.ofEpochDay(e.entry.date), e.entry.income) { editing = null }
    }
}

@Composable
private fun Summary(label: String, value: String, color: Color, modifier: Modifier, bold: Boolean = false) {
    Column(
        modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, fontSize = 11.sp, color = Mono.Sub)
        Text(value, fontSize = 16.sp, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun EntryRow(e: MoneyEntry, balance: Long, onClick: () -> Unit) {
    val d = LocalDate.ofEpochDay(e.date)
    Column {
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Line))
        Row(
            Modifier.fillMaxWidth().height(46.dp).clickable(onClick = onClick).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val sunday = d.dayOfWeek.value == 7
            Text(
                "%d.%02d (%s)".format(d.monthValue, d.dayOfMonth, d.koWeekday()),
                Modifier.width(DateW), fontSize = 12.sp, color = if (sunday) Mono.Sun else Mono.Sub, maxLines = 1,
            )
            Text(e.title, Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (e.income) "+" else "−") + won(e.amount),
                Modifier.width(AmountW), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = if (e.income) IncomeBlue else Mono.Sun, textAlign = TextAlign.End, maxLines = 1,
            )
            Text(
                (if (balance < 0) "−" else "") + won(balance),
                Modifier.width(BalanceW), fontSize = 12.sp, color = Mono.Muted, textAlign = TextAlign.End, maxLines = 1,
            )
        }
    }
}
