package fr.toupiearena.mobile.core

import org.json.JSONArray
import java.io.File

/** Toupies assemblées par le joueur, enregistrées en JSON (même format que sur PC). */
class TopRepository(private val file: File) {
    private val tops = mutableListOf<Top>()
    val customTops: List<Top> get() = tops
    val allTops: List<Top> get() = PartCatalog.presets + tops

    /** False si le fichier existe mais est illisible (il est alors ignoré). */
    fun load(): Boolean {
        tops.clear()
        if (!file.exists()) return true
        return runCatching {
            val array = JSONArray(file.readText())
            for (i in 0 until array.length()) Top.fromJson(array.optJSONObject(i))?.let(::upsert)
        }.isSuccess
    }

    fun save(): Boolean = runCatching {
        file.parentFile?.mkdirs()
        val array = JSONArray()
        tops.forEach { array.put(it.toJson()) }
        // Écriture dans un fichier temporaire puis renommage : jamais de fichier à moitié écrit.
        val tmp = File(file.path + ".tmp")
        tmp.writeText(array.toString(2))
        check(tmp.renameTo(file))
    }.isSuccess

    fun upsert(top: Top) {
        val i = indexOf(top.name)
        if (i >= 0) tops[i] = top else tops += top
    }

    fun remove(name: String) = indexOf(name).takeIf { it >= 0 }?.let { tops.removeAt(it); true } ?: false
    fun contains(name: String) = indexOf(name) >= 0
    private fun indexOf(name: String) = tops.indexOfFirst { it.name.equals(name, ignoreCase = true) }
}
