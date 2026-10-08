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
# Fingerprint consumed inputs, not checkout metadata or generated guidance.
ui_sources=()
for source in UiModel UiParser UiDiagnostic UiChecker UiBorrowedValueChecker UiDealGenerator UiSourceGenerator DealUiDealSource; do
    ui_sources+=("dependencies/deal-ui/src/main/java/deal/ui/$source.java")
done
library_key=$(python3 tools/build-cache.py \
    tools/build.sh tools/build-cache.py \
    dependencies/deal-embedding/build.sh dependencies/deal-embedding/android \
    dependencies/deal-embedding/tools dependencies/deal-embedding/core/generation.deal dependencies/deal-embedding/core/choice-policy.deal \
    dependencies/deal-embedding/core/catalogue-source.deal dependencies/deal-embedding/core/source-literals.deal \
    dependencies/deal-embedding/core/candidate-check.deal dependencies/deal-embedding/core/generation-guidance.deal \
    dependencies/deal-embedding/core/generation-guidance.md dependencies/deal-embedding/core/host \
    dependencies/deal/deal dependencies/deal/std dependencies/deal/skills/write-deal/references \
    dependencies/deal-ui/ui "${ui_sources[@]}" "$ANDROID" \
    "$KOTLIN_HOME/lib/kotlin-compiler.jar" "$KOTLIN_HOME/lib/compose-compiler-plugin.jar" \
    "$KOTLIN_HOME/lib/kotlin-stdlib.jar" "$TOOLS/aidl" "${RUNTIME_JARS[@]}")
library_stamp=build/embedding-consumer/.inputs.sha256
mkdir -p build/embedding-consumer
if [[ ! -f build/capability-api.jar || ! -f build/embedding-consumer/classes.jar || ! -d build/embedding-consumer/assets || ! -f "$library_stamp" || "$(<"$library_stamp")" != "$library_key" ]]; then
    started=$SECONDS
    printf 'Embedding library: rebuilding (inputs changed or outputs missing)\n'
    DEAL_ROOT="$DEAL" DEAL_UI_ROOT="$PWD/dependencies/deal-ui" ANDROID_JAR="$ANDROID" \
        AIDL="$TOOLS/aidl" RUNTIME_CP="$RUNTIME_CP" OUTPUT_DIR="$PWD/build/embedding-library" KOTLIN_HOME="$KOTLIN_HOME" \
        dependencies/deal-embedding/build.sh
    rm -rf build/embedding-consumer build/capability-api
    mkdir -p build/embedding-consumer build/capability-api
    unzip -q build/embedding-library/deal-embedding.aar -d build/embedding-consumer
    (cd build/capability-api && unzip -q ../embedding-consumer/classes.jar 'dev/deal/embedding/capabilities/*')
    jar --create --file build/capability-api.jar -C build/capability-api .
    printf '%s\n' "$library_key" > "$library_stamp"
    printf 'Embedding library: %s s\n' "$((SECONDS - started))"
else
    printf 'Embedding library: cache hit\n'
fi
EMBEDDING_JAR=build/embedding-consumer/classes.jar
# Stable third-party runtime dex never includes the changing embedding/compiler JAR.
# Release dex avoids D8's per-invocation LambdaMethod debug annotation definition,
# which would otherwise be duplicated when independently compiled dex is packaged.
profile_dex() {
    local profile="$1"; shift
    local out="build/runtime-dex/$profile" key started=$SECONDS
    key=$(python3 tools/build-cache.py tools/build.sh tools/build-cache.py "$ANDROID" \
        build/toolchain/r8.jar build/toolchain/desugar.json \
        build/toolchain/desugar_jdk_libs_nio.jar build/toolchain/desugar_jdk_libs_configuration_nio.jar \
        "$KOTLIN_HOME/lib/kotlin-stdlib.jar" "$@")
    if [[ -f "$out/classes.dex" && -f "$out/.inputs.sha256" && "$(<"$out/.inputs.sha256")" == "$key" ]]; then
        printf 'Runtime dex (%s): cache hit\n' "$profile"
        return
    fi
    printf 'Runtime dex (%s): rebuilding\n' "$profile"
    rm -rf "$out" build/desugar-dex; mkdir -p "$out" build/desugar-dex
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --release --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json --output "$out" "$@" "$KOTLIN_HOME/lib/kotlin-stdlib.jar"
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.L8 --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json --output build/desugar-dex \
        build/toolchain/desugar_jdk_libs_nio.jar build/toolchain/desugar_jdk_libs_configuration_nio.jar
    local next
    next=$(find "$out" -name 'classes*.dex' | wc -l)
    for dex in build/desugar-dex/classes*.dex; do
        next=$((next + 1)); cp "$dex" "$out/classes$next.dex"
    done
    printf '%s\n' "$key" > "$out/.inputs.sha256"
    printf 'Runtime dex (%s): %s s\n' "$profile" "$((SECONDS - started))"
}
embedding_dex() {
    local out=build/embedding-dex key started=$SECONDS
    key=$(python3 tools/build-cache.py tools/build.sh tools/build-cache.py "$ANDROID" \
        build/toolchain/r8.jar build/toolchain/desugar.json "$EMBEDDING_JAR" \
        "$KOTLIN_HOME/lib/kotlin-stdlib.jar" "${RUNTIME_JARS[@]}")
    if [[ -f "$out/classes.dex" && -f "$out/.inputs.sha256" && "$(<"$out/.inputs.sha256")" == "$key" ]]; then
        printf 'Embedding dex: cache hit\n'
        return
    fi
    printf 'Embedding dex: rebuilding\n'
    rm -rf "$out"; mkdir -p "$out"
    local classpath_args=() jar
    for jar in "${RUNTIME_JARS[@]}" "$KOTLIN_HOME/lib/kotlin-stdlib.jar"; do classpath_args+=(--classpath "$jar"); done
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --release --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json "${classpath_args[@]}" \
        --output "$out" "$EMBEDDING_JAR"
    test -f "$out/classes.dex"
    printf '%s\n' "$key" > "$out/.inputs.sha256"
    printf 'Embedding dex: %s s\n' "$((SECONDS - started))"
}
for app in "${TARGETS[@]}"; do
    case "$app" in
        calendar) profile="host";   sources=(android/apps/shared-ui/*.kt android/apps/development/*.kt android/apps/calendar/src/*.kt); app_apis=("$EMBEDDING_JAR"); profile_jars=("$EMBEDDING_JAR" "${RUNTIME_JARS[@]}") ;;
        organizer) profile="host";  sources=(android/apps/shared-ui/*.kt android/apps/development/*.kt android/apps/organizer/src/*.kt); app_apis=("$EMBEDDING_JAR"); profile_jars=("$EMBEDDING_JAR" "${RUNTIME_JARS[@]}") ;;
        tests) profile="host";      sources=(android/tests/*.kt android/apps/development/*.kt); app_apis=("$EMBEDDING_JAR"); profile_jars=("$EMBEDDING_JAR" "${RUNTIME_JARS[@]}") ;;
        *) profile="provider";      sources=(android/apps/shared-ui/*.kt "android/apps/$app/src/"*.kt); app_apis=(build/capability-api.jar); profile_jars=("${COMPOSE_JARS[@]}") ;;
    esac
    if [[ "$profile" == host ]]; then
        profile_dex host-stable "${RUNTIME_JARS[@]}"
        embedding_dex
        dex_roots=(build/runtime-dex/host-stable build/embedding-dex)
    else
        profile_dex provider "${profile_jars[@]}"
        dex_roots=(build/runtime-dex/provider)
    fi
    app_started=$SECONDS
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
    app_classpath_args=()
    for jar in "${profile_jars[@]}" "$KOTLIN_HOME/lib/kotlin-stdlib.jar"; do app_classpath_args+=(--classpath "$jar"); done
    "$KOTLIN_HOME/bin/kotlinc" -Xplugin="$KOTLIN_HOME/lib/compose-compiler-plugin.jar" -jvm-target 21 -no-reflect -classpath "$ANDROID:$app_cp:$RUNTIME_CP" \
        -d "build/$app/classes" "${sources[@]}"
    jar --create --file "build/$app/app.jar" -C "build/$app/classes" .
    # The app is dexed on its own so only app code is re-done on each iteration.
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --release --min-api 36 --lib "$ANDROID" \
        --desugared-lib build/toolchain/desugar.json "${app_classpath_args[@]}" \
        --output "build/$app/dex" "build/$app/app.jar"
    test -n "$(find "build/$app/dex" -name 'classes*.dex')" || { echo "D8 produced no dex for $app" >&2; exit 1; }
    # Merge the shared runtime dex without overwriting the app dex names.
    rm -rf "build/$app/merged"; mkdir -p "build/$app/merged"
    cp "build/$app/dex"/classes*.dex "build/$app/merged/"
    next=$(find "build/$app/dex" -name 'classes*.dex' | wc -l)
    for dex_root in "${dex_roots[@]}"; do
        for dex in "$dex_root"/classes*.dex; do
            next=$((next + 1)); cp "$dex" "build/$app/merged/classes$next.dex"
        done
    done
    (cd "build/$app/merged" && zip -q ../unsigned.apk classes*.dex)
    sign "$app"
    printf 'App (%s), compile/dex/package: %s s\n' "$app" "$((SECONDS - app_started))"
done
