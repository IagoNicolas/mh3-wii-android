package com.mh3.wii;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * Picks the European Monster Hunter Tri disc image with the system file picker
 * and copies it into the app's external files directory (the game reads the
 * image in place by path, which a content:// URI cannot give it), then starts
 * the game.
 */
public class SetupActivity extends Activity {
    private static final int PICK_ROM = 1;
    private static final String PREFS = "mh3";
    private static final String KEY_ROM = "rom";

    private TextView status;
    private ProgressBar progress;
    private Button choose;
    private Button play;
    private Button settings;
    private volatile boolean copying;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setVisibility(View.GONE);
        choose = new Button(this);
        choose.setText("Choose disc image (ISO / RVZ)");
        choose.setOnClickListener(v -> pick());
        play = new Button(this);
        play.setText("Play");
        play.setOnClickListener(v -> start(romPath()));

        settings = new Button(this);
        settings.setText("Settings (frame rate, resolution)");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        root.addView(status);
        root.addView(progress);
        root.addView(choose);
        root.addView(play);
        root.addView(settings);
        setContentView(root);
        refresh();
    }

    private String romPath() {
        String path = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ROM, null);
        return path != null && new File(path).isFile() ? path : null;
    }

    private void refresh() {
        String rom = romPath();
        play.setEnabled(rom != null && !copying);
        choose.setEnabled(!copying);
        if (!copying)
            status.setText(rom == null
                ? "Select your own European Monster Hunter Tri disc image (RMHP08).\nThe game contains none of its data."
                : "Disc image: " + new File(rom).getName());
    }

    private void pick() {
        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        open.addCategory(Intent.CATEGORY_OPENABLE);
        open.setType("*/*");
        startActivityForResult(open, PICK_ROM);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_ROM && result == RESULT_OK && data != null && data.getData() != null)
            copy(data.getData());
    }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null)
                return c.getString(0);
        } catch (RuntimeException ignored) {
        }
        return "disc.iso";
    }

    private long size(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[] {OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0))
                return c.getLong(0);
        } catch (RuntimeException ignored) {
        }
        return -1;
    }

    private void copy(Uri uri) {
        String name = displayName(uri).replaceAll("[^A-Za-z0-9._ ()-]", "_");
        String lower = name.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".iso") || lower.endsWith(".rvz") || lower.endsWith(".wia"))) {
            status.setText("Not a disc image: " + name + " (expected .iso or .rvz)");
            return;
        }

        File dir = new File(getExternalFilesDir(null), "rom");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            status.setText("Cannot create " + dir);
            return;
        }

        // One image at a time: drop the previous one first so a switch needs no extra space.
        File[] old = dir.listFiles();
        if (old != null)
            for (File f : old)
                f.delete();

        long total = size(uri);
        if (total > 0 && new StatFs(dir.getPath()).getAvailableBytes() < total + (64L << 20)) {
            status.setText(String.format(Locale.ROOT, "Not enough free space: the image needs %.1f GB.", total / 1e9));
            return;
        }

        File target = new File(dir, name);
        File partial = new File(dir, name + ".part");
        copying = true;
        progress.setVisibility(View.VISIBLE);
        progress.setProgress(0);
        refresh();
        status.setText("Copying " + name + "...");

        new Thread(() -> {
            String error = null;
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(partial)) {
                if (in == null)
                    throw new IOException("The picker returned no data");
                byte[] buffer = new byte[1 << 20];
                long done = 0;
                int shown = -1;
                for (int n; (n = in.read(buffer)) > 0; ) {
                    out.write(buffer, 0, n);
                    done += n;
                    int permille = total > 0 ? (int) (done * 1000 / total) : 0;
                    if (permille != shown) {
                        shown = permille;
                        runOnUiThread(() -> progress.setProgress(permille));
                    }
                }
                out.getFD().sync();
            } catch (IOException | RuntimeException e) {
                error = e.getMessage() != null ? e.getMessage() : e.toString();
            }

            if (error == null && !partial.renameTo(target))
                error = "Cannot rename " + partial;
            if (error != null)
                partial.delete();

            final String failure = error;
            runOnUiThread(() -> {
                copying = false;
                progress.setVisibility(View.GONE);
                if (failure != null) {
                    status.setText("Copy failed: " + failure);
                } else {
                    SharedPreferences.Editor e = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                    e.putString(KEY_ROM, target.getPath()).apply();
                }
                refresh();
                if (failure != null)
                    status.setText("Copy failed: " + failure);
            });
        }, "mh3-copy-rom").start();
    }

    private void start(String rom) {
        if (rom == null)
            return;
        Intent game = new Intent(this, GameActivity.class);
        game.putExtra(GameActivity.EXTRA_ROM, rom);
        startActivity(game);
    }
}
