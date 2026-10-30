// 가학산 수목원 개장 — edit list, poses, scenes. 150 BPM, 1080×1920, 15.0 s.
(function () {
  const { W, H, E, clamp, lerp, inv, rgba, mix, PAL, F, CELL } = G;
  const BEAT = 0.4, BAR = 1.6;
  const S = CELL;

  // ───────── Edit list ─────────
  const CARDS = [
    { kr: '환영의 정원', en: 'WELCOME GARDEN', pk: 'pine', pose: 'welcome', trans: 'rows' },
    { kr: '학온재', en: 'GREENHOUSE HAKONJAE', pk: 'clay', pose: 'greenhouse', trans: 'push' },
    { kr: '사계정원', en: 'FOUR SEASONS GARDEN', pk: 'sun', pose: 'seasons', trans: 'quarter' },
    { kr: '도담도담숲', en: 'DODAM FOREST', pk: 'lake', pose: 'forest', trans: 'cols' },
    { kr: '이끼원', en: 'MOSS GARDEN', pk: 'moss', pose: 'moss', trans: 'flip' },
  ];
  const SH = [{ kind: 'intro', t0: 0, dur: 2 * BAR }];
  CARDS.forEach((c, i) => SH.push(Object.assign({ kind: 'card', i, t0: 2 * BAR + i * BAR, dur: BAR }, c)));
  SH.push({ kind: 'wall', t0: 7 * BAR, dur: 15 - 7 * BAR, trans: 'none' });
  const DUR = 15.0;

  // Sound / cue events (the mixer reads these)
  const EV = [];
  [0, 1, 2, 3].forEach((k) => EV.push({ t: k * 0.1, type: 'tick' }));
  EV.push({ t: 1 * BEAT, type: 'slam' });
  EV.push({ t: 3 * BEAT, type: 'chip' });
  CARDS.forEach((c, i) => {
    const t0 = SH[i + 1].t0;
    EV.push({ t: t0, type: 'cut', card: c.pose, i });
    EV.push({ t: t0 + 2 * BEAT, type: 'action', card: c.pose, i });
  });
  EV.push({ t: 7 * BAR, type: 'wall' });
  [0, 1, 2].forEach((r) => EV.push({ t: 7 * BAR + (2 + r) * BEAT, type: 'rowstop', r }));
  EV.push({ t: 7 * BAR + 8 * BEAT, type: 'final' });
  EV.sort((a, b) => a.t - b.t);

  // ───────── Text bookkeeping for readcheck ─────────
  let TX = [];
  function txt(ctx, id, text, x, y, font, col, p, o = {}) {
    if (p <= 0) return;
    const w = G.maskText(ctx, text, x, y, font, col, p, o);
    ctx.save(); ctx.font = font; ctx.letterSpacing = o.ls || '0px';
    const m = ctx.measureText(text); ctx.restore();
    const x0 = o.align === 'center' ? x - w / 2 : o.align === 'right' ? x - w : x;
    const T = ctx.getTransform();
    const pt = (px, py) => [T.a * px + T.c * py + T.e, T.b * px + T.d * py + T.f];
    const a = pt(x0, y - m.actualBoundingBoxAscent), b = pt(x0 + w, y + m.actualBoundingBoxDescent);
    TX.push({ id, text, x0: Math.min(a[0], b[0]), y0: Math.min(a[1], b[1]), x1: Math.max(a[0], b[0]), y1: Math.max(a[1], b[1]) });
  }

  // ───────── Colours ─────────
  const darkFg = (pk) => PAL[pk].fg !== G.CREAM;
  const discOf = (pk) => (darkFg(pk) ? PAL[pk].t[4] : PAL[pk].t[0]);
  const colorsFor = (pk) => {
    const P = PAL[pk], bg = discOf(pk);
    return { fg: P.fg, far: mix(P.fg, bg, 0.42), far2: mix(P.fg, bg, 0.6), bg, line: rgba(P.fg, 0.55), t: P.t };
  };

  // ───────── Poses (unit = body height, origin = hip, ground at y = +0.45) ─────────
  const GROUND = 0.45;
  const STAND = { torso: 0, aR1: 8, aR2: 12, aL1: -8, aL2: -4, lR1: 4, lR2: 4, lL1: -4, lL2: -4 };
  const arc = (ctx, x, y, r, a0, a1, w, col) => { ctx.strokeStyle = col; ctx.lineWidth = w; ctx.lineCap = 'round'; ctx.beginPath(); ctx.arc(x, y, r, a0, a1); ctx.stroke(); };
  const capsule = (ctx, x, yTop, yBot, w, col) => G.seg(ctx, [x, yTop + w / 2], [x, yBot - w / 2], w, col);

  const POSES = {
    // Walks in through a growing arch gate, opens both arms on beat 2
    welcome: {
      iconU: 3,
      pose: (u) => {
        const walk = G.walkCycle(u * 0.5, 1);
        const x = lerp(-0.5, 0, E.o3(inv(0, 1.6, u)));
        const open = E.oExp(inv(1.6, 2.1, u));
        const p = G.lerpPose(Object.assign({}, walk), { torso: -4, aR1: 140, aR2: 152, aL1: -140, aL2: -152, lR1: 6, lR2: 6, lL1: -6, lL2: -6 }, open);
        p.x = x; return p;
      },
      back: (ctx, u, C) => {
        const g = E.o3(inv(0, 1.2, u));
        arc(ctx, 0, GROUND, 0.62, Math.PI, Math.PI + Math.PI * g, 0.07, C.far2);
        arc(ctx, 0, GROUND, 0.74, Math.PI, Math.PI + Math.PI * E.o3(inv(0.2, 1.4, u)), 0.035, C.far2);
      },
    },
    // A glass dome grows over the figure, who reaches up to a tall tree
    greenhouse: {
      iconU: 3,
      pose: (u) => {
        const r = E.oExp(inv(1.5, 2.1, u));
        const p = G.lerpPose(STAND, { torso: -8, head: -18, aR1: 158, aR2: 168, aL1: -20, aL2: -10, lR1: 8, lR2: 8, lL1: -10, lL2: -10 }, r);
        p.x = -0.2; p.y = -0.02 * r; return p;
      },
      back: (ctx, u, C) => {
        for (let k = 0; k < 3; k++) {
          const g = E.o3(inv(k * 0.25, 1.1 + k * 0.25, u));
          arc(ctx, 0, GROUND, 0.92 - k * 0.001, Math.PI, Math.PI + Math.PI * g, 0.03, C.far2);
          const a = Math.PI + Math.PI * (k + 1) / 4;
          if (g > (k + 1) / 4) G.seg(ctx, [0, GROUND], [Math.cos(a) * 0.92, GROUND + Math.sin(a) * 0.92], 0.02, C.far2);
        }
        const tg = E.o5(inv(0.4, 1.6, u));
        capsule(ctx, 0.32, GROUND - 0.62 * tg, GROUND, 0.05, C.far);
        [[0.32, -0.62, 0.17], [0.2, -0.48, 0.12], [0.45, -0.46, 0.13]].forEach(([x, y, r], k) =>
          G.disc(ctx, x, GROUND + y * tg, r * E.oBack(inv(1.0 + k * 0.15, 1.5 + k * 0.15, u)), k ? C.far : C.fg));
      },
    },
    // Points at a sun whose four quarters light up one per beat
    seasons: {
      iconU: 3.6,
      pose: (u) => {
        const r = E.oExp(inv(0, 0.6, u));
        const p = G.lerpPose(STAND, { torso: -4, head: -14, aR1: 128, aR2: 132, aL1: -14, aL2: -6, lR1: 10, lR2: 10, lL1: -8, lL2: -8 }, r);
        p.x = -0.18; return p;
      },
      back: (ctx, u, C) => {
        const cx = 0.3, cy = -0.42, R = 0.2;
        const cols = ['#e79aa0', '#4f9a5a', '#d9682f', '#a9c3d6']; // spring, summer, autumn, winter
        for (let q = 0; q < 4; q++) {
          const g = E.oBack(inv(q, q + 0.35, u));
          if (g <= 0) continue;
          ctx.fillStyle = cols[q]; ctx.beginPath(); ctx.moveTo(cx, cy);
          ctx.arc(cx, cy, R * g, -Math.PI / 2 + q * Math.PI / 2, q * Math.PI / 2); ctx.closePath(); ctx.fill();
        }
        G.disc(ctx, cx, cy, 0.035 * E.o3(inv(0, 0.3, u)), C.fg);
      },
    },
    // Adult and child walk hand in hand; trees slide past
    forest: {
      iconU: 2,
      pose: (u) => Object.assign(G.walkCycle(u * 0.5, 0.9), { x: -0.16, aR1: 30, aR2: 40 }),
      child: (u) => Object.assign(G.walkCycle(u * 0.5 + 0.5, 1), { x: 0.0, aL1: -22, aL2: -18 }),
      childAt: [0.3, 0], childScale: 0.62,
      back: (ctx, u, C) => {
        for (let k = 0; k < 6; k++) {
          const x = ((k * 0.42 - u * 0.12 + 10) % 2.52) - 1.26;
          const h = 0.5 + (k % 3) * 0.16;
          const g = E.o5(inv(k * 0.12, 0.8 + k * 0.12, u));
          capsule(ctx, x, GROUND - h * g, GROUND, 0.16, k % 2 ? C.far2 : C.far);
        }
      },
    },
    // Crouches on beat 2 to look at moss blooming on the ground
    moss: {
      iconU: 3.5,
      pose: (u) => {
        const r = E.io3(inv(1.2, 2.0, u));
        const crouch = { y: 0.225, torso: 38, head: 22, aR1: 62, aR2: 30, aL1: 30, aL2: 10, lR1: 92, lR2: 2, lL1: 12, lL2: -90 };
        const p = G.lerpPose(STAND, crouch, r); p.x = -0.2; return p;
      },
      back: () => {},
      front: (ctx, u, C) => {
        const rr = G.rng(41);
        for (let k = 0; k < 22; k++) {
          const x = 0.0 + rr() * 0.6, rad = 0.02 + rr() * 0.04;
          const g = E.oBack(inv(1.6 + k * 0.05, 2.1 + k * 0.05, u));
          G.disc(ctx, x, GROUND - rad * 0.4, rad * g, [C.t[0], C.t[1], C.fg][k % 3]);
        }
        const d = (u - 2.6) * 0.8; // one drip
        if (d > 0 && d < 1) G.disc(ctx, 0.36, -0.5 + d * 0.9, 0.022, C.fg);
      },
    },
  };

  function drawPose(f, name, u, C) {
    const def = POSES[name];
    f.save(); def.back(f, u, C); f.restore();
    G.drawFigure(f, G.joints(def.pose(u)), C.fg, C.far);
    if (def.child) {
      f.save(); f.translate(def.childAt[0], GROUND - GROUND * def.childScale); f.scale(def.childScale, def.childScale);
      G.drawFigure(f, G.joints(def.child(u)), C.fg, C.far); f.restore();
    }
    if (def.front) { f.save(); def.front(f, u, C); f.restore(); }
  }

  // ───────── Buffers ─────────
  const bufA = G.canvas(W, H), bufB = G.canvas(W, H), bufFig = G.canvas(W, H);

  // Figure layer, assembled from horizontal bands that slide into register, masked above the text column
  function stageFigure(ctx, name, u, C, x, y, U, asm, maskY) {
    const f = bufFig.getContext('2d');
    f.setTransform(1, 0, 0, 1, 0, 0); f.globalCompositeOperation = 'source-over'; f.clearRect(0, 0, W, H);
    f.save(); f.translate(x, y); f.scale(U, U); drawPose(f, name, u, C); f.restore();
    if (maskY) {
      f.globalCompositeOperation = 'destination-in';
      const g = f.createLinearGradient(0, 0, 0, H);
      g.addColorStop(0, '#000'); g.addColorStop(clamp((maskY - 80) / H), '#000'); g.addColorStop(clamp(maskY / H), 'rgba(0,0,0,0)'); g.addColorStop(1, 'rgba(0,0,0,0)');
      f.fillStyle = g; f.fillRect(0, 0, W, H); f.globalCompositeOperation = 'source-over';
    }
    if (asm >= 0.999) { ctx.drawImage(bufFig, 0, 0); return; }
    const band = 48, k = 1 - asm;
    ctx.save(); ctx.globalAlpha = clamp(asm * 3);
    for (let yy = 0, j = 0; yy < H; yy += band, j++) {
      const dx = k * (j % 2 ? 1 : -1) * (90 + ((j * 37) % 5) * 40);
      ctx.drawImage(bufFig, 0, yy, W, band, dx, yy, W, band);
    }
    ctx.restore();
  }

  function patBg(ctx, pk, seed, t, o = {}) {
    const pat = G.pattern(pk, seed, { spread: o.spread ?? 0.42, density: o.density ?? 0.5 });
    G.drawPattern(ctx, pat, (j) => (o.offs ? o.offs(j) : 0) + (j % 2 ? 1 : -1) * 26 * t + 30 * Math.sin(j * 1.7 + seed));
  }

  function sunDisc(ctx, x, y, r, col, lt) {
    G.disc(ctx, x, y, r, col);
    ctx.save(); ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.clip();
    const sx = x - r * 1.6 + lt * r * 0.9;
    ctx.translate(sx, y); ctx.rotate(-0.5);
    ctx.fillStyle = 'rgba(255,255,255,0.09)'; ctx.fillRect(-r * 0.25, -r * 2, r * 0.5, r * 4);
    ctx.restore();
  }

  // ───────── Scenes ─────────
  const STAGE = { x: 540, y: 760, r: 400, hip: 790, U: 600, mask: 1190 };

  function hud(ctx, left, right, idx, col) {
    ctx.save();
    ctx.font = F.mono(22); ctx.letterSpacing = '4px'; ctx.fillStyle = rgba(col, 0.85);
    ctx.textAlign = 'left'; ctx.fillText(left, 70, 100);
    ctx.textAlign = 'right'; ctx.fillText(right, W - 70, 100);
    if (idx != null) {
      const n = CARDS.length, gap = 14, w = (W - 140 - gap * (n - 1)) / n;
      for (let i = 0; i < n; i++) {
        ctx.fillStyle = rgba(col, i === idx ? 1 : i < idx ? 0.7 : 0.25);
        const h = i === idx ? 12 : 5;
        ctx.fillRect(70 + i * (w + gap), 1840 - h, w, h);
      }
    }
    ctx.restore();
  }

  function cardScene(ctx, s, lt) {
    const P = PAL[s.pk], C = colorsFor(s.pk), u = lt / BEAT;
    patBg(ctx, s.pk, 11 + s.i * 7, lt);
    // text column: a solid panel of the darkest/lightest tone under the type
    const py = 1220;
    const pw = E.o5(inv(0, 0.35, lt));
    ctx.fillStyle = darkFg(s.pk) ? P.t[4] : P.t[0];
    ctx.fillRect(0, py + (1 - pw) * (H - py), W, H - py);
    // outline index number behind the stage
    ctx.save(); ctx.font = F.en(900, 300); ctx.lineWidth = 3; ctx.strokeStyle = rgba(P.fg, 0.22); ctx.textAlign = 'right';
    ctx.strokeText(String(s.i + 1).padStart(2, '0'), W - 50, 440 + (1 - E.oExp(inv(0, 0.5, lt))) * 80); ctx.restore();
    sunDisc(ctx, STAGE.x, STAGE.y, STAGE.r * E.o5(inv(0, 0.4, lt)), C.bg, lt);
    // ground hairline
    G.hair(ctx, 120, STAGE.hip + GROUND * STAGE.U, 840 * E.o3(inv(0.05, 0.5, lt)), C.line, 1, 4);
    stageFigure(ctx, s.pose, u, C, STAGE.x, STAGE.hip, STAGE.U, E.oExp(inv(0, 0.42, lt)), STAGE.mask);
    // type
    const px = G.fitFont(ctx, s.kr, 'kr', 400, 200, W - 160);
    txt(ctx, 'title', s.kr, 80, 1470, F.kr(400, px), P.fg, inv(0.04, 0.5, lt));
    const ep = G.fitFont(ctx, s.en, 'en', 900, 70, W - 160, -0.01);
    txt(ctx, 'en', s.en, 80, 1580, F.en(900, ep), rgba(P.fg, 0.9), inv(0.12, 0.6, lt), { ls: '-0.5px' });
    G.typeText(ctx, `THEME  ${String(s.i + 1).padStart(2, '0')} / 05`, 80, 1680, F.mono(24), rgba(P.fg, 0.75), inv(0.2, 0.6, lt), { ls: '4px', caret: false });
    G.hair(ctx, 80, 1710, (W - 160), rgba(P.fg, 0.35), E.o3(inv(0.2, 0.7, lt)), 2);
    hud(ctx, 'GAHAKSAN ARBORETUM', `${String(s.i + 1).padStart(2, '0')} / 05`, s.i, P.fg);
  }

  // Opening: hairline grid draws on cream, one leaf cell appears, the pattern blooms from it, the name slams in on a band
  function introScene(ctx, s, lt) {
    const P = PAL.pine, u = lt / BEAT;
    ctx.fillStyle = G.CREAM; ctx.fillRect(0, 0, W, H);
    const cols = Math.ceil(W / S), rows = Math.ceil(H / S);
    const seedCell = [2, 5];
    // bloom: cells reveal outward from the seed cell
    const pat = G.pattern('pine', 3, { spread: 0.95, density: 0.55 });
    const jump = (j) => (u >= 4 ? (j % 2 ? 1 : -1) * S * 0.25 * Math.floor(Math.min(u, 8) - 3) % S : 0);
    const off = (j) => (j % 2 ? 1 : -1) * 26 * lt + 30 * Math.sin(j * 1.7 + 3) + jump(j);
    ctx.save(); ctx.beginPath();
    let any = false;
    for (let j = 0; j < rows; j++) for (let i = 0; i < cols; i++) {
      const d = Math.hypot(i - seedCell[0], j - seedCell[1]);
      const p = E.o3(inv(0.2 + d * 0.055, 0.42 + d * 0.055, lt));
      if (p <= 0) continue;
      any = true;
      const cx = i * S + S / 2, cy = j * S + S / 2, h = S / 2 * p + 0.5;
      ctx.rect(cx - h, cy - h, 2 * h, 2 * h);
    }
    ctx.clip();
    if (any) G.drawPattern(ctx, pat, off);
    ctx.restore();
    // the seed cell: a leaf (quarter disc + vein) in the brightest tone
    const sp = E.oBack(inv(0.15, 0.4, lt)) * (1 - inv(0.5, 0.8, lt));
    if (sp > 0) {
      const x = seedCell[0] * S, y = seedCell[1] * S;
      ctx.save(); ctx.translate(x + S / 2, y + S / 2); ctx.scale(sp, sp);
      ctx.fillStyle = P.t[0]; ctx.fillRect(-S / 2, -S / 2, S, S);
      ctx.fillStyle = P.t[4]; ctx.beginPath(); ctx.moveTo(-S / 2, S / 2); ctx.arc(-S / 2, S / 2, S, -Math.PI / 2, 0); ctx.closePath(); ctx.fill();
      G.seg(ctx, [-S / 2, S / 2], [S * 0.2, -S * 0.2], 6, P.t[0]);
      ctx.restore();
    }
    // hairline grid on top (draws on during the first beat, then fades)
    const gp = E.io3(inv(0, 0.4, lt)), ga = 1 - inv(0.6, 1.2, lt);
    if (ga > 0) {
      ctx.fillStyle = rgba(G.INK, 0.25 * ga);
      for (let i = 1; i < cols; i++) ctx.fillRect(i * S - 1, 0, 2, H * gp);
      for (let j = 1; j < rows; j++) ctx.fillRect(0, j * S - 1, W * gp, 2);
    }
    // band (3 rows) grows from the centre on the slam
    const bp = E.o5(inv(0.4, 0.75, lt));
    const by0 = 3.5 * S, bh = 3 * S;
    if (bp > 0) {
      const h = bh * bp;
      ctx.fillStyle = P.t[0]; ctx.fillRect(0, by0 + bh / 2 - h / 2, W, h);
      G.hair(ctx, 0, by0 - 14, W, rgba(G.CREAM, 0.8), E.o3(inv(0.5, 0.9, lt)), 3);
      G.hair(ctx, 0, by0 + bh + 11, W, rgba(G.CREAM, 0.8), E.o3(inv(0.55, 0.95, lt)), 3);
    }
    txt(ctx, 'title1', '가학산', W / 2, by0 + 245, F.kr(400, 220), G.CREAM, inv(0.42, 0.85, lt), { align: 'center' });
    txt(ctx, 'title2', '수목원', W / 2, by0 + 485, F.kr(400, 220), G.CREAM, inv(0.48, 0.92, lt), { align: 'center' });
    // English line above the band, chip below
    const eh = 70;
    ctx.fillStyle = G.CREAM; const ew = 760 * E.o5(inv(0.5, 0.8, lt));
    if (ew > 0) ctx.fillRect(W / 2 - ew / 2, by0 - 160, ew, eh + 40);
    txt(ctx, 'en', 'GAHAKSAN ARBORETUM', W / 2, by0 - 95, F.en(900, 62), P.t[0], inv(0.55, 0.95, lt), { align: 'center', ls: '-0.5px' });
    const cp = E.o5(inv(1.2, 1.5, lt));
    if (cp > 0) {
      const cw = 620 * cp, cy = by0 + bh + 70;
      ctx.fillStyle = G.ACC; ctx.fillRect(W / 2 - cw / 2, cy, cw, 110);
      txt(ctx, 'chip', 'OPEN  2026.10.28', W / 2, cy + 75, F.en(900, 60), G.CREAM, inv(1.25, 1.6, lt), { align: 'center' });
    }
    hud(ctx, 'GWANGMYEONG', 'NOW OPEN', null, G.INK);
  }

  // Ending: the moss card pulls back into a 2×3 wall; rows stop one by one; the wall turns one green on the final hit
  const WALL = { x0: 40, y0: 400, w: 490, h: 430, gx: 20, gy: 20 };
  const tileRect = (k) => { const c = k % 2, r = Math.floor(k / 2); return [WALL.x0 + c * (WALL.w + WALL.gx), WALL.y0 + r * (WALL.h + WALL.gy), WALL.w, WALL.h]; };

  function wallTile(ctx, k, lt, green) {
    const [x, y, w, h] = tileRect(k), r = Math.floor(k / 2);
    const stopAt = (2 + r) * BEAT;
    const tl = Math.min(lt, stopAt) + 3.2;
    ctx.save(); ctx.beginPath(); ctx.rect(x, y, w, h); ctx.clip(); ctx.translate(x, y);
    if (k === 5) { // FREE
      ctx.fillStyle = G.ACC; ctx.fillRect(0, 0, w, h);
      txt(ctx, 'free', '무료', w / 2, 250, F.kr(400, 170), G.CREAM, inv(0.5, 0.9, lt), { align: 'center' });
      txt(ctx, 'free-en', 'FREE ADMISSION', w / 2, 340, F.mono(30), G.CREAM, inv(0.6, 1.0, lt), { align: 'center', ls: '4px' });
      ctx.restore(); return;
    }
    const c = CARDS[k];
    const pk = green > 0.5 ? 'pine' : c.pk;
    const base = colorsFor(c.pk), gC = colorsFor('pine');
    const C = green > 0 ? { fg: mix(base.fg, gC.fg, green), far: mix(base.far, gC.far, green), far2: mix(base.far2, gC.far2, green), bg: mix(base.bg, gC.bg, green), line: gC.line, t: gC.t } : base;
    ctx.scale(w / W, w / W);
    patBg(ctx, pk, 11 + k * 7, tl, { spread: 0.42 });
    if (green > 0 && green < 1) { ctx.fillStyle = rgba(PAL.pine.t[1], green); ctx.fillRect(0, 0, W, H); }
    else if (green >= 1) { ctx.fillStyle = PAL.pine.t[1]; ctx.fillRect(0, 0, W, H); patBg(ctx, 'pine', 11 + k * 7, tl, { spread: 0.42 }); }
    ctx.restore();
    // icon: disc + frozen pose, fitted to the tile
    ctx.save(); ctx.beginPath(); ctx.rect(x, y, w, h); ctx.clip();
    const cx = x + w / 2, cy = y + h * 0.43, R = h * 0.36;
    G.disc(ctx, cx, cy, R, C.bg);
    const U = R * 1.45;
    ctx.translate(cx, cy + U * 0.06); ctx.scale(U, U);
    drawPose(ctx, c.pose, POSES[c.pose].iconU, C);
    ctx.restore();
    // name strip
    const nb = darkFg(c.pk) && green < 0.5 ? PAL[c.pk].t[4] : (green >= 0.5 ? PAL.pine.t[0] : PAL[c.pk].t[0]);
    const nf = green >= 0.5 ? G.CREAM : PAL[c.pk].fg;
    ctx.fillStyle = nb; ctx.fillRect(x, y + h - 86, w, 86);
    const px = G.fitFont(ctx, c.kr, 'kr', 400, 54, w - 120);
    txt(ctx, 'tile' + k, c.kr, x + 24, y + h - 24, F.kr(400, px), nf, inv(0.3 + k * 0.05, 0.7 + k * 0.05, lt));
    ctx.save(); ctx.font = F.mono(22); ctx.fillStyle = rgba(nf, 0.75); ctx.textAlign = 'right';
    ctx.fillText(String(k + 1).padStart(2, '0'), x + w - 22, y + h - 32); ctx.restore();
  }

  function wallScene(ctx, s, lt) {
    ctx.fillStyle = G.CREAM; ctx.fillRect(0, 0, W, H);
    // faint grid echoing the opening
    ctx.fillStyle = rgba(G.INK, 0.06);
    for (let i = 1; i < W / S; i++) ctx.fillRect(i * S - 1, 0, 2, H);
    for (let j = 1; j < H / S; j++) ctx.fillRect(0, j * S - 1, W, 2);
    const green = E.io3(inv(3.2 * 1, 3.2 + 0.3, lt));
    // tiles flip in (scaleX), the moss tile is the pull-back
    for (let k = 0; k < 6; k++) {
      if (k === 4) continue;
      const d = 0.08 + [0, 1, 1, 2, 0, 3][k] * 0.08;
      const p = E.o3(inv(d, d + 0.3, lt));
      if (p <= 0) continue;
      const [x, y, w, h] = tileRect(k);
      ctx.save(); ctx.translate(x + w / 2, 0); ctx.scale(p, 1); ctx.translate(-(x + w / 2), 0);
      wallTile(ctx, k, lt, green);
      ctx.restore();
    }
    // pull-back: moss card shrinks from full frame into tile 4
    const pb = E.ioExp(inv(0, 0.5, lt));
    if (pb < 1) {
      const b = bufB.getContext('2d');
      b.setTransform(1, 0, 0, 1, 0, 0); b.save(); cardScene(b, SH[5], BAR + lt); b.restore();
      const [x, y, w, h] = tileRect(4);
      const sx = lerp(0, x, pb), sy = lerp(0, y, pb), sw = lerp(W, w, pb), sh = lerp(H, h, pb);
      // crop the card around its stage as it shrinks
      const srcH = lerp(H, W * h / w, pb), srcY = lerp(0, STAGE.y - srcH * 0.43, pb);
      ctx.drawImage(bufB, 0, srcY, W, srcH, sx, sy, sw, sh);
      TX = TX.filter((q) => q.id !== 'title' && q.id !== 'en');
    } else wallTile(ctx, 4, lt, green);
    // header & footer
    txt(ctx, 'head', '5개 테마 정원', W / 2, 270, F.kr(400, 112), PAL.pine.t[0], inv(0.3, 0.7, lt), { align: 'center' });
    txt(ctx, 'head-en', '5 THEME GARDENS', W / 2, 340, F.mono(26), rgba(G.INK, 0.7), inv(0.4, 0.8, lt), { align: 'center', ls: '6px' });
    txt(ctx, 'foot', '10.28 OPEN', W / 2, 1860, F.en(900, 96), G.ACC, inv(0.5, 0.9, lt), { align: 'center', ls: '-1px' });
    if (pb > 0.6) hud(ctx, 'GAHAKSAN ARBORETUM', 'GWANGMYEONG', null, G.INK); // the shrinking card still carries its own HUD
  }

  function drawShot(ctx, s, lt) {
    ctx.save();
    if (s.kind === 'intro') introScene(ctx, s, lt);
    else if (s.kind === 'card') cardScene(ctx, s, lt);
    else wallScene(ctx, s, lt);
    ctx.restore();
  }

  // ───────── Transitions (snap to the grid) ─────────
  function composite(ctx, type, p, A, B) {
    const rows = Math.ceil(H / S), cols = Math.ceil(W / S);
    if (type === 'rows') {
      ctx.drawImage(A, 0, 0);
      for (let j = 0; j < rows; j++) {
        const pj = E.ioExp(clamp(p * 1.5 - j * 0.045)), dir = j % 2 ? 1 : -1;
        ctx.drawImage(A, 0, j * S, W, S, dir * W * 0.25 * pj, j * S, W, S);
        ctx.drawImage(B, 0, j * S, W, S, -dir * W * (1 - pj), j * S, W, S);
      }
    } else if (type === 'push') {
      const pj = E.ioExp(p);
      ctx.drawImage(A, -W * pj, 0); ctx.drawImage(B, W * (1 - pj), 0);
    } else if (type === 'cols') {
      ctx.drawImage(A, 0, 0);
      for (let i = 0; i < cols; i++) {
        const pj = E.ioExp(clamp(p * 1.5 - i * 0.08)), dir = i % 2 ? 1 : -1;
        ctx.drawImage(B, i * S, 0, S, H, i * S, -dir * H * (1 - pj), S, H);
      }
    } else if (type === 'quarter') {
      ctx.drawImage(A, 0, 0);
      ctx.save(); ctx.beginPath();
      for (let j = 0; j < rows; j++) for (let i = 0; i < cols; i++) {
        const r = S * 1.42 * E.o3(clamp((p - (i + j) * 0.03) / 0.5));
        if (r <= 0) continue;
        const c = (i + j) % 4, cx = i * S + (c === 1 || c === 2 ? S : 0), cy = j * S + (c >= 2 ? S : 0);
        ctx.moveTo(cx, cy); ctx.arc(cx, cy, r, 0, Math.PI * 2);
      }
      ctx.clip(); ctx.drawImage(B, 0, 0); ctx.restore();
    } else if (type === 'flip') {
      ctx.drawImage(B, 0, 0); ctx.fillStyle = 'rgba(0,0,0,0.45)'; ctx.fillRect(0, 0, W, H);
      for (let j = 0; j < rows; j++) for (let i = 0; i < cols; i++) {
        const pc = clamp((p - ((i * 0.6 + j) / (cols * 0.6 + rows)) * 0.5) / 0.5);
        const src = pc < 0.5 ? A : B, sx = Math.abs(Math.cos(pc * Math.PI)), w = S * sx;
        ctx.drawImage(src, i * S, j * S, S, S, i * S + (S - w) / 2, j * S, Math.max(1, w), S);
      }
    }
  }

  const PRE = 0.12, POST = 0.26;
  function shotAt(t) { let k = 0; for (let i = 0; i < SH.length; i++) if (SH[i].t0 <= t) k = i; return k; }

  function render(ctx, t) {
    TX = [];
    t = clamp(t, 0, DUR - 1e-6);
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    const k = shotAt(t), s = SH[k];
    // inside a transition window around a cut?
    let cut = null;
    if (k + 1 < SH.length && SH[k + 1].t0 - t < PRE) cut = k + 1;
    else if (k > 0 && t - s.t0 < POST) cut = k;
    if (cut != null && SH[cut].trans && SH[cut].trans !== 'none') {
      const a = SH[cut - 1], b = SH[cut], p = (t - (b.t0 - PRE)) / (PRE + POST);
      const A = bufA.getContext('2d'), B = bufB.getContext('2d');
      A.setTransform(1, 0, 0, 1, 0, 0); B.setTransform(1, 0, 0, 1, 0, 0);
      drawShot(A, a, t - a.t0); drawShot(B, b, Math.max(0, t - b.t0));
      composite(ctx, b.trans, clamp(p), bufA, bufB);
      // during a wipe only the incoming texts count once the wipe is done
      if (p < 1) TX = [];
    } else drawShot(ctx, s, t - s.t0);
  }

  window.FILM = { DUR, EV, SH, CARDS, BEAT, render, texts: () => TX.slice() };
})();
