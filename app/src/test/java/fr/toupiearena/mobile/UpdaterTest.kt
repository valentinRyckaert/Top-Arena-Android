package fr.toupiearena.mobile

import fr.toupiearena.mobile.ui.Release
import fr.toupiearena.mobile.ui.isNewerVersion
import fr.toupiearena.mobile.ui.parseLatestRelease
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Comparaison de versions et lecture de la release GitHub (mises à jour de l'application). */
class UpdaterTest {
    @Test fun versionOrder() {
        assertTrue(isNewerVersion("v1.0.2", "1.0.1"))
        assertTrue(isNewerVersion("1.10.0", "1.9.9"))
        assertTrue(isNewerVersion("v2.0.0", "1.99.99"))
        assertFalse(isNewerVersion("v1.0.1", "1.0.1"))
        assertFalse(isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(isNewerVersion("1.0.1", "0.0.0")) // Compilation locale sans tag.
    }

    private fun release(tag: String, vararg assets: String) = JSONObject().put("tag_name", tag)
        .put("assets", org.json.JSONArray(assets.map { JSONObject().put("name", it).put("browser_download_url", "https://example/$it") }))

    @Test fun latestReleaseWithApk() {
        assertEquals(Release("1.1.0", "https://example/TopArena-1.1.0.apk"),
            parseLatestRelease(release("v1.1.0", "notes.txt", "TopArena-1.1.0.apk"), "1.0.1"))
        assertNull(parseLatestRelease(release("v1.0.1", "TopArena-1.0.1.apk"), "1.0.1")) // Déjà installée.
        assertNull(parseLatestRelease(release("v1.1.0", "notes.txt"), "1.0.1")) // Pas d'APK.
        assertNull(parseLatestRelease(release("v1.1.0", "TopArena-1.1.0.apk").put("prerelease", true), "1.0.1"))
    }
}
