package fr.toupiearena.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.toupiearena.mobile.core.Top
import fr.toupiearena.mobile.game.Difficulty
import fr.toupiearena.mobile.game.MatchRules
import fr.toupiearena.mobile.game.Stadium
import fr.toupiearena.mobile.session.LocalSession
import fr.toupiearena.mobile.ui.holo.BtnVariant
import fr.toupiearena.mobile.ui.holo.EdgeMargin
import fr.toupiearena.mobile.ui.holo.Holo
import fr.toupiearena.mobile.ui.holo.HoloButton
import fr.toupiearena.mobile.ui.holo.HoloIcon
import fr.toupiearena.mobile.ui.holo.HoloScreen
import fr.toupiearena.mobile.ui.holo.IconBtn
import fr.toupiearena.mobile.ui.holo.Panel
import fr.toupiearena.mobile.ui.holo.Segmented
import fr.toupiearena.mobile.ui.holo.Stats3
import fr.toupiearena.mobile.ui.holo.TopBar
import fr.toupiearena.mobile.ui.holo.TypeBadge
import fr.toupiearena.mobile.ui.holo.bodyStyle
import fr.toupiearena.mobile.ui.holo.displayStyle
import fr.toupiearena.mobile.ui.holo.tr
import kotlin.random.Random

/** Préparer le combat : votre toupie face à 1 à 3 bots ; choix de toupie et règles dans des tiroirs. */
@Composable
fun SoloSetupScreen(app: AppController) {
    val tops = remember { app.repository.allTops }
    var player by remember { mutableStateOf(tops.first()) }
    val bots = remember { mutableStateListOf<Top?>(null, null, null) } // null : au hasard
    var botCount by remember { mutableIntStateOf(1) }
    var difficulty by remember { mutableStateOf(Difficulty.Normal) }
    var stadium by remember { mutableStateOf(Stadium.classic.id) }
    var rules by remember { mutableStateOf(MatchRules()) }
    var picking by remember { mutableStateOf<Int?>(null) } // -1 : votre toupie ; 0..2 : bot
    var showRules by remember { mutableStateOf(false) }

    HoloScreen {
        Column(Modifier.fillMaxSize()) {
            TopBar(tr("Préparer le combat"), { app.screen = Screen.Menu }, sub = tr("Solo contre 1 à 3 bots")) {
                IconBtn(HoloIcon.Help, { app.screen = Screen.Help })
                IconBtn(HoloIcon.Sliders, { showRules = true }, on = !rules.isDefault)
            }
            Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = EdgeMargin, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Panel(Modifier.weight(1f).fillMaxHeight(), title = tr("Votre toupie"), index = "J1", onClick = { picking = -1 }) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        TopView(player, 128.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(player.name.uppercase(), style = displayStyle(15.sp), maxLines = 1)
                            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TypeBadge(player.type)
                                Text("${specialName(player.special)} · ${tr(player.spin.label).lowercase()}", style = bodyStyle(12.sp, Holo.muted), maxLines = 1)
                            }
                            Stats3(player.stats.attack, player.stats.defense, player.stats.endurance, Modifier.padding(top = 4.dp), 6.dp)
                        }
                    }
                    HoloButton(tr("Changer de toupie"), { picking = -1 }, Modifier.fillMaxWidth(), icon = HoloIcon.Next, height = 40.dp, textSize = 12f)
                }
                VsBadge(Modifier.padding(horizontal = 12.dp))
                Panel(Modifier.weight(1f).fillMaxHeight(), title = tr("Adversaires"), index = if (botCount > 1) tr("Mêlée à %1", botCount + 1) else tr("Duel"), accent = Holo.danger) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Segmented(listOf("1", "2", "3"), botCount - 1, { botCount = it + 1 }, Modifier.width(180.dp), label = tr("Bots"), height = 32.dp)
                        Text(if (botCount > 1) tr("Mêlée à %1 toupies", botCount + 1) else tr("Duel à 2 toupies"), style = bodyStyle(13.sp, Holo.muted), modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Column(Modifier.weight(1f).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                        repeat(botCount) { i ->
                            TopRow(bots[i], height = if (botCount > 1) 40.dp else 56.dp, chevron = true,
                                name = tr("Bot %1 · %2", i + 1, bots[i]?.name ?: tr("Aléatoire"))) { picking = i }
                        }
                    }
                    Segmented(Difficulty.entries.map { tr(it.label) }, difficulty.ordinal, { difficulty = Difficulty.entries[it] },
                        label = if (botCount > 1) tr("Difficulté des bots") else tr("Difficulté du bot"), height = 32.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(52.dp).padding(start = EdgeMargin, end = EdgeMargin), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StadiumStrip(stadium, { stadium = it }, Modifier.weight(1f))
                HoloButton(tr("Combattre"), {
                    val opponents = (0 until botCount).map { bots[it] ?: tops[Random.nextInt(tops.size)] }
                    app.startSolo(player, opponents.take(LocalSession.MAX_BOTS), difficulty, Stadium.find(stadium) ?: Stadium.classic, rules)
                }, Modifier.width(196.dp), BtnVariant.Primary, HoloIcon.Play, height = 52.dp)
            }
            Spacer(Modifier.height(8.dp))
        }
        val target = picking
        TopPickerDrawer(target != null, if (target == -1 || target == null) tr("Votre toupie") else tr("Bot %1", target + 1), tops,
            if (target == -1) player else target?.let { bots[it] }, { t -> if (target == -1) { if (t != null) player = t } else if (target != null) bots[target] = t },
            { picking = null }, allowRandom = target != null && target >= 0)
        RulesDrawer(showRules, rules, { rules = it }, { showRules = false })
    }
}

/** Badge « VS » holographique. */
@Composable
fun VsBadge(modifier: Modifier = Modifier) {
    Box(modifier.width(64.dp).height(56.dp), contentAlignment = Alignment.Center) {
        Text("VS", style = displayStyle(26.sp, Holo.magenta.copy(alpha = 0.7f), 0f, italic = true), modifier = Modifier.padding(end = 4.dp))
        Text("VS", style = displayStyle(26.sp, Holo.cyan.copy(alpha = 0.7f), 0f, italic = true), modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        Text("VS", style = displayStyle(26.sp, Holo.text, 0f, italic = true))
    }
}
