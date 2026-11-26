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

## Pinned toolchain

`build-tools 35.0.0` and `platforms;android-35` are pinned in the workflow `env`.
If the runner lacks them they are installed explicitly; if they still cannot be
resolved the job fails instead of silently compiling against another SDK.

## Signing

One shared release keystore, kept in **Actions secrets**, never in the repo:

| Secret | Contents |
|---|---|
| `KEYSTORE_B64` | base64 of the PKCS12 keystore |
| `KS_ALIAS` | key alias (`harness`) |
| `KS_PASS` | keystore + key password |

The workflow decodes it to `$RUNNER_TEMP` and passes `KEYSTORE` / `KS_ALIAS` /
`KS_PASS` to `build-apk.sh`, which hands the password to `apksigner` through
`env:` so it never appears on a command line.

Local phone builds use the same file:

```sh
KEYSTORE=~/.android/harness-release.keystore \
KS_ALIAS=harness \
KS_PASS="$(cat ~/.android/harness-release.pass)" \
sh build-apk.sh
```

Because both sides share a key, an artifact built here installs over one built
locally. Switching a previously debug-signed install to this key requires an
uninstall first.

## Running it

Push to `main`, or trigger it manually from the Actions tab. The signed APK
lands in the `habits-apk` artifact.

```sh
export GH_REPO=owner/harness-ci-demo   # see note below
gh run list
gh run download <run-id> -n habits-apk
```

### Note on `gh` from Android

`gh` is a Go binary and cannot `execve` anything under the app's files
directory, while bash can (the harness shells out through
`/system/bin/linker64`). Any `gh` subcommand that internally runs `git` —
`gh repo view`, `gh pr create`, `gh repo create --source` — fails with
`fork/exec ...: permission denied`.

Fix: export `GH_REPO=owner/repo` so `gh` never has to discover the repo via
git. `git push` is unaffected because bash runs `git` through the linker
wrapper.
