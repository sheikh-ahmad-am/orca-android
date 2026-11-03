#!/bin/bash
# Manual APK build for Orca — no Gradle; a plain foreground pipeline:
#   aapt2 compile -> aapt2 link -> javac -> d8 -> zipalign -> apksigner
#
# PUBLIC COPY — signing is parameterized. The original project build used a
# private permanent keystore that is (obviously) not in this repo. To build a
# release APK you can install as an update line, create your OWN keystore once
# and reuse it forever — a new keystore makes Android treat updates as a
# different app.
#
# Required environment:
#   JAVA_HOME           JDK 17 home            (default: $HOME/jdk17)
#   ANDROID_SDK         Android SDK home       (default: $HOME/android-sdk)
#                       with build-tools 34.0.0 and platform android-34
#   KEYSTORE            path to your keystore  (default: $HOME/orca.keystore)
#   KEY_ALIAS           key alias              (default: orca)
#   KEYSTORE_PASSWORD   keystore + key password (required; never commit it)
#
# Usage:
#   KEYSTORE_PASSWORD='your-password' ./manual-build.sh
set -e
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-$HOME/jdk17}"
export PATH="$JAVA_HOME/bin:$PATH"
SDK="${ANDROID_SDK:-$HOME/android-sdk}"
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
SRC=src/main

: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD to your keystore password}"
KS="${KEYSTORE:-$HOME/orca.keystore}"
ALIAS="${KEY_ALIAS:-orca}"

rm -rf build
mkdir -p build/gen build/classes build/dex

echo "[1/6] aapt2 compile (resources)"
$BT/aapt2 compile --dir $SRC/res -o build/compiled_res.zip

echo "[2/6] aapt2 link (manifest + resources -> base.apk, R.java)"
$BT/aapt2 link -o build/base.apk \
    -I "$ANDROID_JAR" \
    --manifest $SRC/AndroidManifest.xml \
    --java build/gen \
    --min-sdk-version 26 --target-sdk-version 34 \
    --version-code 3 --version-name 1.2 \
    build/compiled_res.zip

echo "[3/6] javac (app sources + R.java)"
find $SRC/java build/gen -name '*.java' > build/sources.txt
javac -source 8 -target 8 -encoding UTF-8 \
    -cp "$ANDROID_JAR" -d build/classes @build/sources.txt

echo "[4/6] d8 (bytecode -> classes.dex)"
(cd build/classes && jar cf ../classes.jar .)
$BT/d8 --lib "$ANDROID_JAR" --output build/dex build/classes.jar

echo "[5/6] package + zipalign"
cp build/base.apk build/app.apk
(cd build/dex && zip -q ../app.apk classes.dex)
$BT/zipalign -f 4 build/app.apk build/aligned.apk

echo "[6/6] sign (your own permanent keystore — never regenerate, or updates break)"
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" \
        -alias "$ALIAS" -keyalg RSA -keysize 2048 \
        -validity 10000 -storepass "$KEYSTORE_PASSWORD" -keypass "$KEYSTORE_PASSWORD" \
        -dname "CN=Orca" 2>/dev/null
    echo "Created keystore: $KS (keep it backed up and private)"
fi
$BT/apksigner sign --ks "$KS" \
    --ks-pass "pass:$KEYSTORE_PASSWORD" --key-pass "pass:$KEYSTORE_PASSWORD" \
    --v1-signing-enabled true --v2-signing-enabled true \
    --out build/Orca.apk build/aligned.apk

echo "[check] zipalign + signature verification"
$BT/zipalign -c 4 build/Orca.apk
$BT/apksigner verify --verbose build/Orca.apk | head -6
$BT/apksigner verify --print-certs build/Orca.apk | grep "SHA-256"
cp build/Orca.apk ./Orca-v1.2.apk
ls -la ./Orca-v1.2.apk
echo "BUILD OK"
