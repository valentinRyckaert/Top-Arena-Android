package fr.toupiearena.mobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val LATEST_RELEASE = "https://api.github.com/repos/valentinRyckaert/Top-Arena-Android/releases/latest"

/** Release plus récente que la version installée : version (sans « v ») et APK à télécharger. */
data class Release(val version: String, val apk: String)

/** Vrai si [candidate] (« v1.2.0 » ou « 1.2.0 ») est plus récente que [current]. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(v: String) = v.removePrefix("v").split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
    val a = parts(candidate); val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val d = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (d != 0) return d > 0
    }
    return false
}

/** Release GitHub (JSON de l'API) plus récente que [current] et munie d'un APK ; null sinon. */
fun parseLatestRelease(release: JSONObject, current: String): Release? {
    val tag = release.optString("tag_name")
    if (tag.isEmpty() || release.optBoolean("draft") || release.optBoolean("prerelease") || !isNewerVersion(tag, current)) return null
    val assets = release.optJSONArray("assets") ?: return null
    for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        if (asset.optString("name").endsWith(".apk")) return Release(tag.removePrefix("v"), asset.optString("browser_download_url"))
    }
    return null
}

/**
 * Mises à jour depuis les releases GitHub du dépôt mobile : vérification, téléchargement de l'APK
 * puis installateur d'Android, qui remplace l'application (même signature : rien n'est perdu).
 */
class Updater(private val context: Context, private val scope: CoroutineScope) {
    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        data class Available(val release: Release) : State
        data class Downloading(val release: Release, val progress: Float) : State
        data class Failed(val reason: String) : State
    }

    val current: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    var state by mutableStateOf<State>(State.Idle)
        private set
    private fun apk(release: Release) = File(context.cacheDir, "updates/TopArena-${release.version}.apk")

    fun check() {
        if (state is State.Checking || state is State.Downloading) return
        state = State.Checking
        scope.launch {
            state = runCatching {
                val json = withContext(Dispatchers.IO) { JSONObject(get(LATEST_RELEASE).use { it.bufferedReader().readText() }) }
                parseLatestRelease(json, current)?.let { State.Available(it) } ?: State.UpToDate
            }.getOrElse { State.Failed(it.message ?: it.javaClass.simpleName) }
        }
    }

    fun download(release: Release) {
        if (state is State.Downloading) return
        state = State.Downloading(release, 0f)
        scope.launch {
            state = runCatching {
                val apk = apk(release)
                if (!apk.exists()) withContext(Dispatchers.IO) { // Déjà téléchargé : installation directe.
                    apk.parentFile?.listFiles()?.forEach { it.delete() } // Anciens téléchargements.
                    apk.parentFile?.mkdirs()
                    val part = File(apk.path + ".part")
                    val connection = URL(release.apk).openConnection() as HttpURLConnection
                    val total = connection.contentLengthLong
                    connection.inputStream.use { input ->
                        part.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var received = 0L
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                received += n
                                if (total > 0) withContext(Dispatchers.Main) { state = State.Downloading(release, received.toFloat() / total) }
                            }
                        }
                    }
                    check(part.renameTo(apk)) { "renommage impossible" }
                }
                install(apk)
                State.Available(release) // Si l'installation est annulée, on peut relancer.
            }.getOrElse { State.Failed(it.message ?: it.javaClass.simpleName) }
        }
    }

    /** Installateur d'Android ; la première fois, il faut autoriser Top Arena à installer des applications. */
    private fun install(apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun get(url: String) = (URL(url).openConnection() as HttpURLConnection).run {
        setRequestProperty("Accept", "application/vnd.github+json")
        connectTimeout = 10_000; readTimeout = 10_000
        if (responseCode != 200) error("HTTP $responseCode")
        inputStream
    }
}
