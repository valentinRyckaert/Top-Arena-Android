package fr.toupiearena.mobile.ui

import android.content.Context
import fr.toupiearena.mobile.game.DuelMode
import java.util.Locale

/** Schéma de commandes tactiles du combat, au choix dans les paramètres. */
enum class ControlScheme(val label: String, val summary: String) {
    Buttons("Joystick + boutons", "Joystick à gauche, DASH et SPÉCIAL à droite"),
    Tilt("Inclinaison", "Incliner pour piloter, boutons à droite"),
    Gestures("Gestes", "Glisser, toucher, appui long"),
}

/** Réglages de l'application (langue, duel par défaut, commandes, pseudo réseau). */
class Prefs(context: Context) {
    private val store = context.getSharedPreferences("toupie", Context.MODE_PRIVATE)

    var scheme: ControlScheme
        get() = ControlScheme.entries.firstOrNull { it.name == store.getString("scheme", null) } ?: ControlScheme.Buttons
        set(value) = store.edit().putString("scheme", value.name).apply()

    var playerName: String
        get() = store.getString("playerName", null) ?: "Joueur mobile"
        set(value) = store.edit().putString("playerName", value).apply()

    /** Inclinaison nécessaire (en g) pour piloter à fond. */
    var tiltSensitivity: Float
        get() = store.getFloat("tilt", 0.35f)
        set(value) = store.edit().putFloat("tilt", value).apply()

    /** « fr » ou « en » ; par défaut, la langue du système (anglais hors français). */
    var language: String
        get() = store.getString("language", null) ?: if (Locale.getDefault().language == "fr") "fr" else "en"
        set(value) = store.edit().putString("language", value).apply()

    /** Type de duel par défaut des nouvelles parties. */
    var duelMode: DuelMode
        get() = DuelMode.entries.firstOrNull { it.name == store.getString("duelMode", null) } ?: DuelMode.Timing
        set(value) = store.edit().putString("duelMode", value.name).apply()
}
