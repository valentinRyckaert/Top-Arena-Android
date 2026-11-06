package fr.toupiearena.mobile

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import fr.toupiearena.mobile.ui.AppController
import fr.toupiearena.mobile.ui.ToupieApp

class MainActivity : ComponentActivity() {
    private lateinit var app: AppController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Plein écran immersif (paysage) et écran toujours allumé pendant le jeu.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        app = AppController(applicationContext, lifecycleScope)
        setContent { ToupieApp(app) }
    }

    override fun onDestroy() {
        if (isFinishing) app.leaveLobby(goToMenu = false)
        super.onDestroy()
    }
}
