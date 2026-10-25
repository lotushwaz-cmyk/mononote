package com.mononote.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mononote.app.widget.AgendaWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.max

/** 앱의 모든 데이터를 휴대폰 안 data.json 과 images 폴더에 저장한다. */
class AppStore(private val context: Context) {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val dataFile = File(context.filesDir, "data.json")
    val imagesDir: File = File(context.filesDir, "images").apply { mkdirs() }
    private val io = Executors.newSingleThreadExecutor()

    var data: AppData by mutableStateOf(load())
        private set

    private fun load(): AppData = try {
        if (dataFile.exists()) json.decodeFromString(AppData.serializer(), dataFile.readText()).migrated() else AppData()
    } catch (e: Exception) {
        AppData()
    }

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun update(transform: (AppData) -> AppData) {
        val before = data
        data = transform(data)
        val snapshot = data
        io.execute { write(snapshot) }
        // 일정·카테고리가 바뀌면 홈 화면 위젯도 새로 그림
        if (before.events !== snapshot.events || before.categories !== snapshot.categories || before.settings != snapshot.settings) refreshWidget()
    }

    fun refreshWidget() {
        widgetScope.launch {
            delay(300)
            AgendaWidget.refresh(context)
        }
    }

    @Synchronized
    private fun write(d: AppData) {
        val tmp = File(context.filesDir, "data.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), d))
        if (!tmp.renameTo(dataFile)) {
            dataFile.writeText(tmp.readText())
            tmp.delete()
        }
    }

    fun encode(d: AppData = data): String = json.encodeToString(AppData.serializer(), d)

    fun decode(text: String): AppData = json.decodeFromString(AppData.serializer(), text).migrated()

    /** 백업 복원: 데이터 전체를 바꾸고 바로 저장 */
    fun replaceAll(newData: AppData) {
        data = newData
        io.submit(Runnable { write(newData) }).get()
        refreshWidget()
    }

    fun imageFile(name: String) = File(imagesDir, name)

    /** 고른 사진을 긴 변 1280px JPEG 로 줄여 앱 안에 복사하고 파일 이름을 돌려준다. */
    fun saveImage(uri: Uri): String? = try {
        val bitmap = decodeScaled(uri, 1280)
        val name = "img_${UUID.randomUUID()}.jpg"
        imageFile(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    } catch (e: Exception) {
        null
    }

    fun deleteImage(name: String?) {
        if (name != null) imageFile(name).delete()
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = maxSide.toFloat() / max(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts)!! }
    }
}

fun newId(): String = UUID.randomUUID().toString()
