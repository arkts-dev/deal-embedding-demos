#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
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
while read -r digest file url; do
    if [[ ! -f "build/toolchain/$file" ]]; then
        curl -fL --max-time 120 "$url" -o "build/toolchain/$file.tmp"
        mv "build/toolchain/$file.tmp" "build/toolchain/$file"
    fi
    printf '%s  %s\n' "$digest" "build/toolchain/$file" | sha256sum -c -
done < tools/runtime-dependencies.txt
for library in jsengine core lifecycle-runtime versionedparcelable arch-runtime; do
    unzip -p "build/toolchain/$library.aar" classes.jar > "build/toolchain/$library.jar"
done
if [[ ! -d build/compose/dependencies ]]; then
    if [[ ! -x build/toolchain/gradle-9.2.1/bin/gradle ]]; then
        curl -fL --max-time 120 https://downloads.gradle.org/distributions/gradle-9.2.1-bin.zip -o build/toolchain/gradle9.zip
        printf '%s  %s\n' 72f44c9f8ebcb1af43838f45ee5c4aa9c5444898b3468ab3f4af7b6076c5bc3f build/toolchain/gradle9.zip | sha256sum -c -
        unzip -q -o build/toolchain/gradle9.zip -d build/toolchain
    fi
    build/toolchain/gradle-9.2.1/bin/gradle -p tools/compose copyRuntime --no-daemon
fi
(cd build/compose/dependencies && sha256sum -c ../../../tools/compose/dependencies.sha256)
python3 tools/compose/prepare.py
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
    [[ "$app" == calendar ]] && args+=(-A build/assets)
    for res in "${resources[@]}"; do args+=(-R "$res"); done
    "$TOOLS/aapt2" link --auto-add-overlay -I "$ANDROID" --manifest "$manifest" \
        --java "build/$app/rjava" --extra-packages "$(<build/compose/packages.txt)" "${args[@]}" -o "$output"
} 
rm -rf build/kotlin build/dex build/desugar-dex build/assets build/embedding-consumer
mkdir -p build/kotlin build/dex build/desugar-dex build/assets build/embedding-consumer
if [[ -z "${EMBEDDING_AAR:-}" ]]; then
    DEAL_ROOT="$DEAL" DEAL_UI_ROOT="$PWD/dependencies/deal-ui" ANDROID_JAR="$ANDROID" \
        AIDL="$TOOLS/aidl" RUNTIME_CP="$RUNTIME_CP" OUTPUT_DIR="$PWD/build/embedding-library" KOTLIN_HOME="$KOTLIN_HOME" \
        dependencies/deal-embedding/build.sh
    EMBEDDING_AAR="$PWD/build/embedding-library/deal-embedding.aar"
fi
unzip -q "$EMBEDDING_AAR" -d build/embedding-consumer
cp -r build/embedding-consumer/assets/* build/assets/
EMBEDDING_JAR=build/embedding-consumer/classes.jar
# Providers consume only the library's generic capability API, not compiler/renderer classes.
rm -rf build/capability-api; mkdir -p build/capability-api
(cd build/capability-api && unzip -q ../embedding-consumer/classes.jar 'dev/deal/embedding/capabilities/*')
jar --create --file build/capability-api.jar -C build/capability-api .
resource_link calendar build/unsigned.apk
find build/calendar/rjava -name '*.java' > build/r-sources.txt
javac --release 21 -d build/kotlin @build/r-sources.txt
"$KOTLIN_HOME/bin/kotlinc" -Xplugin="$KOTLIN_HOME/lib/compose-compiler-plugin.jar" -jvm-target 21 -no-reflect -classpath "$ANDROID:$EMBEDDING_JAR:$RUNTIME_CP" -d build/kotlin android/apps/shared-ui/*.kt android/apps/development/*.kt android/apps/calendar/src/*.kt
jar --create --file build/app.jar -C build/kotlin .
java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --min-api 36 --lib "$ANDROID" --desugared-lib build/toolchain/desugar.json --output build/dex \
    "$EMBEDDING_JAR" build/app.jar "${RUNTIME_JARS[@]}" "$KOTLIN_HOME/lib/kotlin-stdlib.jar"
java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.L8 --min-api 36 --lib "$ANDROID" \
    --desugared-lib build/toolchain/desugar.json --output build/desugar-dex \
    build/toolchain/desugar_jdk_libs_nio.jar build/toolchain/desugar_jdk_libs_configuration_nio.jar
next=$(find build/dex -name 'classes*.dex' | wc -l)
for dex in build/desugar-dex/classes*.dex; do
    next=$((next + 1))
    cp "$dex" "build/dex/classes$next.dex"
done
mkdir -p build/assets/experience
cp android/apps/calendar/experiences/departure/*.deal android/apps/calendar/experiences/departure/*.dealui build/assets/experience/
resource_link calendar build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes*.dex)
"$TOOLS/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [[ ! -f build/debug.keystore ]]; then
    keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Android Debug'
fi
"$TOOLS/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out build/calendar.apk build/aligned.apk
if [[ "${1:-all}" == calendar ]]; then printf 'Built build/calendar.apk\n'; exit 0; fi
for app in vehicle todo organizer rental pizza tests; do
    rm -rf "build/$app"
    mkdir -p "build/$app/classes" "build/$app/dex"
    if [[ "$app" == tests ]]; then
        sources=(android/tests/*.kt android/apps/development/*.kt)
        app_apis=("$EMBEDDING_JAR")
        app_runtime=("${RUNTIME_JARS[@]}")
    else
        sources=(android/apps/shared-ui/*.kt "android/apps/$app/src/"*.kt)
        app_apis=(build/capability-api.jar)
        app_runtime=("${COMPOSE_JARS[@]}")
    fi
    app_cp=$(IFS=:; echo "${app_apis[*]}")
    resource_link "$app" "build/$app/unsigned.apk"
    find "build/$app/rjava" -name '*.java' > build/r-sources.txt
    javac --release 21 -d "build/$app/classes" @build/r-sources.txt
    "$KOTLIN_HOME/bin/kotlinc" -Xplugin="$KOTLIN_HOME/lib/compose-compiler-plugin.jar" -jvm-target 21 -no-reflect -classpath "$ANDROID:$app_cp:$RUNTIME_CP" \
        -d "build/$app/classes" "${sources[@]}"
    jar --create --file "build/$app/app.jar" -C "build/$app/classes" .
    java -Xmx2g -XX:ActiveProcessorCount=4 -cp build/toolchain/r8.jar com.android.tools.r8.D8 --min-api 36 --lib "$ANDROID" --output "build/$app/dex" \
        "build/$app/app.jar" "${app_apis[@]}" "${app_runtime[@]}" "$KOTLIN_HOME/lib/kotlin-stdlib.jar"
    (cd "build/$app/dex" && zip -q ../unsigned.apk classes*.dex)
    "$TOOLS/zipalign" -f 4 "build/$app/unsigned.apk" "build/$app/aligned.apk"
    "$TOOLS/apksigner" sign --ks build/debug.keystore --ks-pass pass:android --out "build/$app.apk" "build/$app/aligned.apk"
done
printf 'Built build/{calendar,vehicle,todo,organizer,rental,pizza,tests}.apk\n'
