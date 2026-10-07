/*
 * SPDX-FileCopyrightText: 2026 Lunaris-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gallery3d.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.gallery3d.R;

import java.util.function.IntConsumer;

public final class LunarisBlurConfig {
    private static final String PREF_KEY_NAV_BLUR_INTENSITY = "lunaris_nav_blur_intensity";
    private static final int MAX_STRENGTH = 5;
    public static final int DEFAULT_INTENSITY = 100;
    public static final int MAX_INTENSITY = 100;

    private static final long DISMISS_DELAY_MS = 160;
    private static final int[] PRESETS = { 0, 35, 70, 100 };
    private static final int[] PRESET_LABELS = {
            R.string.lunaris_blur_off, R.string.lunaris_blur_low,
            R.string.lunaris_blur_medium, R.string.lunaris_blur_high };

    private LunarisBlurConfig() {
    }

    public static int getBlurIntensity(Context context) {
        final int intensity = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_KEY_NAV_BLUR_INTENSITY, DEFAULT_INTENSITY);
        return intensity < 0 || intensity > MAX_INTENSITY ? DEFAULT_INTENSITY : intensity;
    }

    public static void setBlurIntensity(Context context, int intensity) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(PREF_KEY_NAV_BLUR_INTENSITY, Math.max(0, Math.min(MAX_INTENSITY, intensity)))
                .apply();
    }

    public static int strengthFor(int intensity) {
        return 1 + Math.round(intensity / (float) MAX_INTENSITY * (MAX_STRENGTH - 1));
    }

    public static void showDialog(Activity activity, IntConsumer onPicked) {
        final LayoutInflater inflater = LayoutInflater.from(activity);
        final View content = inflater.inflate(R.layout.lunaris_blur_dialog, null);
        final ViewGroup presets = content.findViewById(R.id.blur_presets);
        final int current = closestPreset(getBlurIntensity(activity));
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.lunaris_nav_blur)
                .setView(content)
                .create();
        for (int i = 0; i < PRESETS.length; i++) {
            final int preset = PRESETS[i];
            final TextView chip = (TextView) inflater.inflate(
                    R.layout.lunaris_blur_preset, presets, false);
            chip.setText(PRESET_LABELS[i]);
            chip.setSelected(preset == current);
            chip.setOnClickListener(v -> {
                for (int j = 0; j < presets.getChildCount(); j++) {
                    presets.getChildAt(j).setSelected(presets.getChildAt(j) == v);
                }
                setBlurIntensity(activity, preset);
                onPicked.accept(preset);
                v.postDelayed(dialog::dismiss, DISMISS_DELAY_MS);
            });
            presets.addView(chip);
        }
        dialog.show();
    }

    private static int closestPreset(int intensity) {
        int best = PRESETS[0];
        for (int preset : PRESETS) {
            if (Math.abs(preset - intensity) < Math.abs(best - intensity)) best = preset;
        }
        return best;
    }
}
