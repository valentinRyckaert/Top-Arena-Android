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


// ============================== M06 COMBAT ==============================
const ARENA = { cx: PW / 2, cy: PH / 2 + 4, R: 178 };
const at = (u, v) => [ARENA.cx + u * ARENA.R, ARENA.cy + v * ARENA.R];
function arena(d, key = 'classique') { return g('Arène', [circle(ARENA.cx, ARENA.cy, ARENA.R * 1.12, { fill: STADCOL(key), opacity: 0.12, filter: d.blur(30) }), Ar.stadium(d, key, ARENA.cx, ARENA.cy, ARENA.R)]); }
const STADCOL = key => U.STAD[key].color;
function tag(x, y, txt, col) {
  const w = tw(txt, 10, true, 1.2) + 16;
  return g(`Étiquette ${txt}`, [path(skew(x - w / 2, y - 16, w, 16, 5), { fill: col }), path(`M${x - 4},${y} l4,5 l4,-5 Z`, { fill: col }), D(x, y - 4.5, txt, { size: 10, ls: 1.2, fill: '#03140A', anchor: 'middle' })]);
}
function spinner(d, u, v, p, o = {}) {
  const [x, y] = at(u, v), out = [];
  if (o.trail) out.push(path(`M${x + o.trail[0]},${y + o.trail[1]} Q${x + o.trail[0] * 0.4},${y + o.trail[1] * 0.6} ${x},${y}`, { fill: 'none', stroke: o.col || C.green, 'stroke-width': 7, opacity: 0.35, 'stroke-linecap': 'round', filter: d.blur(2) }));
  out.push(Ar.top(d, x, y, o.R || 20, p, { angle: o.angle ?? 0.5, aura: o.aura }));
  if (o.tag) out.push(tag(x, y - (o.R || 20) - 6, o.tag, o.col || C.green));
  if (o.out) out.push(circle(x, y, (o.R || 20) + 4, { fill: '#000', opacity: 0.55 }));
  return g(`Toupie ${p.name}`, out);
}
function hudCard(d, x, y, w, who, p, pct, o = {}) {
  const col = o.col || C.green, h = o.h || 74, out = [U.card(d, x, y, w, h, { selected: o.me, accent: col, fill: '#0D0B2E' })];
  out.push(Ar.top(d, x + 22, y + 24, 14, p, { shadow: false, angle: 0.5 }));
  if (!o.out) out.push(D(x + 44, y + 22, who.toUpperCase(), { size: 12.5, ls: 1.2, fill: col }), T(x + 44, y + 37, o.sub || p.name, { size: 11, fill: C.muted }));
  if (!o.out) out.push(D(x + w - 10, y + 26, String(pct), { size: 18, anchor: 'end' }));
  out.push(U.endurance(d, x + 10, y + 46, w - 20, o.out ? 0 : pct, pct < 30 ? C.danger : C.green, { h: 8, color2: pct < 30 ? C.warn : undefined }));
  if (h > 64) out.push(T(x + 10, y + h - 7, o.status || '', { size: 11, fill: o.statusCol || C.muted }));
  if (o.out) out.push(rect(x, y, w, h, { fill: '#05040F', opacity: 0.45 }), D(x + 44, y + 22, `${who} · ${o.out}`.toUpperCase(), { size: 12, fill: C.danger, ls: 1.2 }), T(x + 44, y + 37, p.name, { size: 11, fill: C.dim }));
  return g(`Fiche ${who}`, out);
}
function hudTop(d, o = {}) {
  const out = [U.iconBtn(d, M, 8, 44, 40, 'pause', { on: o.paused, label: 'Menu du combat' })];
  out.push(D(PW - M, 30, o.clock || '0:42', { size: 22, anchor: 'end' }), T(PW - M, 46, o.stad || 'CLASSIQUE · NOYAU HOLO', { size: 10, fill: C.cyan, anchor: 'end', weight: 700, ls: 1 }));
  if (o.round) out.push(g('Manche', [path(chamfer(PW / 2 - 70, 0, 140, 34, [0, 0, 12, 12]), { fill: '#0D0B2E', stroke: C.line }), label(PW / 2, 14, o.round, C.muted, { anchor: 'middle' }),
    D(PW / 2 - 14, 30, String(o.score[0]), { size: 14, fill: C.green, anchor: 'end' }), D(PW / 2, 30, '–', { size: 14, fill: C.dim, anchor: 'middle' }), D(PW / 2 + 14, 30, String(o.score[1]), { size: 14, fill: C.danger })]));
  return g('Haut du combat', out);
}
function battleScreen(d, o = {}) {
  const me = P['Cyclone Furieux'], bot = P['Forteresse'], scheme = o.scheme || 'buttons', out = [U.screenBg(d, { floor: false, cx: 0.5, cy: 0.5 }), arena(d, o.stadium)];
  const cw = 196, lx = M, rx = PW - M - cw;
  if (o.melee) {
    out.push(spinner(d, -0.25, -0.15, me, { tag: 'VOUS', trail: [-40, 30], angle: 0.2 }), spinner(d, 0.3, -0.35, bot, { tag: 'BOT 1', col: C.danger, angle: 1 }),
      spinner(d, 0.15, 0.4, P['Éternelle'], { tag: 'BOT 2', col: C.warn, trail: [40, 20], col2: C.warn }), spinner(d, 0.92, 0.55, P['Comète'], { out: true }));
    out.push(hudTop(d, { clock: '1:07' }));
    out.push(hudCard(d, lx, 56, cw, 'Vous', me, 61, { me: true, h: 62 }), hudCard(d, lx, 124, cw, 'Bot 2', P['Éternelle'], 74, { col: C.warn, h: 62 }));
    out.push(hudCard(d, rx, 56, cw, 'Bot 1', bot, 38, { col: C.danger, h: 62 }), hudCard(d, rx, 124, cw, 'Bot 3', P['Comète'], 0, { col: C.magenta, h: 62, out: 'éjectée · 4e' }));
  } else {
    if (o.aiming) {
      const [x, y] = at(-0.5, 0);
      out.push(Ar.top(d, x, y, 20, me, { angle: 0.4 }), tag(x, y - 26, 'VOUS', C.green), Ar.top(d, ...at(0.5, 0), 20, bot, { angle: 1 }), tag(...[at(0.5, 0)[0], at(0.5, 0)[1] - 26], 'BOT', C.danger));
      out.push(path(`M${x + 24},${y - 6} L${x + 150},${y - 60}`, { stroke: C.green, 'stroke-width': 3, 'stroke-dasharray': '7 6' }), path(`M${x + 150},${y - 60} l-14,-1 l7,12 Z`, { fill: C.green }),
        circle(x + 160, y - 64, 18, { fill: '#FFFFFF', opacity: 0.12, stroke: '#FFFFFF', 'stroke-opacity': 0.5 }), L.icon('hand', x + 168, y - 52, 26, '#FFFFFF'),
        D(x + 96, y - 50, 'FORCE 72 %', { size: 11, fill: C.green, transform: `rotate(-23 ${x + 96} ${y - 50})` }));
      out.push(g('Consigne', [path(skew(PW / 2 - 190, 58, 380, 30, 10), { fill: '#05040F', 'fill-opacity': 0.75, stroke: C.cyan, 'stroke-opacity': 0.6 }),
        T(PW / 2, 78, 'Glissez depuis votre toupie, relâchez pour lancer', { size: 13, anchor: 'middle' })]));
    } else {
      out.push(spinner(d, -0.18, -0.2, me, { tag: 'VOUS', trail: [-60, 40], angle: 0.2 }), spinner(d, 0.12, -0.05, bot, { tag: 'BOT', col: C.danger, angle: 1.2 }));
      out.push(...[0, 1, 2, 3, 4].map(i => { const [x, y] = at(-0.03, -0.13), a = i * 1.2; return line(x + Math.cos(a) * 6, y + Math.sin(a) * 6, x + Math.cos(a) * 18, y + Math.sin(a) * 18, { stroke: i % 2 ? C.warn : '#FFFFFF', 'stroke-width': 1.5 }); }));
    }
    out.push(hudTop(d, { clock: o.aiming ? '0:00' : '0:42', paused: o.pause, round: o.round, score: o.score }));
    out.push(hudCard(d, lx, 56, cw, 'Vous', me, o.aiming ? 100 : 72, { me: true, status: o.aiming ? 'Vise…' : 'Dash prêt · spécial prêt', statusCol: o.aiming ? C.muted : C.green }));
    out.push(hudCard(d, rx, 56, cw, 'Bot · Normal', bot, o.aiming ? 100 : 41, { col: C.danger, status: o.aiming ? 'Prêt à lancer' : 'Onde de choc prête' }));
  }
  if (!o.aiming) {
    if (scheme === 'buttons') out.push(U.joystick(d, M + 62, PH - 70, 52, -0.35, -0.3));
    if (scheme === 'tilt') out.push(g('Inclinaison', [circle(M + 62, PH - 70, 52, { fill: '#0A0826', 'fill-opacity': 0.7, stroke: C.lineHi }),
      line(M + 10, PH - 70, M + 114, PH - 70, { stroke: C.line }), line(M + 62, PH - 122, M + 62, PH - 18, { stroke: C.line }),
      circle(M + 62, PH - 70, 22, { fill: 'none', stroke: C.line, 'stroke-dasharray': '3 5' }), circle(M + 84, PH - 84, 12, { fill: C.green, stroke: '#B9FFD6' }),
      `<g transform="rotate(-18 ${M + 150} ${PH - 92})">${L.icon('phone', M + 150, PH - 92, 30, C.cyan)}</g>`, U.btn(d, M + 124, PH - 54, 92, 34, 'Recalibrer', { variant: 'ghost', size: 10 })]));
    if (scheme === 'gestures') out.push(g('Gestes', [U.chip(d, M, PH - 84, 'Glisser · piloter', C.cyan, 'hand'), U.chip(d, M, PH - 52, 'Toucher · dash', C.green, 'dash'),
      U.chip(d, PW - M - U.chipW('Appui long · spécial', 1), PH - 52, 'Appui long · spécial', C.magenta, 'bolt'),
      circle(220, 300, 26, { fill: '#FFFFFF', opacity: 0.1, stroke: '#FFFFFF', 'stroke-opacity': 0.4 }), path('M160,330 Q190,310 214,304', { fill: 'none', stroke: '#FFFFFF', 'stroke-width': 3, opacity: 0.35, 'stroke-linecap': 'round' })]));
    if (scheme !== 'gestures') out.push(U.roundAction(d, PW - M - 50, PH - 62, 46, o.duel ? 'Appui' : 'Dash', { icon: 'dash', ring: o.cooldown }),
      U.roundAction(d, PW - M - 148, PH - 44, 34, 'Spécial', { glyph: 'lance', color: C.magenta, size: 10 }));
  }
  if (o.tutorial) out.push(g('Tutoriel', [rect(0, 0, PW, PH, { fill: '#03020A', opacity: 0.5 }), circle(PW - M - 50, PH - 62, 56, { fill: 'none', stroke: C.cyan, 'stroke-width': 2.5, 'stroke-dasharray': '6 5' }),
    U.roundAction(d, PW - M - 50, PH - 62, 46, 'Dash', { icon: 'dash' }),
    path(chamfer(PW - M - 410, 196, 330, 104, [14, 0, 14, 0]), { fill: '#0B0930', stroke: C.cyan, 'stroke-width': 1.4 }), rect(PW - M - 410, 210, 3, 32, { fill: C.cyan }),
    D(PW - M - 394, 220, 'DASH', { size: 13, ls: 2, fill: C.cyan }), T(PW - M - 92, 220, '3 / 7', { size: 11, fill: C.muted, anchor: 'end', font: F.display, weight: 700 }),
    T(PW - M - 394, 244, 'L’adversaire est proche : touchez DASH pour', { size: 13 }), T(PW - M - 394, 262, 'foncer sur lui, avec plus d’attaque.', { size: 13 }),
    path(skew(PW - M - 394, 274, 120, 18, 5), { fill: C.cyan, 'fill-opacity': 0.15, stroke: C.cyan }), D(PW - M - 334, 287, 'JEU EN PAUSE', { size: 9.5, fill: C.cyan, anchor: 'middle', ls: 1.4 }),
    path(`M${PW - M - 120},300 l18,20 l18,-20 Z`, { fill: '#0B0930', stroke: C.cyan, 'stroke-width': 1.4 }), rect(PW - M - 119, 298, 34, 4, { fill: '#0B0930' })]));
  if (o.pause) out.push(U.drawer(d, 'left', 380, 'Menu du combat', (x, y, w) => g('Pause', [
    U.btn(d, x, y, w - 10, 50, 'Reprendre', { variant: 'primary', icon: 'play' }), U.btn(d, x, y + 60, w - 10, 44, 'Changer de toupies', { icon: 'build', size: 12.5 }),
    U.seg(d, x, y + 132, w - 10, ['Joystick', 'Inclinaison', 'Gestes'], 0, { label: 'Commandes', h: 34, size: 10.5 }),
    U.btn(d, x, y + 184, (w - 20) / 2, 40, 'Aide', { variant: 'ghost', icon: 'help', size: 12 }), U.btn(d, x + (w - 20) / 2 + 10, y + 184, (w - 20) / 2, 40, 'Règles', { variant: 'ghost', icon: 'sliders', size: 12 }),
    T(x, y + 248, 'Solo : le combat est en pause.', { size: 12, fill: C.muted }), T(x, y + 264, 'En réseau, il continue pendant que ce menu est ouvert.', { size: 12, fill: C.dim }),
    U.btn(d, x, PH - 54, w - 10, 44, 'Quitter le combat', { variant: 'danger', icon: 'quit', size: 12.5 })]), { sub: 'solo · en pause', accent: C.cyan }));
  return out;
}
function battleBoard() {
  return U.board('m06-ecran-combat', 'Mobile · écran', 'Combat', 'L’arène carrée au centre, les fiches de chaque côté, les commandes sous les pouces. Trois schémas de commandes au choix.',
    [{ title: 'Visée', draw: d => battleScreen(d, { aiming: true }), notes: ['Glisser depuis sa toupie : direction et longueur de la flèche = force', 'Relâcher lance la toupie ; pas de bouton à viser'] },
      { title: 'Combat · joystick et boutons', draw: d => battleScreen(d), notes: ['Joystick sous le pouce gauche, DASH et SPÉCIAL sous le pouce droit', 'Le contour de DASH se remplit pendant la recharge'] },
      { title: 'Mêlée à 4 · gestes', draw: d => battleScreen(d, { melee: true, scheme: 'gestures' }), notes: ['Deux fiches de chaque côté ; une toupie éjectée garde sa place grisée', 'Gestes : glisser pour piloter, toucher pour dasher, appui long : spécial'] },
      { title: 'Menu du combat', draw: d => battleScreen(d, { pause: true }), notes: ['Le bouton pause remplace « Menu » et « Changer de toupies » du PC', 'On peut changer de schéma de commandes sans quitter le combat'] }]);
}

// ============================== M07 DUELS ET TUTORIEL ==============================
function duelScreen(d, mode) {
  const me = P['Cyclone Furieux'], bot = P['Forteresse'], out = [U.screenBg(d, { floor: false }), arena(d)];
  out.push(rect(0, 0, PW, PH, { fill: '#03020A', opacity: 0.45 }));
  const cx = PW / 2, cy = 160;
  out.push(Ar.top(d, cx - 30, cy, 34, me, { aura: true, angle: 0.3 }), Ar.top(d, cx + 30, cy, 34, bot, { aura: true, angle: 1 }),
    ...[...Array(10)].map((_, i) => { const a = i * 0.63; return line(cx + Math.cos(a) * 6, cy + Math.sin(a) * 6, cx + Math.cos(a) * (30 + (i % 3) * 10), cy + Math.sin(a) * (30 + (i % 3) * 10), { stroke: i % 2 ? C.warn : '#FFFFFF', 'stroke-width': 1.8 }); }));
  out.push(g('Bannière duel', [path(skew(cx - 150, 48, 300, 46, 16), { fill: d.lin([[0, C.violet, 0], [0.2, C.violet, 0.45], [0.8, C.magenta, 0.45], [1, C.magenta, 0]], 0) }),
    D(cx, 82, mode === 'rush' ? 'DUEL · RAFALE' : 'DUEL !', { size: 28, anchor: 'middle', italic: true, ls: 3 })]));
  out.push(hudCard(d, M, 56, 170, 'Vous', me, 64, { me: true, h: 62 }), hudCard(d, PW - M - 170, 56, 170, 'Bot', bot, 47, { col: C.danger, h: 62 }));
  const bx = 150, bw = PW - 2 * bx - 30;
  if (mode === 'rush') {
    const m = bx + bw * 0.62;
    out.push(g('Corde', [path(skew(bx, 262, bw, 30, 10), { fill: '#07061A', stroke: C.line }), path(skew(bx, 262, m - bx, 30, 10), { fill: d.lin([[0, C.green, 0.9], [1, C.green, 0.35]], 0) }),
      path(skew(m, 262, bx + bw - m, 30, 10), { fill: d.lin([[0, C.danger, 0.35], [1, C.danger, 0.9]], 0) }), line(bx + bw / 2, 254, bx + bw / 2, 300, { stroke: '#FFFFFF', 'stroke-dasharray': '3 3', opacity: 0.6 }),
      path(`M${m - 9},248 h18 l-9,12 Z`, { fill: '#FFFFFF' }), rect(m - 2, 258, 4, 38, { fill: '#FFFFFF' }),
      D(bx, 244, 'VOUS 9', { size: 13, fill: C.green }), D(bx + bw, 244, '7 BOT', { size: 13, fill: C.danger, anchor: 'end' }),
      D(cx - 15, 244, '2,1 S', { size: 13, anchor: 'middle', fill: C.warn }),
      T(cx - 15, 322, 'Touchez le plus vite possible : 12 appuis d’avance gagnent aussitôt', { size: 13, anchor: 'middle' }),
      T(cx - 15, 340, 'Au bout de 3,5 s, le plus avancé gagne · pile au milieu : nul', { size: 12, anchor: 'middle', fill: C.muted })]));
  } else {
    const bar = (y, h, zone, cur, small) => g(small ? 'Barre adverse' : 'Votre barre', [path(skew(bx, y, bw, h, h * 0.35), { fill: d.lin([[0, '#5A1030'], [0.5, '#C2335A'], [1, '#5A1030']], 0), opacity: small ? 0.7 : 1 }),
      path(skew(bx + bw * (0.5 - zone), y, bw * zone * 2, h, h * 0.35), { fill: d.lin([[0, C.green], [1, C.lime]], 0) }), path(`M${bx + bw * cur - 3},${y - 6} h6 l-1,${h + 12} h-4 Z`, { fill: '#FFFFFF' })]);
    out.push(label(bx, 232, 'Bot', C.danger), T(bx + bw, 232, 'série 1', { size: 10.5, fill: C.muted, anchor: 'end' }), bar(238, 12, 0.13, 0.66, true));
    out.push(bar(268, 28, 0.17, 0.47), T(cx - 15, 322, 'Touchez l’écran quand le curseur est dans le vert', { size: 13, anchor: 'middle' }),
      label(bx, 344, 'Série à taille minimale'), ...[0, 1, 2].map(i => path(`M${bx + 146 + i * 18},337 l6,-6 l6,6 l-6,6 Z`, { fill: i < 2 ? C.green : 'none', stroke: C.green })),
      T(bx + bw, 344, 'Le premier qui rate subit 1,5× les dégâts du dash', { size: 11, fill: C.muted, anchor: 'end' }));
  }
  out.push(U.roundAction(d, PW - M - 50, PH - 62, 46, 'Appui', { icon: 'hand', color: C.cyan }), T(PW - M - 50, PH - 6, 'ou n’importe où', { size: 10, fill: C.dim, anchor: 'middle' }));
  return out;
}
function duelBoard() {
  return U.board('m07-ecran-duel-tutoriel', 'Mobile · écran', 'Duels, tutoriel, inclinaison', 'Deux dashs qui se croisent déclenchent le duel choisi ; tout l’écran sert alors de bouton.',
    [{ title: 'Duel · timing', draw: d => duelScreen(d, 'timing'), notes: ['Toucher quand le curseur traverse la zone verte, qui rétrécit', 'Le premier qui rate perd le duel'] },
      { title: 'Duel · rafale', draw: d => duelScreen(d, 'rush'), notes: ['Tir à la corde : chaque toucher pousse le curseur vers l’adversaire', 'Le type de duel se choisit dans les Paramètres ou les règles'] },
      { title: 'Tutoriel', draw: d => battleScreen(d, { tutorial: true }), notes: ['Une bulle par action, le jeu attend le geste présenté', 'Le bouton concerné est mis en avant, le reste est assombri'] },
      { title: 'Commandes · inclinaison', draw: d => battleScreen(d, { scheme: 'tilt', stadium: 'glace', round: 'Manche 2', score: [1, 0] }), notes: ['Incliner le téléphone pour piloter ; la position au lancement sert de neutre', 'Recalibrer reprend la position actuelle comme neutre'] }]);
}

// ============================== M08 RÉSULTAT ==============================
function resultScreen(d, kind) {
  const me = P['Cyclone Furieux'], out = [U.screenBg(d, { floor: false }), arena(d), rect(0, 0, PW, PH, { fill: '#03020A', opacity: 0.72 })];
  const win = kind === 'victory' || kind === 'round', col = win ? C.green : C.danger;
  out.push(...[...Array(12)].map((_, i) => { const a = -Math.PI / 2 + (i - 5.5) * 0.18; return path(`M${200 + Math.cos(a) * 60},${210 + Math.sin(a) * 60} L${200 + Math.cos(a - 0.05) * 260},${210 + Math.sin(a - 0.05) * 260} L${200 + Math.cos(a + 0.05) * 260},${210 + Math.sin(a + 0.05) * 260} Z`, { fill: col, opacity: 0.07 }); }));
  out.push(Ar.top(d, 200, 196, 84, kind === 'lan' ? P['Forteresse'] : me, { aura: true, angle: 0.4 }));
  out.push(D(200, 318, (kind === 'lan' ? 'Forteresse' : me.name).toUpperCase(), { size: 15, ls: 1.6, anchor: 'middle' }), T(200, 336, kind === 'lan' ? 'Bastion · Carapace · Pivot · Tempête' : 'Lame Cyclone · Griffe · Gomme · Perçant', { size: 11.5, fill: C.muted, anchor: 'middle' }));
  const x = 400, title = { victory: 'VICTOIRE !', melee: '2E PLACE', round: 'MANCHE GAGNÉE', lan: 'DÉFAITE…' }[kind];
  out.push(D(x, 96, title, { size: 38, ls: 3, italic: true, fill: kind === 'melee' ? C.warn : win ? '#FFFFFF' : C.danger }));
  const reason = { victory: 'Éjection hors du stadium', melee: 'Mêlée à 4 · Bot 2 l’emporte', round: 'Plus d’endurance · Kira', lan: 'Éjectée par Léa' }[kind];
  out.push(path(skew(x, 108, tw(reason.toUpperCase(), 11, true, 1.4) + 40, 24, 8), { fill: d.lin([[0, col], [1, kind === 'melee' ? C.warn : win ? C.lime : C.warn]], 0) }), D(x + 20, 125, reason.toUpperCase(), { size: 11, ls: 1.4, fill: '#03140A' }));
  if (kind === 'melee') {
    [['1', 'Bot 2', P['Éternelle'], C.warn], ['2', 'Vous', me, C.green], ['3', 'Bot 1', P['Forteresse'], C.danger], ['4', 'Bot 3', P['Comète'], C.magenta]].forEach(([n, who, p, c], i) =>
      out.push(g(`Rang ${n}`, [U.card(d, x, 146 + i * 42, 300, 36, { selected: who === 'Vous', accent: C.green }), D(x + 16, 146 + i * 42 + 24, n, { size: 15, fill: i === 0 ? C.warn : C.muted }),
        Ar.top(d, x + 48, 146 + i * 42 + 18, 12, p, { shadow: false }), D(x + 68, 146 + i * 42 + 23, who.toUpperCase(), { size: 12, fill: c }), T(x + 290, 146 + i * 42 + 23, p.name, { size: 11, fill: C.muted, anchor: 'end' })])));
  } else {
    const stats = kind === 'round' ? [['Score', '1 – 0'], ['Série', 'à 2'], ['Chocs', '9'], ['Endurance', '38']] : [['Durée', '00:18'], ['Chocs', '12'], ['Dashs', '4'], ['Endurance', '46']];
    out.push(U.card(d, x, 146, PW - M - x, 66, { fill: '#100D35' }));
    stats.forEach(([n, v], i) => { const sx = x + 16 + i * ((PW - M - x - 16) / 4); out.push(label(sx, 166, n), D(sx, 196, v, { size: 20, fill: i === 0 && kind === 'round' ? C.green : i === 3 ? C.green : C.text })); });
    out.push(T(x, 236, kind === 'round' ? 'Demi-finale · premier à 2 manches · stadium Glace' : kind === 'lan' ? 'Lobby « Salon de Léa » · 4 joueurs' : 'Victoires 4 · Défaites 1 · Nuls 0', { size: 13, fill: C.muted }));
  }
  const by = PH - 70, bw = PW - M - x;
  if (kind === 'victory') out.push(U.btn(d, x, by, 190, 52, 'Revanche', { variant: 'primary', icon: 'update' }), U.btn(d, x + 200, by, bw - 200 - 110, 52, 'Changer de toupies', { size: 12.5 }), U.btn(d, PW - M - 100, by, 100, 52, 'Menu', { variant: 'ghost' }));
  if (kind === 'melee') out.push(U.btn(d, x, by, 190, 52, 'Revanche', { variant: 'primary', icon: 'update' }), U.btn(d, x + 200, by, bw - 310, 52, 'Changer de toupies', { size: 12.5 }), U.btn(d, PW - M - 100, by, 100, 52, 'Menu', { variant: 'ghost' }));
  if (kind === 'round') out.push(U.btn(d, x, by, bw - 170, 52, 'Manche suivante', { variant: 'primary', icon: 'next' }), U.btn(d, PW - M - 160, by, 160, 52, 'Quitter', { variant: 'ghost', icon: 'save', size: 12.5 }));
  if (kind === 'lan') out.push(g('Attente', [U.card(d, x, by - 4, bw, 56), L.icon('lan', x + 26, by + 24, 20, C.cyan), T(x + 48, by + 20, 'L’hôte ramène tout le monde au lobby…', { size: 13.5 }),
    T(x + 48, by + 38, 'Quitter : bouton retour d’Android', { size: 11.5, fill: C.muted })]));
  return out;
}
function resultBoard() {
  return U.board('m08-ecran-resultat', 'Mobile · écran', 'Résultat', 'Titre sans effet chromatique (comme sur PC), toupie gagnante à gauche, bilan et actions à droite.',
    [{ title: 'Victoire en solo', draw: d => resultScreen(d, 'victory'), notes: ['Revanche sous le pouce droit, changer de toupies, menu', 'Bilan : durée, chocs, dashs, endurance ; série de victoires'] },
      { title: 'Mêlée · classement', draw: d => resultScreen(d, 'melee'), notes: ['En mêlée, le classement complet remplace le bilan', 'Toupie éjectée : rang selon l’ordre d’élimination'] },
      { title: 'Manche de tournoi', draw: d => resultScreen(d, 'round'), notes: ['Score de la série, puis manche suivante', 'Quitter propose de sauvegarder le tournoi solo'] },
      { title: 'Partie en réseau', draw: d => resultScreen(d, 'lan'), notes: ['Chez un client : l’hôte ramène tout le monde au lobby', 'Chez l’hôte : bouton « Retour au lobby » à la place'] }]);
}


// ============================== M09 RÉSEAU LOCAL ==============================
function lobbyRow(d, x, y, w, l) {
  const out = [U.card(d, x, y, w, 56, { fill: l.inGame ? '#0B0928' : undefined })];
  out.push(Ar.stadium(d, l.stad, x + 30, y + 28, 20));
  out.push(D(x + 60, y + 24, l.name.toUpperCase(), { size: 13, ls: 1.2, fill: l.inGame ? C.dim : C.text }), l.tour ? L.icon('trophy', x + 66 + tw(l.name.toUpperCase(), 13, true, 1.2), y + 19, 14, C.warn) : '');
  out.push(T(x + 60, y + 42, `${l.host} · ${l.addr}`, { size: 11.5, fill: C.muted }));
  for (let i = 0; i < l.max; i++) out.push(path(skew(x + w - 250 + i * 16, y + 22, 12, 12, 3), { fill: i < l.n ? C.cyan : 'none', stroke: C.cyan, 'stroke-opacity': 0.7 }));
  out.push(T(x + w - 250, y + 46, l.inGame ? 'en partie' : `${l.n}/${l.max} joueurs`, { size: 11, fill: l.inGame ? C.warn : C.muted }));
  out.push(l.inGame ? T(x + w - 14, y + 33, 'complet', { size: 12, fill: C.dim, anchor: 'end' }) : U.btn(d, x + w - 124, y + 10, 112, 36, 'Rejoindre', { size: 11.5, variant: l.sel ? 'primary' : 'secondary' }));
  return g(`Lobby ${l.name}`, out);
}
function lanScreen(d, o = {}) {
  const out = [U.screenBg(d), U.topBar(d, 'Multijoueur', { sub: 'réseau local', actions: [{ label: 'Créer un lobby', icon: 'plus', variant: 'primary' }, { icon: 'update' }] })];
  out.push(U.chip(d, M, 62, 'Pseudo : valentin', C.green, 'user'), T(M + U.chipW('Pseudo : valentin', 1) + 10, 79, 'modifiable dans les Paramètres', { size: 11.5, fill: C.dim }));
  out.push(g('Recherche', [circle(PW - M - 8, 74, 4, { fill: C.green }), circle(PW - M - 8, 74, 9, { fill: 'none', stroke: C.green, opacity: 0.5 }), T(PW - M - 22, 78, 'Recherche en continu · annonces UDP et balayage du Wi-Fi', { size: 11.5, fill: C.muted, anchor: 'end' })]));
  const lobbies = [{ name: 'Arène Nova', host: 'Nova', addr: '192.168.1.24', stad: 'volcan', n: 3, max: 4, sel: true }, { name: 'Coupe du soir', host: 'Kaï', addr: '192.168.1.31', stad: 'glace', n: 2, max: 4, tour: true },
    { name: 'Salon de Léa', host: 'Léa (PC)', addr: '192.168.1.40', stad: 'beton', n: 2, max: 2, inGame: true }];
  lobbies.forEach((l, i) => out.push(lobbyRow(d, M, 96 + i * 62, PW - 2 * M, l)));
  out.push(g('Rejoindre par adresse', [rect(0, PH - 66, PW, 66, { fill: '#07061A', opacity: 0.85 }), line(M, PH - 66, PW - M, PH - 66, { stroke: C.line }),
    field(d, M, PH - 54, PW - 2 * M - 150, 'ex. « Lobby de Léa » ou 192.168.1.20:45455', { placeholder: true }), U.btn(d, PW - M - 140, PH - 54, 140, 40, 'Rejoindre', { size: 12 })]));
  if (o.create) out.push(U.drawer(d, 'right', 440, 'Créer un lobby', (x, y, w) => g('Formulaire', [field(d, x, y + 14, w - 10, 'Arène Nova', { label: 'Nom du lobby', focus: true }),
    U.seg(d, x, y + 82, w - 10, ['2', '3', '4'], 2, { label: 'Joueurs', h: 32 }), label(x, y + 140, 'Stadium'),
    ...['classique', 'beton', 'terre', 'glace', 'sable', 'volcan'].map((k, i) => U.stadiumChip(d, x + (i % 2) * ((w - 10) / 2 + 4), y + 148 + Math.floor(i / 2) * 50, (w - 10) / 2 - 4, k, { h: 44, selected: k === 'volcan' })),
    rect(x - 22, PH - 64, w + 44, 64, { fill: '#0E0B30' }), U.btn(d, x, PH - 54, w - 10, 44, 'Créer et attendre les joueurs', { variant: 'primary', icon: 'play', size: 12.5 })]), { sub: 'visible sur le Wi-Fi' }));
  return out;
}
function lobbyScreen(d, o = {}) {
  const out = [U.screenBg(d), U.topBar(d, 'Lobby · Arène Nova', { backIcon: 'quit', actions: [{ label: o.client ? 'Je suis prêt' : 'Lancer', icon: o.client ? 'check' : 'play', variant: 'primary' }, { icon: 'sliders', on: o.settings }] })];
  out.push(U.chip(d, M, 62, o.client ? 'Client' : 'Hôte', C.green, 'star'), U.chip(d, M + 78, 62, '192.168.1.24 · visible en LAN', C.cyan, 'wifi'));
  const slots = [{ who: 'Nova', sub: 'vous · hôte', p: P['Cyclone Furieux'], st: 'Prêt', col: C.green }, { who: 'Kaï', sub: '192.168.1.31', p: P['Colosse'], st: 'Prêt', col: C.green },
    { who: 'Mira', sub: '192.168.1.40', p: P['Éternelle'], st: 'Choisit sa toupie…', col: C.warn }, null];
  const cw = (PW - 2 * M - 10) / 2;
  slots.forEach((sl, i) => {
    const x = M + (i % 2) * (cw + 10), y = 98 + Math.floor(i / 2) * 104;
    if (!sl) { out.push(g('Emplacement libre', [path(chamfer(x, y, cw, 96, [10, 0, 10, 0]), { fill: 'none', stroke: C.line, 'stroke-dasharray': '6 6' }), D(x + cw / 2, y + 46, 'EMPLACEMENT LIBRE', { size: 12, ls: 2, fill: C.dim, anchor: 'middle' }), T(x + cw / 2, y + 66, 'en attente d’un joueur sur le réseau local…', { size: 11.5, fill: C.dim, anchor: 'middle' })])); return; }
    out.push(g(`Joueur ${sl.who}`, [U.card(d, x, y, cw, 96, { selected: i === 0 }), label(x + 12, y + 18, `J${i + 1}`, C.dim), Ar.top(d, x + 50, y + 54, 30, sl.p, { angle: 0.6, shadow: false }),
      D(x + 92, y + 36, sl.who.toUpperCase(), { size: 15, ls: 1.4 }), T(x + 92, y + 53, sl.sub, { size: 11.5, fill: C.muted }), D(x + 92, y + 76, sl.p.name.toUpperCase(), { size: 11, ls: 1 }), U.badge(d, x + 96 + tw(sl.p.name.toUpperCase(), 11, true, 1), y + 64, sl.p.type, { h: 14, size: 8 }),
      path(skew(x + cw - 146, y + 14, 134, 26, 8), { fill: sl.col, 'fill-opacity': 0.14, stroke: sl.col }), D(x + cw - 79, y + 31, sl.st.toUpperCase(), { size: 9, fill: sl.col, anchor: 'middle', ls: 0.6 })]));
  });
  out.push(g('Bas du lobby', [rect(0, PH - 66, PW, 66, { fill: '#07061A', opacity: 0.85 }), line(M, PH - 66, PW - M, PH - 66, { stroke: C.line }),
    U.btn(d, M, PH - 54, 300, 42, 'Votre toupie : Cyclone Furieux', { icon: 'build', size: 11.5 }),
    T(PW / 2 + 40, PH - 28, '2 joueurs sur 3 prêts · volcan · 4 places', { size: 12.5, fill: C.warn, anchor: 'middle' }),
    U.btn(d, PW - M - 170, PH - 54, 170, 42, o.client ? 'Quitter' : 'Fermer le lobby', { variant: 'danger', icon: 'quit', size: 11.5 })]));
  if (o.settings) out.push(U.drawer(d, 'right', 430, 'Réglages de l’hôte', (x, y, w) => g('Réglages', [label(x, y + 8, 'Stadium'), U.stadiumChip(d, x, y + 16, w - 10, 'volcan', { selected: true, h: 52 }),
    U.seg(d, x, y + 98, w - 10, ['2', '3', '4'], 2, { label: 'Nombre de joueurs', h: 32 }), U.toggle(d, x, y + 146, w - 10, 'Coups spéciaux', true), U.toggle(d, x, y + 186, w - 10, 'Limite de temps', false),
    U.btn(d, x, y + 226, w - 10, 40, 'Personnalisation…', { icon: 'sliders', size: 12 }),
    T(x, y + 290, 'Mêlée à 3-4 : éliminer un adversaire touché dans les', { size: 11.5, fill: C.dim }), T(x, y + 306, '4 s rend 35 d’endurance. Visible par tous les joueurs.', { size: 11.5, fill: C.dim })]), { sub: 'visibles par tous' }));
  return out;
}
function lanBoard() {
  return U.board('m09-ecran-reseau-local', 'Mobile · écran', 'Multijoueur en réseau local', 'Compatible avec le PC (protocole 4). La liste des lobbys prend tout l’écran ; créer et régler passent dans des tiroirs.',
    [{ title: 'Lobbys détectés', draw: d => lanScreen(d), notes: ['Recherche en continu, un bouton « Rejoindre » par lobby', 'Par nom ou par adresse IP en bas de l’écran'] },
      { title: 'Tiroir : créer un lobby', draw: d => lanScreen(d, { create: true }), notes: ['Nom, nombre de joueurs et stadium', 'Depuis Tournois ▸ réseau, le tiroir ajoute les réglages du tournoi'] },
      { title: 'Lobby · hôte', draw: d => lobbyScreen(d), notes: ['Quatre places en grille, état de chaque joueur', '« Lancer » s’active quand tout le monde est prêt'] },
      { title: 'Tiroir : réglages de l’hôte', draw: d => lobbyScreen(d, { settings: true }), notes: ['Stadium, joueurs, coups spéciaux, durée, personnalisation', 'Chez un client, le même tiroir est en lecture seule'] }]);
}

// ============================== M10 TOURNOIS ==============================
function tournamentHub(d) {
  const out = [U.screenBg(d), U.topBar(d, 'Tournois', { sub: 'ligue ou élimination directe' })];
  const cards = [['Reprendre', 'save', C.warn, 'Coupe Néon', ['Élimination · 8 joueurs', 'Express · tour 2/3', 'sauvegardé hier, 21:40']],
    ['Tournoi solo', 'trophy', C.green, 'Contre des bots', ['Ligue ou élimination', '4 à 32 participants', 'matchs express ou classiques']],
    ['En réseau local', 'lan', C.cyan, '2 à 4 humains', ['Chacun sur son appareil', 'PC et mobiles mélangés', 'complété par des bots']]];
  const cw = (PW - 2 * M - 20) / 3;
  cards.forEach(([t, ic, col, sub, lines], i) => {
    const x = M + i * (cw + 10), y = 66;
    out.push(g(`Carte ${t}`, [U.card(d, x, y, cw, 324, { selected: i === 0, accent: col }), circle(x + cw / 2, y + 86, 46, { fill: col, opacity: 0.12 }), circle(x + cw / 2, y + 86, 46, { fill: 'none', stroke: col, 'stroke-opacity': 0.6 }),
      L.icon(ic, x + cw / 2, y + 86, 40, col), D(x + cw / 2, y + 166, t.toUpperCase(), { size: 16, ls: 1.8, anchor: 'middle' }), T(x + cw / 2, y + 186, sub, { size: 13, fill: col, anchor: 'middle' }),
      ...lines.map((l, k) => T(x + cw / 2, y + 214 + k * 18, l, { size: 12, fill: C.muted, anchor: 'middle' })),
      U.btn(d, x + 14, y + 266, cw - 28, 44, i === 0 ? 'Reprendre' : 'Configurer', { variant: i === 0 ? 'primary' : 'secondary', icon: i === 0 ? 'play' : 'next', size: 12.5 })]));
  });
  return out;
}
function tournamentSetup(d) {
  const out = [U.screenBg(d), U.topBar(d, 'Tournoi solo', { sub: 'contre des bots', actions: [{ label: 'Lancer', icon: 'play', variant: 'primary' }, { icon: 'sliders' }] })];
  const x = M, w = 420;
  out.push(U.seg(d, x, 80, w, ['Ligue', 'Élimination'], 1, { label: 'Format', h: 34 }));
  out.push(label(x, 140, 'Participants'), U.iconBtn(d, x, 148, 44, 38, 'back', { size: 16 }), path(chamfer(x + 50, 148, 90, 38, [8, 0, 8, 0]), { fill: '#07061A', stroke: C.line }),
    D(x + 95, 174, '8', { size: 18, anchor: 'middle' }), U.iconBtn(d, x + 146, 148, 44, 38, 'next', { size: 16 }), T(x + 206, 172, 'vous + 7 bots · 3 tours', { size: 12.5, fill: C.muted }));
  out.push(U.seg(d, x, 216, w, ['Express', 'Classique'], 0, { label: 'Matchs', h: 34 }), T(x, 266, 'Express : premier à 2 manches · Classique : 3 toupies, ordre choisi avant chaque match', { size: 11.5, fill: C.dim }));
  out.push(U.seg(d, x, 300, w, ['Facile', 'Normal', 'Difficile'], 2, { label: 'Difficulté des bots', h: 34 }));
  const rx = M + w + 24, rw = PW - M - rx;
  out.push(U.panel(d, rx, 66, rw, 160, { title: 'Votre toupie', index: '1 TOUPIE' }), U.topRow(d, rx + 12, 106, rw - 24, P['Cyclone Furieux'], { chevron: true, h: 50 }),
    T(rx + 12, 186, 'En classique : trois toupies,', { size: 12, fill: C.dim }), T(rx + 12, 202, 'ordre choisi avant chaque match.', { size: 12, fill: C.dim }));
  out.push(label(rx, 248, 'Stadium'), U.stadiumChip(d, rx, 256, rw, 'glace', { selected: true, h: 52 }), U.btn(d, rx, 320, rw, 40, 'Stadium au hasard à chaque match', { variant: 'ghost', icon: 'random', size: 10.5 }));
  return out;
}
const BR = [[['Vous', 2, 'Cyclone Furieux'], ['Atlas', 0, 'Gardienne']], [['Kira', 2, 'Éternelle'], ['Rook', 1, 'Colosse']], [['Vega', 0, 'Comète'], ['Lyn', 2, 'Juste Milieu']], [['Orion', 2, 'Brise Éternelle'], ['Sol', 1, 'Forteresse']]];
function matchBox(d, x, y, w, pair, o = {}) {
  const out = [U.card(d, x, y, w, 52, { selected: o.me, accent: C.green, stroke: o.todo ? C.green : undefined })];
  pair.forEach(([n, sc, top], k) => { const yy = y + 18 + k * 24; out.push(top ? Ar.top(d, x + 16, yy - 4, 8, P[top], { shadow: false }) : '', D(x + 30, yy, (n || 'À déterminer').toUpperCase(), { size: 10.5, ls: 0.8, fill: n ? (n === 'Vous' ? C.green : C.text) : C.dim }),
    sc !== undefined ? D(x + w - 10, yy, String(sc), { size: 11, anchor: 'end', fill: sc === 2 ? C.green : C.muted }) : ''); });
  if (o.todo) out.push(path(skew(x + w - 66, y - 8, 60, 14, 4), { fill: C.green }), D(x + w - 36, y + 2, 'À JOUER', { size: 8.5, fill: '#03140A', anchor: 'middle', ls: 0.8 }));
  return g('Match', out);
}
function bracketScreen(d, o = {}) {
  const out = [U.screenBg(d), U.topBar(d, 'Coupe Néon', { backIcon: 'quit', sub: 'élimination · 8 · express', actions: [{ label: 'Résultats', icon: 'trophy', on: o.results }] })];
  const bx = M, by = 64, cw = 150, colX = [bx, bx + 186, bx + 372];
  ['Quarts', 'Demi-finales', 'Finale'].forEach((t, i) => out.push(label(colX[i], by + 8, t, i === 1 ? C.green : C.muted)));
  BR.forEach((pair, i) => out.push(matchBox(d, colX[0], by + 18 + i * 76, cw, pair, { me: i === 0 })));
  out.push(matchBox(d, colX[1], by + 56, cw, [['Vous', 0, 'Cyclone Furieux'], ['Kira', 0, 'Éternelle']], { me: true, todo: true }), matchBox(d, colX[1], by + 208, cw, [['Lyn', undefined, 'Juste Milieu'], ['Orion', undefined, 'Brise Éternelle']]));
  out.push(matchBox(d, colX[2], by + 132, cw, [[null], [null]]));
  const ln = (x1, y1, x2, y2, col = C.line) => path(`M${x1},${y1} H${(x1 + x2) / 2} V${y2} H${x2}`, { fill: 'none', stroke: col, 'stroke-width': 1.4 });
  out.push(ln(colX[0] + cw, by + 44, colX[1], by + 82, C.green), ln(colX[0] + cw, by + 120, colX[1], by + 82), ln(colX[0] + cw, by + 196, colX[1], by + 234), ln(colX[0] + cw, by + 272, colX[1], by + 234),
    ln(colX[1] + cw, by + 82, colX[2], by + 158), ln(colX[1] + cw, by + 234, colX[2], by + 158));
  out.push(g('Défilement horizontal', [rect(bx, PH - 12, 520, 3, { fill: C.line }), rect(bx, PH - 12, 360, 3, { fill: C.cyan }), T(bx + 530, PH - 8, 'faire glisser · pincer pour zoomer', { size: 10.5, fill: C.dim })]));
  const rx = 580, rw = PW - M - rx;
  out.push(U.panel(d, rx, 64, rw, 332, { title: 'Votre match', index: 'DEMI-FINALE' }), Ar.top(d, rx + 70, 152, 40, P['Cyclone Furieux'], { aura: true, angle: 0.4 }), Ar.top(d, rx + rw - 70, 152, 40, P['Éternelle'], { aura: true, angle: 1 }),
    D(rx + rw / 2, 160, 'VS', { size: 22, anchor: 'middle', italic: true }), D(rx + 70, 216, 'VOUS', { size: 14, anchor: 'middle', fill: C.green }), T(rx + 70, 232, 'Cyclone Furieux', { size: 11, fill: C.muted, anchor: 'middle' }),
    D(rx + rw - 70, 216, 'KIRA', { size: 14, anchor: 'middle', fill: C.danger }), T(rx + rw - 70, 232, 'Éternelle · difficile', { size: 11, fill: C.muted, anchor: 'middle' }),
    T(rx + rw / 2, 266, 'Premier à 2 manches · stadium Glace', { size: 12, anchor: 'middle' }), U.btn(d, rx + 14, 334, rw - 28, 48, 'Jouer le match', { variant: 'primary', icon: 'play' }));
  if (o.results) out.push(U.drawer(d, 'left', 420, 'Tours et résultats', (x, y, w) => g('Résultats', [label(x, y + 8, 'Quarts de finale · 4 / 4', C.cyan),
    ...BR.map((pr, i) => g('Résultat', [U.card(d, x, y + 16 + i * 46, w - 10, 40, { selected: i === 0, accent: C.green }), D(x + 14, y + 16 + i * 46 + 25, pr[0][0].toUpperCase(), { size: 12, fill: pr[0][0] === 'Vous' ? C.green : C.text }),
      D(x + (w - 10) / 2, y + 16 + i * 46 + 25, `${pr[0][1]} – ${pr[1][1]}`, { size: 13, fill: C.cyan, anchor: 'middle' }), D(x + w - 24, y + 16 + i * 46 + 25, pr[1][0].toUpperCase(), { size: 12, anchor: 'end' })])),
    label(x, y + 212, 'Demi-finales · 0 / 2', C.cyan), T(x, y + 234, 'Vous – Kira · à jouer', { size: 12.5 }), T(x, y + 254, 'Lyn – Orion · en attente', { size: 12.5, fill: C.muted })]), { sub: 'tour 2 / 3' }));
  return out;
}
function leagueScreen(d) {
  const out = [U.screenBg(d), U.topBar(d, 'Ligue des Néons', { backIcon: 'quit', sub: 'ligue · 6 · classique', actions: [{ label: 'Résultats', icon: 'trophy' }] })];
  const x = M, w = 540, rows = [['Vous', 'Cyclone Furieux', 4, 3, 1, 9, '+5'], ['Kira', 'Éternelle', 4, 3, 1, 9, '+3'], ['Rook', 'Colosse', 4, 2, 2, 6, '0'], ['Lyn', 'Juste Milieu', 4, 2, 2, 6, '−1'], ['Atlas', 'Gardienne', 4, 1, 3, 3, '−3'], ['Sol', 'Forteresse', 4, 1, 3, 3, '−4']];
  const cols = [w - 210, w - 165, w - 120, w - 70, w - 16];
  ['J', 'V', 'D', 'Pts', 'Diff.'].forEach((c, i) => out.push(label(x + cols[i], 74, c, C.muted, { anchor: 'end' })));
  rows.forEach((r, i) => {
    const y = 82 + i * 46, me = r[0] === 'Vous', pod = [C.warn, '#C9D8FF', '#FF9A5A'][i];
    out.push(g(`Rang ${i + 1}`, [U.card(d, x, y, w, 40, { selected: me, accent: C.green }), D(x + 16, y + 26, String(i + 1), { size: 15, fill: pod || C.dim }),
      Ar.top(d, x + 50, y + 20, 13, P[r[1]], { shadow: false }), D(x + 72, y + 19, r[0].toUpperCase(), { size: 12.5, fill: me ? C.green : C.text }), T(x + 72, y + 33, r[1], { size: 11, fill: C.muted }),
      ...r.slice(2).map((v, k) => D(x + cols[k], y + 26, String(v), { size: 13, anchor: 'end', fill: k === 3 ? C.text : k === 4 ? (String(v).startsWith('+') ? C.green : String(v) === '0' ? C.muted : C.danger) : C.muted }))]));
  });
  const rx = x + w + 16, rw = PW - M - rx;
  out.push(U.panel(d, rx, 64, rw, 332, { title: 'Votre match', index: 'JOURNÉE 5/5' }), Ar.top(d, rx + rw / 2 - 52, 140, 30, P['Cyclone Furieux'], { angle: 0.4 }), Ar.top(d, rx + rw / 2 + 52, 140, 30, P['Colosse'], { angle: 1 }),
    D(rx + rw / 2, 146, 'VS', { size: 16, anchor: 'middle', italic: true }), T(rx + rw / 2, 196, 'Vous contre Rook', { size: 13, anchor: 'middle' }), T(rx + rw / 2, 214, 'classique · 3 toupies', { size: 11.5, fill: C.muted, anchor: 'middle' }),
    U.btn(d, rx + 12, 286, rw - 24, 40, 'Ordre des toupies', { icon: 'sliders', size: 11.5 }), U.btn(d, rx + 12, 334, rw - 24, 48, 'Jouer', { variant: 'primary', icon: 'play' }));
  return out;
}
function tournamentBoard() {
  return U.board('m10-ecran-tournois', 'Mobile · écran', 'Tournois', 'Un écran d’accueil avec la reprise d’un tournoi sauvegardé ; le tableau défile, « Votre match » reste à portée de pouce.',
    [{ title: 'Accueil des tournois', draw: d => tournamentHub(d), notes: ['Reprendre le tournoi solo sauvegardé (un seul emplacement)', 'Ou en configurer un nouveau, solo ou en réseau local'] },
      { title: 'Configuration solo', draw: d => tournamentSetup(d), notes: ['Format, participants, matchs, difficulté ; règles dans le tiroir', 'Lancer en haut à droite, comme « Enregistrer » dans l’atelier'] },
      { title: 'Élimination directe', draw: d => bracketScreen(d), notes: ['Tableau complet défilant (16 ou 32 joueurs : on fait glisser)', 'Quitter propose de sauvegarder (solo) ou demande confirmation (réseau)'] },
      { title: 'Tiroir : tours et résultats', draw: d => bracketScreen(d, { results: true }), notes: ['Tous les scores, tour par tour', 'Le tableau reste visible sous le voile'] },
      { title: 'Ligue · classement', draw: d => leagueScreen(d), notes: ['Podium coloré, vous en vert, différence de buts', 'En classique, l’ordre des trois toupies se choisit avant chaque match'] }]);
}

// ============================== M11 PARAMÈTRES ET AIDE ==============================
function rail(d, items, sel) {
  return g('Onglets', items.map(([t, ic], i) => { const y = 66 + i * 54, on = i === sel;
    return g(`Onglet ${t}`, [U.card(d, M, y, 170, 46, { selected: on, accent: C.green }), L.icon(ic, M + 24, y + 23, 18, on ? C.green : C.muted), D(M + 44, y + 28, t.toUpperCase(), { size: 12, ls: 1.2, fill: on ? C.text : C.muted })]); }));
}
function settingsScreen(d, tab) {
  const out = [U.screenBg(d), U.topBar(d, 'Paramètres', {}), rail(d, [['Général', 'gear'], ['Commandes', 'hand'], ['Réseau', 'wifi']], tab)];
  const x = M + 190, w = PW - M - x;
  if (tab === 0) {
    out.push(U.panel(d, x, 64, w, 120, { title: 'Langue' }), U.seg(d, x + 14, 104, 300, ['Français', 'English'], 0, { h: 34 }), T(x + 330, 126, 'Le jeu redémarre pour changer de langue.', { size: 12, fill: C.muted }));
    out.push(U.panel(d, x, 194, w, 206, { title: 'Duels' }), U.seg(d, x + 14, 234, w - 28, ['Timing', 'Rafale', 'Sans duel'], 0, { h: 34 }),
      T(x + 14, 292, 'Touchez l’écran quand le curseur traverse la zone verte, de plus en plus étroite :', { size: 12.5 }),
      T(x + 14, 310, 'un raté perd le duel.', { size: 12.5 }),
      T(x + 14, 340, 'C’est le duel par défaut des combats, des parties en réseau et des tournois ;', { size: 11.5, fill: C.dim }),
      T(x + 14, 356, 'la personnalisation des règles permet d’en choisir un autre.', { size: 11.5, fill: C.dim }));
  } else if (tab === 1) {
    const cards = [['Joystick + boutons', 'Joystick à gauche, DASH et SPÉCIAL à droite', 'buttons'], ['Inclinaison', 'Incliner pour piloter, boutons à droite', 'tilt'], ['Gestes', 'Glisser, toucher, appui long', 'gestures']];
    const cw = (w - 20) / 3;
    cards.forEach(([t, sub, k], i) => { const cx = x + i * (cw + 10), on = i === 1;
      out.push(g(`Schéma ${t}`, [U.card(d, cx, 64, cw, 196, { selected: on, accent: C.green }), path(chamfer(cx + 12, 76, cw - 24, 96, [8, 0, 8, 0]), { fill: '#07061A', stroke: C.line }),
        k === 'buttons' ? [circle(cx + 46, 136, 20, { fill: 'none', stroke: C.lineHi }), circle(cx + 52, 130, 9, { fill: C.green }), circle(cx + cw - 46, 140, 15, { fill: C.green, opacity: 0.4, stroke: C.green }), circle(cx + cw - 80, 148, 11, { fill: C.magenta, opacity: 0.4, stroke: C.magenta })].join('') : '',
        k === 'tilt' ? `<g transform="rotate(-15 ${cx + cw / 2} 124)">${L.icon('phone', cx + cw / 2, 124, 54, C.cyan)}</g>` + path(`M${cx + cw / 2 - 40},158 Q${cx + cw / 2},170 ${cx + cw / 2 + 40},158`, { fill: 'none', stroke: C.green, 'stroke-width': 2 }) : '',
        k === 'gestures' ? L.icon('hand', cx + cw / 2, 124, 46, C.cyan) + path(`M${cx + 30},150 Q${cx + cw / 2},110 ${cx + cw - 30},140`, { fill: 'none', stroke: C.green, 'stroke-width': 2, 'stroke-dasharray': '4 4' }) : '',
        D(cx + 12, 194, t.toUpperCase(), { size: 11.5, ls: 1 }), T(cx + 12, 212, sub, { size: 11, fill: C.muted }), on ? L.icon('check', cx + cw - 18, 194 - 4, 14, C.green) : '',
        T(cx + 12, 244, on ? 'Choisi' : 'Toucher pour choisir', { size: 11, fill: on ? C.green : C.dim })]));
    });
    out.push(U.slider(d, x, 276, w - 160, 'Inclinaison pour piloter à fond', 0.42, '35 % · ≈ 20°'), U.btn(d, PW - M - 146, 284, 146, 40, 'Essayer', { variant: 'ghost', icon: 'phone', size: 11.5 }),
      T(x, 352, 'Visée (tous les schémas) : glisser depuis sa toupie, relâcher pour lancer.', { size: 12, fill: C.muted }), T(x, 370, 'Duel : toucher l’écran. Vibrations à chaque choc.', { size: 12, fill: C.muted }));
  }
  return out;
}
function helpScreen(d, tab) {
  const out = [U.screenBg(d), U.topBar(d, 'Aide', { actions: [{ label: 'Paramètres', icon: 'gear' }] }), rail(d, [['Commandes', 'hand'], ['Règles', 'balance'], ['Coups', 'bolt']], tab)];
  const x = M + 190, w = PW - M - x;
  const rows = tab === 0 ? [['Visée', 'glisser depuis sa toupie : la flèche donne la direction, sa longueur la force'], ['Lancer', 'relâcher le doigt'],
    ['Piloter', 'inclinaison du téléphone (schéma choisi : Inclinaison)'], ['Dash', 'bouton DASH'], ['Coup spécial', 'bouton SPÉCIAL — une fois par partie, coûte ¼ de l’endurance'],
    ['Duel', 'toucher l’écran (timing) ou toucher très vite (rafale)'], ['Menu du combat', 'bouton pause en haut à gauche, ou retour d’Android']]
    : [['Victoire', 'la dernière toupie en jeu gagne : éjection ou endurance épuisée'], ['Chocs', 'l’attaque adverse retire de l’endurance, la défense réduit dégâts et recul'],
      ['Rebord', 'le percuter use un peu la rotation ; le longer ne coûte rien'], ['Dash', 'fonce sur l’adversaire le plus proche, avec plus d’attaque'],
      ['Duels', 'deux dashs qui se croisent : timing, rafale ou aucun, selon les règles'], ['Types', 'Attaque bat Endurance, Endurance bat Défense, Défense bat Attaque'],
      ['Stadiums', 'même surface pour tous ; chacun avantage légèrement certains types']];
  out.push(U.panel(d, x, 64, w, 336, { title: tab === 0 ? 'Commandes · inclinaison' : 'Règles du combat' }));
  rows.forEach(([a, b], i) => out.push(D(x + 14, 112 + i * 40, a.toUpperCase(), { size: 11.5, ls: 1, fill: C.green }), T(x + 150, 112 + i * 40, b, { size: 12.5 }), i < rows.length - 1 ? line(x + 14, 124 + i * 40, x + w - 14, 124 + i * 40, { stroke: C.line, opacity: 0.5 }) : ''));
  return out;
}
function settingsBoard() {
  return U.board('m11-ecran-parametres-aide', 'Mobile · écran', 'Paramètres et aide', 'Onglets à gauche, contenu à droite : rien à faire défiler sur un téléphone en paysage.',
    [{ title: 'Paramètres · général', draw: d => settingsScreen(d, 0), notes: ['Langue (le jeu redémarre) et type de duel par défaut', 'Réseau : le pseudo affiché dans les lobbys'] },
      { title: 'Paramètres · commandes', draw: d => settingsScreen(d, 1), notes: ['Les trois schémas illustrés ; l’inclinaison a sa sensibilité', '« Essayer » montre le neutre et la bille en direct'] },
      { title: 'Aide · commandes', draw: d => helpScreen(d, 0), notes: ['Rappel des gestes du schéma choisi', 'Raccourci vers les Paramètres en haut à droite'] },
      { title: 'Aide · règles', draw: d => helpScreen(d, 1), notes: ['Règles du combat en une page', 'L’onglet Coups ouvre la page des coups spéciaux'] }]);
}

if (require.main === module) for (const b of [principles, components, menuBoard, setupBoard, builderBoard, movesBoard, battleBoard, duelBoard, resultBoard, lanBoard, tournamentBoard, settingsBoard]) b().save(OUT);
module.exports = { principles, components, menuBoard, setupBoard, builderBoard, movesBoard, battleBoard, duelBoard, resultBoard, lanBoard, tournamentBoard, settingsBoard };
