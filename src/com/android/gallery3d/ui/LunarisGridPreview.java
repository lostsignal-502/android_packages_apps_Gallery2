/*
 * SPDX-FileCopyrightText: 2026 Lunaris-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gallery3d.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.android.gallery3d.R;

public class LunarisGridPreview extends View {
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ColorStateList mColors;
    private final float mGap;
    private final float mRadius;
    private int mColumns = 4;

    public LunarisGridPreview(Context context) {
        this(context, null);
    }

    public LunarisGridPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mColors = context.getColorStateList(R.color.lunaris_option_content);
        mGap = getResources().getDimension(R.dimen.lunaris_grid_preview_gap);
        mRadius = getResources().getDimension(R.dimen.lunaris_grid_preview_radius);
    }

    public void setColumns(int columns) {
        mColumns = columns;
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(mColors.getColorForState(getDrawableState(), mColors.getDefaultColor()));
        final float cell = (Math.min(getWidth(), getHeight()) - (mColumns - 1) * mGap) / mColumns;
        final float radius = Math.min(mRadius, cell / 3f);
        for (int row = 0; row < mColumns; row++) {
            for (int col = 0; col < mColumns; col++) {
                final float x = col * (cell + mGap);
                final float y = row * (cell + mGap);
                canvas.drawRoundRect(x, y, x + cell, y + cell, radius, radius, mPaint);
            }
        }
    }
}
