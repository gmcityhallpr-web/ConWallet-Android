#!/bin/sh
# Noto Sans KR subsets (OFL) from the Google Fonts CSS API: every character used in film.js.
set -e
H=$(cd "$(dirname "$0")" && pwd)
UA="Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36"
TXT=$(python3 -c "
import urllib.parse
s=open('$H/film.js',encoding='utf8').read()
c=set(ch for ch in s if ord(ch)>=0x20)|set('0123456789%+·—–−≈×?₩㎡')
print(urllib.parse.quote(''.join(sorted(c))))")
for w in 900 500; do
  url=$(curl -fsS -A "$UA" "https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@$w&text=$TXT" | grep -o 'https://[^)]*' | head -1)
  curl -fsS -o "$H/fonts/kr$w.woff2" "$url"
done
