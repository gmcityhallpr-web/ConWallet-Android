#!/bin/sh
# Subset the OFL fonts to the characters used in film.js (keeps the project small). Needs fonttools (the library's .venv has it).
set -e
H=$(cd "$(dirname "$0")" && pwd); PY=${LIB:-/root/lemo-opuscar}/.venv/bin/python
CH=$(python3 -c "s=open('$H/film.js',encoding='utf8').read(); print(''.join(sorted(set(c for c in s if ord(c)>=0x20)|set('0123456789.·'))))")
for f in GowunBatang-Bold NanumPenScript-Regular; do
  [ -f "$H/fonts/$f.full.ttf" ] || mv "$H/fonts/$f.ttf" "$H/fonts/$f.full.ttf"
  "$PY" -m fontTools.subset "$H/fonts/$f.full.ttf" --text="$CH" --output-file="$H/fonts/$f.ttf"
done
