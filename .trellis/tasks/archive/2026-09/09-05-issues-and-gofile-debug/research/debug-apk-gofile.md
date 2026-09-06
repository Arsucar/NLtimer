# Research: debug-apk-gofile

- **Query**: Build NLtimer debug APK with Gradle `--no-daemon`; upload via gofile.io `servers` + `uploadfile`; check `local.properties` `sdk.dir`; find existing release/install scripts and device `100.99.129.110:5555`
- **Scope**: mixed
- **Date**: 2026-09-05

## Findings

### Files Found

| File Path | Description |
|---|---|
| `gradle.properties` | `APP_VERSION_NAME=0.3.11`, `APP_VERSION_CODE=311`, `APP_ID=com.nltimer.app` |
| `app/build.gradle.kts` | Application module; debug `applicationIdSuffix`; APK rename |
| `gradlew.bat` | Windows Gradle wrapper |
| `local.properties` | `sdk.dir` present |
| `.github/workflows/publish-release.yaml` | Tag-triggered `assembleRelease` / fallback `assembleDebug` |
| `.github/workflows/opencode.yml` | Comment-triggered OpenCode; not APK |
| `scripts/check-docs-sync.py` | Docs sync checker only; no APK/install/upload |
| `.codex/hooks/install_debug_on_stop.ps1` | Stop hook: `gradlew.bat installDebug` |
| `.zed/tasks.json` | Assemble + `adb -s ebc3de22 install -r` |
| `.trae/rules/build.md` | Same assemble/install pattern as Zed |
| `.trae/skills/publish-release.md` | Release by pushing `v*.*.*` tag; no local APK |
| `docs/agent/01-project-overview.md` | Agent Gradle must use `--no-daemon` |
| `docs/agent/06-common-bug.md` | Missing SDK: set `sdk.dir` |
| `AGENTS.md` | Agent Gradle must add `--no-daemon` |
| `C:\Users\Administrator\.config\opencode\AGENTS.md` | Install-fail reconnect `100.99.129.110:5555` |
| `app/build/outputs/apk/debug/NLtimer-v0.3.11-debug.apk` | Existing debug APK (113100269 bytes, 2026-09-02) |

### 1. Build debug APK (Gradle `--no-daemon`)

**Task names**

- Root / app: `assembleDebug` (AGP application plugin)
- Fully qualified: `:app:assembleDebug`
- Install variant: `installDebug` / `:app:installDebug`
- CI also uses `compileDebugSources` (non-tag) and `assembleRelease` (tag)

`settings.gradle.kts` includes one application module: `app`. Root `assembleDebug` therefore builds the app APK.

**Exact Windows command (Agent / this task)**

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

Equivalent (same APK; README form):

```powershell
.\gradlew.bat assembleDebug --no-daemon
```

`--no-daemon` is required by:

- `AGENTS.md` line 41
- `docs/agent/01-project-overview.md` lines 26–38 (Gradle Daemon keeps the Agent/CI process from exiting)
- OpenCode user `AGENTS.md`: compile must use `--no-daemon`

`.trae/rules/build.md` says the opposite for Trae (do not pass `--no-daemon`). Agent/OpenCode rules take precedence for this task.

**Version (current `gradle.properties`)**

```properties
APP_VERSION_NAME=0.3.11
APP_VERSION_CODE=311
APP_ID=com.nltimer.app
```

Read in `app/build.gradle.kts`:

```kotlin
versionCode = APP_VERSION_CODE.toInt()
versionName = APP_VERSION_NAME
```

Debug applicationId:

```kotlin
getByName("debug") {
    isDebuggable = true
    applicationIdSuffix = ".debug$worktreeSuffix"
}
```

`$worktreeSuffix` is empty when `rootDir.name` matches `rootProject.name` (`NLtimer`). On the main checkout, debug id is `com.nltimer.app.debug`.

**Output APK path**

`app/build.gradle.kts` `androidComponents` sets:

```kotlin
output.outputFileName.set("NLtimer-v${APP_VERSION_NAME}-${variant.name}.apk")
```

So debug APK is:

```text
app/build/outputs/apk/debug/NLtimer-v0.3.11-debug.apk
```

Absolute:

```text
D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk
```

This file already exists (not rebuilt in this research session).

`README.md` still documents `app/build/outputs/apk/debug/app-debug.apk`. That name is stale relative to `outputFileName`. CI glob `app/build/outputs/apk/debug/*.apk` still matches the renamed file.

Release APK (not this task): `app/build/outputs/apk/release/NLtimer-v0.3.11-release.apk`.

**Wrapper / SDK**

- Gradle wrapper: `gradle-9.5.1-bin.zip`
- JDK expected: 17 (`README.md`, CI `setup-java` temurin 17)
- `org.gradle.daemon=true` in `gradle.properties`; `--no-daemon` overrides for a single invocation

### 2. Upload to gofile.io (`servers` + `uploadfile`)

Live check 2026-09-05:

| Endpoint | Result |
|---|---|
| `GET https://api.gofile.io/servers` | `200` JSON `status=ok` |
| `GET https://api.gofile.io/getServer` | `404` (legacy) |
| `GET https://api.gofile.io/` | `{"status":"ok","data":"api-ap-hkg-1"}` |
| `GET https://api.gofile.io/contents/uploadfile` | `401` |
| `GET https://{server}.gofile.io/uploadFile` | `405` (POST only) |
| `GET https://upload.gofile.io/uploadfile` | `405` (POST only) |
| `GET https://upload.gofile.io/contents/uploadfile` | `405` (POST only) |

`GET https://api.gofile.io/servers` sample (2026-09-05):

```json
{
  "status": "ok",
  "data": {
    "servers": [
      {"name": "store-eu-par-7", "zone": "eu"},
      {"name": "store-eu-par-5", "zone": "eu"},
      {"name": "store8", "zone": "na"}
    ],
    "serversAllZone": [ "...same plus extra names..." ]
  }
}
```

Use `data.servers[0].name` as `{server}`. Upload host is `https://{server}.gofile.io`.

**Current upload path (Gofile2 v2.1, API snapshot `2025-05-16`)**

Source: [partiallywritten/Gofile2 `gofile2.py`](https://raw.githubusercontent.com/partiallywritten/Gofile2/master/gofile2/gofile2.py)

- Guest upload: no token required
- `POST` multipart field name: `file`
- Optional field: `folderId`
- Optional header: `Authorization: Bearer <token>` (account upload)
- URL used by Gofile2:
  - Auto: `https://upload.gofile.io/uploadfile`
  - Regional: `https://{server}.gofile.io/uploadfile` where `{server}` is one of `upload`, `upload-eu-par`, `upload-na-phx`, `upload-ap-sgp`, `upload-ap-hkg`, `upload-ap-tyo`, `upload-sa-sao`

Gofile2 regional names (`upload-eu-par`, …) are **not** the same strings as `api.gofile.io/servers` (`store8`, `store-eu-par-7`, …).

**`servers` + `uploadfile` (task-requested two-step)**

Community scripts (gist `rajtiwariee/9d3de4518f06449032a31a298c8b342b`, `parnexcodes/df24e9ce16f63c3ed1fde8c976d43d81`) still do:

1. `GET https://api.gofile.io/servers` → `data.servers[0].name`
2. `POST https://{name}.gofile.io/uploadFile` (legacy camelCase) with `-F file=@...`

Gofile2 uses lowercase `uploadfile`. Both `GET` probes on `{server}.gofile.io/uploadFile` and `upload.gofile.io/uploadfile` returned HTTP **405**, i.e. the resource exists but GET is not allowed.

If `{server}.gofile.io/uploadfile` fails, the documented fallback is Gofile2 auto endpoint `https://upload.gofile.io/uploadfile` (skips `servers`).

Guest success JSON (wrapper comments): `status=ok`, `data` includes file info, `parentFolder`, `downloadPage`, and for guests `guestToken`.

**Windows: curl.exe (not PowerShell `curl` alias)**

```powershell
# 1) pick a store server
$servers = Invoke-RestMethod -Uri "https://api.gofile.io/servers"
$server  = $servers.data.servers[0].name
Write-Host "server=$server"

# 2) upload APK (guest)
$apk = "D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk"
curl.exe -s -F "file=@$apk" "https://$server.gofile.io/uploadfile"
```

One-liner:

```powershell
$s = (Invoke-RestMethod https://api.gofile.io/servers).data.servers[0].name; curl.exe -s -F "file=@D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk" "https://$s.gofile.io/uploadfile"
```

Auto-server (no `servers` call):

```powershell
curl.exe -s -F "file=@D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk" "https://upload.gofile.io/uploadfile"
```

Legacy camelCase path still used by gists:

```powershell
curl.exe -s -F "file=@D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk" "https://$s.gofile.io/uploadFile"
```

Optional token (account folder):

```powershell
curl.exe -s -H "Authorization: Bearer $env:GOFILE_TOKEN" -F "file=@$apk" "https://$s.gofile.io/uploadfile"
```

Parse `downloadPage` in PowerShell:

```powershell
$json = curl.exe -s -F "file=@$apk" "https://$s.gofile.io/uploadfile" | ConvertFrom-Json
$json.data.downloadPage
```

cmd.exe equivalent:

```bat
curl.exe -s https://api.gofile.io/servers
curl.exe -s -F "file=@D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk" https://store8.gofile.io/uploadfile
```

Replace `store8` with the `name` from step 1. Do not use PowerShell's `curl` alias (`Invoke-WebRequest`); `-F` is curl.exe.

Official HTML docs at `https://gofile.io/api` did not render without JavaScript. `https://docs.gofile.io` did not resolve in this session.

### 3. `local.properties` `sdk.dir`

File exists at repo root. Line 1:

```properties
sdk.dir=D\:\\por\\10_Library\\App_AndroidStudio_sdk
```

Decoded path: `D:\por\10_Library\App_AndroidStudio_sdk`.

Same value is documented in `docs/agent/06-common-bug.md` §4 (missing SDK).

The file also contains `keystore.path`, `keystore.password`, `key.alias`, `key.password` used by `app/build.gradle.kts` release signing. Debug `assembleDebug` does not require those keys.

### 4. Existing release / install scripts

**`scripts/`**

Only `scripts/check-docs-sync.py`. No APK build, adb install, or gofile upload script.

**`.github/workflows`**

`publish-release.yaml`:

- Non-tag: `./gradlew detekt --stacktrace` then `./gradlew compileDebugSources --stacktrace`
- Tag + keystore: `./gradlew assembleRelease --stacktrace`; artifact `app/build/outputs/apk/release/*.apk`
- Tag without keystore: `./gradlew assembleDebug --stacktrace`; artifact `app/build/outputs/apk/debug/*.apk`
- No `--no-daemon` (GitHub `gradle/actions/setup-gradle@v4` on ubuntu-latest)
- No adb / gofile

Release process (`.trae/skills/publish-release.md`): push `v*.*.*` tag; do not build APK locally; Actions publishes.

**Install to device**

No repo script hard-codes `100.99.129.110:5555`. That host is in OpenCode user rules and Trellis notes:

- `C:\Users\Administrator\.config\opencode\AGENTS.md`: on install failure reconnect `100.99.129.110:5555`
- `.trellis/tasks/05-21-review-fixes/prd.md` and `research/phase-b*.md`: stage C `installDebug`; fail → reconnect `100.99.129.110:5555`

Documented reconnect:

```powershell
adb connect 100.99.129.110:5555
.\gradlew.bat :app:installDebug --no-daemon
```

or after assemble:

```powershell
adb connect 100.99.129.110:5555
adb -s 100.99.129.110:5555 install -r "D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\NLtimer-v0.3.11-debug.apk"
```

**Other install helpers (USB serial `ebc3de22`, old APK name)**

`.zed/tasks.json`:

```powershell
.\gradlew.bat :app:assembleDebug
adb -s ebc3de22 install -r "D:\2026Code\Group_android\NLtimer\app\build\outputs\apk\debug\app-debug.apk"
adb -s ebc3de22 shell am start -n "com.nltimer.app/com.nltimer.app.MainActivity"
```

`.trae/rules/build.md` same install, starts `com.nltimer.feature.debug.ui.DebugActivity`. No `--no-daemon`. APK path still `app-debug.apk`.

`.codex/hooks/install_debug_on_stop.ps1`: `& $gradlew installDebug` (no `--no-daemon`, no serial, no reconnect).

`README.md`: `.\gradlew.bat installDebug` / `assembleDebug`; APK listed as `app-debug.apk`.

## Caveats / Not Found

- Official Gofile HTML API page did not expose static docs (JS SPA). Commands above are from live `servers` JSON, Gofile2 v2.1 source, and community gists.
- `api.gofile.io/getServer` is gone (`404`). Use `/servers`.
- `{store*}.gofile.io/uploadfile` vs `upload.gofile.io/uploadfile` vs legacy `uploadFile`: GET all 405; this session did not POST a file.
- `scripts/` has no gofile/apk helper.
- `100.99.129.110:5555` is an OpenCode/Trellis convention, not a committed script.
- `README.md` / Zed / Trae still reference `app-debug.apk`; Gradle rename produces `NLtimer-v0.3.11-debug.apk`.
- Existing debug APK on disk is dated 2026-09-02; a new `:app:assembleDebug --no-daemon` is required for a fresh build.
