#include "android_settings.h"
#include "frame_limit.h"
#include "msaa.h"
#include "upscaling.h"
#include "widescreen.h"

#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <string>

void LoadAndroidSettings(const std::filesystem::path& directory) {
    uint32_t fps = 60;
    float scale = 0.f;
    uint32_t samples = 1;
    bool widescreen = true;
    std::ifstream file(directory / "settings.txt");

    for (std::string line; std::getline(file, line);) {
        const size_t equals = line.find('=');

        if (equals == std::string::npos)
            continue;

        const std::string key = line.substr(0, equals), value = line.substr(equals + 1);

        if (key == "fps_limit") {
            if (value == "off")
                fps = 0;
            else if (const unsigned long hertz = std::strtoul(value.c_str(), nullptr, 10); hertz >= 15 && hertz <= 240)
                fps = uint32_t(hertz);
        } else if (key == "render_scale") {
            if (value == "auto")
                scale = 0.f;
            else if (const float factor = std::strtof(value.c_str(), nullptr); factor > 0.f && factor <= 4.f)
                scale = factor;
        } else if (key == "msaa") {
            samples = value == "2x" ? 2 : value == "4x" ? 4 : 1;
        } else if (key == "widescreen" && (value == "on" || value == "off")) {
            widescreen = value == "on";
        }
    }

    SetFpsLimitSetting(fps);
    SetRenderScaleSetting(scale);
    SetMsaaSetting(samples);
    SetWidescreenSetting(widescreen);
    std::fprintf(stderr, "MH3 Android settings: fps limit %u, render scale %.2f (0: display), MSAA %ux, %s\n", fps, scale, samples, widescreen ? "16:9" : "4:3");
}
