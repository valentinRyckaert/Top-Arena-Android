package fr.toupiearena.mobile.ui.holo

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Traduction façon Qt : le texte français sert de clé, l'anglais vient de assets/i18n/en.tsv
 * (généré depuis le jeu PC, complété pour le mobile). La langue est un état Compose : changer
 * de langue recompose l'interface, sans redémarrer.
 */
object I18n {
    val languages = listOf("fr" to "Français", "en" to "English")
    var language by mutableStateOf("fr")
    private var en: Map<String, String> = emptyMap()

    fun load(context: Context) {
        en = context.assets.open("i18n/en.tsv").bufferedReader().useLines { lines -> parse(lines) }
    }

    fun parse(lines: Sequence<String>): Map<String, String> = lines.filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
        val tab = line.indexOf('\t')
        if (tab < 0) null else unescape(line.substring(0, tab)) to unescape(line.substring(tab + 1))
    }.toMap()

    private fun unescape(s: String) = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { append(when (s[i + 1]) { 'n' -> '\n'; 't' -> '\t'; else -> s[i + 1] }); i += 2 } else { append(c); i++ }
        }
    }

    fun translate(s: String): String = if (language == "en") en[s] ?: s else s
}

/** Texte traduit ; %1, %2… sont remplacés par les arguments (comme sur PC). */
fun tr(s: String, vararg args: Any): String {
    var r = I18n.translate(s)
    args.forEachIndexed { i, a -> r = r.replace("%${i + 1}", a.toString()) }
    return r
}
