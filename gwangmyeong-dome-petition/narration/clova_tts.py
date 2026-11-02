"""Narration with NAVER CLOVA Voice (Premium) — same lines.json and outputs as tts_zh.py (out/<id>.wav, out/dur.json).
Needs NCP_CLIENT_ID and NCP_CLIENT_SECRET in the environment (an NCP application with CLOVA Voice enabled).
Usage: python clova_tts.py lines.json out/ [--speaker nara] [--speed -1]
  --speaker  a CLOVA Voice premium speaker (e.g. nara, vara, vdain, vgoeun, nminjeong; see the NCP console for the full list)
  --speed    -5 (faster) … 5 (slower); a line's "clova_speed" overrides it.  The spoken text is a line's "say" (or "text")."""
import argparse, io, json, os, sys, urllib.parse, urllib.request
import numpy as np
import soundfile as sf

ap = argparse.ArgumentParser()
ap.add_argument('lines'); ap.add_argument('out'); ap.add_argument('--speaker', default='nara'); ap.add_argument('--speed', type=int, default=-1)
a = ap.parse_args()
cid, sec = os.environ.get('NCP_CLIENT_ID'), os.environ.get('NCP_CLIENT_SECRET')
if not cid or not sec:
    sys.exit('clova_tts.py: set NCP_CLIENT_ID and NCP_CLIENT_SECRET (environment settings → Network secrets / environment variables)')
os.makedirs(a.out, exist_ok=True)
URL = 'https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts'
dur = {}
for ln in json.load(open(a.lines, encoding='utf8')):
    body = urllib.parse.urlencode({'speaker': ln.get('clova_speaker', a.speaker), 'text': ln.get('say') or ln['text'],
                                   'speed': ln.get('clova_speed', a.speed), 'format': 'wav', 'sampling-rate': 24000}).encode()
    req = urllib.request.Request(URL, data=body, headers={'X-NCP-APIGW-API-KEY-ID': cid, 'X-NCP-APIGW-API-KEY': sec,
                                                         'Content-Type': 'application/x-www-form-urlencoded'})
    try:
        with urllib.request.urlopen(req, timeout=60) as r: wav = r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f'clova_tts.py: line {ln["id"]}: HTTP {e.code} {e.read()[:300]!r}')
    x, sr = sf.read(io.BytesIO(wav))
    if x.ndim > 1: x = x.mean(1)
    # trim leading/trailing silence (−45 dB), keep 40 ms of air
    idx = np.where(np.abs(x) > 10 ** (-45 / 20))[0]
    if len(idx): x = x[max(0, idx[0] - int(.04 * sr)): idx[-1] + int(.04 * sr)]
    sf.write(os.path.join(a.out, ln['id'] + '.wav'), x, sr)
    dur[ln['id']] = round(len(x) / sr, 3); print(ln['id'], dur[ln['id']], ln['text'])
json.dump(dur, open(os.path.join(a.out, 'dur.json'), 'w'), indent=1)
