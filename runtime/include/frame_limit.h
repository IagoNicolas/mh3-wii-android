#pragma once

// The game thread limit, on by default: the render thread consumes a packet
// once it submitted its frame to the GPU, waiting for a free GPU frame slot
// rather than skipping the draw, and the game thread records packet s only
// after packet s - 2 was consumed, so it runs at the rate frames are shown
// (PaceNativeFramePacket, docs/optimizations/threading/render-thread.md). Off,
// it waits only for a free packet slot and the render thread skips to the
// newest packet. A launcher sets it before the game starts: the game's render
// thread start applies it.
bool FrameLimitSetting();
void SetFrameLimitSetting(bool limit);

// A cap on the game thread's frame rate in Hz, 0 for none (the default): the
// limit above follows the rate the display shows frames at, and a GL swap
// interval of 0 (a 120 Hz phone, a host without VSync) lets that rate, the
// work and the heat run up to what the GPU manages. Loading screens are not
// capped.
uint32_t FpsLimitSetting();
void SetFpsLimitSetting(uint32_t hertz);
