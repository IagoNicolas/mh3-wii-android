package com.mh3.wii;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The game's settings. They are kept as "key=value" lines in settings.txt in
 * the app's external files directory, which the native side reads at start
 * (runtime/src/android_settings.cpp): a change applies the next time the game
 * is started.
 */
public class SettingsActivity extends Activity {
    // Label, value written to settings.txt.
    private static final String[][] FPS = {
        {"30 FPS", "30"}, {"60 FPS (default)", "60"}, {"90 FPS", "90"}, {"120 FPS", "120"}, {"Unlimited (hot)", "off"}};
    private static final String[][] SCALE = {
        {"Native (448p, coolest)", "1"}, {"Display (default)", "auto"}, {"1.5x (672p)", "1.5"}, {"2x (896p)", "2"}};
    private static final String[][] MSAA = {{"Off", "off"}, {"2x", "2x"}, {"4x", "4x"}};

    private final Map<String, String> values = new HashMap<>();
    private File file;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        file = new File(getExternalFilesDir(null), "settings.txt");
        load();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        addChoice(root, "Frame rate limit", "fps_limit", FPS, "60");
        addChoice(root, "Resolution", "render_scale", SCALE, "auto");
        addChoice(root, "Anti-aliasing (MSAA)", "msaa", MSAA, "off");

        Switch wide = new Switch(this);
        wide.setText("Widescreen (16:9)");
        wide.setChecked(!"off".equals(values.get("widescreen")));
        wide.setOnCheckedChangeListener((button, on) -> set("widescreen", on ? "on" : "off"));
        root.addView(wide);

        TextView note = new TextView(this);
        note.setText("\nChanges apply the next time the game starts. A lower frame rate or resolution "
            + "keeps the device cooler.");
        root.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void addChoice(LinearLayout root, String label, String key, String[][] options, String fallback) {
        TextView name = new TextView(this);
        name.setText("\n" + label);
        root.addView(name);

        String[] labels = new String[options.length];
        int selected = 0;
        String current = values.containsKey(key) ? values.get(key) : fallback;
        for (int i = 0; i < options.length; ++i) {
            labels[i] = options[i][0];
            if (options[i][1].equals(current))
                selected = i;
        }

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                set(key, options[position][1]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        root.addView(spinner);
    }

    private void load() {
        values.clear();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            for (String line; (line = in.readLine()) != null; ) {
                int equals = line.indexOf('=');
                if (equals > 0)
                    values.put(line.substring(0, equals), line.substring(equals + 1));
            }
        } catch (IOException ignored) {
            // No file yet: the defaults.
        }
    }

    /** Through a temporary file: a write cut short keeps the previous settings. */
    private void set(String key, String value) {
        if (value.equals(values.get(key)))
            return;
        values.put(key, value);
        File temporary = new File(file.getPath() + ".tmp");
        try (FileWriter out = new FileWriter(temporary)) {
            for (Map.Entry<String, String> entry : values.entrySet())
                out.write(entry.getKey() + "=" + entry.getValue() + "\n");
        } catch (IOException e) {
            return;
        }
        temporary.renameTo(file);
    }
}
