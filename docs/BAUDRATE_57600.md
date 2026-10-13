# 57600 baud 지원 빌드

기준 소스: `TechxArtisanStudio/Openterface_Android`의
`1174b2b0fce8df4f48568cc48c9fee83e83b2566` 커밋.
앱 버전: `1.2.9-57600`, 패키지: `com.openterface.AOS`.

## 사용 방법

1. Android 7.0 이상 기기에 APK를 설치합니다.
2. 앱의 설정에서 **보율 → 57600 bps → 확인**을 선택합니다.
3. 연결된 시리얼 포트는 선택한 속도로 다시 연결됩니다.
4. 선택값은 앱 재시작 및 USB 재연결 후에도 유지됩니다.

이 변경은 호스트 앱의 UART 속도를 설정합니다. 연결 대상의 UART 속도도
57600이어야 하며, 대상 장치의 펌웨어나 UART 설정을 자동으로 변경하지 않습니다.
Mini-KVM v2의 FE0C 직접 USB 경로는 기존 동작을 유지합니다.

배포 APK는 프로젝트의 기본 디버그 키로 서명한 release 빌드입니다.
원본 앱과 서명 키가 다르면 덮어쓰기 설치가 되지 않으므로 기존 앱을 제거한
후 설치해야 합니다. 기존 앱을 제거하면 그 앱의 저장 설정도 삭제됩니다.

## 수정 내용

- 가로 및 세로 설정 화면에 57600 선택 항목을 추가했습니다.
- CH340 및 일반 시리얼 연결의 대체 속도 목록에 57600을 추가했습니다.
- 사용자 선택을 저장하고 장치별 기본값이 선택을 덮어쓰지 않도록 했습니다.
- 속도 변경 시 읽기 루프와 연결 상태를 정리하여 재연결이 실행되도록 했습니다.
- 설정 저장, 복원, 기존 기본값, 잘못된 저장값, 재연결에 대한 회귀 테스트를 추가했습니다.
- 별도 서명 비밀값 없이 APK를 빌드하는 GitHub Actions 워크플로를 추가했습니다.

## 로컬 빌드

JDK 17, Android SDK 36, Build Tools 35.0.0,
NDK 26.3.11579264, CMake 3.22.1 및 Node.js 22가 필요합니다.
소스 ZIP에는 서브모듈 소스도 포함되어 있으므로 ZIP에서 빌드할 때는
아래의 `git submodule` 단계를 생략합니다.

```bash
git submodule update --init --recursive
cd Openterface_WebUI
npm ci
npm run build:webrtc:android
cd ..
chmod +x gradlew
./gradlew :app:assembleRelease :app:testDebugUnitTest \
  --tests com.openterface.AOS.serial.UsbDeviceManagerBaudrateTest \
  -x buildWebClient --console=plain
```

`local.properties`의 `sdk.dir`에는 로컬 Android SDK 경로를 설정합니다.
APK 출력 위치: `app/build/outputs/apk/release/`.

## GitHub Actions

`support-57600` 브랜치에 push하면 **Build 57600 baud APK** 워크플로가 실행됩니다.
Actions에서 해당 워크플로를 수동 실행할 수도 있습니다.
성공한 실행의 **Openterface-1.2.9-57600-apk** artifact에서 APK를 받을 수 있습니다.

원본 프로젝트와 동일한 AGPL-3.0 라이선스를 적용합니다.
