/*
 * SPDX-FileCopyrightText: 2026 Lunaris-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gallery3d.ui;

import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

import java.util.Arrays;

public class LunarisBlurTracker implements ViewTreeObserver.OnPreDrawListener {
    private final GLRoot mRoot;
    private final View mRootView;
    private final int mKey;
    private final int mTint;
    private final View[] mViews;
    private final float[] mRadii;
    private final int[] mLocation = new int[2];
    private final int[] mRootLocation = new int[2];
    private float[] mLast = new float[0];
    private boolean mEnabled;

    public LunarisBlurTracker(GLRoot root, int key, int tint, View[] views, float[] radii) {
        mRoot = root;
        mRootView = (View) root;
        mKey = key;
        mTint = tint;
        mViews = views;
        mRadii = radii;
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled == enabled) return;
        mEnabled = enabled;
        final ViewTreeObserver observer = mViews[0].getViewTreeObserver();
        if (enabled) {
            observer.addOnPreDrawListener(this);
            onPreDraw();
        } else {
            observer.removeOnPreDrawListener(this);
            mLast = new float[0];
            mRoot.setBackdropBlur(mKey, null, mTint);
        }
    }

    @Override
    public boolean onPreDraw() {
        float[] shapes = new float[mViews.length * LunarisBackdropBlur.SHAPE_STRIDE];
        mRootView.getLocationInWindow(mRootLocation);
        int count = 0;
        for (int i = 0; i < mViews.length; i++) {
            View view = mViews[i];
            if (!view.isShown() || view.getWidth() == 0) continue;
            float alpha = alphaOf(view);
            if (alpha <= 0f) continue;
            view.getLocationInWindow(mLocation);
            int o = count++ * LunarisBackdropBlur.SHAPE_STRIDE;
            shapes[o] = mLocation[0] - mRootLocation[0];
            shapes[o + 1] = mLocation[1] - mRootLocation[1];
            shapes[o + 2] = shapes[o] + view.getWidth();
            shapes[o + 3] = shapes[o + 1] + view.getHeight();
            shapes[o + 4] = mRadii[i];
            shapes[o + 5] = alpha;
        }
        shapes = Arrays.copyOf(shapes, count * LunarisBackdropBlur.SHAPE_STRIDE);
        if (!Arrays.equals(shapes, mLast)) {
            mLast = shapes;
            mRoot.setBackdropBlur(mKey, shapes, mTint);
        }
        return true;
    }

    private static float alphaOf(View view) {
        float alpha = view.getAlpha();
        for (ViewParent p = view.getParent(); p instanceof View; p = p.getParent()) {
            alpha *= ((View) p).getAlpha();
        }
        return alpha;
    }
}
