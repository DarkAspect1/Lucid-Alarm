package com.lucid.alarm.ui

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.lucid.alarm.data.prefs.RingingClockFont
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FontManager {

    val FONT_MIME_TYPES = arrayOf(
        "font/*",
        "font/ttf",
        "font/otf",
        "application/x-font-ttf",
        "application/x-font-opentype",
        "application/vnd.android.package-archive",
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
        "*/*"
    )

    const val APP_FONT_BASE = "app_font"
    const val CLOCK_FONT_BASE = "clock_font"
    const val DATE_FONT_BASE = "date_font"
    const val AMPM_FONT_BASE = "ampm_font"
    const val LABEL_FONT_BASE = "alarm_label_font"
    const val SNOOZE_FONT_BASE = "snooze_font"
    const val DISMISS_FONT_BASE = "dismiss_font"

    private const val CUSTOM_FONT_DIR = "custom_fonts"

    private const val EXT_TTF = "ttf"
    private const val EXT_OTF = "otf"

    fun customFontDir(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_FONT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun importFontFromUri(context: Context, uri: Uri, baseName: String): String =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = customFontDir(context)
                dir.listFiles { file -> file.name.startsWith("$baseName.") }?.forEach { it.delete() }
                val stamp = System.currentTimeMillis()
                context.contentResolver.openInputStream(uri)?.use { raw ->
                    val input = if (raw is BufferedInputStream) raw else BufferedInputStream(raw)
                    if (isZipArchive(input)) {
                        extractFirstFontFromArchive(input, dir, baseName, stamp)
                    } else {
                        copyRawFont(input, dir, baseName, stamp)
                    }
                } ?: ""
            }.getOrDefault("")
        }

    private fun copyRawFont(
        input: InputStream,
        dir: File,
        baseName: String,
        stamp: Long
    ): String {
        val destination = File(dir, "$baseName.$stamp.$EXT_TTF")
        destination.outputStream().use { output -> input.copyTo(output) }
        return if (destination.exists() && destination.length() > 0L) destination.absolutePath else ""
    }

    private fun extractFirstFontFromArchive(
        input: InputStream,
        dir: File,
        baseName: String,
        stamp: Long
    ): String {
        val zip = ZipInputStream(input)
        try {
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase(Locale.US)
                if (!entry.isDirectory && (name.endsWith(".$EXT_TTF") || name.endsWith(".$EXT_OTF"))) {
                    val extension = if (name.endsWith(".$EXT_OTF")) EXT_OTF else EXT_TTF
                    val destination = File(dir, "$baseName.$stamp.$extension")
                    destination.outputStream().use { output -> zip.copyTo(output) }
                    return if (destination.exists() && destination.length() > 0L) {
                        destination.absolutePath
                    } else {
                        ""
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        } finally {
            runCatching { zip.close() }
        }
        return ""
    }

    private fun isZipArchive(input: BufferedInputStream): Boolean {
        input.mark(4)
        val header = ByteArray(4)
        val read = runCatching { input.read(header) }.getOrDefault(-1)
        runCatching { input.reset() }
        if (read < 4) return false
        return header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
            (
                (header[2] == 0x03.toByte() && header[3] == 0x04.toByte()) ||
                    (header[2] == 0x05.toByte() && header[3] == 0x06.toByte()) ||
                    (header[2] == 0x07.toByte() && header[3] == 0x08.toByte())
                )
    }

    fun fontFamilyFromPath(fontPath: String?): FontFamily {
        if (fontPath.isNullOrBlank()) return FontFamily.Default
        val file = File(fontPath)
        if (!file.exists() || !file.isFile || file.length() == 0L) return FontFamily.Default
        return runCatching { FontFamily(Font(file)) }.getOrDefault(FontFamily.Default)
    }

    fun hasCustomFont(fontPath: String?): Boolean {
        if (fontPath.isNullOrBlank()) return false
        val file = File(fontPath)
        return file.exists() && file.isFile && file.length() > 0L
    }

    fun typefaceFor(
        font: RingingClockFont,
        customFontPath: String?,
        fontFamily: FontFamily,
        weight: FontWeight
    ): Typeface {
        val style = if (weight.weight >= FontWeight.Bold.weight) {
            Typeface.BOLD
        } else {
            Typeface.NORMAL
        }
        val base = if (font == RingingClockFont.CUSTOM_FILE && hasCustomFont(customFontPath)) {
            runCatching { Typeface.createFromFile(customFontPath) }.getOrNull()
        } else {
            null
        } ?: run {
            when {
                font == RingingClockFont.SERIF || fontFamily == FontFamily.Serif ->
                    Typeface.SERIF
                font == RingingClockFont.CURSIVE || fontFamily == FontFamily.Cursive ->
                    Typeface.create("cursive", style)
                fontFamily == FontFamily.Monospace -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
        }

        return runCatching { Typeface.create(base, style) }.getOrDefault(base)
    }
}

@Composable
fun rememberFontPicker(
    baseName: String,
    onFontImported: (path: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val path = FontManager.importFontFromUri(context, uri, baseName)
                if (path.isNotEmpty()) onFontImported(path)
            }
        }
    }
    return { launcher.launch(FontManager.FONT_MIME_TYPES) }
}
