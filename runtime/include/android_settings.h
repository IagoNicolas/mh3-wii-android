#pragma once

#include <filesystem>

// Android: the settings screen (android/.../SettingsActivity.java) writes
// settings.txt, "key=value" lines, into the app's external files directory;
// this applies them before the game starts. Keys: fps_limit (30, 60, 90, 120
// or off; default 60), render_scale (auto, or a factor of the Wii's 448 rows:
// 1, 1.5, 2), msaa (off, 2x, 4x), widescreen (on, off). Unknown keys and
// values that do not parse are left out.
void LoadAndroidSettings(const std::filesystem::path& directory);
