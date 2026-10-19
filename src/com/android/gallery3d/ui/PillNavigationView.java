/*
 * SPDX-FileCopyrightText: 2026 Lunaris-AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;

import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.gallery3d.R;

public class PillNavigationView extends LinearLayout {

    public interface OnItemSelectedListener {
        void onItemSelected(int id);
    }

    private static final float STIFFNESS = 1400f;
    private static final float DAMPING = 0.9f;

    private static final FloatPropertyCompat<PillNavigationView> START =
            new FloatPropertyCompat<PillNavigationView>("indicatorStart") {
        @Override
        public float getValue(PillNavigationView view) {
            return view.mIndicatorStart;
        }

        @Override
        public void setValue(PillNavigationView view, float value) {
            view.mIndicatorStart = value;
            view.invalidate();
        }
    };

    private static final FloatPropertyCompat<PillNavigationView> END =
            new FloatPropertyCompat<PillNavigationView>("indicatorEnd") {
        @Override
        public float getValue(PillNavigationView view) {
            return view.mIndicatorEnd;
        }

        @Override
        public void setValue(PillNavigationView view, float value) {
            view.mIndicatorEnd = value;
            view.invalidate();
        }
    };

    private final Drawable mIndicator;
    private final SpringAnimation mStartSpring;
    private final SpringAnimation mEndSpring;

    private OnItemSelectedListener mListener;
    private int mSelectedId = NO_ID;
    private float mIndicatorStart;
    private float mIndicatorEnd;
    private boolean mIndicatorPlaced;
    private boolean mShown = true;

    public PillNavigationView(Context context) {
        this(context, null);
    }

    public PillNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClipToPadding(false);
        mIndicator = context.getDrawable(R.drawable.lunaris_pill_indicator);
        mStartSpring = new SpringAnimation(this, START).setSpring(new SpringForce()
                .setStiffness(STIFFNESS).setDampingRatio(DAMPING));
        mEndSpring = new SpringAnimation(this, END).setSpring(new SpringForce()
                .setStiffness(STIFFNESS).setDampingRatio(DAMPING));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            child.setOnClickListener(v -> select(v.getId(), true));
        }
        if (getChildCount() > 0) {
            setSelectedItemId(getChildAt(0).getId());
        }
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mListener = listener;
    }

    public int getSelectedItemId() {
        return mSelectedId;
    }

    public void setSelectedItemId(int id) {
        if (id == mSelectedId) return;
        mSelectedId = id;
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final boolean selected = child.getId() == id;
            child.setSelected(selected);
        }
        moveIndicator(isLaidOut() && mIndicatorPlaced && mShown);
    }

    private void select(int id, boolean fromUser) {
        if (id == mSelectedId) return;
        setSelectedItemId(id);
        if (fromUser) {
            performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK);
            if (mListener != null) mListener.onItemSelected(id);
        }
    }

    private void moveIndicator(boolean animate) {
        final View target = findViewById(mSelectedId);
        if (target == null || target.getWidth() == 0) return;
        final float start = target.getLeft();
        final float end = target.getRight();
        if (!animate) {
            mStartSpring.cancel();
            mEndSpring.cancel();
            mIndicatorStart = start;
            mIndicatorEnd = end;
            mIndicatorPlaced = true;
            invalidate();
            return;
        }
        mStartSpring.animateToFinalPosition(start);
        mEndSpring.animateToFinalPosition(end);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (!mStartSpring.isRunning() && !mEndSpring.isRunning()) {
            moveIndicator(false);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (mIndicatorPlaced) {
            mIndicator.setBounds(Math.round(mIndicatorStart), getPaddingTop(),
                    Math.round(mIndicatorEnd), getHeight() - getPaddingBottom());
            mIndicator.draw(canvas);
        }
        super.dispatchDraw(canvas);
    }

    public boolean isPillShown() {
        return mShown;
    }

    public void setPillShown(boolean shown) {
        if (shown == mShown && (shown == (getVisibility() == VISIBLE))) return;
        mShown = shown;
        animate().cancel();
        if (!isLaidOut()) {
            setVisibility(shown ? VISIBLE : GONE);
            setAlpha(1f);
            setTranslationY(0f);
            return;
        }
        final float offset = getHeight() + ((MarginLayoutParams) getLayoutParams()).bottomMargin;
        if (shown) {
            if (getVisibility() != VISIBLE) {
                setVisibility(VISIBLE);
                setAlpha(0f);
                setTranslationY(offset);
            }
            animate().alpha(1f).translationY(0f).setDuration(280)
                    .setInterpolator(new PathInterpolator(0.05f, 0.7f, 0.1f, 1f))
                    .withEndAction(null);
        } else {
            animate().alpha(0f).translationY(offset).setDuration(150)
                    .setInterpolator(new PathInterpolator(0.3f, 0f, 0.8f, 0.15f))
                    .withEndAction(() -> setVisibility(GONE));
        }
    }
}
