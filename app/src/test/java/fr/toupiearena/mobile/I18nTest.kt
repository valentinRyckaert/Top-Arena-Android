package fr.toupiearena.mobile

import fr.toupiearena.mobile.ui.holo.I18n
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Chaque texte passé à tr("…") dans le code a sa traduction anglaise (comme lupdate sur PC). */
class I18nTest {
    private val english = File("src/main/assets/i18n/en.tsv").useLines { I18n.parse(it) }

    @Test fun everyLiteralHasAnEnglishTranslation() {
        val literal = Regex("""\btr\("((?:[^"\\]|\\.)*)"""")
        val missing = File("src/main/java").walk().filter { it.extension == "kt" }.flatMap { file ->
            literal.findAll(file.readText()).map { it.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\") }
                .filter { it !in english }.map { "${file.name} : $it" }
        }.distinct().toList()
        assertTrue("Textes sans traduction anglaise (à ajouter dans assets/i18n/en.tsv) :\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
