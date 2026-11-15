# Changelog

Versions de l'application mobile (indépendantes de celles du jeu PC).

## 1.0.1

### Corrections
- Le volet des règles d'un combat défile à nouveau : glisser verticalement sur un curseur fait
  défiler le volet au lieu de bouger le curseur.
- La force de lancer affichée va de 50 à 150 %, comme sur PC (elle montrait 0 à 100 %).

### Fluidité
- Combat : l'interface se recompose 20 fois par seconde au lieu de 60 ; l'arène et les jauges de
  duel restent mises à jour à chaque image.
- Le fond holographique, le stadium de l'écran d'accueil, l'aperçu de l'atelier et la toupie de
  l'écran de résultat ne sont plus redessinés à chaque image.

## 1.0.0

Première version publiée de Top Arena sur Android, au niveau de la version PC 2.0.0 (mêmes règles,
même catalogue, cross-play en réseau local avec le protocole v4).

### Jeu
- **Combat contre 1 à 3 bots** (duel ou mêlée), trois niveaux de difficulté, six stadiums de même
  surface, règles personnalisables (vitesse, rebonds, usure, dégâts, endurance, temps maximal,
  coups spéciaux, type de duel).
- **Duels** au choix : timing (toucher dans la zone verte), rafale (toucher le plus vite possible)
  ou aucun ; réglage par défaut dans les Paramètres.
- **Sept coups spéciaux** (onde de choc, lance, givre, vortex, distorsion, siphon, séisme), une fois
  par partie, avec une page qui décrit chacun.
- **Atelier** : assembler, enregistrer et comparer ses toupies.
- **Tournois** solo ou en réseau local : ligue ou élimination directe, 4 à 32 participants, matchs
  express ou classiques ; le tournoi solo se sauvegarde et se reprend.
- **Multijoueur en réseau local** (2 à 4 joueurs, téléphones et PC mélangés) : lobbys détectés
  automatiquement, ou connexion par nom ou adresse IP.
- **Tutoriel** guidé, action par action.

### Interface
- Design « Holo Combat » du PC adapté au téléphone : menus intermédiaires, tiroirs latéraux, action
  principale sous le pouce droit, combat de démonstration sur l'écran d'accueil.
- Trois schémas de commandes (joystick + boutons, inclinaison, gestes), modifiables pendant le
  combat depuis le menu pause ; en solo, le jeu est en pause tant que ce menu est ouvert.
- Français et anglais (langue du téléphone par défaut, modifiable dans les Paramètres).
