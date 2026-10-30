// Subtitle cues for the .srt: when each hand-written note is on screen (same camera track as film.js).
import fs from 'fs';
const src = fs.readFileSync(new URL('./engine.js', import.meta.url), 'utf8');
const monotone = new Function(src + '; return monotone;')();
const camY = monotone([[0, 0], [2.4, 0], [3.2, 260], [11.7, 5240], [12.5, 5500], [15, 5500]]);
const tWhen = f => { for (let t = 0; t <= 15; t += 1 / 60) if (f(camY(t))) return +t.toFixed(2); return 15; };
const ST = [[1350, '환영의 정원 — WELCOME GARDEN'], [2562, '학온재 — FIRST GREENHOUSE'], [3775, '사계정원 — FOUR SEASONS GARDEN'], [4987, '도담도담숲 — DODAM FOREST'], [6200, '이끼원 — MOSS GARDEN']];
const cues = [{ t0: 0.3, t1: tWhen(cy => 480 - cy < 0), text: '가학산 수목원 · 2026.10.28 개장' }];
ST.forEach(([y, text], i) => cues.push({ t0: i ? tWhen(cy => 1880 - (y - cy) >= 80) : 2.0, t1: tWhen(cy => y + 430 - cy < 0), text }));
cues.push({ t0: 12.3, t1: 15.0, text: '무료 개방 — FREE ADMISSION' });
fs.writeFileSync(new URL('./cues.json', import.meta.url), JSON.stringify(cues, null, 1));
console.log(cues);
