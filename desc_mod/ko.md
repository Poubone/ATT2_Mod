> **ATT2 Project**
>
> *Across The Time II — Time For Regrets*용 Fabric 클라이언트 모드입니다. 맵의 여러 시스템과 관련된 화면과 정보를 게임 안에 추가합니다.

## HUD

캐릭터 능력치, 진행 상황, 자원, 장비 상태를 HUD에 표시할 수 있습니다. 설정 화면에서 항목을 표시하거나 숨기고 위치를 바꿀 수 있습니다.

## 퀘스트 일지

퀘스트 책에서 메인, 서브, 일일 퀘스트를 확인할 수 있습니다. 목록 탐색, 필터, 검색을 지원하며 퀘스트 목표를 확인하고 추적할 수 있습니다.

## 주문과 캐릭터

- 재사용 대기시간 표시가 있는 주문 바.
- 일부 주문의 레벨을 고르는 방사형 선택 메뉴.
- 캐릭터 능력치 확인 및 강화 화면.
- 장비 수리 전용 메뉴.

## 기타 인터페이스

상점과 미니게임 등 맵의 일부 상호작용을 전용 화면으로 표시합니다. 순간이동 전환 효과, 바닥 아이템을 찾기 위한 광선, 상황별 단축키도 추가합니다.

## 파티 기능

Party Sync API에 연결하면 같은 그룹의 플레이어 사이에서 아이템 공유와 핑을 전달할 수 있습니다. 이 기능은 API 연결이 필요합니다. Discord 상태에는 게임 활동과 추적 중인 퀘스트를 표시할 수 있습니다.

## 호환성

- Minecraft Java **1.21.11**
- Fabric Loader 및 Fabric API
- Java **21**
- 클라이언트 모드: 플레이어마다 설치해야 하며 Minecraft 서버에는 설치하지 않습니다.

## 설치

### Minecraft 공식 런처

1. 최신 Minecraft 런처에는 보통 Java가 포함되어 있습니다. 별도 Java를 사용한다면 [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21)을 설치하세요.
2. [Fabric 공식 설치 프로그램](https://fabricmc.net/use/installer/)을 다운로드해 실행하고 **Client**, **Minecraft 1.21.11**을 선택하세요. Fabric Loader 버전은 설치 프로그램이 권장하는 값을 사용하면 됩니다.
3. Fabric 프로필로 게임을 한 번 실행한 뒤 종료하세요.
4. [Minecraft 1.21.11용 Fabric API 0.141.6](https://modrinth.com/mod/fabric-api/version/6qAuTtLR)과 아래 버전 목록의 ATT2 Project를 다운로드하세요.
5. 두 `.jar` 파일을 게임의 `mods` 폴더에 넣으세요. 런처의 **설치 설정 → Fabric 프로필 → 게임 폴더 열기**에서 폴더를 찾을 수 있습니다. `mods` 폴더가 없으면 만드세요.
6. **1.21.11** Fabric 프로필로 Minecraft를 실행하세요.

## 소스 코드 및 기여

모드의 소스 코드는 [GitHub](https://github.com/Poubone/ATT2_Mod)에서 공개됩니다. 버그를 신고하고, 개선 사항을 제안하고, 프로젝트에 기여할 수 있습니다.

### 기여자

- Poubone
- Simuciokas
