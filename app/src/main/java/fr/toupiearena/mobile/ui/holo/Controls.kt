package fr.toupiearena.mobile.ui.holo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.game.Vec2

/**
 * Gros bouton rond du combat (DASH, SPÉCIAL, APPUI), déclenché dès l'appui pour la réactivité.
 * [cooldown] (0..1) remplit le contour pendant la recharge ; [art] dessine l'emblème au centre.
 */
@Composable
fun RoundAction(
    label: String, size: Dp, enabled: Boolean, onPress: () -> Unit, modifier: Modifier = Modifier, color: Color = Holo.green,
    icon: HoloIcon? = null, cooldown: Float? = null, art: (DrawScope.(center: Offset, radius: Float, color: Color) -> Unit)? = null,
) {
    Box(modifier.size(size).pointerInput(enabled) { detectTapGestures(onPress = { if (enabled) onPress() }) }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2 - 4.dp.toPx(); val c = center
            if (enabled) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), c + Offset(0f, 3.dp.toPx()), r * 1.25f), r * 1.25f, c + Offset(0f, 3.dp.toPx()))
            drawCircle(if (enabled) Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.18f)), c, r) else Brush.linearGradient(listOf(Color(0xFF0D0B2A), Color(0xFF0D0B2A))), r, c)
            drawCircle(if (enabled) color else Holo.line, r, c, style = Stroke(2.dp.toPx()))
            drawCircle(if (enabled) color else Holo.line, r - 7.dp.toPx(), c, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 12f))), alpha = 0.8f)
            if (cooldown != null && cooldown < 1f) drawArc(Holo.text, -90f, 360f * cooldown, false, Offset(c.x - r - 4.dp.toPx(), c.y - r - 4.dp.toPx()),
                Size((r + 4.dp.toPx()) * 2, (r + 4.dp.toPx()) * 2), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            art?.invoke(this, Offset(c.x, c.y - r * 0.18f), r * 0.28f, if (enabled) Color.White else Holo.dim)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null && art == null) Icon(icon, if (enabled) Color.White else Holo.dim, size * 0.26f)
            Text(label.uppercase(), style = displayStyle((size.value / 8.5f).coerceIn(9f, 13f).sp, if (enabled) Color.White else Holo.dim, 0.1f),
                modifier = if (art != null) Modifier.padding(top = size * 0.28f) else Modifier)
        }
    }
}

/** Joystick virtuel : vecteur de pilotage (longueur ≤ 1) tant que le doigt est posé. */
@Composable
fun Joystick(onSteer: (Vec2) -> Unit, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    Canvas(modifier.size(size).pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown()
            val radius = this.size.width / 2f
            val center = Offset(radius, radius)
            var position = down.position
            while (true) {
                val v = (position - center) / radius
                val len = v.getDistance()
                knob = if (len > 1f) v / len else v
                onSteer(Vec2(knob.x.toDouble(), knob.y.toDouble()))
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                change.consume()
                position = change.position
                if (!change.pressed) break
            }
            knob = Offset.Zero
            onSteer(Vec2.ZERO)
        }
    }) {
        val r = this.size.minDimension / 2 - 2.dp.toPx(); val c = center
        drawCircle(Holo.field.copy(alpha = 0.75f), r, c); drawCircle(Holo.lineHi, r, c, style = Stroke(1.5.dp.toPx()))
        drawCircle(Holo.line, r * 0.62f, c, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 14f))))
        val k = c + knob * (r * 0.62f)
        drawCircle(Holo.green.copy(alpha = 0.3f), r * 0.5f, k + Offset(0f, 3.dp.toPx()))
        drawCircle(Brush.radialGradient(listOf(Holo.lime, Holo.green), k, r * 0.42f), r * 0.42f, k)
        drawCircle(Color(0xFFB9FFD6), r * 0.42f, k, style = Stroke(1.5.dp.toPx()))
    }
}

/** Jauge d'endurance inclinée (repères tous les 10 %, repère rouge à 25 %). */
fun DrawScope.enduranceBar(x: Float, y: Float, w: Float, h: Float, ratio: Float) {
    val s = h * 0.6f; val fw = w * ratio.coerceIn(0f, 1f)
    val col = if (ratio < 0.3f) Holo.danger else Holo.green; val col2 = if (ratio < 0.3f) Holo.warn else Holo.lime
    drawPath(skewPath(x, y, w, h, s), Color(0xFF07061A)); drawPath(skewPath(x, y, w, h, s), Holo.line, style = Stroke(1f))
    if (fw > 0f) {
        drawPath(skewPath(x, y, fw, h, s), Brush.horizontalGradient(listOf(col, col2), x, x + w))
        drawPath(skewPath(x + 2f, y + 2f, (fw - 4f).coerceAtLeast(0f), h * 0.35f, h * 0.2f), Color.White.copy(alpha = 0.28f))
    }
    for (i in 1 until 10) { val xx = x + w * i / 10; drawLine(Color(0xCC07061A), Offset(xx, y + h - 4f), Offset(xx + 1f, y + h), 1.5f) }
    drawLine(Holo.danger, Offset(x + w * 0.25f + h * 0.3f, y - 3f), Offset(x + w * 0.25f - h * 0.3f, y + h + 3f), 2f)
}

/** Pilotage par inclinaison : la position au début sert de neutre. */
@Composable
fun TiltSteering(sensitivity: Float, onSteer: (Vec2) -> Unit) {
    val context = LocalContext.current
    DisposableEffect(sensitivity) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        @Suppress("DEPRECATION")
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        var neutral: Vec2? = null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Repère écran du téléphone tenu en portrait, puis rotation de l'affichage.
                val d = Vec2(-event.values[0].toDouble(), event.values[1].toDouble())
                val screen = when (rotation) {
                    Surface.ROTATION_90 -> Vec2(d.y, -d.x)
                    Surface.ROTATION_270 -> Vec2(-d.y, d.x)
                    Surface.ROTATION_180 -> -d
                    else -> d
                }
                val zero = neutral ?: screen.also { neutral = it }
                var v = (screen - zero) / (SensorManager.GRAVITY_EARTH * sensitivity.toDouble())
                if (v.length() < 0.12) v = Vec2.ZERO
                onSteer(if (v.length() > 1.0) v.normalized() else v)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensor?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose {
            manager.unregisterListener(listener)
            onSteer(Vec2.ZERO)
        }
    }
}
