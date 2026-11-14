'use strict';
// Planches de la version mobile (téléphone en paysage, 915 × 412 dp).
//   node gen/boards-mobile.js svg
const U = require('./mobile-ui');
const { L, Ar, C, TYPE, F, P, PW, PH, M, BAR, tw, label, g, rect, circle, line, path, chamfer, skew, T, D } = U;
const OUT = process.argv[2] || `${__dirname}/../svg`;

// ============================== M00 PRINCIPES ==============================
function principles() {
  const gabarit = d => [U.screenBg(d), U.topBar(d, 'Titre de l’écran', { sub: 'sous-titre', actions: [{ icon: 'sliders' }, { label: 'Action', variant: 'primary', icon: 'play' }] }),
    rect(0, 0, M, PH, { fill: C.magenta, opacity: 0.16 }), rect(PW - M, 0, M, PH, { fill: C.magenta, opacity: 0.16 }),
    rect(M, BAR + 10, PW - 2 * M, PH - BAR - 20, { fill: 'none', stroke: C.cyan, 'stroke-dasharray': '6 6', opacity: 0.7 }),
    T(PW / 2, 150, 'ZONE DE CONTENU', { font: F.display, size: 16, ls: 3, fill: C.cyan, anchor: 'middle', weight: 700 }),
    T(PW / 2, 172, 'une tâche par écran · défilement vertical si besoin', { size: 13, fill: C.muted, anchor: 'middle' }),
    circle(PW - 40, PH - 30, 120, { fill: C.green, opacity: 0.12 }), circle(40, PH - 30, 120, { fill: C.green, opacity: 0.08 }),
    T(PW - 150, PH - 60, 'pouce droit : action principale', { size: 13, fill: C.green, anchor: 'middle' }),
    T(150, PH - 60, 'pouce gauche : pilotage, listes', { size: 13, fill: C.green, anchor: 'middle', opacity: 0.8 }),
    U.btn(d, PW - M - 200, PH - 58, 200, 46, 'Valider', { variant: 'primary', icon: 'play' }),
    T(12, PH / 2 - 60, '24 dp', { size: 11, fill: C.magenta, transform: `rotate(-90 12 ${PH / 2 - 60})`, anchor: 'middle' }),
    T(PW - 10, PH / 2 + 60, '24 dp', { size: 11, fill: C.magenta, transform: `rotate(90 ${PW - 10} ${PH / 2 + 60})`, anchor: 'middle' }),
    U.pin(M + 22, 26, 1), U.pin(PW - M - 220, 26, 2), U.pin(PW - M - 210, PH - 35, 3)];
  const tiroirs = d => [U.screenBg(d), U.topBar(d, 'Préparer le combat', { actions: [{ icon: 'sliders', on: true }] }),
    Ar.top(d, 200, 230, 90, P['Cyclone Furieux'], { aura: true, angle: 0.4 }),
    U.drawer(d, 'right', 400, 'Personnalisation', (x, y, w) => g('Réglages', [
      U.slider(d, x, y, w, 'Vitesse des toupies', 0.5, '×1,0'), U.slider(d, x, y + 48, w, 'Dégâts', 0.62, '×1,2'),
      U.slider(d, x, y + 96, w, 'Endurance de départ', 0.5, '100'), U.seg(d, x, y + 166, w, ['Timing', 'Rafale', 'Sans'], 0, { label: 'Duels' }),
      U.toggle(d, x, y + 222, w, 'Coups spéciaux', true)])),
    U.pin(PW - 380, 30, 1), U.pin(PW - 412, PH / 2, 2)];
  const after = (d, x, y, w) => {
    const out = [L.slashes(x, y + 6, 3, 14, C.green, 8), D(x + 34, y + 20, 'CARTE DE NAVIGATION', { size: 18, ls: 3 })];
    const box = (bx, by, bw, t, o = {}) => g(`Nœud ${t}`, [path(chamfer(bx, by, bw, 44, [10, 0, 10, 0]), { fill: o.main ? '#173B2C' : '#15113F', stroke: o.col || (o.main ? C.green : C.lineHi), 'stroke-width': 1.4 }),
      o.icon ? L.icon(o.icon, bx + 24, by + 22, 16, o.col || C.cyan) : '', D(bx + (o.icon ? 42 : 16), by + 27, t.toUpperCase(), { size: 13, ls: 1.4, fill: o.col || C.text }),
      o.tag ? T(bx + bw - 10, by + 27, o.tag, { size: 12, fill: C.muted, anchor: 'end' }) : '']);
    const arrow = (x1, y1, x2, y2, col = C.line) => path(`M${x1},${y1} H${(x1 + x2) / 2} V${y2} H${x2 - 6} M${x2 - 12},${y2 - 5} L${x2 - 5},${y2} L${x2 - 12},${y2 + 5}`, { fill: 'none', stroke: col, 'stroke-width': 1.6 });
    const y0 = y + 50, c0 = x, c1 = x + 250, c2 = x + 560, c3 = x + 900, c4 = x + 1230, c5 = x + 1540;
    const rowY = { jouer: y0 + 62, multi: y0 + 186, atelier: y0 + 248, param: y0 + 310, aide: y0 + 372 };
    out.push(box(c0, y0 + 186, 200, 'Menu', { main: true, icon: 'play' }));
    [['Jouer', 'play', 'jouer', 'menu intermédiaire'], ['Multijoueur', 'lan', 'multi'], ['Atelier', 'build', 'atelier'], ['Paramètres', 'gear', 'param'], ['Aide', 'help', 'aide']].forEach(([t, ic, k, tag]) => {
      out.push(arrow(c0 + 200, y0 + 208, c1, rowY[k] + 22), box(c1, rowY[k], 260, t, { icon: ic, tag }));
    });
    // Jouer ▸ menu intermédiaire
    [['Contre les bots', 'bot'], ['Tournois', 'trophy'], ['Tutoriel', 'tuto']].forEach(([t, ic], i) => out.push(arrow(c1 + 260, rowY.jouer + 22, c2, y0 + i * 62 + 22, C.green), box(c2, y0 + i * 62, 280, t, { icon: ic, col: C.green })));
    const chain = (yy, items) => items.forEach(([t, tag], i) => {
      const xs = [c3, c4, c5][i], ws = [280, 260, 230][i];
      out.push(arrow(i === 0 ? (yy < y0 + 150 ? c2 + 280 : c1 + 260) : [c3 + 280, c4 + 260][i - 1], yy + 22, xs, yy + 22), box(xs, yy, ws, t, { tag }));
    });
    chain(y0, [['Préparer le combat', '2 tiroirs'], ['Combat', 'pause'], ['Résultat']]);
    chain(y0 + 62, [['Tournois', 'reprendre'], ['Tableau du tournoi', '2 tiroirs'], ['Match → Combat']]);
    chain(y0 + 124, [['Combat guidé', 'bulles']]);
    chain(rowY.multi, [['Lobbys détectés', 'créer : tiroir'], ['Lobby', 'réglages : tiroir'], ['Combat en réseau']]);
    chain(rowY.atelier, [['Atelier', '3 tiroirs'], ['Coups spéciaux']]);
    chain(rowY.param, [['Langue, duels, commandes']]);
    chain(rowY.aide, [['Commandes, règles']]);
    // règles
    const rules = [
      ['Paysage, 915 × 412 dp de référence', 'marges de 24 dp de chaque côté (encoche de la caméra, coins arrondis)'],
      ['Cibles tactiles d’au moins 44 dp', 'action principale en bas à droite, retour en haut à gauche et bouton retour d’Android'],
      ['Une tâche par écran', 'ce qui tient dans un panneau du PC passe dans un tiroir ou un menu intermédiaire'],
      ['Tiroir gauche : choisir, consulter', 'toupies, résultats du tournoi, menu de pause — tiroir droit : régler (règles, lobby, stats)'],
      ['Pas de raccourcis clavier', 'chaque touche du PC devient un bouton visible ; trois schémas de commandes en combat'],
      ['Textes ≥ 11 sp, chiffres en Chakra Petch', 'mêmes couleurs de type, mêmes formes à coins coupés que sur PC'],
    ];
    const ry = y + 520;
    out.push(L.slashes(x, ry - 14, 3, 14, C.green, 8), D(x + 34, ry, 'RÈGLES', { size: 18, ls: 3 }));
    rules.forEach(([a, b], i) => {
      const rx = x + (i % 2) * (w / 2), yy = ry + 34 + Math.floor(i / 2) * 64;
      out.push(path(skew(rx, yy - 13, 10, 16, 4), { fill: C.green }), D(rx + 22, yy, a.toUpperCase(), { size: 14, ls: 1.4 }), T(rx + 22, yy + 24, b, { size: 15.5, fill: C.muted, weight: 500 }));
    });
    return g('Navigation et règles', out);
  };
  return U.board('m00-principes', 'Mobile · principes', 'Top Arena en poche',
    'Même charte Holo Combat que sur PC, en paysage, pensée pour les pouces : menus intermédiaires et tiroirs rétractables.',
    [{ title: 'Gabarit d’écran', draw: gabarit, notes: ['1 Retour (et bouton retour d’Android) · 2 actions de l’écran, l’action principale en vert', '3 L’action principale reste sous le pouce droit, en bas à droite'] },
      { title: 'Tiroirs rétractables', draw: tiroirs, notes: ['1 Le tiroir glisse par-dessus l’écran, qui reste visible sous un voile', '2 Poignée : glisser vers le bord, toucher le voile ou ✕ pour fermer'] }],
    { after, extra: 760 });
}

// ============================== M01 COMPOSANTS ==============================
function components() {
  const W = 2010, H = 1500, d = new L.Doc(W, H, 'm01-composants'), S = 1.55;
  d.add(L.background(d, W, H, { floor: false }));
  d.add(L.boardHeader(d, 70, 80, 'Mobile · composants', 'Composants tactiles', 'Dessinés à 1 dp = 1 unité ; ici agrandis ×1,55. Mêmes formes et couleurs que le design system PC.'));
  const zone = (x, y, title, inner) => g(`Section ${title}`, [L.slashes(x, y - 12, 3, 12, C.green, 7), D(x + 28, y, title.toUpperCase(), { size: 15, ls: 2.5, fill: C.cyan }),
    `<g transform="translate(${x},${y + 18}) scale(${S})">${[inner].flat().join('')}</g>`]);
  const ui = [];
  ui.push(zone(70, 220, 'Barre du haut', [path(chamfer(0, 0, 400, 60, [10, 0, 10, 0]), { fill: '#0B0928' }),
    `<g transform="translate(-24,4)">${U.topBar(d, 'Atelier', { width: 440, actions: [{ label: 'Enregistrer', icon: 'save', variant: 'primary' }, { icon: 'sliders' }] })}</g>`]));
  ui.push(zone(70, 360, 'Boutons (h 44 à 52 dp)', [U.btn(d, 0, 0, 200, 52, 'Combattre', { variant: 'primary', icon: 'play' }), U.btn(d, 214, 0, 180, 52, 'Atelier', { icon: 'build' }),
    U.btn(d, 0, 66, 140, 44, 'Annuler', { variant: 'ghost' }), U.btn(d, 154, 66, 150, 44, 'Supprimer', { variant: 'danger', icon: 'trash' }), U.btn(d, 318, 66, 76, 44, 'Holo', { variant: 'holo' }),
    U.iconBtn(d, 0, 124, 44, 40, 'back'), U.iconBtn(d, 54, 124, 44, 40, 'sliders', { on: true }), U.iconBtn(d, 108, 124, 44, 40, 'trophy', { badge: 2 }), U.iconBtn(d, 162, 124, 44, 40, 'help')]));
  ui.push(zone(70, 700, 'Choix et réglages', [U.seg(d, 0, 18, 330, ['Facile', 'Normal', 'Difficile'], 1, { label: 'Difficulté' }), U.seg(d, 0, 80, 200, ['1', '2', '3'], 0, { label: 'Bots' }),
    U.slider(d, 0, 130, 330, 'Sensibilité de l’inclinaison', 0.4, '35 %'), U.toggle(d, 0, 186, 330, 'Coups spéciaux', true), U.toggle(d, 0, 228, 330, 'Limite de temps', false, 'aucune limite')]));
  ui.push(zone(70, 1150, 'Puces', [U.chip(d, 0, 0, 'Élimination directe', C.cyan, 'trophy'), U.chip(d, 200, 0, '8 joueurs', C.violet, 'user'), U.chip(d, 0, 34, 'Express', C.green, 'timer'), U.chip(d, 110, 34, 'Rafale', C.warn, 'dash')]));
  ui.push(zone(760, 220, 'Listes', [U.topRow(d, 0, 0, 300, P['Cyclone Furieux'], { selected: true, chevron: true }), U.topRow(d, 0, 56, 300, P['Forteresse'], { right: 'Bot 1' }),
    U.topRow(d, 0, 112, 300, P['Éternelle'], { star: true, name: 'Ma Lune Noire' }), U.stadiumChip(d, 0, 172, 190, 'glace', { selected: true }), U.stadiumChip(d, 200, 172, 190, 'volcan')]));
  ui.push(zone(760, 640, 'Cadre et statistiques', [U.panel(d, 0, 0, 300, 120, { title: 'Votre toupie', index: 'J1' }), U.stats3(d, 16, 38, 268, P['Juste Milieu']),
    U.panel(d, 320, 0, 120, 120, { title: 'VS', accent: C.magenta })]));
  ui.push(zone(760, 900, 'Combat', [U.endurance(d, 0, 8, 230, 72), U.endurance(d, 0, 28, 230, 21, C.danger, { color2: C.warn }),
    U.joystick(d, 60, 120, 50), U.roundAction(d, 190, 120, 46, 'Dash', { icon: 'dash' }), U.roundAction(d, 300, 120, 38, 'Spécial', { glyph: 'lance', color: C.magenta, size: 10 }),
    U.roundAction(d, 400, 120, 38, 'Utilisé', { glyph: 'lance', disabled: true, size: 10 })]));
  ui.push(zone(1460, 220, 'Tiroir', [`<g transform="scale(0.36)">${U.screenBg(d)}${U.drawer(d, 'left', 330, 'Mes toupies', (x, y, w) => g('Liste', [P['Cyclone Furieux'], P['Forteresse'], P['Éternelle'], P['Juste Milieu']].map((p, i) => U.topRow(d, x, y + i * 56, w, p, { selected: i === 0 }))))}</g>`,
    T(0, 170, 'Voile, poignée, ✕ — glisser pour fermer', { size: 10, fill: C.muted })]));
  ui.push(zone(1460, 560, 'Bulle de tutoriel', [path(chamfer(0, 0, 300, 92, [12, 0, 12, 0]), { fill: '#0B0930', stroke: C.cyan, 'stroke-width': 1.3 }),
    rect(0, 12, 3, 30, { fill: C.cyan }), D(16, 22, 'DASH', { size: 12, ls: 2, fill: C.cyan }), T(284, 22, '3 / 7', { size: 10, fill: C.muted, anchor: 'end', font: F.display }),
    T(16, 46, 'Touchez DASH : votre toupie fonce', { size: 13 }), T(16, 64, 'sur l’adversaire le plus proche.', { size: 13 }), T(16, 82, 'Le jeu est en pause.', { size: 12, fill: C.muted }),
    path('M40,92 l14,16 l14,-16 Z', { fill: '#0B0930', stroke: C.cyan, 'stroke-width': 1.3 }), rect(41, 90, 26, 4, { fill: '#0B0930' })]));
  d.add(ui);
  return d;
}

if (require.main === module) for (const b of [principles, components]) b().save(OUT);
module.exports = { principles, components };
