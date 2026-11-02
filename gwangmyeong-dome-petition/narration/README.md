# Narration

Generated with `tts_proxy.py` (runs the library's `core/tts/tts_zh.py` through the environment's HTTPS proxy and CA bundle,
which edge-tts does not pick up by itself); WAVs in `out/`, mixed by `../mix.py`. Needs speech.platform.bing.com.
Speech-to-text hears "돔구장" as "동구장" in every phrasing tried, including "돔 경기장" alone — most likely a Whisper bias; listen to line n1. `lines.json` holds the script, one line per shot,
with `start` (seconds in the film), `voice`, `rate`, `text` (subtitles) and `say` (what is spoken: numbers spelled out).

Note: edge-tts uses Microsoft Edge's read-aloud service. Its terms for official city publicity are unclear; a recorded voice
or a licensed TTS (Naver Clova Voice, Azure Speech with a key) is the safer choice for a final public release.

Steps (as done) (LIB = the Lemo-Opuscar library, ffmpeg 7+):
1. `"$LIB/.venv/bin/python" "$LIB/core/tts/tts_zh.py" lines.json out/` — one WAV per line + `dur.json` (tts_zh.py reads `voice`, `rate`, `say`).
2. Check each line fits before the next `start` (shot cuts at 4.5, 9.0, 12.0, 15.05); raise `rate` (≤ +15%) or trim words if not.
3. Speech-to-text check: `"$LIB/.venv/bin/python" "$LIB/core/tts/asr_check.py" lines.json out/ --lang auto`.
4. In `../mix.py`: high-pass 90 Hz and compress 3:1 the voice, place each WAV at its `start`, duck the music ≈ −7 dB under speech
   (and thin 1.2–5 kHz), then master as before; rebuild the `.srt` from `text` + start/duration.

## Switching to NAVER CLOVA Voice (licensed)

`clova_tts.py` writes the same `out/<id>.wav` + `out/dur.json` from `lines.json` with a CLOVA Voice Premium speaker.
Set `NCP_CLIENT_ID` / `NCP_CLIENT_SECRET` in the environment, then:

    "$LIB/.venv/bin/python" clova_tts.py lines.json out/ --speaker nara --speed -1

Check every line still ends before its shot cuts (4.5, 9.0, 12.0, 14.9), run `../mix.py`, re-mux, and update the
narration credit in `../CREDITS` (voice: NAVER CLOVA Voice, speaker name, under the NCP account's terms).
