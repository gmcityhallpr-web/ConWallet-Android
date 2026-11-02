#!/bin/sh
# OFL subsets from the Google Fonts CSS API: every character used in film.js. FORCE=1 to fetch again.
set -e
H=$(cd "$(dirname "$0")" && pwd); D=$H/fonts; mkdir -p "$D"
UA="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"
TXT=$(python3 -c "
import urllib.parse
s=open('$H/film.js',encoding='utf8').read()
c=set(ch for ch in s if ord(ch)>=0x20)|set('0123456789%+·—–−,.')
print(urllib.parse.quote(''.join(sorted(c))))")
font() { [ -s "$D/$2" ] && [ -z "$FORCE" ] && return 0
  url=$(curl -fsS -A "$UA" "https://fonts.googleapis.com/css2?$1&text=$TXT" | grep -o 'https://[^)]*' | head -1); curl -fsS -o "$D/$2" "$url"; echo "$2"; }
font "family=Noto+Sans+KR:wght@900" kr900.woff2
font "family=Noto+Sans+KR:wght@700" kr700.woff2
font "family=Barlow+Condensed:wght@800" b800.woff2
font "family=Barlow+Condensed:wght@600" b600.woff2
font "family=IBM+Plex+Mono:wght@500" plex500.woff2
for f in notosanskr barlowcondensed ibmplexmono; do [ -s "$D/OFL-$f.txt" ] || curl -fsS -o "$D/OFL-$f.txt" "https://raw.githubusercontent.com/google/fonts/main/ofl/$f/OFL.txt"; done
