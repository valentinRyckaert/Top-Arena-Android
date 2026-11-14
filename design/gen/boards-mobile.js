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


// ---------------------------------------------------------------- aides d'écran
const MV = Ar.MOVES, WH = Ar.WHEELS, MI = Ar.MIDDLES, TI = Ar.TIPS;
const moveOf = p => Ar.BOLTS[p.parts[3]];
const spinTxt = p => WH[p.parts[0]].spin === 'G' ? 'anti-horaire' : 'horaire';
const statsTxt = st => `A${st[0]} · D${st[1]} · E${st[2]}`;
const scroll = (x, y, h, at = 0, len = 0.4) => g('Défilement', [rect(x, y, 3, h, { fill: C.line, opacity: 0.6 }), rect(x, y + h * at, 3, h * len, { fill: C.cyan })]);
const fade = (d, x, y, w, h, dir = 0) => rect(x, y, w, h, { fill: d.lin([[0, '#07061A', 0], [1, '#07061A', 0.95]], dir) });
function glyphBox(d, x, y, s, key, on = true) {
  return g(`Emblème ${key}`, [path(chamfer(x, y, s, s, [s * 0.22, 0, s * 0.22, 0]), { fill: '#07061A', stroke: on ? MV[key].color : C.line, 'stroke-width': 1.3 }),
    Ar.glyph(key, x + s / 2, y + s / 2, s * 0.3, on ? '#FFFFFF' : C.dim, 1.8)]);
}
function field(d, x, y, w, val, o = {}) {
  return g(`Champ ${o.label || val}`, [o.label ? label(x, y - 6, o.label) : '',
    path(chamfer(x, y, w, 40, [10, 0, 10, 0]), { fill: '#07061A', stroke: o.focus ? C.cyan : C.line, 'stroke-width': o.focus ? 1.8 : 1.1 }),
    T(x + 14, y + 26, val, { size: 15, fill: o.placeholder ? C.dim : C.text }),
    o.focus ? rect(x + 16 + tw(val, 15), y + 10, 1.6, 20, { fill: C.cyan }) : '', o.icon ? L.icon(o.icon, x + w - 22, y + 20, 16, C.cyan) : '']);
}
function radar(d, cx, cy, R, v, col = TYPE.Attaque, max = 15) {
  const ang = [-90, 30, 150].map(a => a * Math.PI / 180), pt = (i, r) => [cx + Math.cos(ang[i]) * r, cy + Math.sin(ang[i]) * r];
  const ring = k => path(L.polyD([0, 1, 2].map(i => pt(i, R * k))), { fill: 'none', stroke: C.line, 'stroke-width': 1 });
  const shape = L.polyD([0, 1, 2].map(i => pt(i, R * v[i] / max)));
  return g('Radar', [ring(1), ring(0.66), ring(0.33), ...[0, 1, 2].map(i => line(cx, cy, ...pt(i, R), { stroke: C.line })),
    path(shape, { fill: col, 'fill-opacity': 0.3, stroke: col, 'stroke-width': 2 }), ...[0, 1, 2].map(i => circle(...pt(i, R * v[i] / max), 3.5, { fill: '#FFFFFF' })),
    D(cx, cy - R - 8, `ATT ${v[0]}`, { size: 11, fill: TYPE.Attaque, anchor: 'middle' }), D(pt(1, R)[0] + 4, pt(1, R)[1] + 18, `DÉF ${v[1]}`, { size: 11, fill: TYPE['Défense'], anchor: 'middle' }),
    D(pt(2, R)[0] - 4, pt(2, R)[1] + 18, `END ${v[2]}`, { size: 11, fill: TYPE.Endurance, anchor: 'middle' })]);
}
// Petit combat de bots en fond (menu).
function botBattle(d, cx, cy, R, key = 'volcan') {
  return g('Combat de bots', [circle(cx, cy, R * 1.25, { fill: '#FF6A3D', opacity: 0.12, filter: d.blur(40) }), Ar.stadium(d, key, cx, cy, R),
    path(`M${cx - 120},${cy + 40} Q${cx - 70},${cy - 10} ${cx - 22},${cy - 12}`, { fill: 'none', stroke: C.green, 'stroke-width': 4, opacity: 0.55, 'stroke-linecap': 'round' }),
    path(`M${cx + 110},${cy - 50} Q${cx + 60},${cy + 20} ${cx + 22},${cy + 2}`, { fill: 'none', stroke: C.danger, 'stroke-width': 4, opacity: 0.55, 'stroke-linecap': 'round' }),
    Ar.top(d, cx - 22, cy - 10, 20, P['Éternelle'], { angle: 1.1 }), Ar.top(d, cx + 22, cy + 4, 20, P['Cyclone Furieux'], { angle: 0.2 }),
    ...[0, 1, 2, 3, 4, 5, 6].map(i => { const a = i * 0.9 - 0.4; return line(cx + Math.cos(a) * 8, cy - 4 + Math.sin(a) * 8, cx + Math.cos(a) * 24, cy - 4 + Math.sin(a) * 24, { stroke: i % 2 ? C.warn : '#FFFFFF', 'stroke-width': 1.6 }); })]);
}

// ============================== M02 MENU ==============================
function menuScreen(d, o = {}) {
  const out = [U.screenBg(d, { cx: 0.72, cy: 0.5, h3x: 0.72, h3y: 0.5, floor: false }), botBattle(d, 690, 206, 168)];
  out.push(U.chip(d, PW - M - U.chipW('2 lobbys en réseau local', 1), 14, '2 lobbys en réseau local', C.cyan, 'wifi'));
  out.push(`<g transform="translate(${M + 4},0)">${L.logo(d, 0, 84, 0.5)}</g>`);
  out.push(T(M, 136, 'Assemblez votre toupie · affrontez les bots', { size: 12.5, fill: C.muted, weight: 500 }), T(M, 152, 'ou vos amis en réseau local', { size: 12.5, fill: C.muted, weight: 500 }));
  out.push(U.btn(d, M, 170, 340, 52, 'Jouer', { variant: 'primary', icon: 'play', align: 'left', sub: o.jouer ? undefined : undefined }));
  if (o.jouer) out.push(path(chamfer(M - 3, 167, 346, 58, [17, 0, 17, 0]), { fill: 'none', stroke: '#FFFFFF', 'stroke-width': 2 }));
  out.push(U.btn(d, M, 232, 340, 46, 'Multijoueur en réseau local', { icon: 'lan', align: 'left' }), U.btn(d, M, 286, 340, 46, 'Atelier', { icon: 'build', align: 'left' }));
  out.push(U.btn(d, M, 346, 166, 42, 'Paramètres', { variant: 'ghost', icon: 'gear', size: 12 }), U.btn(d, M + 174, 346, 166, 42, 'Aide', { variant: 'ghost', icon: 'help', size: 12 }));
  out.push(T(PW - M, PH - 14, 'v2.0.0', { size: 11, fill: C.dim, anchor: 'end', font: F.display }));
  if (o.jouer) out.push(U.drawer(d, 'right', 470, 'Jouer', (x, y, w) => {
    const item = (yy, ic, t, sub, extra) => g(`Choix ${t}`, [U.card(d, x, yy, w, 84), path(chamfer(x + 12, yy + 16, 52, 52, [12, 0, 12, 0]), { fill: '#07061A', stroke: C.green }),
      L.icon(ic, x + 38, yy + 42, 24, C.green), D(x + 78, yy + 36, t.toUpperCase(), { size: 16, ls: 1.8 }), T(x + 78, yy + 58, sub, { size: 13, fill: C.muted }),
      extra || '', L.icon('next', x + w - 20, yy + 42, 18, C.cyan)]);
    return g('Choix', [item(y, 'bot', 'Contre les bots', '1 à 3 bots · duel ou mêlée'),
      item(y + 96, 'trophy', 'Tournois', 'ligue ou élimination · solo ou réseau', U.chip(d, x + w - 40 - U.chipW('Reprendre · tour 2/3', 1), y + 96 + 8, 'Reprendre · tour 2/3', C.warn, 'save')),
      item(y + 192, 'tuto', 'Tutoriel', 'un combat guidé, action par action')]);
  }, { scrim: 0.45 }));
  return out;
}
function menuBoard() {
  return U.board('m02-ecran-menu', 'Mobile · écran', 'Menu', 'Un combat de bots tourne derrière le menu ; « Jouer » ouvre un menu intermédiaire au lieu de tout empiler.',
    [{ title: 'Menu principal', draw: d => menuScreen(d), notes: ['Trois entrées sous le pouce gauche, Paramètres et Aide en dessous', 'Mises à jour et Quitter disparaissent : gérés par Android'] },
      { title: 'Jouer ▸ menu intermédiaire', draw: d => menuScreen(d, { jouer: true }), notes: ['Bots, tournois et tutoriel regroupés dans un tiroir', 'Un tournoi solo sauvegardé se reprend directement d’ici'] }]);
}

// ============================== M03 PRÉPARATION ==============================
function setupScreen(d, o = {}) {
  const bots = o.bots || 1, out = [U.screenBg(d)];
  out.push(U.topBar(d, 'Préparer le combat', { sub: 'Solo contre 1 à 3 bots', actions: [{ icon: 'help' }, { icon: 'sliders', on: o.rules }] }));
  const me = P['Cyclone Furieux'], x0 = M, y0 = 62, cw = 384, ch = 282;
  out.push(U.panel(d, x0, y0, cw, ch, { title: 'Votre toupie', index: 'J1', selected: o.pick }));
  out.push(Ar.top(d, x0 + 78, y0 + 128, 56, me, { aura: true, angle: 0.4 }));
  out.push(D(x0 + 156, y0 + 62, me.name.toUpperCase(), { size: 16, ls: 1.6 }), U.badge(d, x0 + 156, y0 + 70, me.type));
  out.push(Ar.glyph(moveOf(me), x0 + 238, y0 + 78, 6, C.text, 1.6), T(x0 + 250, y0 + 82, `${MV[moveOf(me)].name} · ${spinTxt(me)}`, { size: 12, fill: C.muted }));
  out.push(U.stats3(d, x0 + 156, y0 + 100, 210, me));
  out.push(U.btn(d, x0 + 14, y0 + 228, cw - 28, 40, 'Changer de toupie', { icon: 'next', size: 12 }));
  out.push(g('VS', [path(chamfer(PW / 2 - 34, 168, 68, 56, [12, 0, 12, 0]), { fill: '#07061A', stroke: d.lin([[0, C.cyan], [1, C.magenta]], 0), 'stroke-width': 1.6 }),
    D(PW / 2 - 2, 206, 'VS', { size: 26, fill: C.magenta, anchor: 'middle', italic: true, opacity: 0.7 }), D(PW / 2 + 2, 207, 'VS', { size: 26, fill: C.cyan, anchor: 'middle', italic: true, opacity: 0.7 }), D(PW / 2, 206, 'VS', { size: 26, anchor: 'middle', italic: true })]));
  const x1 = PW - M - cw;
  out.push(U.panel(d, x1, y0, cw, ch, { title: 'Adversaires', index: bots > 1 ? `MÊLÉE À ${bots + 1}` : 'DUEL', accent: C.danger }));
  out.push(U.seg(d, x1 + 14, y0 + 52, 180, ['1', '2', '3'], bots - 1, { label: 'Bots', h: 32 }), T(x1 + 206, y0 + 74, bots > 1 ? `Mêlée à ${bots + 1} toupies` : 'Duel à 2 toupies', { size: 13, fill: C.muted }));
  const rows = bots > 1 ? [P['Forteresse'], null, P['Comète']].slice(0, bots) : [null];
  rows.forEach((p, i) => {
    const ry = y0 + 98 + i * (bots > 1 ? 42 : 0), rh = bots > 1 ? 38 : 56;
    if (p) out.push(U.topRow(d, x1 + 14, ry, cw - 28, p, { h: rh, chevron: true, name: `Bot ${i + 1} · ${p.name}` }));
    else out.push(g(`Bot ${i + 1} aléatoire`, [U.card(d, x1 + 14, ry, cw - 28, rh), circle(x1 + 40, ry + rh / 2, rh * 0.3, { fill: 'none', stroke: C.cyan, 'stroke-dasharray': '3 4' }),
      D(x1 + 40, ry + rh / 2 + 5, '?', { size: 14, fill: C.cyan, anchor: 'middle' }), D(x1 + 66, ry + rh / 2 - (bots > 1 ? -4 : 2), `BOT ${i + 1} · ALÉATOIRE`, { size: 12.5, ls: 1.4 }),
      bots > 1 ? '' : T(x1 + 66, ry + rh / 2 + 16, 'Toupie tirée au hasard au lancement', { size: 12, fill: C.muted }), L.icon('next', x1 + cw - 32, ry + rh / 2, 14, C.cyan)]));
  });
  const dy = bots > 1 ? y0 + 244 : y0 + 194;
  out.push(U.seg(d, x1 + 14, dy, cw - 28, ['Facile', 'Normal', 'Difficile'], 1, { label: bots > 1 ? 'Difficulté des bots' : 'Difficulté du bot', h: bots > 1 ? 30 : 32 }));
  // stadiums
  out.push(g('Stadiums', [...Ar.STADIUMS.map((s, i) => U.stadiumChip(d, M + i * 146, 352, 140, s.key, { selected: i === (o.stadium ?? 0), h: 52 })),
    fade(d, PW - M - 290, 350, 80, 56, 0), rect(PW - M - 210, 350, 210 + M, 56, { fill: '#07061A', opacity: 0.95 })]));
  out.push(U.btn(d, PW - M - 196, 352, 196, 52, 'Combattre', { variant: 'primary', icon: 'play' }));
  if (o.pick) out.push(U.drawer(d, 'left', 430, 'Votre toupie', (x, y, w) => {
    const els = [label(x, y + 8, 'Mes toupies', C.cyan), U.topRow(d, x, y + 16, w, P['Comète'], { star: true, name: 'Comète Rouge', h: 44 }),
      label(x, y + 78, 'Prédéfinies', C.cyan)];
    ['Cyclone Furieux', 'Forteresse', 'Éternelle', 'Juste Milieu', 'Gardienne'].forEach((n, i) => els.push(U.topRow(d, x, y + 86 + i * 50, w - 10, P[n], { selected: i === 0, h: 44, right: i === 0 ? 'choisie' : undefined })));
    els.push(scroll(x + w - 3, y + 86, 250, 0, 0.55));
    return g('Liste', els);
  }, { sub: '8 prédéfinies · 1 à vous' }));
  if (o.rules) out.push(U.drawer(d, 'right', 440, 'Personnalisation', (x, y, w) => g('Règles', [
    U.slider(d, x, y, w - 12, 'Vitesse des toupies', 0.5, '×1,0'), U.slider(d, x, y + 44, w - 12, 'Rebonds', 0.5, '×1,0'),
    U.slider(d, x, y + 88, w - 12, 'Usure de la rotation', 0.62, '×1,2'), U.slider(d, x, y + 132, w - 12, 'Dégâts des chocs', 0.5, '×1,0'),
    U.toggle(d, x, y + 186, w - 12, 'Coups spéciaux', true),
    U.seg(d, x, y + 238, w - 12, ['Timing', 'Rafale', 'Sans duel'], 1, { label: 'Duels · suivent les Paramètres', h: 32 }),
    scroll(x + w - 3, y, 270, 0, 0.6),
    rect(x - 22, PH - 64, w + 44, 64, { fill: '#0E0B30' }), U.btn(d, x, PH - 54, 150, 44, 'Rétablir', { variant: 'ghost', size: 12 }), U.btn(d, x + 160, PH - 54, w - 172, 44, 'Valider', { variant: 'primary', icon: 'check', size: 13 })]),
    { sub: 'règles du combat' }));
  return out;
}
function setupBoard() {
  return U.board('m03-ecran-preparation', 'Mobile · écran', 'Préparer le combat', 'Les deux camps restent face à face ; le choix d’une toupie et les règles passent dans des tiroirs.',
    [{ title: 'Duel contre un bot', draw: d => setupScreen(d), notes: ['Stadiums en bande défilante, « Combattre » sous le pouce droit', 'Toucher une carte ouvre le choix de la toupie'] },
      { title: 'Mêlée à 4 toupies', draw: d => setupScreen(d, { bots: 3, stadium: 3 }), notes: ['1 à 3 bots : chaque bot a sa toupie (choisie ou au hasard)', 'La difficulté est commune à tous les bots'] },
      { title: 'Tiroir : votre toupie', draw: d => setupScreen(d, { pick: true }), notes: ['Vos toupies en premier (étoile), puis les 8 prédéfinies', 'Même tiroir pour choisir la toupie d’un bot'] },
      { title: 'Tiroir : personnalisation', draw: d => setupScreen(d, { rules: true }), notes: ['Vitesse, rebonds, usure, dégâts, endurance, durée, coups spéciaux, duels', 'Rétablir revient aux règles par défaut (et au duel des Paramètres)'] }]);
}

// ============================== M04 ATELIER ==============================
function partCard(d, x, y, w, h, slot, code, o) {
  const col = o.col, out = [U.card(d, x, y, w, h, { selected: o.selected, accent: col })];
  out.push(label(x + 10, y + 16, slot, col), T(x + w - 10, y + 16, code, { size: 10, fill: C.dim, anchor: 'end', font: F.display, weight: 700 }));
  out.push(o.art, D(x + 64, y + 50, o.name.toUpperCase(), { size: 12.5, ls: 1.2 }), T(x + 64, y + 68, o.sub, { size: 11.5, fill: C.muted }));
  out.push(U.iconBtn(d, x + w - 84, y + h - 34, 36, 28, 'back', { size: 14 }), U.iconBtn(d, x + w - 44, y + h - 34, 36, 28, 'next', { size: 14 }));
  return g(`Pièce ${slot}`, out);
}
function builderScreen(d, o = {}) {
  const me = P['Cyclone Furieux'], [wk, mk, tk, bk] = me.parts, mv = Ar.BOLTS[bk], st = Ar.sumStats(me.parts), out = [U.screenBg(d)];
  out.push(U.topBar(d, 'Atelier', { sub: 'Assembler une toupie', actions: [{ label: 'Enregistrer', icon: 'save', variant: 'primary' }, { icon: 'balance', on: o.stats }, { label: 'Mes toupies', icon: 'build', on: o.mine }] }));
  out.push(field(d, M, 62, 420, 'Cyclone Furieux', { focus: !o.part, icon: 'random' }));
  const cw = 206, chh = 112, gx = M, gy = 112;
  out.push(partCard(d, gx, gy, cw, chh, 'Boulon', bk, { col: C.cyan, art: Ar.boltArt(d, mv, gx + 34, gy + 54, 22, 0), name: 'Perçant', sub: `${MV[mv].name} · vague` }));
  out.push(partCard(d, gx + cw + 8, gy, cw, chh, 'Roue', wk, { col: TYPE.Attaque, selected: o.part, art: Ar.wheelArt(d, wk, gx + cw + 8 + 34, gy + 54, 24), name: WH[wk].name, sub: `${statsTxt(WH[wk].stats)} · ${WH[wk].spin === 'G' ? 'anti-h.' : 'horaire'}` }));
  out.push(partCard(d, gx, gy + chh + 8, cw, chh, 'Intermédiaire', mk, { col: C.warn, art: Ar.middleArt(d, mk, gx + 34, gy + chh + 8 + 54, 24, 0), name: MI[mk].name, sub: statsTxt(MI[mk].stats) }));
  out.push(partCard(d, gx + cw + 8, gy + chh + 8, cw, chh, 'Pointe', tk, { col: C.magenta, art: Ar.tipArt(d, tk, gx + cw + 8 + 34, gy + chh + 8 + 40, 36), name: TI[tk].name, sub: statsTxt(TI[tk].stats) }));
  out.push(T(M, 368, '« Cyclone Furieux » mise à jour.', { size: 13, fill: C.green }), T(M, 386, 'Toucher une pièce pour voir toutes les autres.', { size: 12, fill: C.dim }));
  // aperçu
  const px = 590, py = 206;
  out.push(g('Aperçu', [circle(px, py, 112, { fill: 'none', stroke: C.violet, 'stroke-dasharray': '2 9', opacity: 0.6 }),
    path(`M${px - 118},${py + 40} A124,124 0 0 1 ${px + 20},${py - 122}`, { fill: 'none', stroke: TYPE.Attaque, 'stroke-width': 2.5, opacity: 0.6, 'stroke-linecap': 'round' }),
    Ar.top(d, px, py, 80, me, { aura: true, angle: 0.6 }), label(px, 360, 'Aperçu en rotation', C.dim, { anchor: 'middle' })]));
  const sx = 730;
  out.push(label(sx, 92, 'Type obtenu'), D(sx, 118, 'ATTAQUE', { size: 22, ls: 2, fill: TYPE.Attaque }));
  out.push(U.stats3(d, sx, 134, PW - M - sx, me, { gap: 28 }));
  out.push(g('Coup spécial', [U.card(d, sx, 236, PW - M - sx, 104), glyphBox(d, sx + 10, 248, 40, mv), label(sx + 58, 262, 'Coup spécial'),
    D(sx + 58, 282, MV[mv].name.toUpperCase(), { size: 12, ls: 1 }), T(sx + 58, 299, 'vague', { size: 11.5, fill: C.muted }),
    U.btn(d, sx + 10, 304, PW - M - sx - 20, 30, 'Tous les coups', { icon: 'help', size: 10.5 })]));
  if (o.mine) out.push(U.drawer(d, 'left', 410, 'Mes toupies', (x, y, w) => {
    const els = [];
    [['Cyclone Furieux', 1], ['Forteresse'], ['Éternelle'], ['Juste Milieu'], ['Comète', 0, 'Comète Rouge', true]].forEach(([n, sel, nm, star], i) =>
      els.push(U.topRow(d, x, y + i * 50, w - 10, P[n], { selected: !!sel, h: 44, name: nm, star })));
    els.push(scroll(x + w - 3, y, 240, 0, 0.5), rect(x - 30, PH - 64, w + 50, 64, { fill: '#0E0B30' }),
      U.btn(d, x, PH - 54, (w - 10) / 2, 44, 'Nouvelle', { icon: 'plus', size: 12 }), U.btn(d, x + (w - 10) / 2 + 10, PH - 54, (w - 10) / 2, 44, 'Supprimer', { variant: 'danger', icon: 'trash', size: 12 }));
    return g('Liste', els);
  }, { sub: '8 prédéfinies · 2 à vous' }));
  if (o.part) out.push(U.drawer(d, 'right', 500, 'Roue métallique', (x, y, w) => {
    const els = [], keys = Object.keys(WH).slice(0, 10), cw2 = (w - 14) / 2;
    keys.forEach((k, i) => {
      const rx = x + (i % 2) * (cw2 + 8), ry = y + Math.floor(i / 2) * 56, sel = k === wk;
      els.push(g(`Roue ${WH[k].name}`, [U.card(d, rx, ry, cw2, 50, { selected: sel, accent: TYPE.Attaque }), Ar.wheelArt(d, k, rx + 26, ry + 25, 18),
        D(rx + 50, ry + 22, WH[k].name.toUpperCase(), { size: 11.5, ls: 1 }), T(rx + 50, ry + 39, `${statsTxt(WH[k].stats)} · ${WH[k].spin === 'G' ? 'anti-h.' : 'horaire'}`, { size: 11, fill: C.muted }),
        sel ? L.icon('check', rx + cw2 - 14, ry + 14, 12, C.green) : '']));
    });
    els.push(scroll(x + w + 6, y, 280, 0, 0.7));
    return g('Pièces', els);
  }, { sub: '14 pièces · fixe le sens de rotation' }));
  if (o.stats) out.push(U.drawer(d, 'right', 420, 'Statistiques', (x, y, w) => g('Stats', [radar(d, x + 96, y + 116, 70, st),
    label(x + 200, y + 40, 'Type obtenu'), D(x + 200, y + 64, 'ATTAQUE', { size: 20, ls: 2, fill: TYPE.Attaque }), T(x + 200, y + 84, 'Stat la plus élevée : attaque 14', { size: 12, fill: C.muted }),
    T(x + 200, y + 110, 'Bat l’Endurance, craint la Défense.', { size: 12, fill: C.muted }),
    U.stats3(d, x, y + 214, w - 10, me, { gap: 26 }),
    T(x, y + 316, 'Lame Cyclone : trois crochets d’acier inclinés dans le sens horaire.', { size: 12, fill: C.dim })]), { sub: 'total 23 points' }));
  return out;
}
function builderBoard() {
  return U.board('m04-ecran-atelier', 'Mobile · écran', 'Atelier', 'L’établi occupe l’écran ; vos toupies, les pièces et les statistiques détaillées passent dans des tiroirs.',
    [{ title: 'Établi', draw: d => builderScreen(d), notes: ['Nom, quatre pièces (‹ › pour changer), aperçu, type et coup spécial', '« ? » sur le coup spécial ouvre la page des coups spéciaux'] },
      { title: 'Tiroir : mes toupies', draw: d => builderScreen(d, { mine: true }), notes: ['Prédéfinies et vôtres (étoile) ; toucher une toupie la charge', 'Nouvelle et Supprimer restent en bas du tiroir'] },
      { title: 'Tiroir : choisir une pièce', draw: d => builderScreen(d, { part: true }), notes: ['Toutes les pièces de l’emplacement, illustrées, avec leurs stats', 'La roue fixe le sens de rotation de la toupie'] },
      { title: 'Tiroir : statistiques', draw: d => builderScreen(d, { stats: true }), notes: ['Radar, type obtenu, barres et description de la roue', 'Chaque toupie totalise 23 points : pas de jauge de budget'] }]);
}

// ============================== M05 COUPS SPÉCIAUX ==============================
function movesScreen(d, sel = 'onde') {
  const out = [U.screenBg(d), U.topBar(d, 'Coups spéciaux', { sub: 'une fois par partie · coûte ¼ de l’endurance', backLabel: 'Atelier' })];
  Object.keys(MV).forEach((k, i) => {
    const y = 62 + i * 48, on = k === sel;
    out.push(g(`Coup ${MV[k].name}`, [U.card(d, M, y, 250, 42, { selected: on, accent: MV[k].color }), glyphBox(d, M + 6, y + 5, 32, k, on),
      D(M + 48, y + 26, MV[k].name.toUpperCase(), { size: 11.5, ls: 1.1, fill: on ? C.text : C.muted }), T(M + 242, y + 26, MV[k].form === 'ANNEAU' ? 'anneau' : 'vague', { size: 11, fill: C.dim, anchor: 'end' })]));
  });
  const mv = MV[sel], x = M + 264, w = PW - M - x, y = 62;
  out.push(U.panel(d, x, y, w, 342, { accent: mv.color }));
  out.push(path(chamfer(x + 10, y + 10, w - 20, 146, [10, 0, 10, 0]), { fill: '#07061A' }), `<g clip-path="${d.clip(rect(x + 10, y + 10, w - 20, 146))}">${Ar.moveFx(d, sel, x + 10, y + 10, w - 20, 146)}</g>`);
  out.push(glyphBox(d, x + 14, y + 168, 46, sel), label(x + 72, y + 182, `Boulon ${mv.bolt}`), D(x + 72, y + 208, mv.name.toUpperCase(), { size: 19, ls: 2 }));
  out.push(U.badge(d, x + w - 92, y + 170, mv.color, { label: mv.form.toLowerCase() === 'anneau' ? 'Anneau' : 'Vague' }));
  out.push(path(skew(x + 14, y + 230, 7, 12, 3), { fill: C.green }), T(x + 28, y + 241, mv.plus, { size: 13 }));
  out.push(path(skew(x + 14, y + 252, 7, 12, 3), { fill: C.danger }), T(x + 28, y + 263, mv.minus, { size: 13, fill: C.muted }));
  const gw = (w - 48) / 3;
  [['Dégâts', mv.dmg, 24, TYPE.Attaque], ['Recul', mv.kb, 5, C.cyan], ['Portée', mv.range, 4, C.violet]].forEach(([n, v, m, c], j) =>
    out.push(U.statRow(d, x + 14 + j * (gw + 10), y + 288, gw, n, Math.round(v / m * 8), c, 8, String(v).replace('.', ','))));
  out.push(T(x + 14, y + 330, 'Coûte ¼ de l’endurance du moment · une fois par partie', { size: 11.5, fill: C.dim }));
  return out;
}
function movesBoard() {
  return U.board('m05-ecran-coups-speciaux', 'Mobile · écran', 'Coups spéciaux', 'Ouverte depuis l’atelier : la liste des sept coups à gauche, la fiche du coup choisi à droite.',
    [{ title: 'Onde de choc', draw: d => movesScreen(d, 'onde'), notes: ['Illustration de l’effet, point fort, point faible, jauges', 'Retour (ou bouton retour d’Android) : retour à l’atelier tel qu’il était'] },
      { title: 'Vortex', draw: d => movesScreen(d, 'vortex'), notes: ['La fiche change sans quitter la page', 'Anneau : touche tout le monde autour du lanceur'] }]);
}

if (require.main === module) for (const b of [principles, components, menuBoard, setupBoard, builderBoard, movesBoard]) b().save(OUT);
module.exports = { principles, components, menuBoard, setupBoard, builderBoard, movesBoard };
