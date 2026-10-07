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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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

    private static final float STIFFNESS_LEAD = 480f;
    private static final float DAMPING_LEAD = 0.72f;
    private static final float STIFFNESS_TRAIL = 340f;
    private static final float DAMPING_TRAIL = 0.82f;

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
    private final int mTouchSlop;
    private float mDownX;
    private float mDownY;
    private boolean mIsDragging;

    public PillNavigationView(Context context) {
        this(context, null);
    }

    public PillNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClipToPadding(false);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mIndicator = context.getDrawable(R.drawable.lunaris_pill_indicator);
        mStartSpring = new SpringAnimation(this, START).setSpring(new SpringForce()
                .setStiffness(STIFFNESS_TRAIL).setDampingRatio(DAMPING_TRAIL));
        mEndSpring = new SpringAnimation(this, END).setSpring(new SpringForce()
                .setStiffness(STIFFNESS_LEAD).setDampingRatio(DAMPING_LEAD));
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
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
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

        final boolean movingRight = start > mIndicatorStart;
        if (movingRight) {
            mEndSpring.getSpring().setStiffness(STIFFNESS_LEAD).setDampingRatio(DAMPING_LEAD);
            mStartSpring.getSpring().setStiffness(STIFFNESS_TRAIL).setDampingRatio(DAMPING_TRAIL);
        } else {
            mStartSpring.getSpring().setStiffness(STIFFNESS_LEAD).setDampingRatio(DAMPING_LEAD);
            mEndSpring.getSpring().setStiffness(STIFFNESS_TRAIL).setDampingRatio(DAMPING_TRAIL);
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
            setScaleX(1f);
            setScaleY(1f);
            return;
        }
        final float offset = getHeight() + ((MarginLayoutParams) getLayoutParams()).bottomMargin + 24;
        if (shown) {
            if (getVisibility() != VISIBLE) {
                setVisibility(VISIBLE);
                setAlpha(0f);
                setTranslationY(offset);
                setScaleX(0.90f);
                setScaleY(0.90f);
            }
            animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(340)
                    .setInterpolator(new PathInterpolator(0.12f, 1f, 0.24f, 1f))
                    .withEndAction(null);
        } else {
            animate().alpha(0f).translationY(offset).scaleX(0.92f).scaleY(0.92f)
                    .setDuration(190)
                    .setInterpolator(new PathInterpolator(0.4f, 0f, 0.9f, 0.2f))
                    .withEndAction(() -> setVisibility(GONE));
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                mIsDragging = false;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - mDownX);
                float dy = Math.abs(ev.getY() - mDownY);
                if (dx > mTouchSlop && dx > dy) {
                    mIsDragging = true;
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mShown || getVisibility() != VISIBLE) return super.onTouchEvent(event);
        final float x = event.getX();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        if (x >= child.getLeft() && x <= child.getRight()) {
                            if (child.getId() != mSelectedId) {
                                select(child.getId(), true);
                            }
                            break;
                        }
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (x >= child.getLeft() && x <= child.getRight()) {
                        select(child.getId(), true);
                        break;
                    }
                }
                mIsDragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }
}
