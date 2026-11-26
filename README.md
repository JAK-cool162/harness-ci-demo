# harness-ci-demo

A throwaway repo proving an end-to-end loop: code written and pushed **from an
Android phone**, compiled on **GitHub Actions**, APK pulled back down to the phone.

No Gradle. No local Android SDK. The workflow reuses the runner's preinstalled
SDK with `aapt2` + `javac` + `d8` + `apksigner`.

## What it builds

A small habit tracker (`com.harness.habits`): a rolling seven-day check-in strip
per habit, streaks, and `SharedPreferences` persistence.

## Pipeline

| Stage | Tool |
|---|---|
| Resources | `aapt2 compile` / `aapt2 link` |
| Compile | `javac` (source/target 8) |
| Dex | `d8` |
| Sign | `apksigner` |
| Verify | `apksigner verify`, `aapt2 dump badging`, `unzip -lv` |

The build script appends `classes.dex` to the APK last, which keeps
`resources.arsc` stored and 4-byte aligned — required for Android 11+.

## Running it

Push to `main`, or trigger it manually from the Actions tab. The signed APK
lands in the `habits-apk` artifact.

```sh
gh run list --workflow=build-apk.yml
gh run download <run-id> -n habits-apk
```
