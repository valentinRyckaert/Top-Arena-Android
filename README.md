# Toupie Arena Mobile

Version Android de [Toupie Arena](https://github.com/valentinRyckaert/TOUPIES), écrite en Kotlin
avec Jetpack Compose. Même jeu que la version PC : combat contre le bot, atelier, tournois solo,
tutoriel et **multijoueur en réseau local compatible avec la version PC** (mêmes règles, même
catalogue de pièces, même protocole v4 : téléphones et PC dans un même lobby).

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
= force), relâchez pour lancer. **Duel** : touchez l'écran quand le curseur est dans la zone verte.

Trois schémas de commandes, au choix dans **Paramètres** :

| Schéma | Piloter | Dash | Coup spécial |
|--------|---------|------|--------------|
| Joystick + boutons | joystick virtuel à gauche | bouton DASH | bouton SPÉCIAL |
| Inclinaison | incliner le téléphone (la position au début du combat sert de neutre ; sensibilité réglable) | bouton DASH | bouton SPÉCIAL |
| Gestes | glisser le doigt | toucher brièvement | appui long sans bouger |

La page **Aide** récapitule les commandes du schéma choisi.

## Contenu

- **Combat contre le bot** : choix des toupies, difficulté (facile, normal, difficile), stadium et
  personnalisation (vitesse, rebonds, usure, dégâts, endurance, temps maximal, coups spéciaux…).
- **Atelier** : assembler et enregistrer ses toupies (boulon, roue, partie intermédiaire, pointe).
- **Tournois** solo ou en réseau local : ligue ou élimination directe, 4 à 32 participants, matchs
  express ou classiques (ordre des trois toupies choisi avant chaque match).
- **Tutoriel** : combat guidé par des bulles qui mettent le jeu en pause à chaque action.
- **Multijoueur en réseau local** (2 à 4 joueurs, PC et mobiles mélangés) : lobbys détectés
  automatiquement (annonces UDP, questions UDP, balayage TCP du Wi-Fi), ou connexion par nom de
  lobby ou adresse IP.

## Développement

```sh
./gradlew testDebugUnitTest   # tests JVM : moteur, bots, tournois, réseau
./gradlew assembleDebug        # APK de débogage
```

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
```

Le moteur et le réseau sont en Kotlin pur (testés sur la JVM) ; toute la logique tourne sur le
fil principal, les sockets lisent et écrivent dans des coroutines d'entrée/sortie. Les valeurs de
jeu sont celles de la version PC 1.2.1.
