# Top Arena Mobile

Version Android de [Top Arena](https://github.com/valentinRyckaert/Top-Arena), écrite en Kotlin
avec Jetpack Compose. Même jeu que la version PC 2.0.0 : combat contre 1 à 3 bots, atelier,
tournois, tutoriel et **multijoueur en réseau local compatible avec la version PC** (mêmes règles,
même catalogue de pièces, même protocole v4 : téléphones et PC dans un même lobby).

L'interface reprend le design « Holo Combat » du PC, adapté au téléphone (menus intermédiaires et
tiroirs latéraux, action principale sous le pouce droit) : voir les planches dans `design/`.
En français ou en anglais (Paramètres > Général).

## Tester sur son téléphone

Prérequis sur l'ordinateur : Java 17 ou plus récent et `curl`/`unzip` (le reste s'installe dans
`~/Android/Sdk`, sans droits administrateur).

1. **SDK Android** (une seule fois) :

   ```sh
   ./scripts/setup-android-sdk.sh
   ```

2. **Préparer le téléphone** (Android 8 ou plus récent) :
   - Paramètres > À propos du téléphone : touchez 7 fois « Numéro de build » ;
   - Paramètres > Système > Options pour les développeurs : activez « Débogage USB » ;
   - branchez le câble USB et acceptez « Autoriser le débogage USB ? ».

3. **Installer et lancer** :

   ```sh
   ./scripts/install-on-phone.sh
   ```

   Le script compile l'APK de débogage, l'installe et lance le jeu. Il explique quoi faire si le
   téléphone n'est pas détecté ou pas encore autorisé. L'APK seul se trouve ensuite dans
   `app/build/outputs/apk/debug/app-debug.apk`.

Pour le multijoueur, le téléphone doit être sur le même Wi-Fi que les autres joueurs.

## Commandes

L'écran est en paysage. **Visée** : glissez depuis votre toupie (direction et longueur de la flèche
= force), relâchez pour lancer. **Duel** : touchez l'écran quand le curseur est dans la zone verte
(timing) ou le plus vite possible (rafale). Le bouton pause (ou retour) ouvre le menu du combat :
reprendre, changer de toupies ou de schéma de commandes, aide, règles, quitter ; en solo, le jeu
est en pause tant qu'il est ouvert.

Trois schémas de commandes, au choix dans **Paramètres** ou dans le menu du combat :

| Schéma | Piloter | Dash | Coup spécial |
|--------|---------|------|--------------|
| Joystick + boutons | joystick virtuel à gauche | bouton DASH | bouton SPÉCIAL |
| Inclinaison | incliner le téléphone (la position au début du combat sert de neutre ; sensibilité réglable) | bouton DASH | bouton SPÉCIAL |
| Gestes | glisser le doigt | toucher brièvement | appui long sans bouger |

La page **Aide** récapitule les commandes du schéma choisi.

## Contenu

- **Combat contre 1 à 3 bots** (duel ou mêlée) : choix des toupies, difficulté (facile, normal,
  difficile), stadium et personnalisation (vitesse, rebonds, usure, dégâts, endurance, temps
  maximal, coups spéciaux, type de duel…).
- **Duels** : timing, rafale ou aucun, par défaut dans les Paramètres, modifiable dans les règles.
- **Coups spéciaux** : une page décrit chacun (point fort, point faible, dégâts, recul, portée).
- **Atelier** : assembler et enregistrer ses toupies (boulon, roue, partie intermédiaire, pointe).
- **Tournois** solo ou en réseau local : ligue ou élimination directe, 4 à 32 participants, matchs
  express ou classiques (ordre des trois toupies choisi avant chaque match). Un tournoi solo se
  sauvegarde en le quittant (ou si l'application passe en arrière-plan) et se reprend depuis
  Tournois.
- **Tutoriel** : combat guidé par des bulles qui mettent le jeu en pause à chaque action.
- **Multijoueur en réseau local** (2 à 4 joueurs, PC et mobiles mélangés) : lobbys détectés
  automatiquement (annonces UDP, questions UDP, balayage TCP du Wi-Fi), ou connexion par nom de
  lobby ou adresse IP.

## Développement

```sh
./gradlew testDebugUnitTest   # tests JVM : moteur, bots, tournois, réseau
./gradlew assembleDebug        # APK de débogage
```

Sur l'émulateur, lancez-le avec `-gpu swangle_indirect` (et `-memory 4096`) : avec
`swiftshader_indirect` ou `-gpu guest`, qemu plante par intermittence sur le dessin des stadiums.

`I18nTest` vérifie que chaque texte passé à `tr("…")` et chaque donnée affichée (pièces, stadiums,
coups…) a sa traduction dans `app/src/main/assets/i18n/en.tsv`.

Les tests de **cross-play** jouent une partie contre la bibliothèque réseau de la version PC
(dans les deux sens : téléphone dans un lobby PC, PC dans un lobby mobile). Ils ne s'exécutent
que si `TOUPIE_PC_INTEROP` désigne le petit programme d'essai côté PC (construit avec
`toupie_net` ; voir `CrossPlayTest.kt`) :

```sh
TOUPIE_PC_INTEROP=/chemin/vers/interop ./gradlew testDebugUnitTest
```

### Architecture

```
app/src/main/java/fr/toupiearena/mobile/
  core/     Stats, pièces, toupies, catalogue (empreinte identique au PC), dépôt JSON des toupies
  game/     moteur porté du PC : Battle (duels), BattleConfig, Stadium, SpecialMove, MatchRules,
            BotController, Tournament
  session/  MatchSession (état affiché + actions du joueur), LocalSession (solo)
  net/      protocole v4 : JsonChannel, LobbyServer, LobbyClient, LobbyBrowser, MatchCodec,
            HostSession, ClientSession, TournamentSession
  ui/       Compose : navigation (App), combat (BattleScreen), écrans, tutoriel, réglages
  ui/holo/  design Holo Combat : couleurs, polices, composants, tiroirs, commandes, traduction
  ui/art/   dessin procédural des toupies, stadiums et effets, porté du PC
design/     planches SVG du design mobile et leurs générateurs
```

Le moteur et le réseau sont en Kotlin pur (testés sur la JVM) ; toute la logique tourne sur le
fil principal, les sockets lisent et écrivent dans des coroutines d'entrée/sortie. Les valeurs de
jeu sont celles de la version PC 2.0.0.
