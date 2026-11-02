# Narration — to be added in a session that can reach speech.platform.bing.com

The 20 s cut in `../gwangmyeong-dome-petition.mp4` has no voice yet: the build environment it was made in blocked the
Microsoft neural voice service that `core/tts/tts_zh.py` (edge-tts) uses. `lines.json` holds the script, one line per shot,
with `start` (seconds in the film), `voice`, `rate`, `text` (subtitles) and `say` (what is spoken: numbers spelled out).

Note: edge-tts uses Microsoft Edge's read-aloud service. Its terms for official city publicity are unclear; a recorded voice
or a licensed TTS (Naver Clova Voice, Azure Speech with a key) is the safer choice for a final public release.

Steps in the new session (LIB = the Lemo-Opuscar library, ffmpeg 7+):
1. `"$LIB/.venv/bin/python" "$LIB/core/tts/tts_zh.py" lines.json out/` — one WAV per line + `dur.json` (tts_zh.py reads `voice`, `rate`, `say`).
2. Check each line fits before the next `start` (shot cuts at 4.5, 9.0, 12.0, 15.05); raise `rate` (≤ +15%) or trim words if not.
3. Speech-to-text check: `"$LIB/.venv/bin/python" "$LIB/core/tts/asr_check.py" lines.json out/ --lang auto`.
4. In `../mix.py`: high-pass 90 Hz and compress 3:1 the voice, place each WAV at its `start`, duck the music ≈ −7 dB under speech
   (and thin 1.2–5 kHz), then master as before; rebuild the `.srt` from `text` + start/duration.
