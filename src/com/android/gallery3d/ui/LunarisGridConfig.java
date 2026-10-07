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

public final class LunarisGridConfig {
    private static final String PREF_KEY_GRID_COLUMNS = "lunaris_grid_columns";
    private static final long DISMISS_DELAY_MS = 160;
    public static final int DEFAULT_COLUMNS = 4;
    public static final int MIN_COLUMNS = 3;
    public static final int MAX_COLUMNS = 5;

    private LunarisGridConfig() {
    }

    public static int getColumns(Context context) {
        final int cols = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(PREF_KEY_GRID_COLUMNS, DEFAULT_COLUMNS);
        return cols < MIN_COLUMNS || cols > MAX_COLUMNS ? DEFAULT_COLUMNS : cols;
    }

    public static void setColumns(Context context, int columns) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(PREF_KEY_GRID_COLUMNS,
                        Math.max(MIN_COLUMNS, Math.min(MAX_COLUMNS, columns)))
                .apply();
    }

    public static int getLandColumns(int portCols) {
        return portCols * 2 - 1;
    }

    public static void showDialog(Activity activity, IntConsumer onPicked) {
        final LayoutInflater inflater = LayoutInflater.from(activity);
        final View content = inflater.inflate(R.layout.lunaris_grid_size_dialog, null);
        final ViewGroup options = content.findViewById(R.id.grid_options);
        final int current = getColumns(activity);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.lunaris_grid_size)
                .setView(content)
                .create();
        for (int cols = MIN_COLUMNS; cols <= MAX_COLUMNS; cols++) {
            final int value = cols;
            final View option = inflater.inflate(R.layout.lunaris_grid_option, options, false);
            ((LunarisGridPreview) option.findViewById(R.id.grid_preview)).setColumns(cols);
            ((TextView) option.findViewById(R.id.grid_label)).setText(activity.getResources()
                    .getQuantityString(R.plurals.lunaris_grid_columns, cols, cols));
            option.setSelected(cols == current);
            option.setOnClickListener(v -> {
                for (int i = 0; i < options.getChildCount(); i++) {
                    options.getChildAt(i).setSelected(options.getChildAt(i) == v);
                }
                v.postDelayed(() -> {
                    dialog.dismiss();
                    if (value != current) onPicked.accept(value);
                }, DISMISS_DELAY_MS);
            });
            options.addView(option);
        }
        dialog.show();
    }
}
