<div align="center">
  <img src="app/src/main/res/drawable/app_icon.png" width="100" height="100" alt="OplusConfigHook icon" />
  <h1>一加墓碑/自启Hook OplusConfigHook</h1>
  <p>APP最好的归宿是坟墓</p>
</div>

<p align="center">
  <img alt="Android 15" src="https://img.shields.io/badge/Android-15%20only-3DDC84?logo=android&logoColor=white">
  <img alt="LSPosed" src="https://img.shields.io/badge/LSPosed-required-111111">
<img alt="Scope" src="https://img.shields.io/badge/Scope-athena%20%2B%20battery-orange">
</p>


> Jump to: **[English](#english)** · **[日本語](#japanese)** · **[한국어](#korean)**

---

云控下发什么的去死吧。



## 说明

### 支持范围

- 仅支持：**Oxygen OS 15 , Color OS 15(maybe)**
- LSPosed 作用域：必须同时勾选  
  - `com.oplus.athena`（墓碑策略读取方）  
  - `com.oplus.battery`（自启/电池相关策略读取方）

### 使用步骤

1. 安装 APK，LSPosed 启用模块。
2. 在 LSPosed 中同时勾选作用域：`com.oplus.athena` + `com.oplus.battery`。
3. 打开APP，给予ROOT权限，修改配置并应用策略
4. 重启手机

**请务必配合一个“开机动画阶段可停用/卸载 LSPosed”的救砖模块使用！！！**

### 快捷操作：秒冻与保活

秒冻：**把目标应用纳入更激进的场景冻结治理**（并移出常见优待/豁免名单）。它不是“立刻杀进程”的 API；实际生效取决于系统检查周期与场景触发。

#### 秒冻（压制）——冻结时序档位（Timing Preset）

当你勾选“同步冻结时序为 0（全局，高风险）”时，会提供 L1/L2/L3 三档：

- **L1（保守）**：仅对 `highExtremeMode + appType=1000` 的冻结时序关键字段置 0  
- **L2（均衡）**：对 `highExtreme/extreme/highPerf/highLoad + appType=1000` 置 0  
- **L3（激进）**：在 L2 基础上，appType 扩展到 `1000/4/7/4#7`

说明：

- 默认 **不修改** `checkImportance`。
- 这是全局配置变更：不是“只对某个包生效”。

#### 保活

保活：把目标包加入 DoNotFreeze / whitePkg / IM / CPUCtl / AppCard 等“关键豁免/白名单”体系，模拟Oxygen对系统APP/通讯软件的保护。  

## 配置解读

1）全局开关与基础节奏  

`enableConfig / lcdOffConfig / lcdOnConfig`：决定 Hans/OFreezer 是否启用，以及亮/灭屏下从活跃到冻结的时间窗、检查周期等。

2）灭屏快速收敛后台（fast freezer）  

`ffConfig / ffTimeout / ffPkg`：灭屏后更快冻结后台来压耗电/唤醒；用 `ffPkg type="black"` 明确哪些包“不要快冻”（避免误伤）。

3）关键/高风险/海外差异的包做特殊分类  

`whitePkg`：关键包/自家包/第三方保活包白名单（category 三位标志）。  

`gms`：GMS 栈核心包名单，配合 `gmsEnable`。  

`blackHansPkg`：海外/出口版本黑名单占位。

4）系统级黑管控（重点治理对象）  

`SysBlackPolicy / SysBlackRestrictMask`：定义不同场景（night/lcdoff/lcdon/charging/extremeFg/fastFreeze/…）的限制模板/上限。  

`SysBlackApp`：把包名挂到模板上，并为广播/网络/GPS 提供例外（excludeAction/forceProxy），目标是“限制后台能力但尽量不把关键流程弄坏”。

5）代理/批处理（减少唤醒）  

`proxyConfig` 总开关：alarm/service/job/broadcast 是否代理。  

`defaultProxyBc / proxyBcExcludeAction / proxy*`：分别治理广播、wakelock、传感器、binder 回调、定位、蓝牙扫描等典型耗电源。

6）失败兜底与动态解冻  
`trafficConfig`：检测到真实网络活动时短时解冻避免断连。  

`serviceBumpUnFzReasonList`：create/start/bind 等关键 Service 行为允许解冻/提升优先级。  

`prevent`：特定包在特定 scene 下强制限制（硬规则，可按版本/渠道控制）。

7）极端场景治理与配额  

`cpuCtlRus / high-loading / strictMode`：高负载/极端省电下进一步收敛（多数 ROM 的 strictMode 默认 enable=false）。  

`android-freeze`：与 AOSP freezer 联动。  

`appQuotaConfig`：后台任务配额，超额进入保护/限制状态。

---

## 自启管理

系统会对底层白名单中的应用放行自启权限。

如果你用特殊方法（例如Oxygen OS 15低版本AOSP自启管理页 / ROOT唤起启动管理页）关闭受白名单保护的应用，过一会你会发现系统自动给它恢复权限了。

在本APP中，请勿取消勾选一加系APP。若您有宗教信仰需要用到祈祷提醒类APP，请在此予以放行。毕竟Heytap / Oppo Push适配的APP并不多，对于适配的APP也可能受地区网络条件影响，有一定的推送延迟。

此处对APP预加载行为起不到压制作用。

若您是在电量，手机使用寿命这一块较为敏感的用户，推荐在系统后台管理页面对冷启动开销较大的应用下划锁定。



**重启场景未能命中自启保护策略替换持久化，推荐直接使用ROOT打开电池的启动管理页面。**

---

## 卸载/禁用建议

- 卸载前建议先使用 App 内的“清除系统级配置持久化”按钮（避免残留配置影响系统行为）。  
- 如需紧急恢复：优先通过救砖 Magisk 模块在开机动画阶段停用/卸载 LSPosed，再进入系统处理。

---

## 捐赠

如果本项目对你有帮助，欢迎请我喝杯咖啡 ☕️

<img src="app/src/main/res/drawable/donation_qr.png" width="280" alt="donation qr" />

---

**PS**：项目有一定自嗨成分，一是不想惯着系统APP，二是我以后开发类似的墓碑管理应用会照着这个来，三是欧加真系统仍有其他地方有类似实现（流体云、HDR杜比视界、内容拖拽等），我不确定我有没有时间继续开发，暂时先打通一些可复用的内容之后看看情况。





<a id="english"></a>

## Overview

### Supported scope

- Supported only on: **OxygenOS 15, ColorOS 15 (maybe)**
- LSPosed scope: you must enable **both** of the following:
  - `com.oplus.athena` (reads tombstone-related policies)
  - `com.oplus.battery` (reads auto-start / battery-related policies)

### Steps

1. Install the APK and enable the module in LSPosed.
2. In LSPosed, enable both scopes: `com.oplus.athena` + `com.oplus.battery`.
3. Open the app, grant **root** permission, modify the configuration, and apply the policy.
4. Reboot the phone.

**You must use this together with an anti-brick module that can disable or uninstall LSPosed during the boot animation stage.**

### Quick actions: Fast Freeze and Keep alive

Fast Freeze: **puts the target app under a more aggressive scenario-based freeze policy** (and removes it from common priority / exemption lists). It is **not** an API for “killing the process immediately”; actual effect depends on the system’s inspection cycle and scenario triggers.

#### Suppress now — freeze timing presets

When you enable **“Sync freeze timing to 0 (global, high risk)”**, three levels are available: L1 / L2 / L3.

- **L1 (Conservative):** sets only the key freeze-timing fields of `highExtremeMode + appType=1000` to `0`
- **L2 (Balanced):** sets `highExtreme / extreme / highPerf / highLoad + appType=1000` to `0`
- **L3 (Aggressive):** based on L2, expands `appType` to `1000 / 4 / 7 / 4#7`

Notes:

- By default, **`checkImportance` is not modified**.
- This is a **global configuration change**; it does **not** apply to just one package.

#### Keep alive

Keep alive adds the target package to critical exemption / whitelist systems such as DoNotFreeze, whitePkg, IM, CPUCtl, and AppCard, simulating OxygenOS-style protection for system apps and communication apps.

## Configuration details

1) Global toggles and base timing

`enableConfig / lcdOffConfig / lcdOnConfig`: determine whether Hans/OFreezer is enabled, as well as the time window and inspection cycle from active to frozen under screen-on and screen-off states.

2) Fast convergence of background apps when the screen is off (fast freezer)

`ffConfig / ffTimeout / ffPkg`: freezes background apps more quickly after screen-off to reduce power drain and wakeups; use `ffPkg type="black"` to explicitly specify packages that should **not** be fast-frozen (to avoid false positives).

3) Special classification for critical, high-risk, or overseas-differentiated packages

`whitePkg`: whitelist for critical packages, first-party packages, and third-party keep-alive packages (`category` is a three-bit flag).

`gms`: core package list for the GMS stack, used together with `gmsEnable`.

`blackHansPkg`: blacklist placeholder for overseas / export variants.

4) System-level blacklist control (key governance targets)

`SysBlackPolicy / SysBlackRestrictMask`: define restriction templates / upper bounds for different scenarios (`night / lcdoff / lcdon / charging / extremeFg / fastFreeze / ...`).

`SysBlackApp`: attaches package names to those templates and provides exceptions for broadcast / network / GPS (`excludeAction / forceProxy`), with the goal of restricting background capability without breaking critical flows whenever possible.

5) Proxy / Batch (reduce wakeups)

`proxyConfig` master switch: whether `alarm / service / job / broadcast` should be proxied.

`defaultProxyBc / proxyBcExcludeAction / proxy*`: govern typical power-drain sources such as broadcasts, wakelocks, sensors, binder callbacks, location, Bluetooth scanning, etc.

6) Failure fallback and dynamic unfreeze

`trafficConfig`: temporarily unfreezes the app when real network activity is detected, to avoid disconnects.

`serviceBumpUnFzReasonList`: allows unfreezing / priority bumping for critical Service behaviors such as `create / start / bind`.

`prevent`: forcibly restricts specific packages in specific scenes (hard rules, can be controlled by version / channel).

7) Extreme-scenario governance and quotas

`cpuCtlRus / high-loading / strictMode`: further tightens control under high-load or extreme power-saving conditions (on most ROMs, `strictMode` defaults to `enable=false`).

`android-freeze`: links with the AOSP freezer.

`appQuotaConfig`: background task quotas; apps exceeding the quota enter a protected / restricted state.

---

## Autostart management

The system automatically grants auto-start permission to apps that are on the low-level whitelist.

If you disable an app protected by that whitelist through special methods (for example, the AOSP auto-start management on early OxygenOS 15 builds, or by opening the startup management page through root), you may find that the system restores its permission automatically after a while.

In this app, do **not** uncheck OnePlus-related apps. If you rely on prayer reminder apps for personal or religious reasons, allow them here. After all, not many apps support Heytap / Oppo Push, and even supported apps may still experience push delays depending on regional network conditions.

This section cannot meaningfully suppress app preloading behavior.

If you are particularly sensitive to battery wear or device longevity, it is recommended to lock down apps with high cold-start overhead in the system’s background management page.



**If the reboot scenario fails to hit the persistent replacement for the auto-start protection policy, it is recommended to use root to open the Battery app’s Startup Management page directly.**

---

## Uninstall / recovery recommendations

- Before uninstalling, it is recommended to first use the in-app **"Clear system-level persistent configuration (Root required)"** button (to avoid leftover settings affecting system behavior).
- If you need an emergency recovery: first use a rescue Magisk module to disable or uninstall LSPosed during the boot animation stage, then enter the system and handle the rest.

---

## Donations

If this project has helped you, feel free to buy me a coffee.

<img src="app/src/main/res/drawable/donation_qr.png" width="280" alt="donation qr" />



**P.S.** This project is partly a personal indulgence. First, I do not want to keep giving system apps special treatment. Second, if I develop similar tombstone-management apps in the future, this project will serve as the reference implementation. Third, Oneplus still has similar mechanisms in other parts of the system (such as Fluid Cloud, HDR Dolby Vision, content drag-and-drop, and so on). I am not sure whether I will have the time to continue developing this further, so for now I plan to first build out some reusable components and then see how things go.

<a id="japanese"></a>

## 概要

### 対応範囲

- 対応環境：**OxygenOS 15、ColorOS 15（Maybe）**
- LSPosed の対象は必ず両方有効にしてください  
  - `com.oplus.athena`（バックグラウンド凍結ポリシーの読み取り側）  
  - `com.oplus.battery`（自動起動関連ポリシーの読み取り側）

### 手順

1. APK をインストールし、LSPosed で本モジュールを有効化します。  
2. LSPosed で `com.oplus.athena` と `com.oplus.battery` の両方を対象に追加します。  
3. アプリを開き、**Root 権限**を付与して設定を変更し、**「ポリシーを適用」** を押します。  
4. 端末を再起動します。  

**必ず、起動アニメーション中に LSPosed を無効化またはアンインストールできる救済用モジュールと併用してください。**

### クイック操作：今すぐ抑制 と 常駐維持

**今すぐ抑制** は、対象アプリをより厳しいシーン別凍結制御の対象に追加し、優待 / 免除リストから外すためのクイック操作です。  
これは**即時にプロセスを kill する機能ではありません**。実際の反映は、システムのチェック周期やシーン発火条件に依存します。

#### 今すぐ抑制 — 凍結タイミング(Freeze timing) プリセット

**「凍結タイミングパラメータを 0 に同期（全体・高リスク）」** を有効にすると、L1 / L2 / L3 のプリセットを選べます。

- **L1（保守プリセット）: highExtremeMode + appType=1000**
- **L2（標準プリセット）: highExtreme/extreme/highPerf/highLoad + appType=1000**
- **L3（強攻プリセット）: L2 + appType=1000/4/7/4#7**

補足：

- `checkImportance` は変更しません。  
- 変更対象は `freezeInterval` 内の**既存フィールドのみ**です。新しいフィールドは追加しません。  
- これは**全体に影響する高リスク設定**であり、特定パッケージだけに限定して適用されるものではありません。  

#### 常駐維持

**常駐維持** は、対象パッケージを優先 / 免除リスト側に寄せるためのクイック操作です。  
状況に応じて `SysBlackApp` の各シーンから除外し、対応している場合は `ffPkg.black` や `whitePkg.category=100` などにも反映します。  
変更は **「ポリシーを適用」** を押すまで一時保存です。

## バックグラウンド凍結の見方

1）全体スイッチと基本タイミング  

`enableConfig / lcdOffConfig / lcdOnConfig` は、バックグラウンド凍結制御を有効にするかどうか、および画面オン / オフ状態での実行→遷移→凍結タイミングを決めます。

2）Fast Freeze（高速凍結）  

`ffConfig / ffTimeout / ffPkg` は、画面オフ後にバックグラウンドをより早く凍結して、消費電力や wakeup を抑えるための設定です。  
`ffPkg type="black"` を使うと、**高速凍結の対象外にするアプリ**を明示できます。

3）Critical Allow / Whitelist（重要許可 / ホワイトリスト）  

`DoNotFreeze / whitePkg / imPkg / cpuCtlWhiteList / appCard.ignoreApp` など、重要な優先 / 免除リストをまとめて扱います。  
通知・常駐・起動後の維持に影響するため、変更は慎重に行ってください。

4）Sys Black Governance（システム黒管制）  

`SysBlackPolicy / SysBlackRestrictMask / SysBlackApp` を使って、**画面オン / 画面オフ / 夜間** などのシーンごとに、より厳しい凍結制御を適用します。  
対象アプリをシーン別ブラックリストへ追加しつつ、必要に応じて `excludeAction` や `forceProxy` などの例外も与えられます。

5）Proxy / Batch（プロキシ / バッチ）  

`proxyConfig` はプロキシ系の全体スイッチです。  
`defaultProxyBc / proxyBcExcludeAction / proxySensor / proxyGps / proxyBtScan / proxyBinder` などを通じて、broadcast、sensor、binder callback、位置情報、Bluetooth scan などの wakeup 源をまとめて制御します。

6）Unfreeze / Bump（解凍 / Bump）  

`serviceBumpUnFzReasonList / trafficConfig / prevent / allow` などを使って、誤凍結を減らすための解凍理由やフォールバック規則を調整します。  
バックグラウンド制御を強くしすぎた場合の保険として使う領域です。

7）High Load / Strict / Quota（高負荷 / 厳格制御 / クォータ）  

`high-loading / strictMode / android-freeze / appQuotaConfig / cpuCtlRus` など、高負荷時や厳格制御シナリオ向けの項目です。  
**高リスク寄りの設定**なので、意味が分からないまま変更しないことをおすすめします。

---

## 自動起動管理

システムは、低レベルのホワイトリストに入っているアプリに対して、自動起動を自動的に許可する場合があります。

特殊な方法（たとえば一部ビルドの自動起動管理画面や、Root で起動管理ページを直接開く方法）で権限を切っても、しばらくするとシステムが自動的に戻してしまうことがあります。

このアプリでは、**OnePlus 系アプリのチェックを外さない**ことをおすすめします。  
また、祈祷 / 礼拝リマインダー系アプリなど、通知遅延を避けたいアプリがある場合は、ここで許可対象に入れてください。

なお、この項目は**アプリの事前読み込み挙動そのものを強く抑えるものではありません**。  
電池劣化や端末寿命を重視する場合は、コールドスタート負荷の高いアプリをシステムのバックグラウンド管理側で別途整理するのが無難です。

**再起動シナリオで自動起動保護ポリシーの永続置き換えが反映されない場合は、Root で Battery アプリの起動管理ページを直接開いて設定することをおすすめします。**

---

## アンインストール / 復旧の推奨事項

- アンインストール前に、まずアプリ内の **「システムレベルの永続設定を消去（Root 必須）」** を実行することをおすすめします。  
- 緊急復旧が必要な場合は、救済用 Magisk モジュールで起動アニメーション中に LSPosed を無効化またはアンインストールし、その後システムに入って残りの処理を行ってください。  

---

## 寄付

このプロジェクトが役に立ったなら、コーヒー代をおごっていただけると嬉しいです。

<img src="app/src/main/res/drawable/donation_qr.png" width="280" alt="donation qr" />

---

**P.S.**  
このプロジェクトには多少の自己満足も入っています。  
ひとつは、システムアプリだけを特別扱いする挙動に付き合いたくないから。  
ふたつめは、今後似たようなバックグラウンド凍結管理アプリを作るなら、これを土台にしたいから。  
みっつめは、Oneplus 系システムには他にも似た仕組みが残っているからです。  
今後どこまで開発を続けられるかはまだ分かりませんが、ひとまず再利用できる部分を整えてから、その後を考えるつもりです。

<a id="korean"></a>

## 개요

### 지원 범위

- 지원 환경: **OxygenOS 15, ColorOS 15(Maybe)**
- LSPosed 범위는 반드시 둘 다 활성화해야 합니다  
  - `com.oplus.athena` (백그라운드 동결 정책을 읽는 쪽)  
  - `com.oplus.battery` (자동 시작 관련 정책을 읽는 쪽)

### 사용 방법

1. APK를 설치하고 LSPosed에서 모듈을 활성화합니다.  
2. LSPosed에서 `com.oplus.athena` 와 `com.oplus.battery` 두 범위를 모두 추가합니다.  
3. 앱을 열고 **Root 권한**을 부여한 뒤 설정을 수정하고 **“정책 적용”** 을 누릅니다.  
4. 기기를 재부팅합니다.  

**반드시 부팅 애니메이션 단계에서 LSPosed를 비활성화하거나 제거할 수 있는 구제용 모듈과 함께 사용하세요.**

### 빠른 작업: 지금 억제 와 상시 유지

**지금 억제** 는 대상 앱을 더 엄격한 장면별 동결 제어 대상으로 추가하고, 우대 / 면제 목록에서 빼는 빠른 작업입니다.  
이것은 **즉시 프로세스를 죽이는 기능이 아닙니다**. 실제 반영은 시스템의 검사 주기와 장면 트리거에 따라 달라집니다.

#### 지금 억제 — 동결 타이밍(Freeze timing) 프리셋

**“동결 타이밍 파라미터를 0으로 동기화(전역·고위험)”** 를 켜면 L1 / L2 / L3 프리셋을 선택할 수 있습니다.

- **L1(보수 프리셋): highExtremeMode + appType=1000**
- **L2(균형 프리셋): highExtreme/extreme/highPerf/highLoad + appType=1000**
- **L3(공격 프리셋): L2 + appType=1000/4/7/4#7**

참고:

- `checkImportance` 는 변경하지 않습니다.  
- 변경 대상은 `freezeInterval` 내부의 **기존 필드만** 입니다. 새 필드는 추가하지 않습니다.  
- 이것은 **전역에 영향을 주는 고위험 설정**이며, 특정 패키지 하나에만 제한해서 적용되는 기능이 아닙니다.  

#### 상시 유지

**상시 유지** 는 대상 패키지를 우선 / 예외 목록 쪽으로 보내는 빠른 작업입니다.  
상황에 따라 `SysBlackApp` 장면에서 제거하고, 지원되는 경우 `ffPkg.black` 나 `whitePkg.category=100` 등에도 반영합니다.  
변경 사항은 **“정책 적용”** 을 누르기 전까지 임시 저장됩니다.

## 백그라운드 동결 해설

1) 전역 스위치와 기본 타이밍  

`enableConfig / lcdOffConfig / lcdOnConfig` 는 백그라운드 동결 제어를 켤지, 그리고 화면 켜짐 / 꺼짐 상태에서 실행→전이→동결로 넘어가는 타이밍을 어떻게 잡을지 결정합니다.

2) Fast Freeze(고속 동결)  

`ffConfig / ffTimeout / ffPkg` 는 화면이 꺼진 뒤 백그라운드 앱을 더 빨리 동결해 전력 소모와 wakeup 을 줄이기 위한 설정입니다.  
`ffPkg type="black"` 을 사용하면 **고속 동결 대상에서 제외할 앱**을 명시할 수 있습니다.

3) Critical Allow / Whitelist(핵심 허용 / 화이트리스트)  

`DoNotFreeze / whitePkg / imPkg / cpuCtlWhiteList / appCard.ignoreApp` 등 핵심 우선 / 예외 목록을 한곳에서 다룹니다.  
알림, 상주, 시작 이후 유지 동작에 영향을 줄 수 있으므로 신중하게 수정해야 합니다.

4) Sys Black Governance(시스템 블랙 제어)  

`SysBlackPolicy / SysBlackRestrictMask / SysBlackApp` 를 사용해 **화면 켜짐 / 화면 꺼짐 / 야간** 같은 장면별로 더 엄격한 동결 제어를 적용합니다.  
대상 앱을 장면별 블랙리스트에 넣고, 필요하면 `excludeAction` 이나 `forceProxy` 같은 예외도 함께 조정할 수 있습니다.

5) Proxy / Batch(프록시 / 배치)  

`proxyConfig` 는 프록시 계열 전체 스위치입니다.  
`defaultProxyBc / proxyBcExcludeAction / proxySensor / proxyGps / proxyBtScan / proxyBinder` 등을 통해 broadcast, sensor, binder callback, 위치, Bluetooth scan 같은 wakeup 원인을 묶어서 제어합니다.

6) Unfreeze / Bump(해동 / Bump)  

`serviceBumpUnFzReasonList / trafficConfig / prevent / allow` 등을 사용해 오동결을 줄이기 위한 해동 사유와 폴백 규칙을 조정합니다.  
백그라운드 제어를 너무 강하게 걸었을 때의 안전장치 성격이 강한 영역입니다.

7) High Load / Strict / Quota(고부하 / 엄격 제어 / 쿼터)  

`high-loading / strictMode / android-freeze / appQuotaConfig / cpuCtlRus` 등 고부하 상황이나 엄격 제어 시나리오용 항목입니다.  
**고위험에 가까운 설정**이므로, 의미를 모르면 함부로 바꾸지 않는 편이 낫습니다.

---

## 자동 시작 관리

시스템은 저수준 화이트리스트에 들어 있는 앱에 대해 자동 시작을 자동으로 허용하는 경우가 있습니다.

특수한 방법(예: 일부 빌드의 자동 시작 관리 화면, 또는 Root 로 시작 관리 페이지를 직접 여는 방법)으로 권한을 꺼도, 시간이 지나면 시스템이 자동으로 다시 복구할 수 있습니다.

이 앱에서는 **OnePlus 계열 앱의 체크를 해제하지 않는 것**을 권장합니다.  
또한 기도 / 예배 알림 앱처럼 푸시 지연을 피하고 싶은 앱이 있다면, 여기에서 허용 대상으로 추가해 두세요.

다만 이 항목은 **앱의 사전 로드 동작 자체를 강하게 억제하는 기능은 아닙니다**.  
배터리 열화나 기기 수명에 민감하다면, 콜드 스타트 비용이 큰 앱은 시스템의 백그라운드 관리 쪽에서 별도로 정리하는 편이 더 안전합니다.

**재부팅 시나리오에서 자동 시작 보호 정책의 영구 치환이 적용되지 않는다면, Root 로 Battery 앱의 시작 관리 페이지를 직접 열어 설정하는 것을 권장합니다.**

---

## 제거 / 복구 권장 사항

- 제거하기 전에 먼저 앱 내부의 **“시스템 레벨 영구 설정 지우기(Root 필요)”** 를 실행하는 것을 권장합니다.  
- 긴급 복구가 필요하다면, 구제용 Magisk 모듈로 부팅 애니메이션 단계에서 LSPosed를 비활성화하거나 제거한 뒤 시스템에 진입해 나머지 처리를 진행하세요.  

---

## 후원

이 프로젝트가 도움이 되었다면, 커피 한 잔 정도 후원해 주셔도 좋습니다.

<img src="app/src/main/res/drawable/donation_qr.png" width="280" alt="donation qr" />

---

**P.S.**  
이 프로젝트에는 어느 정도 개인적인 취향도 들어 있습니다.  
첫째, 시스템 앱만 특별대우하는 방식에 더는 맞춰주고 싶지 않았습니다.  
둘째, 앞으로 비슷한 백그라운드 동결 관리 앱을 만든다면 이 프로젝트를 기준으로 삼고 싶었습니다.  
셋째, Oneplus 계열 시스템에는 여전히 비슷한 메커니즘이 다른 곳에도 남아 있습니다.  
앞으로 이걸 계속 개발할 시간이 있을지는 아직 확실하지 않지만, 우선 재사용 가능한 부분을 먼저 정리해 두고 그다음 상황을 보려 합니다.
