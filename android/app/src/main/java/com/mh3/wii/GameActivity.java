package com.mh3.wii;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import org.libsdl.app.SDLActivity;

/** Runs the game: SDL loads libSDL3.so and libmain.so and calls SDL_main. */
public class GameActivity extends SDLActivity {
    static final String EXTRA_ROM = "rom";

    @Override
    protected String[] getLibraries() {
        return new String[] {"SDL3", "main"};
    }

    /** The disc image SetupActivity copied; the native side checks it on every start. */
    @Override
    protected String[] getArguments() {
        String rom = getIntent().getStringExtra(EXTRA_ROM);
        return rom == null ? new String[0] : new String[] {"--rom", rom};
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        hideSystemBars();
    }

    // SDL's window is not fullscreen, so the bars come back whenever the window
    // regains focus (a dialog, the notification shade): hide them again.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hideSystemBars();
    }

    /** Immersive mode: a swipe from the edge shows the bars for a moment. */
    private void hideSystemBars() {
        Window window = getWindow();
        // Draw under a camera cutout too (landscape: the short edges).
        WindowManager.LayoutParams params = window.getAttributes();
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        window.setAttributes(params);

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.systemBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
