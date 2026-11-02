// 광명 돔구장 유치 서명운동 — Broadcast News Package, campaign promo. 1080×1920, 15.0 s, 120 BPM (beat 0 at 1.5 s).
// Canvas 2D, deterministic render(t). No narration, no burned-in subtitles (sidecar .srt); one strap from the first graphic to the end card.
const W = 1080, H = 1920, DUR = 15.0;
const cv = document.getElementById('out'), ctx = cv.getContext('2d');

const C = {
  ink: '#121417', ink2: '#1B1E23', ink3: '#22262C', line: '#454B55',
  lemon: '#F2CF1D', lemonL: '#F8DE4A', lemonD: '#D8B30C', gold: '#D4B064',
  paper: '#F4F1E8', white: '#FFFFFF', grey: '#C9CCD2', grey2: '#7D838D', olive: '#6E5A1E',
};
const F = { k9: s => `900 ${s}px KR`, k7: s => `700 ${s}px KR`, b8: s => `800 ${s}px Barlow`, b6: s => `600 ${s}px Barlow`, mono: s => `500 ${s}px Plex` };
const clamp = (x, a = 0, b = 1) => Math.min(b, Math.max(a, x));
const seg = (t, a, b) => clamp((t - a) / (b - a));
const lerp = (a, b, k) => a + (b - a) * k;
const eo = x => 1 - Math.pow(1 - clamp(x), 3);
const eio = x => { x = clamp(x); return x < .5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2; };
const back = (x, s = 1.70158) => 1 + (s + 1) * Math.pow(x - 1, 3) + s * Math.pow(x - 1, 2);
const fmt = n => Math.round(n).toLocaleString('en-US');

let TX = [];
function text(s, x, y, font, color, align = 'left', ls = 0, id = null) {
  ctx.font = font; ctx.letterSpacing = ls + 'px'; ctx.fillStyle = color; ctx.textAlign = align; ctx.textBaseline = 'alphabetic';
  ctx.fillText(s, x, y); const w = ctx.measureText(s).width; ctx.letterSpacing = '0px';
  if (id) { const x0 = align === 'center' ? x - w / 2 : align === 'right' ? x - w : x; const sz = parseInt(font.match(/(\d+)px/)[1]); TX.push({ id, text: s, x0, y0: y - sz, x1: x0 + w, y1: y + sz * .25 }); }
  return w;
}
function tw(s, font, ls = 0) { ctx.font = font; ctx.letterSpacing = ls + 'px'; const w = ctx.measureText(s).width; ctx.letterSpacing = '0px'; return w; }
// a line pushed up out of a mask (starts fully below, never drawn at progress 0)
function push(k, x, y, h, fn) { if (k <= 0) return; ctx.save(); ctx.beginPath(); ctx.rect(0, y - h, W, h + 24); ctx.clip(); ctx.translate(0, (1 - eo(k)) * (h + 30)); fn(); ctx.restore(); }

// ============ Package parts ============
// Brand plate: the campaign's own mark (not a broadcaster). DOME letters drop in.
function plate(cx, cy, s, glint = -1, drop = 1) {
  const w = 520 * s, h = 180 * s, k = h * .28;
  ctx.save(); ctx.translate(cx, cy);
  const path = new Path2D(); path.moveTo(-w / 2 + k, -h / 2); path.lineTo(w / 2 + k, -h / 2); path.lineTo(w / 2 - k, h / 2); path.lineTo(-w / 2 - k, h / 2); path.closePath();
  const g = ctx.createLinearGradient(0, -h / 2, 0, h / 2); g.addColorStop(0, C.lemonL); g.addColorStop(1, C.lemonD);
  ctx.fillStyle = g; ctx.fill(path);
  ctx.save(); ctx.clip(path);
  const gx = -w / 2 - k;
  ctx.fillStyle = C.ink; ctx.beginPath(); ctx.moveTo(gx + 2 * k, -h / 2); ctx.lineTo(gx + 2 * k + w * .085, -h / 2); ctx.lineTo(gx + w * .085, h / 2); ctx.lineTo(gx, h / 2); ctx.fill();
  ctx.fillStyle = C.gold; ctx.beginPath(); ctx.moveTo(gx + 2 * k + w * .085, -h / 2); ctx.lineTo(gx + 2 * k + w * .11, -h / 2); ctx.lineTo(gx + w * .11, h / 2); ctx.lineTo(gx + w * .085, h / 2); ctx.fill();
  ctx.font = F.b8(h * .8); ctx.textAlign = 'left'; ctx.textBaseline = 'alphabetic';
  let x = -w * .25;
  ['D', 'O', 'M', 'E'].forEach((L, i) => {
    const d = clamp(drop * 4 - i), yy = h * .29 + (1 - eo(d)) * -h * 1.1;
    ctx.save(); ctx.globalAlpha *= clamp(d * 3); ctx.fillStyle = C.ink; ctx.fillText(L, x, yy); ctx.restore();
    x += ctx.measureText(L).width - h * .01;
  });
  if (glint >= 0 && glint <= 1) {
    const gx2 = lerp(-w, w, glint), gg = ctx.createLinearGradient(gx2 - 80 * s, 0, gx2 + 80 * s, 0);
    gg.addColorStop(0, 'rgba(255,255,255,0)'); gg.addColorStop(.5, 'rgba(255,255,255,.6)'); gg.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = gg; ctx.fillRect(-w, -h, w * 2, h * 2);
  }
  ctx.restore(); ctx.restore();
}
const BUG = { x: 905, y: 118, s: .3 };
function bug(t) {
  if (t < 1.45 || t >= 10.3) return;
  ctx.globalAlpha = .95; plate(BUG.x, BUG.y, BUG.s, -1, 1);
  text('광명 돔구장', BUG.x - 105, BUG.y + 12, F.k9(30), C.white, 'right', 0, 'bug');
  text('CAMPAIGN', BUG.x - 105, BUG.y + 44, F.mono(16), C.gold, 'right', 3);
  ctx.globalAlpha = 1;
}
// Lower-third strap: one headline for the whole lead package; source line under it
const STRAP = { a: 1.5, b: 10.5, tab: ['서명운동', 'PETITION'], kr: '광명에 5만 석 돔구장을', en: 'BRING A 50,000-SEAT DOME TO GWANGMYEONG' };
function strap(t) {
  if (t < STRAP.a - .3 || t >= STRAP.b) return;
  const x0 = 60, ty = 1556, by = 1608, bh = 150, bw = 840;
  ctx.save();
  ctx.fillStyle = 'rgba(0,0,0,.3)'; ctx.fillRect(x0 + 8, by + 8, bw, bh);
  ctx.fillStyle = C.paper; ctx.fillRect(x0, by, bw, bh);
  ctx.fillStyle = C.lemon; ctx.fillRect(x0, by, 14, bh);
  ctx.fillStyle = C.ink; ctx.fillRect(x0 + bw, by, 120, bh); ctx.fillStyle = C.lemon; ctx.fillRect(x0 + bw, by, 120, 6);
  text('DOME', x0 + bw + 60, by + 92, F.b8(40), C.lemon, 'center', 1);
  text(STRAP.kr, x0 + 40, by + 78, F.k9(60), C.ink, 'left', 0, 'strap');
  text(STRAP.en, x0 + 42, by + 122, F.b6(29), C.olive, 'left', 1.2, 'strape');
  const tabW = 40 + tw(STRAP.tab[0], F.k9(32)) + 14 + tw(STRAP.tab[1], F.b8(32), 2);
  ctx.fillStyle = C.lemon; ctx.fillRect(x0, ty, tabW, by - ty);
  const w1 = text(STRAP.tab[0], x0 + 20, ty + 40, F.k9(32), C.ink); text(STRAP.tab[1], x0 + 20 + w1 + 14, ty + 40, F.b8(32), C.ink, 'left', 2);
  ctx.globalAlpha = .78;
  text('SOURCE: GWANGMYEONG CITY · 2026.10.26', 1020, 1812, F.mono(18), C.gold, 'right', 1.5);
  text('출처 광명시 발표', 1020, 1846, F.k7(22), C.gold, 'right');
  ctx.globalAlpha = 1; ctx.restore();
}
function studioBg(t) {
  const g = ctx.createLinearGradient(0, 0, W, H); g.addColorStop(0, '#1A1D22'); g.addColorStop(1, '#0E1013');
  ctx.fillStyle = g; ctx.fillRect(0, 0, W, H);
  ctx.fillStyle = 'rgba(244,241,232,.04)';
  for (let x = 30 - (t * 10) % 48; x < W; x += 48) for (let y = 24; y < H; y += 48) ctx.fillRect(x, y, 2, 2);
  // light fins
  ctx.save(); ctx.globalAlpha = .06; ctx.fillStyle = C.gold;
  for (let i = 0; i < 4; i++) { const x = 140 + i * 260 + Math.sin(t * .6 + i) * 20; ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x + 60, 0); ctx.lineTo(x - 220, H); ctx.lineTo(x - 280, H); ctx.fill(); }
  ctx.restore();
}
function eyebrow(s, t, a, y = 300) { const k = eo(seg(t, a, a + .25)); ctx.globalAlpha = k; ctx.fillStyle = C.gold; ctx.fillRect(60, y - 26, 8, 34); text(s, 84, y, F.mono(28), C.gold, 'left', 3, 'eyebrow'); ctx.globalAlpha = 1; }
function stamp(t, t0, x, y, s1, s2) {
  const k = seg(t, t0, t0 + .16); if (k <= 0) return;
  const sc = lerp(1.9, 1, eo(k));
  ctx.save(); ctx.translate(x, y); ctx.rotate(-.13); ctx.scale(sc, sc); ctx.globalAlpha = clamp(k * 3);
  const w = Math.max(tw(s1, F.k9(56)), tw(s2, F.b8(26), 3)) + 70, h = 132;
  ctx.fillStyle = C.ink; ctx.fillRect(-w / 2, -h / 2, w, h);
  ctx.strokeStyle = C.lemon; ctx.lineWidth = 5; ctx.strokeRect(-w / 2 + 8, -h / 2 + 8, w - 16, h - 16); ctx.lineWidth = 2; ctx.strokeRect(-w / 2 + 18, -h / 2 + 18, w - 36, h - 36);
  text(s1, 0, 6, F.k9(56), C.lemon, 'center'); text(s2, 0, 42, F.b8(24), C.lemon, 'center', 3);
  ctx.restore();
  const r = seg(t, t0 + .1, t0 + .6); if (r > 0 && r < 1) { ctx.strokeStyle = `rgba(242,207,29,${1 - r})`; ctx.lineWidth = 4; ctx.beginPath(); ctx.ellipse(x, y, 200 + r * 260, 80 + r * 120, -.13, 0, 7); ctx.stroke(); }
  if (k > 0) TX.push({ id: 'stamp', text: s1, x0: x - 200, y0: y - 66, x1: x + 200, y1: y + 66 });
}
function flipTile(x, y, w, h, from, to, t0, t, font, col) {
  const p = from === null ? 1 : seg(t, t0, t0 + .26);
  const half = (s, top, sc = 1) => {
    if (s === null) return;
    ctx.save(); ctx.beginPath(); ctx.rect(x, top ? y : y + h / 2, w, h / 2); ctx.clip();
    ctx.translate(0, y + h / 2); ctx.scale(1, sc); ctx.translate(0, -(y + h / 2));
    ctx.fillStyle = top ? '#2B3038' : '#262A31'; ctx.fillRect(x, y, w, h);
    text(s, x + w / 2, y + h * .78, font, col, 'center');
    ctx.restore();
  };
  if (p >= 1) { half(to, true); half(to, false); }
  else if (p <= 0) { half(from, true); half(from, false); }
  else { half(to, true); half(from, false); if (p < .5) half(from, true, 1 - p * 2); else half(to, false, (p - .5) * 2); }
  ctx.fillStyle = 'rgba(0,0,0,.6)'; ctx.fillRect(x, y + h / 2 - 1.5, w, 3);
}

// ============ Shots ============
function shotSting(t) {
  studioBg(t);
  [[.04, .3, 22, -60], [.1, .36, 8, 40]].forEach(([a, b, h, dy]) => {
    const k = seg(t, a, b); if (k <= 0 || k >= 1) return;
    ctx.save(); ctx.translate(540, 820 + dy); ctx.rotate(-.24);
    const x = lerp(-1500, 1500, eio(k)); ctx.fillStyle = C.lemon; ctx.fillRect(x - 600, -h / 2, 1200, h); ctx.restore();
  });
}
function stingLogo(t) {
  if (t > 1.5) return;
  const k = seg(t, .16, .42), fly = eio(seg(t, 1.12, 1.45));
  const shake = t > .42 && t < .7 ? Math.sin(t * 90) * 14 * (1 - seg(t, .42, .7)) : 0;
  const cx = lerp(560, BUG.x, fly) + shake, cy = lerp(800, BUG.y, fly), s = lerp(back(k, 2.2) * 1.3, BUG.s, fly);
  if (k > 0) { ctx.save(); ctx.translate(cx, cy); ctx.rotate((1 - eo(k)) * -.12 * (1 - fly)); ctx.translate(-cx, -cy); plate(cx, cy, s, seg(t, .85, 1.12) || -1, seg(t, .3, .7)); ctx.restore(); }
  const r = seg(t, .42, .8); if (r > 0 && r < 1) { ctx.strokeStyle = `rgba(242,207,29,${1 - r})`; ctx.lineWidth = 6 * (1 - r) + 1; ctx.beginPath(); ctx.ellipse(560, 800, 360 + r * 500, 140 + r * 260, 0, 0, 7); ctx.stroke(); }
  const wa = seg(t, .5, .8), wo = 1 - seg(t, 1.05, 1.18);
  if (wa > 0 && wo > 0) {
    ctx.save(); ctx.globalAlpha = wo; ctx.beginPath(); ctx.rect(540 - 480 * eo(wa), 960, 960 * eo(wa), 200); ctx.clip();
    text('광명 돔구장 유치', 540, 1060, F.k9(76), C.white, 'center');
    text('CAMPAIGN · 2026', 540, 1120, F.mono(26), C.gold, 'center', 4);
    ctx.restore();
  }
}
// 1.5–4.5: spec card — the dome and its seats
function shotSpec(t) {
  studioBg(t);
  eyebrow('광명시 · 2026.10.26', t, 1.35);
  push(seg(t, 1.4, 1.75), 60, 470, 130, () => text('복합형 돔구장', 56, 470, F.k9(124), C.white, 'left', 0, 'name'));
  push(seg(t, 1.5, 1.85), 60, 540, 50, () => text('MULTI-USE DOME STADIUM', 62, 540, F.b6(40), C.lemon, 'left', 3, 'name-en'));
  // category chip
  const ck = eo(seg(t, 1.7, 1.95)); if (ck > 0) { ctx.globalAlpha = ck; const cw = tw('스포츠·공연', F.k9(40)) + tw('SPORTS & CONCERTS', F.b8(26), 2) + 70; ctx.fillStyle = C.ink3; ctx.fillRect(60, 600, cw, 76); ctx.fillStyle = C.lemon; ctx.fillRect(60, 600, cw, 5); const w1 = text('스포츠·공연', 84, 652, F.k9(40), C.white, 'left', 0, 'chip'); text('SPORTS & CONCERTS', 84 + w1 + 22, 650, F.b8(26), C.gold, 'left', 2); ctx.globalAlpha = 1; }
  // seats: the number rolls, the bar grows
  const nk = seg(t, 2.0, 2.6), v = 50000 * eo(nk);
  ctx.globalAlpha = eo(seg(t, 1.95, 2.1));
  text('좌석 SEATS', 64, 980, F.k7(36), C.grey, 'left', 1, 'seats');
  const nw = text(fmt(v), 56, 1230, F.b8(250), nk >= 1 ? C.lemon : C.white, 'left', 0);
  text('석', 56 + nw + 18, 1230, F.k9(110), nk >= 1 ? C.lemon : C.white);
  ctx.fillStyle = C.ink3; ctx.fillRect(60, 1300, 960, 56); ctx.fillStyle = C.lemon; ctx.fillRect(60, 1300, 960 * eo(nk), 56);
  for (let i = 1; i < 5; i++) { ctx.fillStyle = C.ink; ctx.fillRect(60 + i * 192, 1300, 4, 56); }
  text('0', 60, 1400, F.mono(24), C.grey2); text('50,000', 1020, 1400, F.mono(24), C.grey2, 'right');
  ctx.globalAlpha = 1;
  if (t >= 1.95) TX.push({ id: 'seatsn', text: '50,000석', x0: 56, y0: 1030, x1: 900, y1: 1250 });
}
// 4.5–7.5: survey — responses vs in favour on one scale, then the badge
function shotSurvey(t) {
  studioBg(t);
  eyebrow('SURVEY · 2026.10', t, 4.5);
  push(seg(t, 4.55, 4.85), 60, 470, 130, () => text('시민 설문', 56, 470, F.k9(124), C.white, 'left', 0, 'sv'));
  push(seg(t, 4.65, 4.95), 60, 540, 50, () => text('SURVEYED BY THE CITY', 62, 540, F.b6(40), C.lemon, 'left', 3, 'sv-en'));
  const rows = [['응답', 'RESPONSES', 6830, C.grey, 4.7], ['찬성', 'IN FAVOUR', 5620, C.lemon, 4.95]];
  const max = 6830, BW = 960;
  rows.forEach(([kr, en, val, col, a], i) => {
    const y = 780 + i * 320, k = seg(t, a, a + .45);
    ctx.globalAlpha = eo(seg(t, a - .1, a + .1));
    const w1 = text(kr, 64, y, F.k9(56), C.white, 'left', 0, 'r' + i); text(en, 64 + w1 + 18, y - 4, F.b8(30), C.grey2, 'left', 2);
    ctx.fillStyle = C.ink3; ctx.fillRect(60, y + 30, BW, 70); ctx.fillStyle = col; ctx.fillRect(60, y + 30, BW * val / max * eo(k), 70);
    text(fmt(val * eo(k)) + '명', 1020, y, F.b8(76), k >= 1 ? col : C.white, 'right');
    ctx.globalAlpha = 1;
    if (t >= a - .1) TX.push({ id: 'rv' + i, text: fmt(val) + '명', x0: 700, y0: y - 70, x1: 1020, y1: y + 10 });
  });
  // badge
  const bk = seg(t, 5.3, 5.46);
  if (bk > 0) {
    const sc = lerp(1.6, 1, eo(bk)); ctx.save(); ctx.translate(540, 1410); ctx.scale(sc, sc); ctx.globalAlpha = clamp(bk * 3);
    ctx.fillStyle = C.lemon; ctx.fillRect(-300, -100, 600, 200);
    text('82%', -40, 52, F.b8(170), C.ink, 'center'); text('찬성', 190, 30, F.k9(64), C.ink, 'center'); ctx.restore();
    if (bk > 0) TX.push({ id: 'badge', text: '82% 찬성', x0: 240, y0: 1310, x1: 840, y1: 1510 });
  }
}
// 7.5–10.5: the goal — split-flap boards; the stamp
function shotGoal(t) {
  studioBg(t);
  eyebrow('PETITION · 2026.11', t, 7.5);
  ctx.globalAlpha = eo(seg(t, 7.5, 7.7));
  text('서명 목표', 64, 520, F.k9(64), C.white, 'left', 0, 'g1'); text('SIGNATURE GOAL', 64 + tw('서명 목표', F.k9(64)) + 20, 516, F.b8(32), C.grey2, 'left', 2);
  text('마감', 64, 940, F.k9(64), C.white, 'left', 0, 'g2'); text('DEADLINE', 64 + tw('마감', F.k9(64)) + 20, 936, F.b8(32), C.grey2, 'left', 2);
  ctx.globalAlpha = 1;
  const goal = ['1', '0', ',', '0', '0', '0'], date = ['1', '1', '.', '0', '6'];
  const tile = (arr, y, t0, w, h) => arr.forEach((d, i) => flipTile(64 + i * (w + 12), y, w, h, '0', d, t0 + i * .06, t, F.b8(h * .78), C.lemon));
  tile(goal, 560, 7.6, 132, 200);
  text('명', 64 + 6 * 144 + 6, 730, F.k9(80), C.white);
  tile(date, 980, 8.0, 150, 200);
  if (t >= 7.6) { TX.push({ id: 'goaln', text: '10,000명', x0: 64, y0: 560, x1: 1000, y1: 760 }); TX.push({ id: 'daten', text: '11.06', x0: 64, y0: 980, x1: 860, y1: 1180 }); }
  stamp(t, 7.55, 700, 1390, '문체부 전달', 'TO THE MINISTRY');
}
// 10.5–15: end card — call to action with the QR code
let QR = null;
function shotEnd(t) {
  studioBg(t);
  const m = (a, b) => eo(seg(t, a, b));
  plate(540, 210, .62, seg(t, 11.6, 12.0) || -1, seg(t, 10.4, 10.8));
  push(seg(t, 10.45, 10.75), 60, 470, 130, () => text('지금 서명하기', 540, 470, F.k9(120), C.white, 'center', 0, 'cta'));
  ctx.globalAlpha = m(10.55, 10.8); text('SCAN TO SIGN', 540, 530, F.mono(30), C.lemon, 'center', 6, 'cta-en'); ctx.globalAlpha = 1;
  // QR on a paper card, pops in
  const qk = seg(t, 10.6, 10.85);
  if (qk > 0 && QR) {
    const sz = 560, sc = lerp(.6, 1, back(qk, 1.6)); ctx.save(); ctx.translate(540, 900); ctx.scale(sc, sc); ctx.globalAlpha = clamp(qk * 3);
    ctx.fillStyle = C.paper; ctx.fillRect(-sz / 2 - 24, -sz / 2 - 24, sz + 48, sz + 48);
    ctx.fillStyle = C.lemon; ctx.fillRect(-sz / 2 - 24, -sz / 2 - 24, sz + 48, 10);
    ctx.imageSmoothingEnabled = false; ctx.drawImage(QR, -sz / 2, -sz / 2, sz, sz); ctx.imageSmoothingEnabled = true;
    ctx.restore();
  }
  ctx.globalAlpha = m(10.7, 10.95); text('naver.me/IIDyvaSU', 540, 1290, F.mono(44), C.white, 'center', 2, 'url'); ctx.globalAlpha = 1;
  const dk = m(10.7, 10.95);
  if (dk > 0) { ctx.globalAlpha = dk; const dw = tw('11월 6일까지', F.k9(70)) + 60; ctx.fillStyle = C.lemon; ctx.fillRect(540 - dw / 2, 1360, dw * dk, 110); text('11월 6일까지', 540, 1440, F.k9(70), C.ink, 'center', 0, 'until'); ctx.globalAlpha = 1; }
  ctx.globalAlpha = m(10.8, 11.05);
  text('행정복지센터·도서관 서명부', 540, 1580, F.k7(46), C.white, 'center', 0, 'offline');
  text('OR AT COMMUNITY CENTRES', 540, 1630, F.b6(30), C.grey2, 'center', 3, 'offline-en');
  ctx.fillStyle = C.line; ctx.fillRect(60, 1720, 960, 1);
  text('광명시 돔구장 유치 캠페인 · 출처 광명시 발표', 540, 1780, F.k7(26), C.grey, 'center');
  ctx.globalAlpha = 1;
}

// ============ Compositing ============
function wipeBand(bx) { ctx.fillStyle = C.lemon; ctx.fillRect(bx - 84, 0, 84, H); ctx.fillStyle = C.ink; ctx.fillRect(bx, 0, 5, H); ctx.fillStyle = C.gold; ctx.fillRect(bx + 5, 0, 14, H); }
function layer(fn, t, tov) { fn(t); strap(tov); }
function wipe(t, a, b, oldFn, newFn) {
  const bx = lerp(-140, W + 140, eio(seg(t, a, b)));
  const n0 = TX.length; layer(newFn, t, b + .001); const NEW = TX.splice(n0);
  ctx.save(); ctx.beginPath(); ctx.rect(bx, 0, W, H); ctx.clip(); layer(oldFn, t, a - .001); ctx.restore();
  const OLD = TX.splice(n0); TX.push(...(seg(t, a, b) < .5 ? OLD : NEW));
  wipeBand(bx);
}
const END = t => shotEnd(t + .2);   // the end card's entry starts under the slanted wipe
const SCENES = [[1.5, shotSpec], [4.5, shotSurvey], [7.5, shotGoal], [10.55, END]];
function scene(t) {
  if (t < 1.5) { if (t >= 1.2) wipe(t, 1.2, 1.5, shotSting, shotSpec); else layer(shotSting, t, t); stingLogo(t); return; }
  if (t >= 10.3 && t < 10.55) {   // slanted wipe into the end card (its content already moving); the strap leaves with the old picture
    layer(shotGoal, t, t);
    const k = eio(seg(t, 10.3, 10.55)), x = W - k * (W + 600);
    ctx.save(); ctx.beginPath(); ctx.moveTo(x + 600, 0); ctx.lineTo(W + 600, 0); ctx.lineTo(W + 600, H); ctx.lineTo(x, H); ctx.closePath(); ctx.clip(); TX = []; END(t); ctx.restore();
    ctx.fillStyle = C.lemon; ctx.beginPath(); ctx.moveTo(x + 600, 0); ctx.lineTo(x + 640, 0); ctx.lineTo(x + 40, H); ctx.lineTo(x, H); ctx.fill();
    return;
  }
  let fn = SCENES[0][1]; for (const [a, f] of SCENES) if (t >= a) fn = f;
  layer(fn, t, t);
}
function render(t) {
  TX = []; ctx.setTransform(1, 0, 0, 1, 0, 0); ctx.globalAlpha = 1;
  scene(t); bug(t);
}

const EV = [
  { t: 0.04, type: 'whoosh' }, { t: 0.16, type: 'sting' }, { t: 1.2, type: 'whoosh' },
  { t: 1.5, type: 'strap' }, { t: 2.0, type: 'rattle', d: .6 }, { t: 2.6, type: 'glock', n: 0 },
  { t: 4.5, type: 'timp' }, { t: 4.7, type: 'rattle', d: .45 }, { t: 4.95, type: 'rattle', d: .45 }, { t: 5.15, type: 'glock', n: 1 }, { t: 5.4, type: 'glock', n: 2 }, { t: 5.3, type: 'pop' },
  { t: 7.5, type: 'timp' }, ...[...Array(6).keys()].map(i => ({ t: 7.6 + i * .06, type: 'flap' })), ...[...Array(5).keys()].map(i => ({ t: 8.0 + i * .06, type: 'flap' })),
  { t: 7.55, type: 'stamp' }, { t: 10.0, type: 'stop' }, { t: 10.3, type: 'whoosh' }, { t: 10.5, type: 'end' }, { t: 10.6, type: 'pop' },
].sort((a, b) => a.t - b.t);

window.DUR = DUR; window.EV = EV;
window.render = render;
window.TEXTS = t => { render(t); return TX; };
(async () => {
  QR = await new Promise((ok, no) => { const i = new Image(); i.onload = () => ok(i); i.onerror = no; i.src = 'assets/qr.png'; });
  await Promise.all([F.k9(40), F.k7(40), F.b8(40), F.b6(40), F.mono(40)].map(f => document.fonts.load(f, '가A')));
  render(0); window.READY = true;
})();
