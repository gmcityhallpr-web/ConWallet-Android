#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

echo "[1/5] Java/Android SDK 확인"
if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ] && [ -d "$HOME/Library/Android/sdk" ]; then
  SDK="$HOME/Library/Android/sdk"
fi
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
  echo "Android SDK를 찾을 수 없습니다. Android Studio를 먼저 설치하고 한 번 실행해 주세요."
  read -r -p "Enter를 눌러 종료..."
  exit 1
fi
printf 'sdk.dir=%s\n' "$SDK" > local.properties

if ! command -v java >/dev/null 2>&1; then
  echo "Java를 찾을 수 없습니다. Android Studio 설치를 확인해 주세요."
  read -r -p "Enter를 눌러 종료..."
  exit 1
fi

if [ ! -f "gradlew" ]; then
  echo "[2/5] Gradle 8.13 임시 다운로드"
  TMP="$(mktemp -d)"
  trap 'rm -rf "$TMP"' EXIT
  curl -L --fail --retry 3 -o "$TMP/gradle.zip" "https://services.gradle.org/distributions/gradle-8.13-bin.zip"
  unzip -q "$TMP/gradle.zip" -d "$TMP"
  echo "[3/5] Gradle Wrapper 생성"
  "$TMP/gradle-8.13/bin/gradle" wrapper --gradle-version 8.13 --distribution-type bin
fi
chmod +x gradlew

echo "[4/5] APK 빌드"
./gradlew :app:assembleDebug

SRC="app/build/outputs/apk/debug/app-debug.apk"
DEST="ConWallet-Android-debug.apk"
cp "$SRC" "$DEST"

echo "[5/5] 완료"
echo "APK: $(pwd)/$DEST"
open -R "$DEST" 2>/dev/null || true
read -r -p "Enter를 눌러 종료..."
