package fr.toupiearena.mobile

import fr.toupiearena.mobile.core.PartCatalog
import fr.toupiearena.mobile.core.PartKind
import fr.toupiearena.mobile.core.TopRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Toupies du joueur : enregistrement, relecture, remplacement par nom, fichier abîmé. */
class RepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun savedTopsAreReadBack() {
        val file = folder.root.resolve("data/tops.json")
        val repo = TopRepository(file)
        assertTrue(repo.load()) // Pas encore de fichier : rien à lire, pas d'erreur.
        val preset = PartCatalog.presets.first()
        val otherTip = PartCatalog.parts(PartKind.Tip).first { it.id != preset.tip.id }
        repo.upsert(preset.copy(name = "Éclair"))
        repo.upsert(preset.copy(name = "ÉCLAIR", tip = otherTip)) // Même nom : remplace.
        repo.upsert(preset.copy(name = "Tonnerre"))
        assertTrue(repo.save())
        assertFalse(file.resolveSibling("tops.json.tmp").exists())

        val again = TopRepository(file)
        assertTrue(again.load())
        assertEquals(listOf("ÉCLAIR", "Tonnerre"), again.customTops.map { it.name })
        assertEquals(otherTip.id, again.customTops.first().tip.id)
        assertEquals(PartCatalog.presets.size + 2, again.allTops.size)
        assertTrue(again.remove("tonnerre"))
        assertFalse(again.contains("Tonnerre"))
    }

    @Test fun unreadableFileIsIgnored() {
        val file = folder.newFile("tops.json").apply { writeText("{ pas du json") }
        val repo = TopRepository(file)
        assertFalse(repo.load())
        assertTrue(repo.customTops.isEmpty())
        assertEquals(PartCatalog.presets, repo.allTops)
    }
}
