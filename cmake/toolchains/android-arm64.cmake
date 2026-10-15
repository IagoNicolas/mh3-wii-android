# Cross-compile MH3 for arm64-v8a Android with the NDK's Clang (NDK r28 or
# newer; the runtime wants a Clang 19+ and r29 ships Clang 21).
# Usage: cmake -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/android-arm64.cmake ...
# ANDROID_NDK_HOME (or ANDROID_NDK_ROOT, or the newest $ANDROID_HOME/ndk/*)
# names the NDK; scripts/build-android.sh resolves it.
if(NOT DEFINED ANDROID_NDK)
  foreach(mh3_ndk_var ANDROID_NDK_HOME ANDROID_NDK_ROOT)
    if(DEFINED ENV{${mh3_ndk_var}})
      set(ANDROID_NDK "$ENV{${mh3_ndk_var}}")
      break()
    endif()
  endforeach()
endif()

if(NOT ANDROID_NDK OR NOT EXISTS "${ANDROID_NDK}/build/cmake/android.toolchain.cmake")
  message(FATAL_ERROR "Set ANDROID_NDK_HOME to an Android NDK (install one with Android Studio's SDK Manager)")
endif()

set(ANDROID_ABI arm64-v8a)
# API 29: Android 10, the oldest release with the GLES 3.0 + Vulkan-capable
# drivers this port is worth running on; memfd_create is reached by syscall.
set(ANDROID_PLATFORM android-29)
set(ANDROID_STL c++_static)
include("${ANDROID_NDK}/build/cmake/android.toolchain.cmake")
