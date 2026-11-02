"""News theme + package foley, placed from events.json. 120 BPM, beat 0 at 1.5 s. D minor theme under the package;
one beat of silence (14.5–15.0) before the end card, then an F-major lift held to the end. Synthesised; no voice."""
import json, os, sys
import numpy as np
import soundfile as sf

LIB = os.environ['LIB']
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
from sfx import SR, t_, env_exp, noise, bp, lp, hp, norm, whoosh, pop, compress, limit  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ev = json.load(open(os.path.join(HERE, 'events.json')))
DUR, EV = ev['dur'], ev['ev']
B0, BEAT = 1.5, 0.5
N = int((DUR + 0.05) * SR)
music = np.zeros((N, 2)); fx = np.zeros((N, 2))

def mtof(m): return 440 * 2 ** ((m - 69) / 12)
def put(buf, x, at, g=1.0, pan=0.0):
    i = int(round(at * SR))
    if i >= N or i < 0: return
    x = x[:N - i]; l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    buf[i:i + len(x), 0] += x * g * l * 1.414; buf[i:i + len(x), 1] += x * g * r * 1.414

# ───────── instruments ─────────
def spicc(m, d=.16):   # short bowed note: detuned saws, fast decay
    tt = t_(d); x = sum(2 * ((tt * mtof(m + dt)) % 1) - 1 for dt in (-.07, .07))
    return norm(lp(x, 3200) * env_exp(d, .05) * (1 - np.exp(-tt / .004)))
def pizz(m, d=.4):
    tt = t_(d); f = mtof(m)
    return norm(lp(sum(a * np.sin(2 * np.pi * f * h * tt) for h, a in [(1, 1), (2, .5), (3, .25)]), 1200) * env_exp(d, .12) * (1 - np.exp(-tt / .002)))
def horn(ms, d):
    tt = t_(d); x = sum(sum(a * np.sin(2 * np.pi * mtof(m) * h * tt) for h, a in [(1, 1), (2, .4), (3, .2)]) for m in ms)
    return norm(lp(x, 1400) * np.minimum(1, tt / .25) * np.minimum(1, (d - tt) / .3))
def saw16(m, d=.11):
    tt = t_(d); return norm(lp(2 * ((tt * mtof(m)) % 1) - 1, 700) * env_exp(d, .05))
def kick():
    d = .35; tt = t_(d); f = 50 * (1 + 2.5 * np.exp(-tt / .03))
    return norm(np.sin(2 * np.pi * np.cumsum(f) / SR) * env_exp(d, .12))
def snare():
    d = .22; tt = t_(d)
    return norm(bp(noise(d), 1500, 7000) * env_exp(d, .05) + np.sin(2 * np.pi * 190 * tt) * env_exp(d, .04) * .7)
def timp(f=55):
    d = 1.4; tt = t_(d); return norm(np.sin(2 * np.pi * f * tt * (1 + .03 * np.exp(-tt / .1))) * env_exp(d, .45) + lp(noise(d), 250) * env_exp(d, .03) * .6)
def glock(m, d=1.2):
    tt = t_(d); f = mtof(m)
    return norm(sum(a * np.sin(2 * np.pi * f * k * tt) * env_exp(d, tau) for k, a, tau in [(1, 1, .5), (2.76, .3, .15), (5.4, .12, .05)]))
def tutti(ms, d=1.2):
    tt = t_(d); x = sum(sum(2 * ((tt * mtof(m + dt)) % 1) - 1 for dt in (-.08, .08)) for m in ms)
    bright = np.exp(-tt / .15)
    return norm((lp(x, 3500) * bright + lp(x, 1400) * (1 - bright)) * env_exp(d, .5) * (1 - np.exp(-tt / .01)))
def cymbal(d=1.6):
    tt = t_(d); return norm(hp(noise(d), 5000) * env_exp(d, .5))
def click(): d = .025; return norm(hp(noise(d), 2500) * env_exp(d, .003))
def rattle(d):
    out = np.zeros(int(d * SR) + 2000)
    for k in range(int(d / .045)):
        c = click(); i = int(k * .045 * SR); out[i:i + len(c)] += c[:len(out) - i] * (.5 + .5 * (k % 2))
    return out
def thump(): d = .3; tt = t_(d); return norm(np.sin(2 * np.pi * 75 * tt) * env_exp(d, .07) + lp(noise(d), 800) * env_exp(d, .01) * .7)

# ───────── theme ─────────
DM = [(38, [62, 65, 69]), (34, [58, 62, 65]), (36, [60, 64, 67]), (33, [57, 61, 64])]   # Dm Bb C A
FM = [(41, [65, 69, 72]), (36, [64, 67, 72]), (38, [62, 65, 69]), (34, [62, 65, 70])]  # F C Dm Bb
for b in range(int((DUR - B0) / BEAT)):
    t = B0 + b * BEAT
    if 14.5 <= t < 15.0: continue
    end = t >= 15.0; bar = b // 4; bib = b % 4
    root, ch = (FM if end else DM)[bar % 4]
    if not end or bib == 0: put(music, kick(), t, .7)
    if bib in (1, 3) and not end: put(music, snare(), t, .3, .1)
    put(music, pizz(root), t, .5)
    if not end:
        for e, iv in enumerate([0, 7]):   # spiccato eighths: root–fifth, then the octave on the off-beats
            put(music, spicc(root + 24 + iv + (12 if bib % 2 else 0)), t + e * BEAT / 2, .14, -.3 + .6 * e)
        for q in range(4): put(music, saw16(root + 12), t + q * BEAT / 4, .07, .2)
    if bib == 0: put(music, horn(ch, 2.0 if not end else 4.4), t, .16 if not end else .2, 0)
# ───────── hits & foley ─────────
for e in EV:
    t, ty = e['t'], e['type']
    if ty == 'sting': put(music, tutti([50, 57, 62, 65, 69]), t, .35); put(music, timp(55), t, .6); put(music, cymbal(), t, .12)
    elif ty == 'whoosh': put(fx, whoosh(.3), t, .22, .3)
    elif ty == 'strap': put(fx, click(), t, .3)
    elif ty == 'rattle': put(fx, rattle(e['d']), t, .12, .2)
    elif ty == 'glock': put(music, glock([81, 84, 86][e['n']]), t, .16, -.2 + .2 * e['n'])
    elif ty == 'timp': put(music, timp(55), t, .55)
    elif ty == 'pop': put(fx, pop(), t, .2)
    elif ty == 'rise': [put(fx, pop(), t + i * .1, .16, -.3 + .2 * i) for i in range(4)]
    elif ty == 'light': put(music, glock([74, 77, 81, 86][e['i']]), t, .16, -.3 + .2 * e['i'])
    elif ty == 'flap': put(fx, click(), t, .3, .3); put(fx, click(), t + .02, .15, .3)
    elif ty == 'stamp': put(fx, thump(), t, .55); put(fx, lp(click(), 2000), t + .03, .3)
    elif ty == 'end': put(music, tutti([53, 60, 65, 69, 72], 2.5), t, .38); put(music, timp(43.6), t, .6); put(music, cymbal(2.5), t, .14); [put(music, glock(m, 2), t + .3 + .12 * i, .12) for i, m in enumerate([77, 81, 84])]

# ───────── narration: one line per shot (narration/lines.json), HP 90 Hz + 3:1; music ducked ≈ −7 dB and its 1.2–5 kHz thinned under speech ─────────
import soxr  # noqa: E402
NAR = os.path.join(HERE, 'narration')
voice = np.zeros(N); speech = np.zeros(N)
for ln in json.load(open(os.path.join(NAR, 'lines.json'))):
    x, sr = sf.read(os.path.join(NAR, 'out', ln['id'] + '.wav'))
    if x.ndim > 1: x = x.mean(1)
    x = soxr.resample(x, sr, SR); x = compress(hp(x, 90), thr=.2, ratio=3.0)
    x = x / (np.sqrt((x ** 2).mean()) + 1e-9) * .1
    i = int(ln['start'] * SR); x = x[:N - i]; voice[i:i + len(x)] += x; speech[i:i + len(x)] = 1
k = int(.08 * SR); env = np.convolve(speech, np.ones(k) / k, 'same')            # smoothed speech envelope
duck = 1 - env * (1 - 10 ** (-7 / 20))
for c in range(2):
    band = bp(music[:, c], 1200, 5000)
    music[:, c] = (music[:, c] - band * env * .5) * duck
# voice ≈ 8 dB over the music while it speaks
mrms = np.sqrt((music[speech > 0] ** 2).mean()) if speech.any() else .05
voice *= mrms * 10 ** (8 / 20) / (np.sqrt((voice[speech > 0] ** 2).mean()) + 1e-9)

# ───────── master ─────────
mix = music + fx + voice[:, None]
for c in range(2): mix[:, c] = compress(mix[:, c], thr=.18, ratio=3.0)
mix *= np.minimum(1, (N - np.arange(N)) / (.4 * SR))[:, None]
mix *= 10 ** (-15.5 / 20) / np.sqrt((mix ** 2).mean())
for _ in range(2):
    for c in range(2): mix[:, c] = limit(mix[:, c], .7)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)
print('mix written', DUR, 's')
