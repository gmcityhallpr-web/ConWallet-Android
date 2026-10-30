// 가학산 수목원 개장 — Watercolor. One tall journal sheet (1080×7420), the camera tilts down it. 1080×1920, 15.0 s.
const cv = document.getElementById('c'), ctx = cv.getContext('2d');
const DUR = 15.0, SHEET = 7420;
let PAPER_IMG;

// ---------- camera: locked on the title, one tilt down the sheet, settles on the stamp ----------
const camY = monotone([[0, 0], [2.4, 0], [3.2, 260], [11.7, 5240], [12.5, 5500], [15, 5500]]);

// ---------- palette: morning garden; the one accent is the vermilion stamp ----------
const SKY = '#a9c1cc', SKY2 = '#d7c9a6', SOIL = '#b9a27a', PATH = '#d2bb8e';
const G1 = ['#6f9a5a', '#5a8650', '#86a96a', '#4f7a4a'], G2 = ['#3f6f4f', '#2f5a45', '#56845a', '#4a7a57'];
const FLOW = ['#e3a0ae', '#e7c35a', '#d97a5a', '#c99ad0'];
const GLASS = '#7f98a6', OLIVE = ['#9aa88a', '#8a9a7c', '#a8b49a'];
const SEASON = [['#e7a3ad', '#efbcc3', '#d98f9c'], ['#6f9a5a', '#5a8650', '#86a96a'], ['#d9813a', '#e0a04a', '#c96a32'], ['#c3cfd6', '#b1bfc8', '#dbe2e6']];
const MOSS = ['#7d9a3c', '#8fa94a', '#6a8a34', '#a3b85a'], STONE = '#9a9690';
const VERM = '#c8442a';

// station centres (world y) and their screen-position painting window
const ST = [
  { id: 'welcome', y: 1350, side: 1, kr: '환영의 정원', en: 'WELCOME GARDEN' },
  { id: 'green', y: 2562, side: -1, kr: '학온재', en: 'FIRST GREENHOUSE' },
  { id: 'seasons', y: 3775, side: 1, kr: '사계정원', en: 'FOUR SEASONS GARDEN' },
  { id: 'forest', y: 4987, side: -1, kr: '도담도담숲', en: 'DODAM FOREST' },
  { id: 'moss', y: 6200, side: 1, kr: '이끼원', en: 'MOSS GARDEN' },
];
const STAMP = { x: 540, y: 6930, t: 12.3 };
// painting progress from where a station sits on screen (it paints as it rises into view)
const prog = (st, t) => st.id === 'welcome' ? seg(t, 1.4, 3.6) : seg(1880 - (st.y - camY(t)), 0, 900);
// the first station is already on the title page: its note follows the clock, the others follow the tilt
const noteProg = (st, t, cy) => st.id === 'welcome' ? seg(t, 2.0, 2.8) : seg(1880 - (st.y - cy), 80, 500);
const tAt = y => { for (let t = 0; t <= DUR; t += 1 / 60) if (camY(t) >= y) return t; return DUR; };

// ---------- builders (local coords: origin = ground centre, y up negative) ----------
function ground(S, w, col, y = 0) {
  S.push(mk(qcurve(-w / 2, y, w / 2, y, rnd(-6, 6), 20), 26, col, { prof: 'even', nb: 6, a: .45, rough: .35 }));
  S.push(mk(qcurve(-w / 2 + 30, y + 14, w / 2 - 30, y + 12, rnd(-4, 4), 20), 2.4, INK2, { prof: 'even', nb: 0, a: .6, rough: .4 }));
}
function welcome() {
  const S = [];
  ground(S, 860, SOIL);
  // path running into the gate
  S.push(mk([[0, 40], [0, -10]], 160, PATH, { prof: 'even', nb: 7, a: .5 }));
  // posts and arch
  for (const x of [-170, 170]) { const p = mk(qcurve(x, 0, x, -360, rnd(-3, 3), 10), 22, '#8a6a4a', { prof: 'even', nb: 4, a: .9 }); S.push(p, edgeOf(p, 1, 2.2, INK)); }
  const arch = mk(qctrl(-180, -350, 0, -520, 180, -350, 24), 20, '#8a6a4a', { prof: 'even', nb: 4, a: .9 }); S.push(arch, edgeOf(arch, -1, 2.2, INK));
  // vines along the arch, flowers in the vines
  for (let i = 0; i <= 10; i++) { const u = i / 10, x = lerp(-180, 180, u), y = -350 - Math.sin(u * Math.PI) * 85; clump(S, x, y, 30, 7, G1, { l0: 10, l1: 22, w0: 6, w1: 11 }); }
  for (let i = 0; i < 14; i++) { const u = rnd(), x = lerp(-180, 180, u), y = -350 - Math.sin(u * Math.PI) * 85 + rnd(-20, 20); S.push(mk(qcurve(x, y, x + 4, y - 4, 0, 3), rnd(9, 14), pick(FLOW), { prof: 'leaf', nb: 0, a: .9 })); }
  // flower beds either side
  for (const side of [-1, 1]) {
    const cx = side * 330;
    S.push(mk(qcurve(cx - 120, -10, cx + 120, -10, -26, 10), 50, G1[1], { prof: 'leaf', nb: 6, a: .5 }));
    for (let i = 0; i < 26; i++) { const x = cx + rnd(-120, 120), y = -20 - rnd(0, 60); S.push(mk(qcurve(x, y + 30, x + rnd(-6, 6), y, 3, 4), 2.2, G2[0], { prof: 'tip', nb: 0, a: .8 })); S.push(mk(qcurve(x, y, x + 3, y - 3, 0, 3), rnd(10, 16), pick(FLOW), { prof: 'leaf', nb: 0, a: .9 })); }
  }
  return S;
}
function greenhouse() {
  const S = [];
  ground(S, 900, SOIL);
  // glass wash first (structure), then ribs, then plants inside
  const dome = []; for (let i = 0; i <= 40; i++) { const a = Math.PI + Math.PI * i / 40; dome.push([Math.cos(a) * 340, Math.sin(a) * 380 - 10]); }
  S.push(mk([[-300, -150], [300, -150]], 300, '#bcd0d8', { prof: 'even', nb: 8, a: .35, rough: .3 }));
  S.push(mk(qctrl(-300, -300, 0, -470, 300, -300, 20), 150, '#bcd0d8', { prof: 'leaf', nb: 6, a: .3 }));
  S.push(mk(dome, 7, GLASS, { prof: 'even', nb: 3, a: .9, rough: .2 }));
  for (const k of [-.66, -.33, 0, .33, .66]) { const x = k * 340, top = -Math.sqrt(Math.max(0, 1 - k * k)) * 380; S.push(mk(qcurve(x, -8, x, top - 6, 0, 10), 3.4, GLASS, { prof: 'even', nb: 0, a: .85 })); }
  for (const y of [-130, -250]) { const hw = Math.sqrt(Math.max(0, 1 - Math.pow((-y - 10) / 380, 2))) * 340; S.push(mk(qcurve(-hw, y, hw, y, 0, 14), 3, GLASS, { prof: 'even', nb: 0, a: .7 })); }
  // inside: a palm and two olive trees
  const palm = withSeed(77, () => fanPalm(1.0)); palm.S.forEach(s => S.push(s));
  for (const x of [-190, 190]) {
    const tr = mk(qcurve(x, 0, x + rnd(-14, 14), -120, rnd(-12, 12), 10), 12, '#7a6650', { prof: 'trunk', nb: 3 }); S.push(tr, edgeOf(tr, 1, 1.6, INK));
    clump(S, x, -150, 60, 34, OLIVE, { l0: 10, l1: 20, w0: 5, w1: 9, sx: 1.3, sy: .7 });
  }
  for (let i = 0; i < 16; i++) { const x = rnd(-260, 260); S.push(mk(qcurve(x, -6, x + 3, -10, 0, 3), rnd(10, 15), pick(['#d97a5a', '#e7c35a', '#c99ad0']), { prof: 'leaf', nb: 0, a: .85 })); }
  return S;
}
function seasonsTree() {
  const S = [];
  ground(S, 860, SOIL);
  const tr = mk(qcurve(0, 0, 8, -330, 14, 12), 34, '#8a6a4a', { prof: 'trunk', nb: 5, a: .95 }); S.push(tr, edgeOf(tr, 1, 2.6, INK), edgeOf(tr, -1, 1.4, '#5e4a38', .7));
  for (const [a, l] of [[-2.3, 170], [-1.9, 200], [-1.25, 190], [-.85, 170], [-1.57, 150]]) S.push(mk(polar(6, -300, a, l, rnd(-20, 20), 8), 12, '#7a5c40', { prof: 'tip', nb: 2 }));
  // crown in four quarters: spring (upper left), summer (upper right), autumn (lower right), winter (lower left)
  const cx = 6, cy = -470;
  [[-1, -1], [1, -1], [1, 1], [-1, 1]].forEach(([sx, sy], q) => {
    for (let i = 0; i < 70; i++) {
      const a = rnd(0, Math.PI / 2), r = 230 * Math.sqrt(rnd());
      const x = cx + sx * Math.cos(a) * r * 1.15, y = cy + sy * Math.sin(a) * r * .8;
      const d = rnd(0, Math.PI * 2), len = rnd(12, 24), w = rnd(8, 15);
      S.push(mk(qcurve(x, y, x + Math.cos(d) * len, y + Math.sin(d) * len, rnd(-3, 3), 5), w, pick(SEASON[q]), { prof: 'leaf', nb: w > 12 ? 3 : 0, a: rnd(.75, .95), rough: .3 }));
    }
  });
  // fallen leaves under the autumn side, snow dabs under winter
  for (let i = 0; i < 12; i++) { const x = rnd(60, 300), y = rnd(-6, 8); S.push(mk(qcurve(x, y, x + 8, y + 2, 0, 3), rnd(8, 12), pick(SEASON[2]), { prof: 'leaf', nb: 0, a: .85 })); }
  for (let i = 0; i < 10; i++) { const x = rnd(-300, -60), y = rnd(-6, 8); S.push(mk(qcurve(x, y, x + 10, y, 0, 3), rnd(10, 16), pick(SEASON[3]), { prof: 'leaf', nb: 0, a: .9 })); }
  return S;
}
function forest() {
  const S = [];
  ground(S, 960, '#8f9a6a');
  // a winding path in the middle
  const xs = [-400, -280, -160, 150, 270, 390];
  xs.forEach((x, i) => {
    const h = rnd(420, 560), tr = mk(qcurve(x, 0, x + rnd(-10, 10), -h, rnd(-10, 10), 12), rnd(14, 20), '#6a5440', { prof: 'trunk', nb: 3 });
    S.push(tr, edgeOf(tr, 1, 1.8, INK));
    for (let k = 0; k < 3; k++) clump(S, x + rnd(-20, 20), -h + k * 70, 80 - k * 8, 30, i % 2 ? G2 : G1, { l0: 12, l1: 24, w0: 7, w1: 13, sx: 1.1, sy: .8 });
  });
  return S;
}
function moss() {
  const S = [];
  ground(S, 860, '#7d8a5a');
  // two shade trees at the back give the low garden its height
  for (const [x, sc] of [[-260, 1.15], [280, 1.0]]) withSeed(700 + x, () => rfTree(sc)).S.forEach(q => { q.pts = q.pts.map(([a, b]) => [a + x, b - 70]); q.a *= .8; S.push(q); });
  // shade plants behind (ferns, two shrubs), then stones, then moss cushions over them, a small basin with water
  for (const [x, sc] of [[-300, 1.3], [300, 1.2]]) withSeed(300 + x, () => shrub(sc, G2)).S.forEach(q => { q.pts = q.pts.map(([a, b]) => [a + x, b - 60]); S.push(q); });
  for (const x of [-120, 140, -400, 420]) withSeed(500 + x, () => groundFern(1.6)).S.forEach(q => { q.pts = q.pts.map(([a, b]) => [a + x, b - 20]); S.push(q); });
  const stones = [[-230, -40, 120, 70], [10, -30, 160, 60], [250, -45, 110, 80], [-60, -110, 90, 50]];
  for (const [x, y, w, h] of stones) {
    S.push(mk(qcurve(x - w / 2, y, x + w / 2, y, -h, 14), h * .9, STONE, { prof: 'leaf', nb: 5, a: .55 }));
    S.push(mk(qcurve(x - w / 2, y + h * .2, x + w / 2, y + h * .2, -h * 1.1, 14), 2.2, INK, { prof: 'even', nb: 0, a: .55 }));
  }
  for (const [x, y, w] of [[-230, -70, 70], [10, -55, 90], [250, -80, 60], [-60, -125, 50], [-380, -10, 70], [380, -10, 70], [130, -10, 80]]) {
    for (let i = 0; i < 26; i++) { const a = rnd(Math.PI, Math.PI * 2), r = w * Math.sqrt(rnd()); const px = x + Math.cos(a) * r, py = y + Math.sin(a) * r * .5; S.push(mk(qcurve(px, py, px + 3, py - 3, 0, 3), rnd(9, 15), pick(MOSS), { prof: 'leaf', nb: 0, a: rnd(.7, .95) })); }
  }
  S.push(mk(qcurve(-120, 30, 120, 30, 10, 14), 34, '#8fb0bd', { prof: 'leaf', nb: 4, a: .55 }));
  return S;
}
const BUILD = { welcome, green: greenhouse, seasons: seasonsTree, forest, moss };

// ---------- annotations ----------
let TX = [];
function hand(txt, x, y, p, o = {}) {
  if (p <= 0) return;
  ctx.save(); ctx.font = o.font || `${o.size || 66}px "Nanum Pen Script"`; ctx.textAlign = o.align || 'left'; ctx.textBaseline = 'middle';
  const w = ctx.measureText(txt).width, x0 = o.align === 'right' ? x - w : o.align === 'center' ? x - w / 2 : x;
  ctx.beginPath(); ctx.rect(x0 - 8, y - 60, (w + 16) * eo(p), 120); ctx.clip();
  ctx.shadowColor = rgba(PAPER, .95); ctx.shadowBlur = 12; ctx.fillStyle = rgba(o.col || INK, o.a ?? .9); ctx.fillText(txt, x, y); ctx.fillText(txt, x, y);
  ctx.restore();
  return [x0, w];
}
function leader(x0, y0, x1, y1, p, bend = 20, a = .75) {
  if (p <= 0) return;
  const pts = qcurve(x0, y0, x1, y1, bend, 16), m = Math.max(1, Math.round(16 * clamp(p)));
  ctx.beginPath(); for (let i = 0; i <= m; i++) i ? ctx.lineTo(pts[i][0], pts[i][1]) : ctx.moveTo(pts[i][0], pts[i][1]);
  ctx.strokeStyle = rgba(INK, a); ctx.lineWidth = 2; ctx.lineCap = 'round'; ctx.stroke();
  if (p >= 1) { ctx.beginPath(); ctx.arc(x1, y1, 4, 0, 7); ctx.fillStyle = rgba(INK, a); ctx.fill(); }
}
function textItem(id, txt, x, yScreen, w, h) { if (yScreen - h / 2 > -10 && yScreen + h / 2 < H + 10) TX.push({ id, text: txt, x0: x, y0: yScreen - h / 2, x1: x + w, y1: yScreen + h / 2 }); }

// ---------- world ----------
let SPR = {}, STR = {}, SKYS, TITLE_LINE, STAMP_S;
const SC = 1.3;   // stations are drawn 1.3× so the subject fills a third of the frame
function drawStation(st, i, t, cy) {
  const p = prog(st, t), oy = st.y + 300 - cy;   // ground line sits 300 px under the station centre
  if (oy < -900 || oy > H + 200 || p <= 0) return;
  ctx.save(); ctx.translate(540, oy); ctx.scale(SC, SC);
  const sway = p >= 1 && (st.id === 'seasons' || st.id === 'forest') ? .006 * Math.sin(1.15 * t + i) + .0024 * Math.sin(2.7 * t + 2 * i) : 0;
  if (sway) ctx.transform(1, 0, sway, 1, 0, 0);
  if (p < 1) drawList(ctx, STR[st.id], ss(p)); else ctx.drawImage(SPR[st.id].cv, SPR[st.id].x0, SPR[st.id].y0);
  ctx.restore();
  // the note: written as the station rises; placed under the subject so it stays on screen longest
  const ny = st.y + 430 - cy, lx = st.side > 0 ? 600 : 90, ax = st.side > 0 ? 800 : 280, ay = st.y + 140 - cy;
  const np = noteProg(st, t, cy);
  if (np > 0) {
    leader(lx + (st.side > 0 ? 40 : 200), ny - 40, ax, ay, seg(np, 0, .5), st.side * 18);
    const r = hand(st.kr, lx, ny, seg(np, .2, .9));
    hand(st.en, lx + 4, ny + 58, seg(np, .4, 1), { font: '500 22px "DM Mono"', a: .7 });
    if (r) { textItem('n' + i, st.kr, r[0], ny, r[1], 70); ctx.font = '500 22px "DM Mono"'; textItem('e' + i, st.en, lx, ny + 58, ctx.measureText(st.en).width, 26); }
  }
}

function render(t) {
  TX = [];
  ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1;
  const cy = camY(t);
  ctx.drawImage(PAPER_IMG, 0, cy, W, H, 0, 0, W, H);
  // morning sky wash at the head of the page
  if (cy < 900) { ctx.save(); ctx.translate(0, -cy); drawList(ctx, SKYS, ss(seg(t, .2, 1.6))); ctx.restore(); }
  // title + date
  const ty = 330 - cy;
  if (ty > -200) {
    const p1 = seg(t, .3, 1.2);
    if (p1 > 0) {
      ctx.save(); ctx.font = '700 118px "Gowun Batang"'; ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      const w = ctx.measureText('가학산 수목원').width; ctx.beginPath(); ctx.rect(540 - w / 2 - 10, ty - 90, (w + 20) * eo(p1), 180); ctx.clip();
      ctx.fillStyle = rgba(INK, .88); ctx.fillText('가학산 수목원', 540, ty); ctx.restore();
      textItem('title', '가학산 수목원', 540 - w / 2, ty, w, 130);
    }
    ctx.save(); ctx.translate(0, -cy); drawS(ctx, TITLE_LINE, seg(t, .6, 1.2)); ctx.restore();
    const r = hand('2026.10.28 개장', 540, ty + 150, seg(t, .7, 1.4), { align: 'center', size: 72 });
    if (r) textItem('date', '2026.10.28 개장', r[0], ty + 150, r[1], 70);
  }
  ST.forEach((st, i) => drawStation(st, i, t, cy));
  // the stamp: one accent, thumps down at the foot of the page
  const sy = STAMP.y - cy, k = seg(t, STAMP.t, STAMP.t + .16);
  if (k > 0) {
    ctx.save(); ctx.translate(STAMP.x, sy); ctx.rotate(-.06); const sc = lerp(1.25, 1, eo(k)); ctx.scale(sc, sc); ctx.globalAlpha = clamp(k * 2);
    ctx.drawImage(STAMP_S, -STAMP_S.width / 2, -STAMP_S.height / 2);
    ctx.restore();
    textItem('stamp', '무료 개방', STAMP.x - 230, sy, 460, 160);
  }
}

// a stamp: vermilion square, characters left as paper, uneven ink
function buildStamp() {
  const c = document.createElement('canvas'); c.width = 520; c.height = 220; const g = c.getContext('2d');
  withSeed(91, () => {
    for (let p = 0; p < 3; p++) { g.globalAlpha = .45; g.fillStyle = VERM; g.beginPath(); g.moveTo(14 + rnd(-4, 4), 14 + rnd(-4, 4)); g.lineTo(506 + rnd(-4, 4), 12 + rnd(-4, 4)); g.lineTo(508 + rnd(-4, 4), 206 + rnd(-4, 4)); g.lineTo(12 + rnd(-4, 4), 208 + rnd(-4, 4)); g.closePath(); g.fill(); }
    g.globalAlpha = 1; g.globalCompositeOperation = 'destination-out';
    g.font = '700 110px "Gowun Batang"'; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText('무료 개방', 260, 116);
    for (let i = 0; i < 260; i++) { g.globalAlpha = rnd(.1, .5); g.beginPath(); g.arc(rnd(0, 520), rnd(0, 220), rnd(1, 5), 0, 7); g.fill(); }
  });
  return c;
}

window.DUR = DUR;
window.render = render;
window.TEXTS = t => { render(t); return TX; };
window.EV = [
  { t: .3, type: 'pencil', d: .9 }, { t: .2, type: 'wash', d: 1.4, pan: 0 }, { t: .7, type: 'pencil', d: .7 },
  ...ST.map((st, i) => ({ t: i ? tAt(st.y - 1880) : 1.4, type: 'paint', i, d: 1.6, pan: st.side * .4 })),
  ...ST.map((st, i) => ({ t: i ? tAt(st.y - 1880 + 80) + .2 : 2.2, type: 'pencil', d: .8 })),
  { t: tAt(6200 - 1880) + 1.2, type: 'drip' },
  { t: STAMP.t, type: 'stamp' },
].sort((a, b) => a.t - b.t);

(async () => {
  PAPER_IMG = new Image(); PAPER_IMG.src = 'paper.jpg'; await PAPER_IMG.decode();
  await Promise.all(['700 118px "Gowun Batang"', '66px "Nanum Pen Script"', '500 22px "DM Mono"'].map(f => document.fonts.load(f, '가학산수목원개장')));
  withSeed(5, () => {
    SKYS = [];
    for (let i = 0; i < 6; i++) SKYS.push(mk(qcurve(-40, 80 + i * 90 + rnd(-10, 10), 1120, 90 + i * 90 + rnd(-10, 10), rnd(-20, 20), 20), rnd(70, 110), i < 3 ? SKY : SKY2, { prof: 'even', nb: 6, a: .22 - i * .025, rough: .35 }));
    TITLE_LINE = mk(qcurve(250, 420, 830, 420, -6, 20), 5, INK2, { prof: 'even', nb: 3, a: .8, rough: .3 });
  });
  ST.forEach((st, i) => { STR[st.id] = withSeed(100 + i * 17, BUILD[st.id]); SPR[st.id] = sprite(STR[st.id]); });
  STAMP_S = buildStamp();
  render(0); window.READY = true;
})();
