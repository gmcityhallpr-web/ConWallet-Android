# 콘지갑 Android

개인용 기프티콘 보관 앱. 로그인/서버/광고 없이 Android 기기 내부에 저장합니다.

## 구현 기능

- 갤러리에서 기프티콘 이미지 선택
- ML Kit 한국어 OCR로 브랜드/상품명/유효기간 자동 추정
- QR/바코드 자동 감지
- 저장 전 인식값 직접 수정
- D-30 / D-7 / D-1 오전 9시 만료 알림
- 사용 가능 / 7일 이내 / 기간 만료 / 사용 완료 / 전체 필터
- 상품명/브랜드/메모 검색
- 사용 완료 표시 및 취소
- 매장용 전체 화면 + 화면 밝기 최대 + 핀치 줌
- JSON 백업/복원(이미지 포함)
- iOS 콘지갑 v1 백업 JSON과 호환

## 요구사항

- Android 6.0(API 23) 이상
- Android Studio / JDK 17
- 최초 빌드에는 인터넷 연결 필요(Gradle 및 ML Kit 라이브러리 다운로드)

## 가장 쉬운 APK 만들기 (Mac)

1. Android Studio를 설치하고 한 번 실행해 Android SDK 설치를 완료합니다.
2. 이 폴더의 `build-apk-mac.command`를 더블클릭합니다.
3. macOS가 실행을 막으면 파일 우클릭 → `열기` → `열기`를 선택합니다.
4. 완료 후 프로젝트 루트에 `ConWallet-Android-debug.apk`가 생성됩니다.

스크립트는 필요한 경우 Gradle 8.13을 받아 Wrapper를 만든 뒤 `:app:assembleDebug`를 실행합니다.

## Android Studio에서 APK 만들기

프로젝트 폴더를 Android Studio로 연 뒤 Gradle Sync가 끝나면:

- Build → Generate Bundle(s) / APK(s) → Generate APK(s)
- 디버그 APK 경로: `app/build/outputs/apk/debug/app-debug.apk`

디버그 APK는 자동으로 디버그 키로 서명되어 개인 기기에 직접 설치할 수 있습니다.

## GitHub Actions

`.github/workflows/build-apk.yml`이 포함되어 있습니다. GitHub 저장소에 올린 뒤 Actions → `Build ConWallet APK` → Run workflow를 누르면 `ConWallet-Android-debug` 아티팩트로 APK가 생성됩니다.

## iPhone ↔ Android 백업

양쪽 앱의 설정에서 JSON 백업을 내보내고 불러올 수 있습니다. `version: 1` 형식을 맞췄으며 다음 필드를 공유합니다.

`id, title, brand, memo, expiryDate, createdAt, updatedAt, isUsed, usedAt, notificationsEnabled, barcodePayload, barcodeSymbology, imageData`

이미지는 Base64로 JSON 안에 포함됩니다.

## 알림 시각

Android의 절전/Doze 정책 때문에 D-30/D-7/D-1 알림은 오전 9시를 기준으로 예약하지만 약간 늦게 표시될 수 있습니다. 정확 알람 권한을 요구하지 않는 개인용 설계입니다.
