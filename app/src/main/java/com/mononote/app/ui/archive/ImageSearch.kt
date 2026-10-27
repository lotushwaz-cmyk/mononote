package com.mononote.app.ui.archive

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mononote.app.data.AppStore
import com.mononote.app.ui.IconBtn
import com.mononote.app.ui.Mono
import com.mononote.app.ui.MonoIcons
import com.mononote.app.ui.TextButtonMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val UA =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

private fun searchUrl(q: String) =
    "https://www.google.com/search?tbm=isch&hl=ko&q=" + URLEncoder.encode(q, "UTF-8")

/** 이미지 주소(또는 data: 주소)를 받아 바이트로 */
private fun download(src: String): ByteArray {
    if (src.startsWith("data:")) {
        val comma = src.indexOf(',')
        return Base64.decode(src.substring(comma + 1), Base64.DEFAULT)
    }
    val conn = URL(src).openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", UA)
    conn.connectTimeout = 15_000
    conn.readTimeout = 20_000
    conn.instanceFollowRedirects = true
    return conn.inputStream.use { it.readBytes() }
}

/**
 * 구글 이미지 검색 화면. 원하는 이미지를 길게 누르면 그 이미지를 저장하고 파일 이름을 돌려준다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImageSearchDialog(store: AppStore, initialQuery: String, onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var candidate by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    fun search() {
        if (query.isNotBlank()) webView?.loadUrl(searchUrl(query.trim()))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = Mono.Bg) {
            BackHandler {
                val wv = webView
                if (wv != null && wv.canGoBack()) wv.goBack() else onDismiss()
            }
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBtn(MonoIcons.Back, "닫기", onClick = onDismiss)
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        if (query.isEmpty()) Text("검색어", color = Mono.Muted, fontSize = 15.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 15.sp, color = Mono.Ink),
                            cursorBrush = SolidColor(Mono.Ink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { search() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconBtn(MonoIcons.Search, "검색") { search() }
                }
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Mono.Ink))
                Text(
                    "마음에 드는 이미지를 꾹 길게 누르면 넣을 수 있어요. 한 번 눌러 크게 본 뒤 길게 누르면 더 선명해요.",
                    Modifier.fillMaxWidth().background(Mono.Soft).padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Mono.Sub,
                )
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Mono.Ink,
                        trackColor = Mono.Line,
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = UA
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val scheme = request.url.scheme
                                        return scheme != "http" && scheme != "https"
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        progress = 5
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        progress = 100
                                    }
                                }
                                setOnLongClickListener {
                                    val hit = hitTestResult
                                    val src = hit.extra
                                    if ((hit.type == WebView.HitTestResult.IMAGE_TYPE ||
                                            hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) && !src.isNullOrBlank()
                                    ) {
                                        candidate = src
                                        true
                                    } else {
                                        false
                                    }
                                }
                                if (initialQuery.isNotBlank()) loadUrl(searchUrl(initialQuery.trim()))
                                webView = this
                            }
                        },
                        onRelease = { it.destroy() },
                    )
                    if (saving) {
                        Box(Modifier.fillMaxSize().background(Mono.Bg.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Mono.Ink)
                        }
                    }
                }
            }
        }
    }

    candidate?.let { src ->
        AlertDialog(
            onDismissRequest = { candidate = null },
            containerColor = Mono.Bg,
            title = { Text("이 이미지를 넣을까요?", fontSize = 17.sp) },
            confirmButton = {
                TextButtonMono("넣기", bold = true) {
                    candidate = null
                    saving = true
                    scope.launch {
                        val name = withContext(Dispatchers.IO) {
                            runCatching { store.saveImageBytes(download(src)) }.getOrNull()
                        }
                        saving = false
                        if (name != null) onPicked(name)
                        else Toast.makeText(context, "이 이미지는 가져올 수 없어요. 다른 이미지를 길게 눌러 보세요", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            dismissButton = { TextButtonMono("취소", color = Mono.Sub) { candidate = null } },
        )
    }
}
