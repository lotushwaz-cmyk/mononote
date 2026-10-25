package com.mononote.app.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mononote.app.data.AppData
import com.mononote.app.data.AppStore
import com.mononote.app.data.Backup
import com.mononote.app.data.InvalidBackupException
import com.mononote.app.data.WrongPasswordException
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.OutlineButton
import com.mononote.app.ui.OutlinedBox
import com.mononote.app.ui.PrimaryButton
import com.mononote.app.ui.SmallLabel
import com.mononote.app.ui.TextButtonMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private class PendingRestore(val data: AppData, val images: Map<String, ByteArray>)

@Composable
fun SettingsScreen(store: AppStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    // 내보내기
    var askExportPassword by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var exportPassword2 by remember { mutableStateOf("") }

    // 불러오기
    var importBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importPassword by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingRestore?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pw = exportPassword.takeIf { it.isNotEmpty() }
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { Backup.export(context, store, uri, pw) } }
            busy = false
            exportPassword = ""
            exportPassword2 = ""
            toast(if (result.isSuccess) "백업 파일을 만들었어요" else "백업에 실패했어요: ${result.exceptionOrNull()?.message}")
        }
    }

    fun tryOpen(bytes: ByteArray, password: String?) {
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { Backup.open(store, bytes, password) } }
            busy = false
            result.onSuccess { (data, images) ->
                importBytes = null
                importPassword = ""
                importError = null
                pending = PendingRestore(data, images)
            }.onFailure { e ->
                when (e) {
                    is WrongPasswordException -> importError = "비밀번호가 맞지 않아요"
                    is InvalidBackupException -> { importBytes = null; toast("겹 백업 파일이 아니에요") }
                    else -> { importBytes = null; toast("파일을 읽지 못했어요") }
                }
            }
        }
    }

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { runCatching { Backup.read(context, uri) } }.getOrNull()
            busy = false
            when {
                bytes == null -> toast("파일을 열 수 없어요")
                Backup.isEncrypted(bytes) -> { importBytes = bytes; importError = null }
                else -> tryOpen(bytes, null)
            }
        }
    }

    val d = store.data
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBtn(MonoIcons.Back, "뒤로", onClick = onBack)
                Text("설정", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedBox {
                    SmallLabel("AUTO BACKUP")
                    Text("구글 계정 자동 백업", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        "휴대폰의 구글 백업이 켜져 있으면, 하루에 한 번쯤 충전 중·와이파이 연결 상태일 때 자동으로 백업돼요. " +
                            "새 휴대폰에 같은 구글 계정으로 이 앱을 설치하면 자동으로 복원돼요.\n\n" +
                            "확인: 휴대폰 설정 › Google › 백업 (기종마다 위치가 조금 달라요)\n" +
                            "자동 백업은 25MB까지라서, 사진이 많아지면 아래 백업 파일도 함께 만들어 두세요.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Mono.Sub,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                }

                OutlinedBox {
                    SmallLabel("BACKUP FILE")
                    Text("백업 파일", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        "일정·일기(덮어 쓴 글 포함)·아카이브·사진을 파일 하나로 저장해요. " +
                            "구글 드라이브, 내 파일, 카톡 나에게 보내기 등 원하는 곳에 보관하세요.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Mono.Sub,
                        modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                    )
                    PrimaryButton("백업 파일 만들기") { askExportPassword = true }
                    Spacer(Modifier.height(8.dp))
                    OutlineButton("백업 파일 불러오기") { openDoc.launch(arrayOf("*/*")) }
                }

                LockSection()
                WidgetSection(store)

                OutlinedBox {
                    SmallLabel("MY DATA")
                    Text(
                        "일정 ${d.events.size}개 · 일기 ${d.diaries.size}일 · 아카이브 ${d.archive.size}개 · 카테고리 ${d.categories.size}개",
                        fontSize = 13.sp,
                        color = Mono.Sub,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        if (busy) {
            Box(Modifier.fillMaxSize().background(Mono.Bg.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mono.Ink)
            }
        }
    }

    if (askExportPassword) {
        val mismatch = exportPassword.isNotEmpty() && exportPassword != exportPassword2
        AlertDialog(
            onDismissRequest = { askExportPassword = false },
            containerColor = Mono.Bg,
            title = { Text("백업 비밀번호", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "비밀번호를 넣으면 파일이 암호화돼서 비밀번호 없이는 열 수 없어요. 잊어버리면 복원할 수 없으니 주의하세요. 비워두면 암호 없이 저장해요.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Mono.Sub,
                    )
                    PasswordField(exportPassword, { exportPassword = it }, "비밀번호 (선택)")
                    if (exportPassword.isNotEmpty()) PasswordField(exportPassword2, { exportPassword2 = it }, "비밀번호 확인")
                    if (mismatch && exportPassword2.isNotEmpty()) Text("비밀번호가 서로 달라요", fontSize = 12.sp, color = Mono.Sun)
                }
            },
            confirmButton = {
                TextButtonMono("저장 위치 고르기", bold = true) {
                    if (mismatch) return@TextButtonMono
                    askExportPassword = false
                    val name = "gyeop-backup-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".mnbak"
                    createDoc.launch(name)
                }
            },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { askExportPassword = false } },
        )
    }

    importBytes?.let { bytes ->
        AlertDialog(
            onDismissRequest = { importBytes = null },
            containerColor = Mono.Bg,
            title = { Text("비밀번호 입력", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("암호화된 백업 파일이에요.", fontSize = 13.sp, color = Mono.Sub)
                    PasswordField(importPassword, { importPassword = it }, "비밀번호")
                    importError?.let { Text(it, fontSize = 12.sp, color = Mono.Sun) }
                }
            },
            confirmButton = { TextButtonMono("열기", bold = true) { tryOpen(bytes, importPassword) } },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { importBytes = null; importPassword = "" } },
        )
    }

    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = Mono.Bg,
            title = { Text("백업으로 바꿀까요?", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "백업 파일: 일정 ${p.data.events.size}개 · 일기 ${p.data.diaries.size}일 · 아카이브 ${p.data.archive.size}개\n\n" +
                        "지금 휴대폰에 있는 내용은 모두 백업 파일 내용으로 바뀌어요.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Mono.Sub,
                )
            },
            confirmButton = {
                TextButtonMono("바꾸기", color = Mono.Sun, bold = true) {
                    pending = null
                    busy = true
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { runCatching { Backup.apply(store, p.data, p.images) } }
                        busy = false
                        toast(if (r.isSuccess) "복원했어요" else "복원에 실패했어요")
                    }
                }
            },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { pending = null } },
        )
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Column {
        Box(Modifier.padding(vertical = 8.dp)) {
            if (value.isEmpty()) Text(placeholder, color = Mono.Muted, fontSize = 15.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(fontSize = 15.sp, color = Mono.Ink),
                cursorBrush = SolidColor(Mono.Ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Ink))
    }
}
