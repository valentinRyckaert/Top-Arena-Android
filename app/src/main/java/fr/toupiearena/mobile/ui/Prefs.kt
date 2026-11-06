package fr.toupiearena.mobile.ui

import android.content.Context

/** Schéma de commandes tactiles du combat, au choix dans les paramètres. */
enum class ControlScheme(val label: String, val description: String) {
    Buttons("Joystick + boutons",
        "Joystick virtuel à gauche pour piloter, gros boutons DASH et SPÉCIAL à droite."),
    Tilt("Inclinaison",
        "Inclinez le téléphone pour piloter (position de départ = neutre) ; boutons DASH et SPÉCIAL à droite."),
    Gestures("Gestes",
        "Glissez le doigt pour piloter, touchez l'écran pour dasher, appui long (sans bouger) pour le coup spécial."),
}

/** Réglages de l'application (pseudo réseau, commandes). */
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
}
