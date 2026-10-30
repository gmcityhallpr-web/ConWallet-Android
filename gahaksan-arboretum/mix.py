"""Score + foley for 가학산 수목원 개장, picture-locked to events.json (150 BPM, D major pentatonic).
Woody percussion (frame drum, woodblock, rim, shaker), synthesised marimba and a plucked bass; per-card accents.
Writes out/mix.wav and prints an onset check."""
import json, os, sys
import numpy as np
import soundfile as sf

LIB = os.environ['LIB']
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
from sfx import SR, t_, env_exp, noise, bp, lp, hp, norm, whoosh, ding, compress, limit, add, brown  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ev = json.load(open(os.path.join(HERE, 'events.json')))
DUR = ev['dur']; EV = ev['ev']
BEAT = 0.4; BAR = 1.6
N = int((DUR + 0.05) * SR)
music = np.zeros((N, 2)); fx = np.zeros((N, 2))
rng = np.random.default_rng(5)

def mtof(m): return 440 * 2 ** ((m - 69) / 12)

# ───────── Instruments ─────────
def frame_drum(v=1.0, f=62):
    d = .5; tt = t_(d)
    body = np.sin(2 * np.pi * f * tt * (1 + .5 * np.exp(-tt / .02))) * env_exp(d, .12)
    skin = bp(noise(d), 180, 900) * env_exp(d, .03) * .5
    return norm(body + skin) * v

def woodblock(v=1.0, f=1150):
    d = .12; tt = t_(d)
    return norm(np.sin(2 * np.pi * f * tt) * env_exp(d, .025) + .4 * np.sin(2 * np.pi * f * 2.7 * tt) * env_exp(d, .01) + hp(noise(d), 3000) * env_exp(d, .002) * .4) * v

def rim(v=1.0):
    d = .07; tt = t_(d)
    return norm(hp(noise(d), 2000) * env_exp(d, .004) + np.sin(2 * np.pi * 1700 * tt) * env_exp(d, .012) * .6) * v

def shaker(v=1.0):
    d = .09; tt = t_(d)
    return norm(hp(noise(d), 5000) * np.sin(np.pi * tt / d) ** 2) * v

def marimba(m, v=1.0, d=1.0):
    tt = t_(d); f = mtof(m)
    x = np.sin(2 * np.pi * f * tt) * env_exp(d, .35) + .35 * np.sin(2 * np.pi * f * 4 * tt) * env_exp(d, .05) + .12 * np.sin(2 * np.pi * f * 10 * tt) * env_exp(d, .012)
    x *= 1 - np.exp(-tt / .002)
    return norm(x) * v

def pluck_bass(m, v=1.0, d=.45):
    tt = t_(d); f = mtof(m)
    x = sum(a * np.sin(2 * np.pi * f * h * tt) for h, a in [(1, 1), (2, .45), (3, .25)])
    return norm(lp(x, 900) * env_exp(d, .16) * (1 - np.exp(-tt / .003))) * v

def stab(ms, v=1.0, d=.9):
    tt = t_(d); x = np.zeros(len(tt))
    for m in ms:
        for det in (-.08, .08):
            x += 2 * ((tt * mtof(m + det)) % 1) - 1
    return norm(lp(x, 2400) * env_exp(d, .22) * (1 - np.exp(-tt / .004))) * v

def chime(m, v=1.0):
    d = 1.4; tt = t_(d); f = mtof(m)
    return norm(sum(a * np.sin(2 * np.pi * f * k * tt) * env_exp(d, tau) for k, a, tau in [(1, 1, .5), (2.76, .4, .2), (5.4, .2, .08)])) * v

def rustle(v=1.0):
    d = .35; tt = t_(d)
    return norm(bp(noise(d), 2500, 8000) * (np.abs(np.sin(2 * np.pi * 18 * tt)) ** 2) * np.sin(np.pi * tt / d)) * v

def pok(v=1.0):
    d = .15; tt = t_(d); f = 420 * (1 + 1.2 * np.exp(-tt / .01))
    return norm(np.sin(2 * np.pi * f * tt) * env_exp(d, .04)) * v

def drip(v=1.0):
    d = .25; tt = t_(d); f = 900 + 1600 * (1 - np.exp(-tt / .03))
    return norm(np.sin(2 * np.pi * np.cumsum(f) / SR) * env_exp(d, .05)) * v

def put(buf, x, at, g=1.0, pan=0.0):
    i = int(round(at * SR))
    if i >= N: return
    x = x[:N - i]
    l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    buf[i:i + len(x), 0] += x * g * l * 1.414; buf[i:i + len(x), 1] += x * g * r * 1.414

# ───────── Score ─────────
D = 62  # D4
PENT = [0, 2, 4, 7, 9]
deg = lambda k: D + PENT[k % 5] + 12 * (k // 5)
# bass roots per bar (D, B, G, A cycle)
ROOTS = [38, 35, 31, 33]
RIFF = [0, 2, 4, 2, 5, 4, 2, 3]  # degrees over 8 eighths

def muted(t): return 10.8 <= t < 11.2

nbeats = int(DUR / BEAT)
for b in range(nbeats + 1):
    t = b * BEAT
    if t >= DUR: break
    bar = int(t // BAR); bib = b % 4
    if t < 3.2:  # opening: woodblock pulse, rising shaker
        if b >= 1: put(music, woodblock(.5, 1150 if bib % 2 == 0 else 900), t, .35, -.3)
        for e in range(2):
            put(music, shaker(), t + e * BEAT / 2, .08 + .1 * t / 3.2, .4)
        if b >= 4: put(music, rim(), t, .25 if bib % 2 else 0, .2)
        if b >= 2 and bib in (0, 2): put(music, frame_drum(), t, .5)
        continue
    if muted(t): continue
    half = t >= 11.2
    # drums
    if bib in (0, 2) and not (half and bib == 2): put(music, frame_drum(), t, .75)
    if bib in (1, 3): put(music, rim(), t, .35, .15)
    for e in range(2):
        tt = t + e * BEAT / 2
        if not muted(tt): put(music, shaker(), tt, .14 if e else .09, .45)
    if bib % 2 == 1: put(music, woodblock(.6, 1300), t + BEAT / 2, .18, -.5)
    # bass on beats 1 and 3 (and the "and" of 4)
    root = ROOTS[bar % 4]
    if bib in (0, 2): put(music, pluck_bass(root + (12 if bib == 2 else 0)), t, .55)
    if bib == 3 and not half: put(music, pluck_bass(root + 7, .7, .25), t + BEAT / 2, .4)
    # marimba riff in eighths
    for e in range(2):
        tt = t + e * BEAT / 2
        if muted(tt): continue
        k = RIFF[(bib * 2 + e) % 8] + (2 if bar % 2 else 0)
        if half and e == 1: continue
        put(music, marimba(deg(k + 5), .8, .7), tt, .22, -.25 + .5 * ((bib * 2 + e) % 2))

# swell under the wall
for k, m in enumerate([D + 12, D + 16, D + 19, D + 21]):
    put(music, marimba(m, .7, 2.5), 11.2 + k * .05, .18, -.3 + .2 * k)

# ───────── Picture-locked accents ─────────
CARD_FX = {'welcome': lambda t: put(fx, rustle(), t - .05, .35, -.2),
           'greenhouse': lambda t: put(fx, ding(), t, .16, .2),
           'seasons': lambda t: [put(fx, chime(deg(5 + q * 2)), t + q * BEAT, .14, -.4 + .27 * q) for q in range(4)],
           'forest': lambda t: [put(fx, pok(), t + q * BEAT, .25, (-.2, .2)[q % 2]) for q in range(2)],
           'moss': lambda t: put(fx, drip(), t + 0.6 * BEAT, .22, .3)}
onsets = []
for e in EV:
    t, ty = e['t'], e['type']
    if ty == 'tick': put(fx, rim(.8), t, .25, -.6 + .4 * (t / .1) / 3)
    elif ty == 'slam':
        put(fx, whoosh(.3), t - .3, .25); put(music, frame_drum(1, 52), t, .9); put(music, stab([D, D + 4, D + 7, D + 12]), t, .35); onsets.append(t)
    elif ty == 'chip': put(fx, woodblock(1, 1500), t, .3, .3)
    elif ty == 'cut':
        put(fx, whoosh(.22), t - .17, .18, (-.4, .4)[e['i'] % 2]); put(music, woodblock(1, 1600), t, .25)
        if e['i'] == 0: put(music, stab([D, D + 4, D + 9]), t, .25)
        onsets.append(t)
    elif ty == 'action':
        if e['card'] == 'seasons': CARD_FX['seasons'](t - 2 * BEAT)
        else: CARD_FX[e['card']](t)
        onsets.append(t)
    elif ty == 'wall':
        put(music, frame_drum(1, 48), t, 1.0); put(music, stab([D - 12, D, D + 4, D + 7]), t, .3); put(fx, whoosh(.3), t - .25, .2); onsets.append(t)
    elif ty == 'rowstop': put(fx, woodblock(1, 1000 + 200 * e['r']), t, .3, (-.4, 0, .4)[e['r']])
    elif ty == 'final':
        put(music, frame_drum(1, 44), t, 1.0); put(music, stab([D - 12, D, D + 7, D + 12, D + 16], 1, 1.2), t, .35)
        put(fx, chime(D + 24), t, .18); onsets.append(t)

# ambience: soft wind bed, out for the silence
wind = lp(brown(DUR + .05), 700)[:N]
gate = np.ones(N); i0, i1 = int(10.75 * SR), int(11.2 * SR); gate[i0:i1] = .25
fade = np.minimum(1, np.arange(N) / (.3 * SR))
for c in range(2): fx[:, c] += wind * .05 * gate * fade

# ───────── Mix ─────────
mix = music + fx
for c in range(2): mix[:, c] = compress(mix[:, c], thr=.18, ratio=3.0)
tail = np.minimum(1, (N - np.arange(N)) / (.25 * SR))
mix *= tail[:, None]
# bring the RMS near the target loudness, then let the limiter take the transients (keeps loudnorm in linear mode)
mix *= 10 ** (-15.5 / 20) / np.sqrt((mix ** 2).mean())
for _ in range(2):
    for c in range(2): mix[:, c] = limit(mix[:, c], .7)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)

# onset check: every accent lands on the beat grid
bad = [t for t in onsets if abs(t / BEAT - round(t / BEAT)) * BEAT > .015]
print(f'onsets {len(onsets) - len(bad)}/{len(onsets)} on the grid; low-band check:',
      round(10 * np.log10((lp(mix[:, 0], 120) ** 2).mean() / (mix[:, 0] ** 2).mean() + 1e-12), 1), 'dB below 120 Hz')
