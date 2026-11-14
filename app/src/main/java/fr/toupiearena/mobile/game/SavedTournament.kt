package fr.toupiearena.mobile.game

import org.json.JSONObject
import java.io.File

/**
 * Tournoi solo sauvegardé : un seul emplacement, à côté des toupies du joueur
 * (« solo-tournament.json » près de « tops.json », comme sur PC). Contenu : Tournament.toJson(), compact.
 */
class SavedTournament(private val file: File) {
    val exists get() = file.exists()

    fun save(tournament: Tournament): Boolean = runCatching {
        file.parentFile?.mkdirs()
        // Écriture dans un fichier temporaire puis renommage : jamais de fichier à moitié écrit.
        val tmp = File(file.path + ".tmp")
        tmp.writeText(tournament.toJson().toString())
        check(tmp.renameTo(file))
    }.isSuccess

    /** Tournoi sauvegardé (null s'il n'y en a pas ou s'il est illisible). */
    fun load(): Tournament? = runCatching { Tournament.fromJson(JSONObject(file.readText())) }.getOrNull()

    fun clear() {
        file.delete()
    }
}
