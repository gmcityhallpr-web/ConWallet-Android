#!/bin/sh
# One-command build: paper → fonts → picture → events → score/foley → master → subtitles. 1080×1920, 30 fps.
set -e
LIB=${LIB:-/root/lemo-opuscar}
HERE=$(cd "$(dirname "$0")" && pwd)
export LIB
[ -f "$LIB/core/render/video.mjs" ] || { echo "set LIB to the library folder"; exit 1; }
[ -f "$HERE/paper.jpg" ] || (cd "$HERE" && "$LIB/.venv/bin/python" paper.py)
sh "$HERE/fonts.sh"
cd "$LIB"
node core/render/events.mjs "$HERE" --size 1080x1920
node core/render/video.mjs "$HERE" --fps 30 --size 1080x1920 --workers 4 --out "$HERE/out/video.mp4"
"$LIB/.venv/bin/python" "$HERE/mix.py"
sh core/render/mux.sh "$HERE/out/video.mp4" "$HERE/out/mix.wav" "$HERE/gahaksan-watercolor.mp4" 30 1
node "$HERE/cues.mjs" >/dev/null
"$LIB/.venv/bin/python" core/render/srt.py "$HERE/cues.json" "$HERE/gahaksan-watercolor.srt"
