// 가학산 수목원 개장 — Caption News, graphics only. 1080×1920, 15.0 s, 120 BPM (cuts on the 0.5 s grid).
const cv = document.getElementById('out'), ctx = cv.getContext('2d');
const W = 1080, H = 1920, DUR = 15.0;
const clamp = (x, a = 0, b = 1) => Math.max(a, Math.min(b, x));
const lerp = (a, b, t) => a + (b - a) * t;
const seg = (t, a, b) => clamp((t - a) / (b - a));
const eo = t => 1 - Math.pow(1 - clamp(t), 3);
const eio = t => { t = clamp(t); return t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; };
const back = (t, s = 1.8) => { t = clamp(t) - 1; return 1 + t * t * ((s + 1) * t + s); };

// ---------- palette, type, layout ----------
const C = { bg: '#0E0E0C', panel: '#151512', ink: '#F7F5EF', honey: '#FFC21A', grey: '#8D8B83', dark: '#5E5C55', rule: 'rgba(255,255,255,.045)', margin: 'rgba(255,74,61,.38)', dim: 'rgba(247,245,239,.74)' };
const F = { k9: "'KR9'", k5: "'KR5'", n8: "'IT','KR9'", n5: "'IT','KR5'" };
const BAND = 1560;                                  // graphics band; captions live below it
const CAP = { x: 80, y1: 1670, y2: 1785, zh: 92, enY: 1865, en: 34 };

// ---------- edit list ----------
const SHOTS = [
  { a: 0.0, b: 3.5, k: 'hook' },
  { a: 3.5, b: 6.5, k: 'site' },
  { a: 6.5, b: 10.0, k: 'spaces' },
  { a: 10.0, b: 15.0, k: 'turn' },
];
// captions: [start, end, chunk1, chunk2, english, delay of chunk 2]
const CAPS = [
  [0.15, 3.5, '가학산 수목원', '{개장}', 'A ₩12bn arboretum opens', .12],
  [3.5, 6.5, '부지', '{3만 2,820㎡}', 'Site: 32,820 m²', .12],
  [6.5, 10.0, '테마 공간', '{5곳}', 'Five themed spaces', .12],
  [10.0, 15.0, '입장료는?', '{무료}', 'And admission? Free.', 1.0],
];
const SPACES = [
  ['환영의 정원', 'WELCOME GARDEN'], ['학온재', 'GREENHOUSE HAKONJAE', '광명시 첫 전시온실'],
  ['사계정원', 'FOUR SEASONS GARDEN'], ['도담도담숲', 'DODAM FOREST'], ['이끼원', 'MOSS GARDEN'],
];
const SRC = '광명시 발표 · 2026.10.28';
const shotAt = t => SHOTS.find(s => t >= s.a && t < s.b) || SHOTS[SHOTS.length - 1];

// ---------- helpers ----------
let TX = [];
function text(s, x, y, font, color, align = 'left') { ctx.font = font; ctx.fillStyle = color; ctx.textAlign = align; ctx.textBaseline = 'alphabetic'; ctx.fillText(s, x, y); }
function tw(s, font) { ctx.font = font; return ctx.measureText(s).width; }
function flash(t, t0, dur = 0.12) { const k = seg(t, t0, t0 + dur); if (k <= 0 || k >= 1) return; ctx.fillStyle = `rgba(255,255,255,${(1 - k) * .9})`; ctx.fillRect(0, 0, W, H); }
function camPush(t, a, b, z0 = 1, z1 = 1.03, cx = W / 2, cy = BAND / 2) { const z = lerp(z0, z1, eio(seg(t, a, b))); ctx.translate(cx, cy); ctx.scale(z, z); ctx.translate(-cx, -cy); }
// the ledger sheet: ruled lines and a red double margin; slides in from the right with speed streaks
function ledger(t, a, slide = true) {
  const k = slide ? eo(seg(t, a, a + .18)) : 1, ox = (1 - k) * 140;
  ctx.fillStyle = C.panel; ctx.fillRect(0, 0, W, BAND);
  ctx.save(); ctx.translate(ox, 0);
  ctx.strokeStyle = C.rule; ctx.lineWidth = 2;
  for (let y = 120; y < BAND; y += 54) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke(); }
  ctx.strokeStyle = C.margin; [58, 66].forEach(x => { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, BAND); ctx.stroke(); });
  ctx.restore();
  if (slide && k < 1) { ctx.fillStyle = `rgba(255,194,26,${(1 - k) * .5})`; for (let i = 0; i < 7; i++) ctx.fillRect(W - (1 - k) * 700 - i * 90, 240 + i * 170, 480 * (1 - k), 3); }
}
// numbers with fixed-width digits, so a rolling counter never jitters
function num(s, x, y, size, color, align = 'left', font = F.n8) {
  ctx.font = `800 ${size}px ${font}`; const cw = ctx.measureText('0').width;
  const wOf = ch => /[0-9]/.test(ch) ? cw : ctx.measureText(ch).width;
  let total = 0; for (const ch of s) total += wOf(ch);
  let cx = align === 'right' ? x - total : align === 'center' ? x - total / 2 : x;
  ctx.fillStyle = color; ctx.textBaseline = 'alphabetic';
  for (const ch of s) { const w = wOf(ch); ctx.textAlign = 'center'; ctx.fillText(ch, cx + w / 2, y); cx += w; }
  return total;
}
function numW(s, size, font = F.n8) { ctx.font = `800 ${size}px ${font}`; const cw = ctx.measureText('0').width; let w = 0; for (const ch of s) w += /[0-9]/.test(ch) ? cw : ctx.measureText(ch).width; return w; }
function slam(t, t0, x, y, draw, s0 = 1.3) {   // scale 1.3 → 1 with a back ease, opacity in 0.04 s
  if (t < t0) return false; const k = seg(t, t0, t0 + .13);
  ctx.save(); ctx.translate(x, y); const sc = lerp(s0, 1, back(k, 1.6)); ctx.scale(sc, sc); ctx.globalAlpha = clamp(k * 3); draw(); ctx.restore(); return true;
}
// channel bug (fictional channel) + topic, top safe area
function bug() {
  const x = 60, y = 70;
  const w1 = tw('초록브리핑', `900 30px ${F.k9}`) + 32;
  ctx.fillStyle = C.honey; ctx.fillRect(x, y, w1, 50); text('초록브리핑', x + 16, y + 36, `900 30px ${F.k9}`, C.bg);
  const w2 = tw('가학산 수목원', `900 28px ${F.k9}`) + tw('GREEN BRIEF', `800 18px ${F.n8}`) + 54;
  ctx.fillStyle = 'rgba(14,14,12,.72)'; ctx.fillRect(x + w1, y, w2, 50);
  text('가학산 수목원', x + w1 + 18, y + 35, `900 28px ${F.k9}`, C.ink);
  text('GREEN BRIEF', x + w1 + w2 - 18, y + 33, `800 18px ${F.n8}`, C.honey, 'right');
  TX.push({ id: 'bug', text: '초록브리핑 가학산 수목원', x0: x, y0: y, x1: x + w1 + w2, y1: y + 50 });
}
function sourceTag(t, t0, label = SRC) {   // slides in under the bug whenever a figure is on screen
  const k = eo(seg(t, t0, t0 + .2)); if (k <= 0) return; ctx.save(); ctx.globalAlpha = k; const x = 60 - (1 - k) * 40, y = 132;
  const w1 = tw('출처', `900 22px ${F.k9}`) + 20, w2 = tw(label, `500 21px ${F.n5}`) + 22;
  ctx.fillStyle = C.ink; ctx.fillRect(x, y, w1, 38); text('출처', x + 10, y + 27, `900 22px ${F.k9}`, C.bg);
  ctx.fillStyle = 'rgba(14,14,12,.85)'; ctx.fillRect(x + w1, y, w2, 38); text(label, x + w1 + 11, y + 26, `500 21px ${F.n5}`, C.ink); ctx.restore();
}

// ---------- caption card: two stacked chunks + one English line ----------
function chunk(p, x, y, size, t, t0) {
  let cx = 0;
  for (const q of p.split(/(\{[^}]+\})/).filter(Boolean)) {
    const hot = q[0] === '{', s = hot ? q.slice(1, -1) : q, f = `900 ${size}px ${F.k9}`, w = tw(s, f);
    if (hot) { const wk = eo(seg(t, t0 + .05, t0 + .17)); ctx.fillStyle = C.honey; ctx.fillRect(cx - 14, -size * .9, (w + 28) * wk, size * 1.12); }
    else { ctx.shadowColor = 'rgba(0,0,0,.6)'; ctx.shadowBlur = 20; ctx.shadowOffsetY = 4; }
    text(s, cx, 0, f, hot ? C.bg : C.ink); ctx.shadowColor = 'transparent'; cx += w + (hot ? 28 : 0);
  }
}
function capCard(c, t) {
  const [a, b, c1, c2, en, d2] = c; if (t < a || t >= b) return;
  const size = CAP.zh;
  slam(t, a + .04, CAP.x, CAP.y1, () => chunk(c1, 0, 0, size, t, a + .04));
  slam(t, a + .04 + d2, CAP.x + (c2[0] === '{' ? 14 : 0), CAP.y2, () => chunk(c2, 0, 0, size, t, a + .04 + d2));
  const e0 = a + (d2 > .5 ? d2 : .1), ke = eo(seg(t, e0, e0 + .18));
  ctx.save(); ctx.globalAlpha = ke; text(en, CAP.x, CAP.enY + (1 - ke) * 10, `500 ${CAP.en}px ${F.n5}`, C.dim); ctx.restore();
  const plain = (c1 + ' ' + c2).replace(/[{}]/g, '');
  TX.push({ id: 'cap' + a, text: plain, x0: CAP.x, y0: CAP.y1 - size, x1: CAP.x + 900, y1: CAP.y2 + 20 });
  if (t >= e0) TX.push({ id: 'capen' + a, text: en, x0: CAP.x, y0: CAP.enY - 34, x1: CAP.x + tw(en, `500 ${CAP.en}px ${F.n5}`), y1: CAP.enY + 8 });
}

// ---------- shots ----------
// 0–3.5: the hook — 120억 lands on frame 4 and rolls
function shotHook(s, t) {
  ledger(t, s.a, false);
  ctx.save(); camPush(t, s.a, s.b, 1, 1.03);
  slam(t, .06, 100, 640, () => { ctx.shadowColor = 'rgba(0,0,0,.6)'; ctx.shadowBlur = 22; text('총사업비', 0, 0, `900 104px ${F.k9}`, C.ink); });
  if (t >= .13) {
    const roll = Math.round(120 * eo(seg(t, .13, .58)));
    slam(t, .13, 100, 1010, () => {
      const nf = 280, wn = numW('120', nf), wu = tw('억', `900 160px ${F.k9}`);
      ctx.fillStyle = C.honey; ctx.fillRect(-26, -246, wn + wu + 84, 306);
      num(String(roll), wn, 0, nf, C.bg, 'right'); text('억', wn + 24, -12, `900 160px ${F.k9}`, C.bg);
    }, 1.45);
    slam(t, .4, 100, 1210, () => { ctx.shadowColor = 'rgba(0,0,0,.6)'; ctx.shadowBlur = 22; text('원 투입', 0, 0, `900 104px ${F.k9}`, C.ink); });
    const ke = eo(seg(t, .3, .5)); ctx.save(); ctx.globalAlpha = ke; text('TOTAL PROJECT COST · KRW', 104, 1290, `500 30px ${F.n5}`, C.grey); ctx.restore();
  }
  ctx.restore();
  sourceTag(t, .25);
  TX.push({ id: 'hook', text: '총사업비 120억 원 투입', x0: 70, y0: 520, x1: 1000, y1: 1300 });
}

// 3.5–6.5: site area — counter + a plot that fills row by row
function shotSite(s, t) {
  ledger(t, s.a);
  ctx.save(); camPush(t, s.a + .18, s.b, 1, 1.025);
  const k0 = eo(seg(t, s.a + .1, s.a + .3));
  ctx.save(); ctx.globalAlpha = k0; text('부지 면적', 100, 330, `900 64px ${F.k9}`, C.ink); text('SITE AREA', 104, 380, `500 28px ${F.n5}`, C.grey); ctx.restore();
  const v = Math.round(32820 * eo(seg(t, s.a + .1, s.a + .7))), arrived = t >= s.a + .7;
  const str = v.toLocaleString('en-US');
  if (t >= s.a + .1) { const w = num(str, 100, 620, 190, arrived ? C.honey : C.ink); text('㎡', 100 + w + 16, 620, `900 110px ${F.k9}`, arrived ? C.honey : C.ink); }
  // plot plan: 12×12 cells fill from the top-left, row by row
  const x0 = 140, y0 = 720, S = 800, n = 12, cs = S / n, p = seg(t, s.a + .8, s.a + 1.8);
  ctx.strokeStyle = 'rgba(247,245,239,.35)'; ctx.lineWidth = 2; ctx.setLineDash([10, 8]);
  ctx.strokeRect(x0, y0, S * eo(seg(t, s.a + .2, s.a + .7)), S * eo(seg(t, s.a + .3, s.a + .8))); ctx.setLineDash([]);
  const filled = Math.floor(p * n * n);
  for (let i = 0; i < filled; i++) { const r = Math.floor(i / n), c = i % n; ctx.fillStyle = (r + c) % 2 ? 'rgba(255,194,26,.30)' : 'rgba(255,194,26,.22)'; ctx.fillRect(x0 + c * cs + 2, y0 + r * cs + 2, cs - 4, cs - 4); }
  ctx.restore();
  sourceTag(t, s.a + .2);
  TX.push({ id: 'site', text: '부지 면적 32,820㎡', x0: 90, y0: 270, x1: 1000, y1: 640 });
}

// 6.5–10.0: five spaces — panels rise staggered, each lights in turn
function shotSpaces(s, t) {
  ledger(t, s.a, false);
  const k0 = eo(seg(t, s.a - .1, s.a + .15));
  ctx.save(); ctx.globalAlpha = k0; text('5개 테마 공간', 100, 290, `900 60px ${F.k9}`, C.ink); ctx.restore();
  const PH = 214, G = 18, y0 = 340;
  for (let i = 0; i < 5; i++) {
    const t0 = s.a - .12 + i * .1, k = eo(seg(t, t0, t0 + .28)), dy = (1 - k) * 260, y = y0 + i * (PH + G) + dy;
    const on = t >= s.a + .5 + i * .5, kf = 1 - seg(t, s.a + .5 + i * .5, s.a + .8 + i * .5);
    ctx.save(); ctx.globalAlpha = clamp(k * 1.5);
    ctx.fillStyle = on ? 'rgba(255,194,26,.10)' : 'rgba(14,14,12,.72)'; ctx.fillRect(100, y, W - 160, PH);
    if (on) { ctx.strokeStyle = `rgba(255,194,26,${.5 + .5 * kf})`; ctx.lineWidth = 6; ctx.strokeRect(103, y + 3, W - 166, PH - 6); }
    num(String(i + 1), 132, y + 62, 34, C.honey);
    const [kr, en, tag] = SPACES[i], f = `900 80px ${F.k9}`, wk = tw(kr, f);
    ctx.fillStyle = on ? C.honey : 'rgba(0,0,0,0)'; ctx.fillRect(176, y + 40, (wk + 36) * (on ? eo(seg(t, s.a + .5 + i * .5, s.a + .62 + i * .5)) : 0), 104);
    text(kr, 194, y + 120, f, on ? C.bg : C.ink);
    text(en, 196, y + 184, `500 28px ${F.n5}`, C.grey);
    if (tag) { const ft = `900 30px ${F.k9}`, wt = tw(tag, ft) + 28; ctx.fillStyle = C.ink; ctx.fillRect(W - 80 - wt, y + 24, wt, 48); text(tag, W - 94, y + 59, ft, C.bg, 'right'); }
    ctx.restore();
    TX.push({ id: 'sp' + i, text: kr, x0: 176, y0: y0 + i * (PH + G) + 40, x1: 176 + wk + 36, y1: y0 + i * (PH + G) + 144 });
    TX.push({ id: 'spen' + i, text: en, x0: 196, y0: y0 + i * (PH + G) + 160, x1: 196 + tw(en, `500 28px ${F.n5}`), y1: y0 + i * (PH + G) + 192 });
    if (tag) TX.push({ id: 'sptag', text: tag, x0: W - 360, y0: y0 + i * (PH + G) + 24, x1: W - 80, y1: y0 + i * (PH + G) + 72 });
  }
  TX.push({ id: 'sphead', text: '5개 테마 공간', x0: 100, y0: 230, x1: 600, y1: 300 });
  sourceTag(t, s.a + .1);
}

// 10.0–15.0: the turn — silence, a grey "?" block; flash; the block rolls 120 → 0 and lands as 0원
function shotTurn(s, t) {
  ledger(t, s.a, false);
  const T = 11.0;
  ctx.save();
  const pz = t >= T ? lerp(1, 1.08, eo(seg(t, T, T + .3))) : 1; ctx.translate(W / 2, 920); ctx.scale(pz, pz); ctx.translate(-W / 2, -920);
  slam(t, s.a + .06, 100, 600, () => { text('ADMISSION', 0, 0, `800 40px ${F.n8}`, C.grey); });
  const nf = 330;
  if (t < T) {
    slam(t, s.a + .1, 100, 960, () => {
      const pulse = 1 + .03 * Math.sin((t - s.a) * Math.PI * 4);
      ctx.scale(pulse, pulse); ctx.fillStyle = C.dark; ctx.fillRect(-26, -282, 380, 350); num('?', 164, 0, nf, C.ink, 'center');
    });
  } else {
    const k = seg(t, T, T + .45), v = Math.round(120 * (1 - eo(k))), done = k >= 1;
    slam(t, T, 100, 960, () => {
      const s2 = done ? '0' : String(v), unit = done ? '원' : '억';
      const wn = numW(s2, nf), wu = tw(unit, `900 190px ${F.k9}`);
      ctx.fillStyle = done ? C.honey : C.ink; ctx.fillRect(-26, -282, wn + wu + 90, 350);
      num(s2, wn, 0, nf, C.bg, 'right'); text(unit, wn + 26, -12, `900 190px ${F.k9}`, C.bg);
    }, 1.2);
  }
  ctx.restore();
  // verdict lines
  const l1 = eo(seg(t, 11.6, 11.8)), l2 = eo(seg(t, 12.0, 12.2));
  if (l1 > 0) { ctx.save(); ctx.globalAlpha = l1; ctx.translate(0, (1 - l1) * 20); ctx.fillStyle = C.honey; ctx.fillRect(100, 1160, 10, 70); text('외부 정원 상시 개방', 136, 1216, `900 60px ${F.k9}`, C.ink); text('Outdoor gardens open at all hours', 138, 1270, `500 28px ${F.n5}`, C.grey); ctx.restore(); }
  if (l2 > 0) { ctx.save(); ctx.globalAlpha = l2; ctx.translate(0, (1 - l2) * 20); ctx.fillStyle = C.honey; ctx.fillRect(100, 1340, 10, 70); text('학온재 09:00–18:00', 136, 1396, `900 60px ${F.n8}`, C.ink); text('Greenhouse & café hours', 138, 1450, `500 28px ${F.n5}`, C.grey); ctx.restore(); }
  sourceTag(t, 11.1);
  TX.push({ id: 'adm', text: 'ADMISSION', x0: 100, y0: 560, x1: 400, y1: 610 });
  if (t >= T) TX.push({ id: 'zero', text: '0원', x0: 74, y0: 660, x1: 600, y1: 1030 });
  if (t >= 11.6) TX.push({ id: 'v1', text: '외부 정원 상시 개방', x0: 100, y0: 1160, x1: 900, y1: 1230 });
  if (t >= 12.0) TX.push({ id: 'v2', text: '학온재 09:00–18:00', x0: 100, y0: 1340, x1: 900, y1: 1410 });
}

function draw(t) {
  TX = []; ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1;
  ctx.fillStyle = C.bg; ctx.fillRect(0, 0, W, H);
  const s = shotAt(t);
  ctx.save(); ctx.beginPath(); ctx.rect(0, 0, W, BAND); ctx.clip();
  if (s.k === 'hook') shotHook(s, t);
  else if (s.k === 'site') shotSite(s, t);
  else if (s.k === 'spaces') shotSpaces(s, t);
  else shotTurn(s, t);
  ctx.restore();
  // caption zone: soft shade from the band into the dark
  const g = ctx.createLinearGradient(0, BAND - 160, 0, BAND); g.addColorStop(0, 'rgba(14,14,12,0)'); g.addColorStop(1, 'rgba(14,14,12,1)');
  ctx.fillStyle = g; ctx.fillRect(0, BAND - 160, W, 160);
  ctx.fillStyle = 'rgba(255,194,26,.9)'; ctx.fillRect(CAP.x, BAND + 12, 60, 4);
  for (const c of CAPS) capCard(c, t);
  bug();
  if (s.k === 'turn') flash(t, 11.0, .14);   // the white flash on the turn covers the whole frame
}

// cue events for the mixer
const EV = [
  { t: .06, type: 'slam' }, { t: .13, type: 'hook' }, { t: .4, type: 'slam' },
  { t: 3.5, type: 'card' }, { t: 3.6, type: 'rattle', d: .6 }, { t: 4.3, type: 'fill', d: 1.0 },
  { t: 6.38, type: 'rise' }, ...[0, 1, 2, 3, 4].map(i => ({ t: 7.0 + i * .5, type: 'light', i })),
  { t: 10.0, type: 'stop' }, { t: 10.06, type: 'slam' },
  { t: 11.0, type: 'turn' }, { t: 11.0, type: 'rattle', d: .45 },
  { t: 11.6, type: 'pop' }, { t: 12.0, type: 'pop' }, { t: 14.0, type: 'bells' },
  ...CAPS.flatMap(c => [{ t: c[0] + .04, type: 'chunk' }, { t: c[0] + .04 + c[5], type: 'chunk' }]),
].sort((a, b) => a.t - b.t);

window.DUR = DUR; window.EV = EV;
window.render = t => draw(t);
window.TEXTS = t => { draw(t); return TX; };
document.fonts.load(`900 40px ${F.k9}`, '가').then(() => Promise.all([document.fonts.load(`500 40px ${F.k5}`, '가'), document.fonts.load('800 40px IT', 'A'), document.fonts.load('500 40px IT', 'A')]))
  .then(() => { draw(0); window.READY = true; });
