# Android build (arm64-v8a)

**Status:** builds with NDK 30 (Clang 21) and runs on an AYN Thor
(Snapdragon 8 Gen 2, Adreno 740) with its built-in controls. Other devices,
older Android versions and emulators are untested.

The game runs as it does on arm64 Linux: translated code, the same runtime,
Aurora's OpenGL ES 3.0 renderer through SDL3. Android differences:

- `MH3` is a shared library (`libmain.so`, `runtime/CMakeLists.txt:mh3_add_game`).
  `SDLActivity` loads it with `libSDL3.so` and calls `SDL_main`
  (`<SDL3/SDL_main.h>` in `runtime/src/main.cpp`).
- `android/` is the Gradle app. `SetupActivity` picks the disc image with the
  system file picker and **copies** it to `Android/data/com.mh3.wii/files/rom/`
  (the runtime reads the image in place by path, which a `content://` URI cannot
  give it; plan for the image's size in free space). `GameActivity` starts the game with
  `--rom <copy>`. The disc metadata, converted-asset cache and saves live beside it
  (`disc/`, `saves/`), where USB file transfer can reach them.
- The Java side patches SDL's `SDLSurface.java` to call Aurora's
  `auroraNativeSetSurfaceReady`; without it Aurora never presents a frame (black
  screen). `GameActivity` hides the system bars (immersive mode).
- Input is SDL gamepads only (Bluetooth/USB controllers); there is no touch overlay.
- No OpenSSL in the NDK, so online play's TLS is unavailable.
- stdout and stderr go to logcat under the tag `MH3`
  (`adb logcat -s MH3 SDL AndroidRuntime DEBUG`).
- `libGLESv3` is linked (aurora_core.cmake), and the vendored SDL is patched to
  export `Android_(Un)LockActivityMutex`, which Aurora's `SurfaceLock` calls
  (`aurora-main/cmake/AuroraSDL3Patches.cmake`).
- The DNS lookup through glibc's `_res` is skipped; 8.8.8.8 stays the default.
- Backtraces on fatal errors are off (`execinfo.h` needs API 33).
- `memfd_create` is called by syscall; the guest window base (`0x10'0000'0000`)
  fits Android's 39-bit address space.

## Settings

The app's Settings screen (`SettingsActivity`) writes `settings.txt` in the app's
external files directory; `runtime/src/android_settings.cpp` applies it at start,
so a change takes effect the next time the game starts.

| Key | Values | Effect |
| --- | --- | --- |
| `fps_limit` | `30`, `60` (default), `90`, `120`, `off` | caps the game thread's frame rate (`FpsLimitSetting`, `native_frame_packet.cpp`); the GL swap interval is 0, so without a cap a 120 Hz phone draws as fast as it can: hot, and anything counted in frames runs fast |
| `render_scale` | `auto` (default), `1`, `1.5`, `2` | the EFB at that many times the Wii's 448 rows; `auto` sizes it for the display (`RenderScaleSetting`, `graphics_host.cpp`) |
| `msaa` | `off`, `2x`, `4x` | multisampling, where the driver offers it |
| `widescreen` | `on` (default), `off` | 16:9 or the console's 4:3 |

## Building

Needs what the README lists for translating, plus an **NDK r28 or newer**
(r29 ships Clang 21) from Android Studio's SDK Manager, and Gradle (Android
Studio creates the wrapper on first sync of `android/`; `android/gradlew` pins Gradle 8.13).
Run Gradle on **JDK 17 or 21**: Gradle 8.13 cannot read Java 25 (Android Studio's
bundled JBR) and the Android plugin 8.13 breaks on Gradle 9.6+. Set `JAVA_HOME`
and `ANDROID_HOME` before `scripts/build-android.sh`.

```sh
scripts/translate.sh            # once, as for the other targets
export DOTNET_BIN=/path/to/dotnet   # CMake re-runs native dispatch preparation; it
                                    # looks in tools/dotnet/ unless this is set
scripts/build-android.sh        # native libraries, then android/app/build/outputs/apk/debug/app-debug.apk
MH3_ANDROID_SKIP_APK=1 scripts/build-android.sh   # native libraries only
```

Install the APK, open the app, choose the European disc image (ISO or RVZ), and
press Play. The first start prepares the disc metadata as on the Vita.
