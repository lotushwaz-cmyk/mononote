package com.mononote.app.data

import android.content.Context
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPasswordException : Exception("비밀번호가 맞지 않아요")
class InvalidBackupException : Exception("모노 노트 백업 파일이 아니에요")

/**
 * 백업 파일 = data.json + images/ 를 묶은 zip.
 * 비밀번호를 넣으면 AES-256-GCM 으로 암호화하고 앞에 MAGIC 표시를 붙인다.
 */
object Backup {
    private val MAGIC = "MONOBK1".toByteArray()
    private const val ITERATIONS = 120_000

    fun export(context: Context, store: AppStore, uri: Uri, password: String?) {
        val zip = ByteArrayOutputStream().also { bos ->
            ZipOutputStream(bos).use { zos ->
                zos.putNextEntry(ZipEntry("data.json"))
                zos.write(store.encode().toByteArray())
                zos.closeEntry()
                store.imagesDir.listFiles()?.forEach { f ->
                    zos.putNextEntry(ZipEntry("images/" + f.name))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }.toByteArray()
        val out = if (password.isNullOrEmpty()) zip else encrypt(zip, password)
        val stream = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IllegalStateException("파일을 만들 수 없어요")
        stream.use { it.write(out) }
    }

    fun read(context: Context, uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("파일을 열 수 없어요")

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /** 백업 내용을 확인만 하고 돌려준다 (아직 적용 X) */
    fun open(store: AppStore, bytes: ByteArray, password: String?): Pair<AppData, Map<String, ByteArray>> {
        val zip = if (isEncrypted(bytes)) decrypt(bytes, password ?: throw WrongPasswordException()) else bytes
        var data: AppData? = null
        val images = HashMap<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(zip)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "data.json" -> data = store.decode(zis.readBytes().toString(Charsets.UTF_8))
                        name.startsWith("images/") && !entry.isDirectory -> {
                            val file = name.substringAfterLast('/')
                            if (file.isNotBlank() && !file.contains("..")) images[file] = zis.readBytes()
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            throw InvalidBackupException()
        }
        return (data ?: throw InvalidBackupException()) to images
    }

    /** 현재 데이터를 백업 내용으로 바꾼다 */
    fun apply(store: AppStore, data: AppData, images: Map<String, ByteArray>) {
        store.imagesDir.listFiles()?.forEach(File::delete)
        images.forEach { (name, bytes) -> store.imageFile(name).writeBytes(bytes) }
        store.replaceAll(data)
    }

    private fun key(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(raw, "AES")
    }

    private fun encrypt(plain: ByteArray, password: String): ByteArray {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also(rnd::nextBytes)
        val iv = ByteArray(12).also(rnd::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    private fun decrypt(bytes: ByteArray, password: String): ByteArray {
        val p = MAGIC.size
        if (bytes.size < p + 28) throw InvalidBackupException()
        val salt = bytes.copyOfRange(p, p + 16)
        val iv = bytes.copyOfRange(p + 16, p + 28)
        val body = bytes.copyOfRange(p + 28, bytes.size)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
            cipher.doFinal(body)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
    }
}
