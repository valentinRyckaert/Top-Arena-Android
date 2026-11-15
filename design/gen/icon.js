// Logo de l'application : Vortyx vue de biais, une traînée derrière elle, sur fond bleu/violet.
// usage: node icon.js <dossier>  →  top-arena.svg (icône complète) et top-arena-foreground.svg
// (sans fond, réduite à la zone sûre des icônes adaptatives Android).
const { Doc, g, rect, circle, path } = require('./lib');
const art = require('./art');

const S = 512;
const VORTYX = art.PRESETS[0];

function icon(name, { background, scale }) {
  const d = new Doc(S, S, name), out = [];
  if (background) {
    out.push(rect(0, 0, S, S, { rx: 112, fill: d.lin([[0, '#2F6BFF'], [0.55, '#5B36E6'], [1, '#9A2BD6']], 55) }));
    out.push(rect(0, 0, S, S, { rx: 112, fill: d.rad([[0, '#A9C4FF', 0.45], [1, '#05040F', 0]], 0.28, 0.22, 0.75) }));
    out.push(rect(0, 0, S, S, { rx: 112, fill: d.rad([[0.55, '#05040F', 0], [1, '#05040F', 0.45]], 0.5, 0.5, 0.75) }));
  }
  // Toupie lancée vers le haut à droite ; traînée en fuseau derrière elle, vers le bas à gauche.
  const cx = 300, cy = 212, R = 132;
  const tail = [58, 458], dx = cx - tail[0], dy = cy - tail[1], len = Math.hypot(dx, dy);
  const ux = dx / len, uy = dy / len, nx = -uy, ny = ux;
  const at = (back, side) => [cx - ux * back + nx * side, cy - uy * back + ny * side];
  const P = p => `${p[0].toFixed(1)},${p[1].toFixed(1)}`;
  // Fuseau : pointe à la queue, bords courbes, extrémité large cachée sous la toupie.
  const spindle = (w, off, start, end) => {
    const t = [tail[0] + nx * off + ux * start, tail[1] + ny * off + uy * start];
    const a = at(end, off + w), b = at(end, off - w);
    return `M${P(t)} Q${P(at(R * 1.6, off + w * 0.9))} ${P(a)} L${P(b)} Q${P(at(R * 1.6, off - w * 0.9))} ${P(t)} Z`;
  };
  const angle = Math.atan2(dy, dx) * 180 / Math.PI;
  const trail = d.lin([[0, '#34D6FF', 0], [0.6, '#34D6FF', 0.6], [1, '#E6FBFF', 0.95]], angle);
  out.push(circle(cx, cy, R * 1.2, { fill: '#C23BFF', opacity: 0.35, filter: d.blur(34) }));
  out.push(path(spindle(R * 0.85, 0, 0, R * 0.35), { fill: d.lin([[0, '#7B3CFF', 0], [1, '#34D6FF', 0.55]], angle), filter: d.blur(16) }));
  out.push(path(spindle(R * 0.5, 0, 0, R * 0.35), { fill: trail }));
  out.push(path(spindle(R * 0.1, R * 0.62, 70, R * 0.95), { fill: trail, opacity: 0.75 }));
  out.push(path(spindle(R * 0.09, -R * 0.62, 110, R * 1.05), { fill: trail, opacity: 0.6 }));
  // De biais : inclinée vers l'avant (aplatie et tournée).
  out.push(`<g transform="translate(${cx} ${cy}) rotate(-28) scale(1 0.68) translate(${-cx} ${-cy})">`
    + art.top(d, cx, cy, R, VORTYX, { shadow: false, aura: false, angle: 0.5 }) + '</g>');
  // Étincelles à l'avant.
  for (const [x, y, r] of [[438, 118, 7], [462, 166, 4.5], [404, 82, 4]]) out.push(circle(x, y, r, { fill: '#E6FBFF', opacity: 0.9 }));
  const body = g('Logo', out, scale === 1 ? {} : { transform: `translate(${S / 2} ${S / 2}) scale(${scale}) translate(${-S / 2} ${-S / 2})` });
  return d.add(body);
}

const dir = process.argv[2] || '.';
icon('top-arena', { background: true, scale: 1 }).save(dir);
icon('top-arena-foreground', { background: false, scale: 0.72 }).save(dir);
