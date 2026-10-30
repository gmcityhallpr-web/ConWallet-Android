"""Score + foley for the watercolor film, placed from events.json. Free time, gentle and acoustic:
a felt-piano-like phrase and kalimba on each painting-in, a soft pad under the tilt, brush rustles, pencil, birds,
a drip in the moss garden, a near-silence before the stamp, then a glockenspiel chord. No voice."""
import json, os, sys
import numpy as np
import soundfile as sf

LIB = os.environ['LIB']
sys.path.insert(0, os.path.join(LIB, 'core', 'audio'))
from sfx import SR, t_, env_exp, noise, bp, lp, hp, norm, compress, limit, brown  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ev = json.load(open(os.path.join(HERE, 'events.json')))
DUR, EV = ev['dur'], ev['ev']
N = int((DUR + 0.05) * SR)
music = np.zeros((N, 2)); fx = np.zeros((N, 2))
rng = np.random.default_rng(11)

def mtof(m): return 440 * 2 ** ((m - 69) / 12)
def put(buf, x, at, g=1.0, pan=0.0):
    i = int(round(at * SR))
    if i >= N or i < 0: return
    x = x[:N - i]; l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    buf[i:i + len(x), 0] += x * g * l * 1.414; buf[i:i + len(x), 1] += x * g * r * 1.414

# ───────── instruments ─────────
def piano(m, d=2.2, v=1.0):   # felt piano: soft hammer, few partials, slow decay
    tt = t_(d); f = mtof(m)
    x = sum(a * np.sin(2 * np.pi * f * k * tt * (1 + .0004 * k * k)) * env_exp(d, tau) for k, a, tau in [(1, 1, .9), (2, .35, .5), (3, .12, .3), (4, .05, .2)])
    return norm(lp(x * (1 - np.exp(-tt / .006)), 2600)) * v
def kalimba(m, d=1.2):
    tt = t_(d); f = mtof(m)
    return norm((np.sin(2 * np.pi * f * tt) + .25 * np.sin(2 * np.pi * f * 5.4 * tt) * env_exp(d, .03)) * env_exp(d, .35) * (1 - np.exp(-tt / .002)))
def glock(m, d=2.0):
    tt = t_(d); f = mtof(m)
    return norm(sum(a * np.sin(2 * np.pi * f * k * tt) * env_exp(d, tau) for k, a, tau in [(1, 1, .7), (2.76, .3, .2), (5.4, .12, .06)]))
def pad(ms, d):
    tt = t_(d); x = sum(np.sin(2 * np.pi * mtof(m) * tt + .3 * np.sin(2 * np.pi * .2 * tt)) for m in ms)
    env = np.minimum(1, tt / 1.5) * np.minimum(1, (d - tt) / 1.2)
    return norm(lp(x, 900) * env)
def rustle(d=1.2):
    tt = t_(d); x = bp(noise(d), 1800, 7000) * (.6 + .4 * np.abs(np.sin(2 * np.pi * 7 * tt))) * np.sin(np.pi * tt / d) ** 1.5
    return norm(x)
def pencil(d=.8):
    tt = t_(d); x = hp(noise(d), 3500) * (np.abs(np.sin(2 * np.pi * 11 * tt)) ** 4) * np.sin(np.pi * tt / d)
    return norm(x)
def bird(f0=3200):
    d = .18; tt = t_(d); f = f0 * (1 + .25 * np.sin(2 * np.pi * 14 * tt))
    return norm(np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * tt / d) ** 2)
def drip():
    d = .3; tt = t_(d); f = 800 + 1700 * (1 - np.exp(-tt / .03))
    return norm(np.sin(2 * np.pi * np.cumsum(f) / SR) * env_exp(d, .06))
def stamp():
    d = .4; tt = t_(d)
    return norm(np.sin(2 * np.pi * 85 * tt) * env_exp(d, .06) + lp(noise(d), 900) * env_exp(d, .015) * .8)

# ───────── score: D major, one phrase per painted space ─────────
PHR = [[62, 66, 69, 74], [64, 67, 71, 76], [66, 69, 73, 78], [62, 67, 71, 74], [61, 64, 69, 73]]
put(music, pad([50, 57, 62, 66], 12.1), .2, .35)                 # under the title and the tilt; ends before the stamp
put(music, piano(62, 3), .3, .5, -.2); put(music, piano(69, 3), 1.1, .4, .2)
for e in EV:
    t, ty = e['t'], e['type']
    if ty == 'paint':
        ph = PHR[e['i']]
        for k, m in enumerate(ph): put(music, piano(m, 2.4, .8 - .1 * k), t + k * .28, .32, -.3 + .2 * k)
        put(music, kalimba(ph[-1] + 12), t + 1.2, .14, .3)
        put(fx, rustle(e['d']), t, .16, e.get('pan', 0))
    elif ty == 'wash': put(fx, rustle(e['d']), t, .14, 0)
    elif ty == 'pencil': put(fx, pencil(e['d']), t, .07, .3)
    elif ty == 'drip': put(fx, drip(), t, .2, .3)
    elif ty == 'stamp':
        put(fx, stamp(), t, .55)
        for k, m in enumerate([74, 78, 81, 86]): put(music, glock(m), t + .25 + k * .09, .15, -.3 + .2 * k)
        put(music, piano(50, 3.5), t + .25, .4); put(music, piano(62, 3.5), t + .25, .3)
# birds and a room bed; the bed thins in the 0.5 s before the stamp (the brush lifted)
for k in range(9): put(fx, bird(rng.uniform(2600, 3800)), rng.uniform(.5, 14), .05, rng.uniform(-.7, .7))
bed = lp(brown(DUR + .05), 500)[:N] * .04
for c in range(2): fx[:, c] += bed

# ───────── master ─────────
mix = music + fx
for c in range(2): mix[:, c] = compress(mix[:, c], thr=.2, ratio=2.5)
mix *= np.minimum(1, np.arange(N) / (.05 * SR))[:, None] * np.minimum(1, (N - np.arange(N)) / (.6 * SR))[:, None]
mix *= 10 ** (-15.5 / 20) / np.sqrt((mix ** 2).mean())
for _ in range(2):
    for c in range(2): mix[:, c] = limit(mix[:, c], .7)
os.makedirs(os.path.join(HERE, 'out'), exist_ok=True)
sf.write(os.path.join(HERE, 'out', 'mix.wav'), mix.astype(np.float32), SR)
print('mix written', round(DUR, 2), 's')
