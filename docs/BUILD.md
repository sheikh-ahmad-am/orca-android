# Building Orca for Android

Orca builds **without Gradle**. The whole build is one transparent shell
script — `manual-build.sh` — running the standard Android command-line
tools in sequence. What you see is exactly what ships.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 | `javac`, `jar`, `keytool` — set `JAVA_HOME` |
| Android SDK build-tools | 34.0.0 | `aapt2`, `d8`, `zipalign`, `apksigner` |
| Android platform | android-34 | provides `android.jar` to compile against |
| bash + zip | — | the script is plain bash |

App targets: **minSdk 26** (Android 8.0), **targetSdk 34**, versionCode 3 /
versionName 1.2.

## Configure signing (once)

Release APKs must be signed, and Android only accepts an update if it is
signed with the **same key** as the installed app. So: create one keystore,
back it up, and use it forever. **Never commit a keystore or its password
to git** — this repo deliberately contains neither; the project's own
release keystore is private.

The build script reads its signing setup from the environment:

| Variable | Default | Meaning |
|---|---|---|
| `KEYSTORE` | `$HOME/orca.keystore` | path to your keystore |
| `KEY_ALIAS` | `orca` | key alias inside the keystore |
| `KEYSTORE_PASSWORD` | — (required) | keystore + key password |
| `JAVA_HOME` | `$HOME/jdk17` | JDK 17 home |
| `ANDROID_SDK` | `$HOME/android-sdk` | Android SDK home |

If the keystore file doesn't exist, the script creates it on first run with
the equivalent of:

```bash
keytool -genkeypair -keystore "$KEYSTORE" \
    -alias orca -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$KEYSTORE_PASSWORD" -keypass "$KEYSTORE_PASSWORD" \
    -dname "CN=Orca"
```

## Build

```bash
KEYSTORE_PASSWORD='your-password' ./manual-build.sh
```

Output: **`Orca-v1.2.apk`** in the repo root (plus intermediates under
`build/`, which is git-ignored).

## What the script does, step by step

1. **aapt2 compile** — compiles `src/main/res` into `build/compiled_res.zip`.
2. **aapt2 link** — links the manifest + resources against `android.jar`,
   producing `build/base.apk` and the generated `R.java`, stamped with
   minSdk 26 / targetSdk 34 / versionCode 3 / versionName 1.2.
3. **javac** — compiles the app sources (`MainActivity.java`,
   `Engine.java`, `Problems.java`) plus `R.java` against `android.jar`
   (`-source 8 -target 8`, UTF-8).
4. **d8** — converts the compiled classes to Dalvik bytecode
   (`classes.dex`).
5. **Package + zipalign** — the dex is added to the base APK and the result
   is zipaligned to 4 bytes.
6. **apksigner sign** — signs the aligned APK with your keystore, v1 + v2
   signing enabled.
7. **Verify** — `zipalign -c` and `apksigner verify` run on the finished
   APK, and the signer certificate fingerprint is printed so you can
   confirm an update build carries the same key as the last release.

## Installing your build

```bash
adb install -r Orca-v1.2.apk
```

…or copy the APK to a phone and open it (allow "install from this source"
when Android asks). An install-over-update only succeeds when the new APK
is signed with the same keystore as the installed one — that's by design.

## Troubleshooting

- **`KEYSTORE_PASSWORD` error** — the script refuses to sign without it;
  export it in your shell (don't hardcode it anywhere).
- **Install rejected / "app not installed"** — almost always a signature
  mismatch: the APK was signed with a different keystore than the installed
  app. Rebuild with the original keystore, or uninstall first (which wipes
  progress).
- **Verifying a downloaded release** — `apksigner verify --print-certs
  Orca-v1.2.apk` shows the signer fingerprint; all official Orca releases
  share one fingerprint.

---
**Built by Ahmad** — Ahmad شیخ
Md Ahmad (Ahmad Sheikh) · Data Analyst
GitHub: https://github.com/sheikh-ahmad-am · Portfolio: https://sheikh-ahmad-am.github.io/portfolio/ · LinkedIn: https://www.linkedin.com/in/sheikh-ahmad-am/
