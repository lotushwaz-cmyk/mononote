package com.mononote.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

val BoxShape = RoundedCornerShape(18.dp)

/** 시안의 얇은 외곽선 박스 */
fun Modifier.outlinedBox(): Modifier = this.border(1.dp, Mono.Box, BoxShape).clip(BoxShape)

@Composable
fun OutlinedBox(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.outlinedBox().padding(padding), content = content)
}

@Composable
fun IconBtn(icon: ImageVector, label: String, tint: Color = Mono.Ink, size: Dp = 22.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
fun SmallLabel(text: String, color: Color = Mono.Sub, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, color = color, fontSize = 11.sp, letterSpacing = 1.8.sp)
}

/** 검정 사각 + 버튼 */
@Composable
fun AddButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Mono.Ink)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(MonoIcons.Plus, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, dot: Color? = null, onClick: () -> Unit) {
    Row(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(if (selected) Modifier.background(Mono.Ink) else Modifier.border(1.dp, Mono.Box, RoundedCornerShape(18.dp)))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot != null) Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dot))
        Text(text, fontSize = 13.sp, color = if (selected) Color.White else Mono.Ink)
    }
}

@Composable
fun TextButtonMono(text: String, color: Color = Mono.Ink, bold: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 14.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

/** 큰 검정 버튼 */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Mono.Ink else Mono.Box)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, color: Color = Mono.Ink, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Mono.Box, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 15.sp)
    }
}

/** 밑줄 한 줄 입력칸 */
@Composable
fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
) {
    Column(modifier) {
        Box(Modifier.padding(vertical = 10.dp)) {
            if (value.isEmpty()) Text(placeholder, color = Mono.Muted, fontSize = fontSize.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = fontSize.sp, color = Mono.Ink),
                cursorBrush = SolidColor(Mono.Ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Ink))
    }
}

/** 설정형 한 줄: 왼쪽 이름, 오른쪽 값 */
@Composable
fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = Mono.Sub, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = Mono.Ink)
    }
}

@Composable
fun StarRow(rating: Int, size: Dp = 20.dp, gap: Dp = 3.dp, onRate: ((Int) -> Unit)? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        for (n in 1..5) {
            val m = if (onRate != null) {
                Modifier.size(size + 12.dp).clip(RoundedCornerShape(8.dp))
                    .clickable(onClickLabel = "별점 ${n}점") { onRate(if (rating == n) n - 1 else n) }
            } else Modifier.size(size)
            Box(m, contentAlignment = Alignment.Center) {
                Icon(
                    MonoIcons.Star,
                    contentDescription = if (n == 1 && onRate == null) "별점 5점 만점에 ${rating}점" else null,
                    tint = if (n <= rating) Mono.Ink else Color(0xFFDADADA),
                    modifier = Modifier.size(size),
                )
            }
        }
    }
}

fun pickDate(context: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
}

fun pickTime(context: Context, minutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
}
