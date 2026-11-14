# Top Arena mobile — maquette « Holo Combat »

Planches SVG de la version Android (téléphone en **paysage**), avec la même charte que le jeu PC
(`toupie-arena/design/`) : mêmes couleurs, polices, formes à coins coupés et art procédural.
Chaque planche montre un écran et ses états (menus intermédiaires, tiroirs ouverts), dans un
cadre de téléphone de **915 × 412 dp** dessiné à l'échelle 1 (1 unité = 1 dp).

| Fichier | Contenu |
|---|---|
| `m00-principes` | gabarit d'écran (marges, zones des pouces), tiroirs rétractables, carte de navigation, règles |
| `m01-composants` | barre du haut, boutons, choix et réglages, puces, listes, cadres, commandes du combat, tiroir, bulle |
| `m02-ecran-menu` | menu avec combat de bots en fond, menu intermédiaire « Jouer » |
| `m03-ecran-preparation` | duel, mêlée à 4, tiroir « votre toupie », tiroir « personnalisation » |
| `m04-ecran-atelier` | établi, tiroirs « mes toupies », « choisir une pièce », « statistiques » |
| `m05-ecran-coups-speciaux` | liste des sept coups et fiche du coup choisi |
| `m06-ecran-combat` | visée, combat (joystick + boutons), mêlée à 4 (gestes), menu du combat |
| `m07-ecran-duel-tutoriel` | duel timing, duel rafale, tutoriel, commandes par inclinaison |
| `m08-ecran-resultat` | victoire, classement de mêlée, manche de tournoi, partie en réseau |
| `m09-ecran-reseau-local` | lobbys détectés, tiroir « créer un lobby », lobby, tiroir « réglages de l'hôte » |
| `m10-ecran-tournois` | accueil (reprise), configuration solo, élimination directe, tiroir des résultats, ligue |
| `m11-ecran-parametres-aide` | paramètres (général, commandes), aide (commandes, règles) |

Les planches d'art du PC (toupies, pièces, stadiums, coups spéciaux) restent valables telles
quelles : elles ne sont pas dupliquées ici.

## Importer dans Figma

Glisser-déposer les fichiers de `svg/` : chaque SVG devient un cadre aux calques nommés et aux
textes éditables. Polices : **Chakra Petch** et **Rajdhani** (Google Fonts).

## Régénérer

```sh
node gen/boards-mobile.js svg
FONTS=/chemin/vers/polices bash gen/preview.sh svg png   # aperçus PNG (Firefox sans fenêtre)
```

`gen/lib.js`, `gen/art.js` et `gen/boards-art.js` sont des copies du générateur PC (logo
« TOP ARENA », Onde de choc rééquilibrée et stadiums de même taille mis à jour) ;
`gen/mobile-ui.js` ajoute le cadre de téléphone et les composants tactiles, et
`gen/boards-mobile.js` dessine les planches.
