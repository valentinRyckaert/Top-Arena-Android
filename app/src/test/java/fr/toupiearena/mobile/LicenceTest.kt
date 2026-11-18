package fr.toupiearena.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Licences embarquées dans l'APK (Aide > Licences) : présentes et identiques à celles du dépôt. */
class LicenceTest {
    private val assets = File("src/main/assets/licences")

    @Test fun assetCopiesMatchTheRepository() {
        assertEquals(File("../LICENSE").readText(), assets.resolve("LICENSE.txt").readText())
        assertEquals(File("../THIRD-PARTY-NOTICES.md").readText(), assets.resolve("THIRD-PARTY-NOTICES.md").readText())
    }

    @Test fun everyShownLicenceExists() {
        for ((name, expected) in listOf("LICENSE.txt" to "MIT License", "Apache-2.0.txt" to "Apache License",
            "OFL-ChakraPetch.txt" to "SIL Open Font License", "OFL-Rajdhani.txt" to "SIL Open Font License")) {
            assertTrue(name, assets.resolve(name).readText().contains(expected))
        }
    }
}
