package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.SpinDirection
import fr.toupiearena.mobile.core.TopType
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.SpecialMove
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.ui.ControlScheme
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

    /** Données du jeu affichées via tr(variable) : pièces, toupies prédéfinies, stadiums, coups, libellés. */
    @Test fun catalogTextsHaveAnEnglishTranslation() {
        val texts = PartCatalog.all.map { it.name } + PartCatalog.presets.map { it.name } +
            Stadium.all.flatMap { listOf(it.name, it.codename) } + SpecialMove.all.map { it.name } +
            TopType.entries.map { it.label } + SpinDirection.entries.map { it.label } + Difficulty.entries.map { it.label } +
            ControlScheme.entries.flatMap { listOf(it.label, it.summary) }
        val missing = texts.filter { it.isNotEmpty() && it !in english }.distinct()
        assertTrue("Données sans traduction anglaise :\n" + missing.joinToString("\n"), missing.isEmpty())
    }
}
