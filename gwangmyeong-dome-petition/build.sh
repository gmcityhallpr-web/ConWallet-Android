#!/bin/sh
# One-command build: fonts → picture → events → score/foley → master → subtitles. 1080×1920, 30 fps.
# assets/qr.png encodes https://naver.me/IIDyvaSU (made with the qrcode package; regenerate it if the link changes).
set -e
LIB=${LIB:-/root/lemo-opuscar}
HERE=$(cd "$(dirname "$0")" && pwd)
export LIB
[ -f "$LIB/core/render/video.mjs" ] || { echo "set LIB to the library folder"; exit 1; }
sh "$HERE/fonts.sh"
cd "$LIB"
node core/render/events.mjs "$HERE" --size 1080x1920
node core/render/video.mjs "$HERE" --fps 30 --size 1080x1920 --workers 4 --out "$HERE/out/video.mp4"
# narration (edge-tts through the environment's proxy; needs speech.platform.bing.com)
[ -f "$HERE/narration/out/n1.wav" ] || (cd "$HERE/narration" && "$LIB/.venv/bin/python" tts_proxy.py lines.json out/)
"$LIB/.venv/bin/python" "$HERE/mix.py"
sh core/render/mux.sh "$HERE/out/video.mp4" "$HERE/out/mix.wav" "$HERE/gwangmyeong-dome-petition.mp4" 30 0
"$LIB/.venv/bin/python" core/render/srt.py "$HERE/cues.json" "$HERE/gwangmyeong-dome-petition.srt"
