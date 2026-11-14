'use strict';
// Composants « Holo Combat » pour téléphone en paysage. Unité = 1 dp (écran 915 × 412 dp),
// cibles tactiles ≥ 44 dp, textes ≥ 11 sp. Mêmes couleurs, polices et formes que le jeu PC
// (lib.js, art.js) : seules les tailles et la navigation changent.
const L = require('./lib');
const Ar = require('./art');
const { C, TYPE, F, Doc, g, rect, circle, line, path, chamfer, skew, T, D, r2 } = L;

const PW = 915, PH = 412;  // écran (dp)
const M = 24;              // marge latérale (encoche de la caméra en paysage)
const BAR = 52;            // hauteur de la barre du haut
const P = Object.fromEntries(Ar.PRESETS.map(p => [p.name, p]));
const STAD = Object.fromEntries(Ar.STADIUMS.map(s => [s.key, s]));

// Largeur approximative d'un texte (pour placer les éléments voisins).
const tw = (s, size, display = false, ls = 0) => String(s).length * (size * (display ? 0.66 : 0.5) + ls);
const label = (x, y, s, col = C.muted, o = {}) => T(x, y, s.toUpperCase(), { size: o.size || 10.5, ls: 1.6, fill: col, weight: 700, anchor: o.anchor });

// ---------------------------------------------------------------- écran
// Fond de l'écran (même fond holographique que le PC, à l'échelle du téléphone).
const screenBg = (d, o = {}) => L.background(d, PW, PH, { horizon: 0.74, ...o });
const scrim = (op = 0.62) => rect(0, 0, PW, PH, { fill: '#03020A', opacity: op, id: 'Voile' });

// Barre du haut : retour, titre, sous-titre, actions à droite (de droite à gauche).
function topBar(d, title, o = {}) {
  const out = [], W = o.width || PW;
  if (o.back !== false) out.push(iconBtn(d, M, 6, 44, 40, o.backIcon || 'back', { label: o.backLabel }));
  const tx = o.back === false ? M : M + 56;
  out.push(L.slashes(tx, 17, 3, 16, C.green, 8));
  out.push(D(tx + 30, 33, title.toUpperCase(), { size: 18, ls: 2.4 }));
  if (o.sub) out.push(T(tx + 30 + tw(title, 18, true, 2.4) + 14, 32, o.sub, { size: 13, fill: C.muted, weight: 500 }));
  let x = W - M;
  for (const a of o.actions || []) {
    const w = a.w || (a.label ? tw(a.label.toUpperCase(), 12, true, 1.44) + (a.icon ? 54 : 32) : 44);
    x -= w;
    out.push(a.label ? btn(d, x, 8, w, 36, a.label, { variant: a.variant || 'secondary', icon: a.icon, size: 12 })
      : iconBtn(d, x, 6, 44, 40, a.icon, { on: a.on, badge: a.badge }));
    x -= 8;
  }
  out.push(line(M, BAR, W - M, BAR, { stroke: C.line }), line(M, BAR, M + 180, BAR, { stroke: C.cyan, 'stroke-width': 2 }));
  return g('Barre du haut', out);
}

// Bouton carré à icône (retour, menu, réglages…).
function iconBtn(d, x, y, w, h, ic, o = {}) {
  const col = o.on ? C.green : o.color || C.cyan, out = [];
  out.push(path(chamfer(x, y, w, h, [10, 0, 10, 0]), { fill: o.on ? C.green : '#0A0826', 'fill-opacity': o.on ? 0.16 : 0.85, stroke: o.on ? C.green : C.lineHi, 'stroke-width': 1.2 }));
  out.push(L.icon(ic, x + w / 2, y + h / 2, o.size || 20, col));
  if (o.badge) out.push(circle(x + w - 4, y + 4, 8, { fill: C.magenta }), D(x + w - 4, y + 8, String(o.badge), { size: 10, anchor: 'middle' }));
  return g(`Bouton ${o.label || ic}`, out);
}

// Bouton : mêmes variantes que le PC (principal, secondaire, fantôme, danger, holo), sans touche clavier.
const VAR = {
  primary: { fill: [C.green, C.lime], text: '#03140A', stroke: '#B9FFD6' },
  secondary: { fill: ['#231C6B', '#151049'], text: C.text, stroke: C.lineHi },
  ghost: { fill: null, text: C.muted, stroke: C.line },
  danger: { fill: ['#3A0F2A', '#220820'], text: '#FFC3D2', stroke: C.danger },
  holo: { fill: [C.blue, C.violet, C.magenta], text: C.white, stroke: '#C9B8FF' },
};
function btn(d, x, y, w, h, txt, o = {}) {
  const v = VAR[o.variant || 'secondary'], c = h * 0.3, out = [], size = o.size || (h >= 48 ? 15 : 13), ls = size * 0.12;
  if (o.variant === 'primary' || o.variant === 'holo') out.push(path(chamfer(x, y + 5, w, h, [c, 0, c, 0]), { fill: o.variant === 'holo' ? C.violet : C.green, opacity: 0.35, filter: d.blur(10) }));
  out.push(path(chamfer(x, y, w, h, [c, 0, c, 0]), { fill: v.fill ? d.lin(v.fill.map((col, i) => [i / Math.max(1, v.fill.length - 1), col]), o.variant === 'primary' ? 0 : 20) : C.void,
    'fill-opacity': v.fill ? 1 : 0.4, stroke: v.stroke, 'stroke-width': 1.3, opacity: o.disabled ? 0.4 : 1 }));
  if (o.variant === 'primary') out.push(path(chamfer(x + 3, y + 3, w - 6, h * 0.4, [c - 2, 0, 0, 0]), { fill: '#FFFFFF', opacity: 0.18 }));
  const label = txt.toUpperCase(), lw = tw(label, size, true, ls), iw = o.icon ? size + 10 : 0;
  const x0 = o.align === 'left' ? x + c + 12 : x + (w - lw - iw) / 2;
  if (o.icon) out.push(L.icon(o.icon, x0 + size / 2, y + h / 2, size + 2, o.variant === 'primary' ? v.text : C.cyan));
  out.push(D(x0 + iw, y + h / 2 + size * 0.36, label, { size, ls, fill: v.text, opacity: o.disabled ? 0.5 : 1 }));
  if (o.sub) out.push(T(x0 + iw, y + h / 2 + size * 0.36 + 16, o.sub, { size: 11.5, fill: C.muted, weight: 500 }));
  return g(o.id || `Bouton ${txt}`, out);
}

// Badge de type compact.
function badge(d, x, y, type, o = {}) {
  const col = TYPE[type] || type, txt = (o.label || type).toUpperCase(), h = o.h || 16, size = o.size || 9, w = tw(txt, size, true, 1.1) + 22;
  return g(`Badge ${txt}`, [path(skew(x, y, w, h, 5), { fill: col, 'fill-opacity': 0.16, stroke: col, 'stroke-width': 1 }),
    path(skew(x + 3, y + 3.5, 7, h - 7, 2.5), { fill: col }), D(x + 14, y + h / 2 + size * 0.36, txt, { size, fill: col, ls: 1.1 })]);
}

// Cadre de contenu compact (titre en petites capitales, coins coupés, accents).
function panel(d, x, y, w, h, o = {}) {
  const c = o.c ?? 14, acc = o.accent || C.cyan, out = [];
  out.push(path(chamfer(x, y, w, h, [c, 0, c, 0]), { fill: d.lin([[0, '#1A1556', 0.94], [1, '#0D0A2E', 0.94]], 100), stroke: o.selected ? acc : C.line, 'stroke-width': o.selected ? 1.8 : 1.1 }));
  out.push(path(`M${x},${y + c + 16} V${y + c} L${x + c},${y} H${x + c + 34}`, { stroke: acc, 'stroke-width': 2.4, fill: 'none' }));
  out.push(path(`M${x + w},${y + h - c - 16} V${y + h - c} L${x + w - c},${y + h} H${x + w - c - 34}`, { stroke: C.violet, 'stroke-width': 2.4, fill: 'none' }));
  if (o.title) {
    out.push(D(x + c + 8, y + 21, o.title.toUpperCase(), { size: 11.5, ls: 2, fill: C.text }));
    if (o.index) out.push(T(x + w - 12, y + 21, o.index, { font: F.display, size: 10, fill: acc, anchor: 'end', ls: 1.6, weight: 700 }));
    out.push(line(x + c + 8, y + 30, x + w - 12, y + 30, { stroke: C.line }));
  }
  return g(o.id || `Cadre ${o.title || ''}`.trim(), out);
}
const card = (d, x, y, w, h, o = {}) => path(chamfer(x, y, w, h, o.c ?? [10, 0, 10, 0]), {
  fill: o.fill || (o.selected ? '#1E2A6A' : '#100D35'), 'fill-opacity': 0.95,
  stroke: o.selected ? (o.accent || C.green) : o.stroke || C.line, 'stroke-width': o.selected ? 1.8 : 1, id: o.id,
});

// Tiroir latéral : voile sur l'écran, panneau qui glisse depuis un bord, poignée de fermeture.
function drawer(d, side, w, title, inner, o = {}) {
  const x = side === 'left' ? 0 : PW - w, out = [scrim(o.scrim ?? 0.6)];
  const edge = side === 'left' ? x + w : x;
  out.push(path(side === 'left' ? chamfer(x - 30, 0, w + 30, PH, [0, 26, 26, 0]) : chamfer(x, 0, w + 30, PH, [26, 0, 0, 26]),
    { fill: d.lin([[0, '#1B1660'], [1, '#0B0928']], 100), stroke: C.lineHi, 'stroke-width': 1.3 }));
  out.push(rect(side === 'left' ? edge - 3 : edge, 40, 3, PH - 80, { fill: o.accent || C.cyan, opacity: 0.8 }));
  out.push(rect(side === 'left' ? edge - 10 : edge - 4, 60, 14, PH - 120, { fill: o.accent || C.cyan, opacity: 0.18, filter: d.blur(8) }));
  // poignée (glisser pour fermer)
  out.push(path(side === 'left' ? chamfer(edge, PH / 2 - 34, 16, 68, [0, 8, 8, 0]) : chamfer(edge - 16, PH / 2 - 34, 16, 68, [8, 0, 0, 8]),
    { fill: '#1B1660', stroke: C.lineHi }));
  out.push(L.icon(side === 'left' ? 'back' : 'next', side === 'left' ? edge + 8 : edge - 8, PH / 2, 12, C.cyan));
  const ix = side === 'left' ? M : x + 22;
  out.push(L.slashes(ix, 18, 3, 14, o.accent || C.green, 7), D(ix + 26, 31, title.toUpperCase(), { size: 15, ls: 2.2 }));
  if (o.sub) out.push(T(ix + 26 + tw(title, 15, true, 2.2) + 10, 31, o.sub, { size: 12, fill: C.muted, weight: 500 }));
  out.push(iconBtn(d, (side === 'left' ? x + w : PW) - 54 - (side === 'left' ? 4 : M - 8), 6, 40, 38, 'quit', { size: 16, color: C.muted, label: 'Fermer' }));
  out.push(line(ix, 50, (side === 'left' ? x + w - 16 : PW - M), 50, { stroke: C.line }));
  out.push(g('Contenu', [inner(side === 'left' ? M : x + 22, 62, w - (side === 'left' ? M + 16 : 22 + M), PH - 74)]));
  return g(`Tiroir ${title}`, out);
}

// Contrôle segmenté compact (h 36).
function seg(d, x, y, w, opts, sel, o = {}) {
  const h = o.h || 36, sw = w / opts.length, out = [];
  if (o.label) out.push(label(x, y - 6, o.label));
  opts.forEach((t, i) => {
    const on = i === sel;
    out.push(path(skew(x + i * sw, y, sw - 5, h, 9), { fill: on ? d.lin([[0, C.green], [1, C.lime]], 0) : '#07061A', stroke: on ? C.green : C.line }));
    out.push(D(x + i * sw + sw / 2 - 2.5, y + h / 2 + 4.5, t.toUpperCase(), { size: o.size || 12, ls: 1.4, fill: on ? '#03140A' : C.muted, anchor: 'middle' }));
  });
  return g(`Segment ${o.label || opts.join('/')}`, out);
}

// Curseur compact (h 40 avec libellé).
function slider(d, x, y, w, lab, v, val) {
  const kx = x + w * v;
  return g(`Curseur ${lab}`, [label(x, y + 10, lab), D(x + w, y + 11, val, { size: 12.5, fill: C.cyan, anchor: 'end' }),
    path(skew(x, y + 22, w, 6, 3), { fill: '#07061A', stroke: C.line }), path(skew(x, y + 22, w * v, 6, 3), { fill: d.lin([[0, C.blue], [1, C.cyan]], 0) }),
    path(chamfer(kx - 9, y + 14, 18, 22, [5, 0, 5, 0]), { fill: C.text, stroke: C.cyan, 'stroke-width': 1.6 })]);
}

// Interrupteur (h 30).
function toggle(d, x, y, w, lab, on, hint) {
  return g(`Interrupteur ${lab}`, [T(x, y + 14, lab, { size: 14, fill: on ? C.text : C.muted }),
    hint ? T(x, y + 30, hint, { size: 11.5, fill: C.dim, weight: 500 }) : '',
    path(skew(x + w - 58, y, 58, 26, 8), { fill: on ? C.green : '#07061A', 'fill-opacity': on ? 0.25 : 1, stroke: on ? C.green : C.line, 'stroke-width': 1.3 }),
    path(skew(on ? x + w - 28 : x + w - 54, y + 4, 24, 18, 6), { fill: on ? C.green : C.dim })]);
}

// Ligne de liste : vignette de toupie, nom, badge de type, détail à droite.
function topRow(d, x, y, w, p, o = {}) {
  const h = o.h || 48, out = [card(d, x, y, w, h, { selected: o.selected, accent: o.accent })];
  out.push(Ar.top(d, x + 26, y + h / 2, 16, p, { angle: o.angle ?? 0.6, shadow: false }));
  out.push(D(x + 52, y + h / 2 - 2, (o.name || p.name).toUpperCase(), { size: 12.5, ls: 1.4 }));
  out.push(badge(d, x + 52, y + h / 2 + 4, p.type, { h: 15, size: 8.5 }));
  if (o.star) out.push(L.icon('star', x + w - 18, y + 14, 12, C.cyan));
  if (o.right) out.push(T(x + w - 12, y + h / 2 + 5, o.right, { size: 12, fill: C.muted, anchor: 'end' }));
  if (o.chevron) out.push(L.icon('next', x + w - 18, y + h / 2, 14, C.cyan));
  return g(`Ligne ${p.name}`, out);
}

// Barres de stats compactes (attaque, défense, endurance).
function statRow(d, x, y, w, n, v, col, max = 15) {
  const segs = max, gap = 2, sw = (w - (segs - 1) * gap) / segs, out = [label(x, y, n), D(x + w, y + 1, String(v), { size: 12, fill: col, anchor: 'end' })];
  for (let i = 0; i < segs; i++) out.push(path(skew(x + i * (sw + gap), y + 6, sw + 2, 6, 2), { fill: i < v ? col : C.line, opacity: i < v ? 1 : 0.5 }));
  return g(`Stat ${n}`, out);
}
function stats3(d, x, y, w, p, o = {}) {
  const [a, de, e] = o.values || Ar.sumStats(p.parts), gap = o.gap || 26;
  return g('Stats', [['Attaque', a, TYPE.Attaque], ['Défense', de, TYPE['Défense']], ['Endurance', e, TYPE.Endurance]]
    .map(([n, v, col], i) => statRow(d, x, y + 10 + i * gap, w, n, v, col)));
}

// Puce (filtres, résumés).
const chipW = (t, ic) => tw(t, 10.5, true, 1.3) + (ic ? 40 : 24);
function chip(d, x, y, t, col = C.cyan, ic) {
  const w = chipW(t, ic);
  return g(`Puce ${t}`, [path(skew(x, y, w, 24, 7), { fill: col, 'fill-opacity': 0.1, stroke: col, 'stroke-opacity': 0.7 }),
    ic ? L.icon(ic, x + 20, y + 12, 12, col) : '', D(x + (ic ? 32 : 14), y + 16, t.toUpperCase(), { size: 10.5, ls: 1.3, fill: col })]);
}

// Vignette de stadium (art + nom + code).
function stadiumChip(d, x, y, w, key, o = {}) {
  const s = STAD[key], h = o.h || 58, out = [card(d, x, y, w, h, { selected: o.selected })];
  out.push(Ar.stadium(d, key, x + h / 2 + 2, y + h / 2, h * 0.36));
  out.push(D(x + h + 6, y + h / 2 - 2, s.name.toUpperCase(), { size: 12, ls: 1.4 }));
  out.push(T(x + h + 6, y + h / 2 + 13, s.code, { size: 10, fill: s.color, weight: 700, ls: 1 }));
  if (o.selected) out.push(L.icon('check', x + w - 14, y + 12, 12, C.green));
  return g(`Stadium ${s.name}`, out);
}

// Bouton d'action rond du combat (DASH, SPÉCIAL…).
function roundAction(d, cx, cy, r, txt, o = {}) {
  const col = o.color || C.green, off = o.disabled, out = [];
  if (!off) out.push(circle(cx, cy + 3, r, { fill: col, opacity: 0.35, filter: d.blur(10) }));
  out.push(circle(cx, cy, r, { fill: off ? '#0D0B2A' : d.rad([[0, col, 0.55], [1, col, 0.18]]), stroke: off ? C.line : col, 'stroke-width': 2 }));
  out.push(circle(cx, cy, r - 7, { fill: 'none', stroke: off ? C.line : col, 'stroke-width': 1, 'stroke-dasharray': '3 6', opacity: 0.8 }));
  if (o.glyph) out.push(Ar.glyph(o.glyph, cx, cy - 8, r * 0.28, off ? C.dim : '#FFFFFF', 2));
  else if (o.icon) out.push(L.icon(o.icon, cx, cy - 8, r * 0.5, off ? C.dim : '#FFFFFF'));
  out.push(D(cx, cy + (o.glyph || o.icon ? r * 0.5 : 5), txt.toUpperCase(), { size: o.size || 12, ls: 1.4, fill: off ? C.dim : '#FFFFFF', anchor: 'middle' }));
  if (o.ring !== undefined) out.push(path(`M${cx},${cy - r - 4} A${r + 4},${r + 4} 0 ${o.ring > 0.5 ? 1 : 0} 1 ${r2(cx + Math.sin(o.ring * 2 * Math.PI) * (r + 4))},${r2(cy - Math.cos(o.ring * 2 * Math.PI) * (r + 4))}`,
    { fill: 'none', stroke: col, 'stroke-width': 3, 'stroke-linecap': 'round' }));
  return g(`Action ${txt}`, out);
}

// Joystick virtuel.
function joystick(d, cx, cy, r, dx = 0.3, dy = -0.35) {
  return g('Joystick', [circle(cx, cy, r, { fill: '#0A0826', 'fill-opacity': 0.75, stroke: C.lineHi, 'stroke-width': 1.5 }),
    circle(cx, cy, r * 0.62, { fill: 'none', stroke: C.line, 'stroke-dasharray': '3 7' }),
    ...[0, 1, 2, 3].map(i => L.icon('next', cx + Math.cos(i * Math.PI / 2) * r * 0.8, cy + Math.sin(i * Math.PI / 2) * r * 0.8, 10, C.dim)).map((s, i) => s.replace('translate', `rotate(${i * 90} ${r2(cx + Math.cos(i * Math.PI / 2) * r * 0.8)} ${r2(cy + Math.sin(i * Math.PI / 2) * r * 0.8)}) translate`)),
    circle(cx + dx * r, cy + dy * r + 3, r * 0.42, { fill: C.green, opacity: 0.35, filter: d.blur(8) }),
    circle(cx + dx * r, cy + dy * r, r * 0.42, { fill: d.rad([[0, C.lime], [1, C.green]]), stroke: '#B9FFD6', 'stroke-width': 1.5 })]);
}

// Jauge d'endurance compacte.
const endurance = (d, x, y, w, pct, col = C.green, o = {}) => L.enduranceBar(d, x, y, w, o.h || 9, pct, col, o);

// Annotation de planche : pastille numérotée + flèche vers un point de l'écran.
function pin(x, y, n, col = C.magenta) {
  return g(`Repère ${n}`, [circle(x, y, 11, { fill: col, stroke: '#FFFFFF', 'stroke-width': 1.5 }), D(x, y + 4.5, String(n), { size: 12, anchor: 'middle' })]);
}

// ---------------------------------------------------------------- planches
// Cadre de téléphone (bezel arrondi, poinçon de la caméra à gauche) autour d'un écran.
function phone(d, x, y, screen, o = {}) {
  const b = 11, clip = d.clip(path(`M30,0 H${PW - 30} A30,30 0 0 1 ${PW},30 V${PH - 30} A30,30 0 0 1 ${PW - 30},${PH} H30 A30,30 0 0 1 0,${PH - 30} V30 A30,30 0 0 1 30,0 Z`));
  return g(o.id || 'Téléphone', [
    path(`M${x - b + 38},${y - b} H${x + PW + b - 38} A38,38 0 0 1 ${x + PW + b},${y - b + 38} V${y + PH + b - 38} A38,38 0 0 1 ${x + PW + b - 38},${y + PH + b} H${x - b + 38} A38,38 0 0 1 ${x - b},${y + PH + b - 38} V${y - b + 38} A38,38 0 0 1 ${x - b + 38},${y - b} Z`,
      { fill: '#07060F', stroke: '#2A2560', 'stroke-width': 2, filter: d.shadow(18, 22, '#000', 0.7) }),
    `<g id="Écran" transform="translate(${x},${y})" clip-path="${clip}">${screen}</g>`,
    circle(x + 13, y + PH / 2, 5.5, { fill: '#000', stroke: '#1C1940', 'stroke-width': 1.5, id: 'Caméra' }),
  ]);
}

// Planche : en-tête + écrans de téléphone (2 par rangée) avec légende sous chacun.
const COLS = 2, GAP_X = 70, PAD = 70, HEAD = 230, ROW = PH + 160;
function board(name, kicker, title, sub, screens, o = {}) {
  const rows = Math.ceil(screens.length / COLS), W = PAD * 2 + COLS * PW + (COLS - 1) * GAP_X;
  const H = HEAD + rows * ROW + (o.extra || 0) + 30, d = new Doc(W, H, name);
  d.add(L.background(d, W, H, { floor: false }));
  d.add(L.boardHeader(d, PAD, 80, kicker, title, sub));
  screens.forEach((s, i) => {
    const x = PAD + (i % COLS) * (PW + GAP_X), y = HEAD + Math.floor(i / COLS) * ROW;
    d.add(phone(d, x, y, s.draw(d), { id: `Écran ${s.title}` }));
    const ly = y + PH + 44;
    d.add(g(`Légende ${s.title}`, [path(skew(x, ly - 18, 34, 24, 7), { fill: C.green }), D(x + 16, ly - 1, String.fromCharCode(65 + i), { size: 14, fill: '#03140A', anchor: 'middle' }),
      D(x + 46, ly, s.title.toUpperCase(), { size: 17, ls: 2.2 }),
      ...(s.notes || []).map((n, k) => T(x + 46, ly + 26 + k * 21, n, { size: 15.5, fill: C.muted, weight: 500 }))]));
  });
  if (o.after) d.add(o.after(d, PAD, HEAD + rows * ROW, W - 2 * PAD));
  return d;
}

module.exports = { L, Ar, C, TYPE, F, P, STAD, PW, PH, M, BAR, tw, label, screenBg, scrim, topBar, iconBtn, btn, badge, statRow, panel, card, drawer, seg, slider,
  toggle, topRow, stats3, chip, chipW, stadiumChip, roundAction, joystick, endurance, pin, phone, board, g, rect, circle, line, path, chamfer, skew, T, D, r2 };
