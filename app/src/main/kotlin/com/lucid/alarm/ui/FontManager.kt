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

/**
 * Runtime `.ttf` / `.otf` ingestion, including the extraction of fonts bundled inside `.apk` / `.zip`
 * font packages.
 *
 * Compose cannot load a `content://` URI directly into a [FontFamily], so every picked font is
 * first streamed into app internal storage (`filesDir/custom_fonts/`) as a real file, the absolute
 * path is persisted in [com.lucid.alarm.data.prefs.SettingsDataStore], and the `FontFamily` is then
 * built from that file with `Font(File(path))`.
 *
 * When the picked file is actually a zip container (an `.apk` package or a plain `.zip`), the stream
 * is read as a [ZipInputStream] and the first `.ttf` / `.otf` entry found inside — commonly under
 * `assets/fonts/` in an APK — is extracted into the same folder and used directly.
 *
 * Each import is written to a **uniquely named** file (`<base>.<timestamp>.ttf`). This is the key to
 * instant, restart-free updates: the persisted path changes on every import, so `remember(path)`
 * invalidates and Compose re-reads the new font without caching the previous (stale) file.
 *
 * Two independent slots exist so the general app font and the alarm ringing clock font never
 * overwrite each other:
 *  - [APP_FONT_BASE]   -> [com.lucid.alarm.data.prefs.AppFontStyle.CUSTOM_FILE]
 *  - [CLOCK_FONT_BASE] -> [com.lucid.alarm.data.prefs.RingingClockFont.CUSTOM_FILE]
 */
object FontManager {

    /** MIME types accepted by the font picker (ttf, otf, apk/zip packages + octet-stream fallback). */
    val FONT_MIME_TYPES = arrayOf(
        "font/ttf",
        "font/otf",
        "application/x-font-ttf",
        "application/x-font-opentype",
        "application/vnd.android.package-archive",
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream"
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

    /**
     * Imports the font backing [uri] into app internal storage and returns the absolute path of a
     * freshly-versioned file. Any previous version in the same slot is deleted first.
     *
     * If the stream is a zip container (`.apk` / `.zip`), the first `.ttf` / `.otf` entry inside is
     * extracted instead; otherwise the stream is treated as a raw font file. Returns `""` on
     * failure.
     */
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

    /** Copies a non-archive stream verbatim into a versioned `.ttf` slot and returns its path. */
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

    /**
     * Reads [input] as a [ZipInputStream] and extracts the first `.ttf` / `.otf` entry into [dir],
     * preserving the original extension. Returns the extracted path, or `""` when the archive has no
     * usable font.
     */
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

    /**
     * Peeks the first four bytes to decide whether [input] is a zip container. The stream must
     * support mark/reset; [BufferedInputStream] does. Recognises the empty-archive and spanned
     * signatures in addition to the standard local-file header.
     */
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

    /**
     * Builds a [FontFamily] from an on-disk file path. Returns [FontFamily.Default] when the path is
     * blank or the file is missing / unreadable, so a deleted font never crashes the UI.
     */
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

    /**
     * Resolves the *real* platform [Typeface] that backs a ringing-clock font selection, so the
     * vertical clock can measure true glyph ink bounds with an [android.graphics.Paint].
     *
     * Compose's [FontFamily] deliberately hides the underlying [Typeface], and decorative display
     * fonts (e.g. `ROTHEFIGHT`) declare compressed em-box metrics while their outlines extend far
     * beyond them — so we must reach the same `Typeface` the digits render with to measure them
     * accurately.
     *
     * [fontFamily] is used only to disambiguate the built-in system families; the custom file path
     * takes precedence whenever it points at a readable font.
     */
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
        // Apply the requested weight without discarding the resolved family/face.
        return runCatching { Typeface.create(base, style) }.getOrDefault(base)
    }
}

/**
 * Convenience wrapper: launches the [ActivityResultContracts.OpenDocument] font picker described in
 * [FontManager.FONT_MIME_TYPES], version-imports the picked file into the [baseName] slot, and
 * reports the resulting absolute path through [onFontImported].
 */
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
