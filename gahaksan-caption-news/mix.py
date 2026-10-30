"""Score + foley for the caption-news cut, picture-locked to events.json. 120 BPM; A minor pulse → silence → C major turn.
Synthesised kit, eighth-note bass, sixteenth marimba ostinato, off-beat vibraphone, glockenspiel, brass-like stabs. No voice."""
import json, os, sys
import numpy as np
import soundfile as sf

LIB = os.environ['LIB']
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
from sfx import SR, t_, env_exp, noise, bp, lp, hp, norm, whoosh, pop, compress, limit  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ev = json.load(open(os.path.join(HERE, 'events.json')))
DUR, EV = ev['dur'], ev['ev']
BEAT = 0.5
N = int((DUR + 0.05) * SR)
music = np.zeros((N, 2)); fx = np.zeros((N, 2))

def mtof(m): return 440 * 2 ** ((m - 69) / 12)
def put(buf, x, at, g=1.0, pan=0.0):
    i = int(round(at * SR))
    if i >= N or i < 0: return
    x = x[:N - i]; l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    buf[i:i + len(x), 0] += x * g * l * 1.414; buf[i:i + len(x), 1] += x * g * r * 1.414

# ───────── instruments ─────────
def kick(v=1.0):
    d = .35; tt = t_(d); f = 52 * (1 + 2.5 * np.exp(-tt / .03))
    return norm(np.sin(2 * np.pi * np.cumsum(f) / SR) * env_exp(d, .11) + hp(noise(d), 3000) * env_exp(d, .002) * .3) * v
def hat(v=1.0, d=.05): return norm(hp(noise(d), 7000) * env_exp(d, .012)) * v
def snare(v=1.0):
    d = .22; tt = t_(d)
    return norm(bp(noise(d), 1500, 7000) * env_exp(d, .05) + np.sin(2 * np.pi * 190 * tt) * env_exp(d, .04) * .7) * v
def bass(m, d=.24):
    tt = t_(d); f = mtof(m)
    x = sum(a * np.sin(2 * np.pi * f * h * tt) for h, a in [(1, 1), (2, .5), (3, .3)])
    return norm(lp(x, 1000) * env_exp(d, .12) * (1 - np.exp(-tt / .003)))
def marimba(m, d=.35):
    tt = t_(d); f = mtof(m)
    x = np.sin(2 * np.pi * f * tt) * env_exp(d, .18) + .3 * np.sin(2 * np.pi * f * 4 * tt) * env_exp(d, .03)
    return norm(x * (1 - np.exp(-tt / .002)))
def vib(ms, d=.45):
    tt = t_(d); x = sum(np.sin(2 * np.pi * mtof(m) * tt) for m in ms) * (1 + .25 * np.sin(2 * np.pi * 6 * tt))
    return norm(x * env_exp(d, .25) * (1 - np.exp(-tt / .004)))
def glock(m, d=1.0):
    tt = t_(d); f = mtof(m)
    return norm(sum(a * np.sin(2 * np.pi * f * k * tt) * env_exp(d, tau) for k, a, tau in [(1, 1, .45), (2.76, .35, .15), (5.4, .15, .05)]))
def brass(ms, d=.6, v=1.0):
    tt = t_(d); x = np.zeros(len(tt))
    for m in ms:
        for det in (-.06, .06): x += 2 * ((tt * mtof(m + det)) % 1) - 1
    swell = 1 - np.exp(-tt / .02)
    bright = np.exp(-tt / .1)   # filter opens on the attack, closes as it rings
    return norm((lp(x, 3300) * bright + lp(x, 1500) * (1 - bright)) * swell * env_exp(d, .3)) * v
def timp(v=1.0, f=65):
    d = 1.2; tt = t_(d)
    return norm(np.sin(2 * np.pi * f * tt) * env_exp(d, .4) + lp(noise(d), 250) * env_exp(d, .04) * .6) * v
def click(): d = .03; return norm(hp(noise(d), 2500) * env_exp(d, .003))
def thump(): d = .2; tt = t_(d); return norm(np.sin(2 * np.pi * 70 * tt) * env_exp(d, .05))
def rattle(d):
    out = np.zeros(int(d * SR) + 2000)
    for k in range(int(d / .045)):
        c = click() * (.5 + .5 * (k % 2)); i = int(k * .045 * SR); out[i:i + len(c)] += c[:len(out) - i]
    return out
def crack(): d = .25; return norm(hp(noise(d), 1500) * env_exp(d, .03))

# ───────── score ─────────
A_MIN = [(45, [57, 60, 64]), (41, [57, 60, 65]), (43, [59, 62, 67]), (45, [57, 60, 64])]   # Am F G Am
C_MAJ = [(36, [60, 64, 67]), (43, [59, 62, 67]), (45, [57, 60, 64]), (41, [57, 60, 65])]   # C G Am F
OST_A = [69, 72, 76, 72, 69, 72, 74, 72]
OST_C = [72, 76, 79, 76, 72, 76, 74, 76]

def silent(t): return 10.0 <= t < 11.0
nb = int(DUR / BEAT)
for b in range(nb):
    t = b * BEAT; bar = b // 4; bib = b % 4
    if silent(t): continue
    turn = t >= 11.0
    root, chord = (C_MAJ if turn else A_MIN)[bar % 4]
    ost = OST_C if turn else OST_A
    drop = 9.75 <= t < 10.0   # short drop before the stop: hats only
    if not drop:
        put(music, kick(), t, .8)
        if bib in (1, 3): put(music, snare(), t, .25 if not turn else .3, .1)
        for e in range(2): put(music, bass(root + (12 if e else 0)), t + e * BEAT / 2, .45)
        for q in range(4): put(music, marimba(ost[(bib * 4 + q) % 8] - (0 if q % 2 else 12)), t + q * BEAT / 4, .13, -.3 + .2 * q)
        put(music, vib(chord), t + BEAT / 2, .12, .3)
    put(music, hat(), t + BEAT / 2, .18, .4)
    if bib % 2 == 0: put(music, hat(.6), t + BEAT * .75, .1, .4)
# ring-out chord
put(music, brass([60, 64, 67, 72], 1.0, .9), 14.0, .22)

# ───────── picture-locked hits & foley ─────────
onsets = []
for e in EV:
    t, ty = e['t'], e['type']
    if ty == 'hook': put(music, brass([57, 64, 69]), t, .3); put(music, kick(1), t, .6); put(fx, rattle(.45), t, .14, -.2); onsets.append(t)
    elif ty in ('slam', 'chunk'): put(fx, click(), t, .22, -.1); put(fx, thump(), t, .25)
    elif ty == 'card': put(fx, whoosh(.25), t - .2, .2, .4); onsets.append(t)
    elif ty == 'rattle': put(fx, rattle(e['d']), t, .14, .2)
    elif ty == 'fill': put(fx, lp(noise(e['d']), 3000) * np.linspace(0, 1, int(round(e['d'] * SR))) * .3, t, .4)
    elif ty == 'rise': [put(fx, pop(), t + i * .1, .18, -.4 + .2 * i) for i in range(5)]
    elif ty == 'light': put(music, glock(81 + [0, 3, 5, 7, 12][e['i']]), t, .16, -.3 + .15 * e['i']); onsets.append(t)
    elif ty == 'turn':
        put(fx, crack(), t, .3); put(music, brass([48, 60, 64, 67, 72], .9), t, .38); put(music, timp(1, 65), t, .7); onsets.append(t)
    elif ty == 'pop': put(fx, pop(), t, .2, .2)
    elif ty == 'bells': [put(music, glock(m, 1.5), 14.0 + .12 * i, .15, -.2 + .2 * i) for i, m in enumerate([84, 88, 91])]

# ───────── master ─────────
mix = music + fx
for c in range(2): mix[:, c] = compress(mix[:, c], thr=.18, ratio=3.0)
mix *= np.minimum(1, (N - np.arange(N)) / (.3 * SR))[:, None]
mix *= 10 ** (-15.5 / 20) / np.sqrt((mix ** 2).mean())
for _ in range(2):
    for c in range(2): mix[:, c] = limit(mix[:, c], .7)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)
bad = [t for t in onsets if abs(t / .5 - round(t / .5)) * .5 > .015 and abs(t - .13) > .01]
print(f'onsets {len(onsets) - len(bad)}/{len(onsets)} on the half-beat grid (hook on frame 4 exempt)')
