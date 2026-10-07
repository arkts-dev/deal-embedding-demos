#!/usr/bin/env bash
# Build the library when needed, then only the requested targets.
# Runtime dex is built once per profile and reused, because dexing Compose dominates build time.
set -euo pipefail
cd "$(dirname "$0")/.."
TARGETS=("$@")
[[ ${#TARGETS[@]} -eq 0 ]] && TARGETS=(calendar vehicle todo organizer rental pizza tests)
SDK="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
TOOLS="$SDK/build-tools/36.0.0"
ANDROID="$SDK/platforms/android-37.0/android.jar"
KOTLIN_HOME="${KOTLIN_HOME:-/snap/kotlin/current}"
DEAL="$PWD/dependencies/deal"
mkdir -p build/toolchain
exec 9> /tmp/igelhaus-deal-embed-build.lock
flock 9
fetch_tool() {
    local artifact="$1" digest="$2" file="build/toolchain/$1.jar"
    if [[ ! -f "$file" ]]; then
        curl -fL --max-time 120 "https://dl.google.com/dl/android/maven2/com/android/tools/$artifact/2.1.5/$artifact-2.1.5.jar" -o "$file.tmp"
        mv "$file.tmp" "$file"
    fi
    printf '%s  %s\n' "$digest" "$file" | sha256sum -c -
}
fetch_tool desugar_jdk_libs_nio d8044befae095781b9a80bf1faa92edc30382d75d437476784c1bf991598a976
fetch_tool desugar_jdk_libs_configuration_nio 7db51661cd07d1fd5cf12769a75ba201910624bb3801c09363dd3cb28e31c51b
if [[ ! -f build/toolchain/r8.jar ]]; then
    curl -fL --max-time 120 https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.4.28/r8-9.4.28.jar -o build/toolchain/r8.jar.tmp
    mv build/toolchain/r8.jar.tmp build/toolchain/r8.jar
fi
printf '%s  %s\n' 51af0a9d41cc541d619064224aec628923bd779cbcc5a62cb914ea2d669c4063 build/toolchain/r8.jar | sha256sum -c -
unzip -p build/toolchain/desugar_jdk_libs_configuration_nio.jar META-INF/desugar/d8/desugar.json > build/toolchain/desugar.json
STAMP=build/toolchain/.verified
if [[ ! -f "$STAMP" || tools/runtime-dependencies.txt -nt "$STAMP" ]]; then
    while read -r digest file url; do
        if [[ ! -f "build/toolchain/$file" ]]; then
            curl -fL --max-time 120 "$url" -o "build/toolchain/$file.tmp"
            mv "build/toolchain/$file.tmp" "build/toolchain/$file"
        fi
        printf '%s  %s\n' "$digest" "build/toolchain/$file" | sha256sum -c -
    done < tools/runtime-dependencies.txt
    touch "$STAMP"
fi
for library in jsengine core lifecycle-runtime versionedparcelable arch-runtime; do
    [[ -f "build/toolchain/$library.jar" ]] || unzip -p "build/toolchain/$library.aar" classes.jar > "build/toolchain/$library.jar"
done
if [[ ! -d build/compose/dependencies ]]; then
    if [[ ! -x build/toolchain/gradle-9.2.1/bin/gradle ]]; then
        curl -fL --max-time 120 https://downloads.gradle.org/distributions/gradle-9.2.1-bin.zip -o build/toolchain/gradle9.zip
        printf '%s  %s\n' 72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f build/toolchain/gradle9.zip | sha256sum -c -
        unzip -q -o build/toolchain/gradle9.zip -d build/toolchain
    fi
    build/toolchain/gradle-9.2.1/bin/gradle -p tools/compose copyRuntime --no-daemon
fi
COMPOSE_STAMP=build/compose/.prepared
if [[ ! -f "$COMPOSE_STAMP" || tools/compose/dependencies.sha256 -nt "$COMPOSE_STAMP" ]]; then
    (cd build/compose/dependencies && sha256sum -c ../../../tools/compose/dependencies.sha256)
    python3 tools/compose/prepare.py
    touch "$COMPOSE_STAMP"
fi
COMPOSE_CP=$(<build/compose/classpath.txt)
IFS=: read -ra COMPOSE_JARS <<< "$COMPOSE_CP"
RUNTIME_JARS=(build/toolchain/{jsengine,guava,failureaccess}.jar "${COMPOSE_JARS[@]}")
RUNTIME_CP=$(IFS=:; echo "${RUNTIME_JARS[*]}")
resource_link() {
    local app="$1" output="$2"
    local manifest="android/apps/$app/AndroidManifest.xml"
    [[ "$app" == tests ]] && manifest="android/tests/AndroidManifest.xml"
    mkdir -p "build/$app/rjava"
    mapfile -t resources < build/compose/resources.txt
    local args=()
    [[ -d "build/assets/$app" ]] && args+=(-A "build/assets/$app")
    for res in "${resources[@]}"; do args+=(-R "$res"); done
    "$TOOLS/aapt2" link --auto-add-overlay -I "$ANDROID" --manifest "$manifest" \
        --java "build/$app/rjava" --extra-packages "$(<build/compose/packages.txt)" "${args[@]}" -o "$output"
}
sign() {
    local app="$1"
    "$TOOLS/zipalign" -f 4 "build/$app/unsigned.apk" "build/$app/aligned.apk"
    if [[ ! -f build/debug.keystore ]]; then
        keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android \
            -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Android Debug'
    fi
    "$TOOLS/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out "build/$app.apk" "build/$app/aligned.apk"
    printf 'Built build/%s.apk\n' "$app"
}
mkdir -p build/embedding-consumer
if [[ ! -f build/capability-api.jar || ! -f build/embedding-consumer/classes.jar || -n "$(find dependencies/deal-embedding -newer build/embedding-consumer/classes.jar -type f 2>/dev/null)" ]]; then
    DEAL_ROOT="$DEAL" DEAL_UI_ROOT="$PWD/dependencies/deal-ui" ANDROID_JAR="$ANDROID" \
        AIDL="$TOOLS/aidl" RUNTIME_CP="$RUNTIME_CP" OUTPUT_DIR="$PWD/build/embedding-library" KOTLIN_HOME="$KOTLIN_HOME" \
        dependencies/deal-embedding/build.sh
    rm -rf build/embedding-consumer build/capability-api
    mkdir -p build/embedding-consumer build/capability-api
    unzip -q build/embedding-library/deal-embedding.aar -d build/embedding-consumer
    (cd build/capability-api && unzip -q ../embedding-consumer/classes.jar 'dev/deal/embedding/capabilities/*')
    jar --create --file build/capability-api.jar -C build/capability-api .
fi
EMBEDDING_JAR=build/embedding-consumer/classes.jar
# Desugared platform libraries and the JS engine are identical for every target: dex them once.
profile_dex() {
    local profile="$1"; shift
    local out="build/runtime-dex/$profile"
    if [[ -d "$out" && "$out" -nt build/toolchain/desugar_jdk_libs_nio.jar && -z "$(printf '%s\n' "$@" | while read -r j; do [[ "$j" -nt "$out/classes.dex" ]] && echo stale; done)" ]]; then
        return
    fi
    rm -rf "$out" build/desugar-dex; mkdir -p "$out" build/desugar-dex
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json --output "$out" "$@" "$KOTLIN_HOME/lib/kotlin-stdlib.jar"
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.L8 --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json --output build/desugar-dex \
        build/toolchain/desugar_jdk_libs_nio.jar build/toolchain/desugar_jdk_libs_configuration_nio.jar
    local next
    next=$(find "$out" -name 'classes*.dex' | wc -l)
    for dex in build/desugar-dex/classes*.dex; do
        next=$((next + 1)); cp "$dex" "$out/classes$next.dex"
    done
}
for app in "${TARGETS[@]}"; do
    case "$app" in
        calendar) profile="host";   sources=(android/apps/shared-ui/*.kt android/apps/development/*.kt android/apps/calendar/src/*.kt); app_apis=("$EMBEDDING_JAR"); profile_jars=("$EMBEDDING_JAR" "${RUNTIME_JARS[@]}") ;;
        organizer) profile="host";  sources=(android/apps/shared-ui/*.kt android/apps/development/*.kt android/apps/organizer/src/*.kt); app_apis=("$EMBEDDING_JAR"); profile_jars=("$EMBEDDING_JAR" "${RUNTIME_JARS[@]}") ;;
        tests) profile="host";      sources=(android/tests/*.kt android/apps/development/*.kt); app_apis=("$EMBEDDING_JAR"); profile_jars=("$EMBEDDING_JAR" "${RUNTIME_JARS[@]}") ;;
        *) profile="provider";      sources=(android/apps/shared-ui/*.kt "android/apps/$app/src/"*.kt); app_apis=(build/capability-api.jar); profile_jars=("${COMPOSE_JARS[@]}") ;;
    esac
    profile_dex "$profile" "${profile_jars[@]}"
    rm -rf "build/$app"; mkdir -p "build/$app/classes" "build/$app/dex"
    if [[ "$profile" == "host" ]]; then
        rm -rf "build/assets/$app"; mkdir -p "build/assets/$app"
        cp -r build/embedding-consumer/assets/* "build/assets/$app/"
        if [[ "$app" == calendar ]]; then
            mkdir -p "build/assets/$app/experience"
            cp android/apps/calendar/experiences/departure/*.deal "build/assets/$app/experience/" 2>/dev/null || true
            cp android/apps/calendar/experiences/departure/*.dealui "build/assets/$app/experience/" 2>/dev/null || true
        fi
    fi
    resource_link "$app" "build/$app/unsigned.apk"
    find "build/$app/rjava" -name '*.java' > build/r-sources.txt
    javac --release 21 -d "build/$app/classes" @build/r-sources.txt
    app_cp=$(IFS=:; echo "${app_apis[*]}")
    profile_cp="${profile_jars[*]}"
    "$KOTLIN_HOME/bin/kotlinc" -Xplugin="$KOTLIN_HOME/lib/compose-compiler-plugin.jar" -jvm-target 21 -no-reflect -classpath "$ANDROID:$app_cp:$RUNTIME_CP" \
        -d "build/$app/classes" "${sources[@]}"
    jar --create --file "build/$app/app.jar" -C "build/$app/classes" .
    # The app is dexed on its own so only app code is re-done on each iteration.
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json --classpath $profile_cp \
        --output "build/$app/dex" "build/$app/app.jar"
    test -n "$(find "build/$app/dex" -name 'classes*.dex')" || { echo "D8 produced no dex for $app" >&2; exit 1; }
    # Merge the shared runtime dex without overwriting the app dex names.
    rm -rf "build/$app/merged"; mkdir -p "build/$app/merged"
    cp "build/$app/dex"/classes*.dex "build/$app/merged/"
    next=$(find "build/$app/dex" -name 'classes*.dex' | wc -l)
    for dex in "build/runtime-dex/$profile"/classes*.dex; do
        next=$((next + 1)); cp "$dex" "build/$app/merged/classes$next.dex"
    done
    (cd "build/$app/merged" && zip -q ../unsigned.apk classes*.dex)
    sign "$app"
done
