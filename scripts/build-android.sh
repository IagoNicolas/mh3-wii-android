#!/usr/bin/env bash
# Build the Android app (arm64-v8a): $MH3_ANDROID_BUILD_DIR/ holds the native
# libraries (libmain.so, libSDL3.so), android/app/build/outputs/apk/ the APK.
# ANDROID_NDK_HOME names the NDK (default: the newest under $ANDROID_HOME/ndk,
# or ~/Android/Sdk/ndk). MH3_EXTRA_CMAKE_ARGS adds CMake options (for example
# -DMH3_DEVELOPMENT=ON); BUILD_JOBS sets the parallelism. MH3_ANDROID_SKIP_APK=1
# stops after the native libraries. Needs generated/ from scripts/translate.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
build_dir=${MH3_ANDROID_BUILD_DIR:-build/android-arm64}
sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}

if [[ -z ${ANDROID_NDK_HOME:-} ]]; then
  ANDROID_NDK_HOME=$(ls -d "$sdk"/ndk/*/ 2>/dev/null | sort -V | tail -1 || true)
  ANDROID_NDK_HOME=${ANDROID_NDK_HOME%/}
fi

if [[ -z $ANDROID_NDK_HOME || ! -d $ANDROID_NDK_HOME ]]; then
  echo "No Android NDK found: install one (SDK Manager > SDK Tools > NDK) or set ANDROID_NDK_HOME" >&2
  exit 1
fi

export ANDROID_NDK_HOME
[[ -d generated ]] || { echo "generated/ is missing: run scripts/translate.sh first" >&2; exit 1; }
mkdir -p logs
cmake -S runtime -B "$build_dir" -G Ninja \
  -DCMAKE_TOOLCHAIN_FILE="$PWD/cmake/toolchains/android-arm64.cmake" \
  -DMH3_AURORA_ROOT="${AURORA_ROOT:-$PWD/aurora-main}" \
  -DFETCHCONTENT_BASE_DIR="$PWD/build/aurora-android-arm64/_deps" \
  -DCMAKE_BUILD_TYPE=Release \
  -DAURORA_SDL3_PROVIDER=vendor -DAURORA_SDL3_LINKAGE=shared -DAURORA_ENABLE_CARD=OFF \
  -DMH3_BUILD_VITA_TEXTURE_PACKER=OFF \
  -DCMAKE_PROJECT_aurora_INCLUDE="$PWD/scripts/aurora-compat.cmake" ${MH3_EXTRA_CMAKE_ARGS:-} > logs/android-configure.log 2>&1
cmake --build "$build_dir" --target MH3 --parallel "${BUILD_JOBS:-$(nproc)}" > logs/android-build.log 2>&1

# Package: the native libraries go where Gradle looks for them.
jni=android/app/src/main/jniLibs/arm64-v8a
mkdir -p "$jni"
# Debug info is ~180 MB of the library; the unstripped one stays in $build_dir.
strip=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip
if [[ -x $strip ]]; then "$strip" --strip-debug -o "$jni/libmain.so" "$build_dir/libmain.so"; else cp "$build_dir/libmain.so" "$jni/"; fi
# Every shared library libmain.so needs that this build produced (libSDL3.so,
# libpng16.so, ...) goes into the APK; the rest are the platform's.
readelf=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf
for lib in $("$readelf" -d "$build_dir/libmain.so" | sed -n 's/.*Shared library: \[\(.*\)\]/\1/p'); do
  built=$(find "$build_dir" build/aurora-android-arm64/_deps -name "$lib" -not -path '*/CMakeFiles/*' | head -1)
  [[ -n $built ]] && cp "$built" "$jni/"
done
[[ -f $jni/libSDL3.so ]] || { echo "libSDL3.so was not built: check logs/android-build.log" >&2; exit 1; }
echo "Built native libraries in $jni"

[[ ${MH3_ANDROID_SKIP_APK:-0} == 1 ]] && exit 0
sdl_java=$(find "$PWD/build/aurora-android-arm64/_deps" -type d -path '*android-project/app/src/main/java' | head -1)
[[ -n $sdl_java ]] || { echo "SDL's Java sources (android-project) not found under build/aurora-android-arm64/_deps" >&2; exit 1; }
# Android Studio creates the wrapper on first sync; otherwise use gradle on PATH.
if [[ -x android/gradlew ]]; then gradle=./gradlew; else gradle=${GRADLE:-gradle}; fi
(cd android && "$gradle" -PsdlJava="$sdl_java" assembleDebug)
echo "Built android/app/build/outputs/apk/debug/app-debug.apk"
