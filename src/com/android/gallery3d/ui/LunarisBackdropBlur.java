/*
 * SPDX-FileCopyrightText: 2026 Lunaris-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gallery3d.ui;

import android.graphics.RectF;

import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;

public class LunarisBackdropBlur {
    public static final int REQUEST_NAVIGATION = 0;
    public static final int SHAPE_STRIDE = 6;

    private static final int LEVELS = 5;
    private static final int PADDING = 48;
    private static final float MIN_ALPHA = 0.004f;

    private final float[][] mRequests = new float[1][];
    private final int[] mTints = new int[1];
    private volatile int mStrength = LEVELS;
    private final RawTexture[] mLevels = new RawTexture[LEVELS];
    private final RectF mSource = new RectF();
    private final RectF mTarget = new RectF();
    private final RectF mBounds = new RectF();
    private RawTexture mScene;
    private int mWidth;
    private int mHeight;
    private int mCapacityWidth;
    private int mCapacityHeight;

    private float[] mShapes = new float[0];
    private int[] mShapeTints = new int[0];
    private int mShapeCount;

    public void setStrength(int levels) {
        mStrength = Math.max(1, Math.min(LEVELS, levels));
    }

    public synchronized void setShapes(int key, float[] shapes, int tint) {
        mRequests[key] = shapes;
        mTints[key] = tint;
    }

    public boolean begin(GLCanvas canvas, int width, int height) {
        if (!snapshot() || width <= 0 || height <= 0) return false;

        mBounds.setEmpty();
        for (int i = 0; i < mShapeCount; i++) {
            int o = i * SHAPE_STRIDE;
            mBounds.union(mShapes[o], mShapes[o + 1], mShapes[o + 2], mShapes[o + 3]);
        }
        mBounds.inset(-PADDING, -PADDING);
        if (!mBounds.intersect(0, 0, width, height)) return false;
        mBounds.set((float) Math.floor(mBounds.left), (float) Math.floor(mBounds.top),
                (float) Math.ceil(mBounds.right), (float) Math.ceil(mBounds.bottom));

        if (width != mWidth || height != mHeight || mScene == null) {
            recycle();
            mWidth = width;
            mHeight = height;
            mScene = new RawTexture(width, height, true);
        }
        int bw = (int) mBounds.width();
        int bh = (int) mBounds.height();
        if (bw > mCapacityWidth || bh > mCapacityHeight || mLevels[0] == null) {
            allocateLevels(Math.max(bw, mCapacityWidth), Math.max(bh, mCapacityHeight));
        }
        canvas.beginRenderTarget(mScene);
        return true;
    }

    public void end(GLCanvas canvas) {
        canvas.endRenderTarget();

        mSource.set(0, 0, mWidth, mHeight);
        canvas.drawTexture(mScene, mSource, mSource);

        final int levels = mStrength;
        float w = mBounds.width();
        float h = mBounds.height();
        mSource.set(mBounds);
        RawTexture prev = mScene;
        for (int i = 0; i < levels; i++) {
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
            draw(canvas, prev, mSource, mLevels[i], w, h);
            mSource.set(0, 0, w, h);
            prev = mLevels[i];
        }
        for (int i = levels - 2; i >= 0; i--) {
            w = Math.max(1, mBounds.width() / (2 << i));
            h = Math.max(1, mBounds.height() / (2 << i));
            draw(canvas, prev, mSource, mLevels[i], w, h);
            mSource.set(0, 0, w, h);
            prev = mLevels[i];
        }

        canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
        for (int i = 0; i < mShapeCount; i++) {
            composite(canvas, prev, w, h, i * SHAPE_STRIDE, mShapeTints[i]);
        }
        canvas.restore();
    }

    public void recycle() {
        if (mScene != null) mScene.recycle();
        mScene = null;
        mWidth = mHeight = 0;
        recycleLevels();
    }

    private synchronized boolean snapshot() {
        int count = 0;
        for (float[] request : mRequests) {
            if (request != null) count += request.length / SHAPE_STRIDE;
        }
        if (mShapes.length < count * SHAPE_STRIDE) {
            mShapes = new float[count * SHAPE_STRIDE];
            mShapeTints = new int[count];
        }
        mShapeCount = 0;
        for (int key = 0; key < mRequests.length; key++) {
            float[] request = mRequests[key];
            if (request == null) continue;
            for (int o = 0; o + SHAPE_STRIDE <= request.length; o += SHAPE_STRIDE) {
                if (request[o + 5] < MIN_ALPHA) continue;
                System.arraycopy(request, o, mShapes, mShapeCount * SHAPE_STRIDE, SHAPE_STRIDE);
                mShapeTints[mShapeCount++] = mTints[key];
            }
        }
        return mShapeCount > 0;
    }

    private void recycleLevels() {
        for (int i = 0; i < LEVELS; i++) {
            if (mLevels[i] != null) mLevels[i].recycle();
            mLevels[i] = null;
        }
        mCapacityWidth = mCapacityHeight = 0;
    }

    private void allocateLevels(int width, int height) {
        recycleLevels();
        mCapacityWidth = width;
        mCapacityHeight = height;
        for (int i = 0; i < LEVELS; i++) {
            width = Math.max(1, width / 2);
            height = Math.max(1, height / 2);
            mLevels[i] = new RawTexture(width + 1, height + 1, true);
        }
    }

    private void draw(GLCanvas canvas, RawTexture from, RectF source, RawTexture to,
            float w, float h) {
        canvas.beginRenderTarget(to);
        canvas.setAlpha(1f);
        mTarget.set(0, 0, w, h);
        canvas.drawTexture(from, source, mTarget);
        canvas.endRenderTarget();
    }

    private void composite(GLCanvas canvas, RawTexture blurred, float bw, float bh,
            int o, int tint) {
        float left = mShapes[o];
        float top = mShapes[o + 1];
        float right = mShapes[o + 2];
        float bottom = mShapes[o + 3];
        float radius = Math.min(mShapes[o + 4], Math.min(right - left, bottom - top) / 2);
        canvas.setAlpha(mShapes[o + 5]);

        float sx = bw / mBounds.width();
        float sy = bh / mBounds.height();
        int rows = (int) Math.ceil(radius);
        for (int r = 0; r < rows; r++) {
            float d = radius - (r + 0.5f);
            float inset = radius - (float) Math.sqrt(radius * radius - d * d);
            row(canvas, blurred, sx, sy, left + inset, top + r, right - inset,
                    Math.min(top + r + 1, top + radius), tint);
            row(canvas, blurred, sx, sy, left + inset, Math.max(bottom - r - 1, bottom - radius),
                    right - inset, bottom - r, tint);
        }
        row(canvas, blurred, sx, sy, left, top + radius, right, bottom - radius, tint);
    }

    private void row(GLCanvas canvas, RawTexture blurred, float sx, float sy,
            float left, float top, float right, float bottom, int tint) {
        if (right <= left || bottom <= top) return;
        mSource.set((left - mBounds.left) * sx, (top - mBounds.top) * sy,
                (right - mBounds.left) * sx, (bottom - mBounds.top) * sy);
        mTarget.set(left, top, right, bottom);
        canvas.drawTexture(blurred, mSource, mTarget);
        canvas.fillRect(left, top, right - left, bottom - top, tint);
    }
}
