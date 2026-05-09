/*
 * Copyright (C) 2026 FUTO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.futo.inputmethod.keyboard.internal;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.TextUtils;

import org.futo.inputmethod.keyboard.PointerTracker;
import org.futo.inputmethod.latin.R;
import org.futo.inputmethod.latin.common.Constants;
import org.futo.inputmethod.latin.common.CoordinateUtils;

import javax.annotation.Nonnull;

/**
 * Floating text preview shown while swiping the spacebar to switch languages.
 */
public final class LanguageSwitchDrawingPreview extends AbstractDrawingPreview {
    private static final long PREVIEW_DURATION_MS = 720;
    private static final long PREVIEW_FADE_MS = 180;
    private static final float PREVIEW_SIZE_SCALE = 0.75f;
    private static final float PREVIEW_BACKGROUND_ALPHA_SCALE = 0.75f;

    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mTextBounds = new Rect();
    private final RectF mPreviewRectangle = new RectF();
    private final int[] mDownCoords = CoordinateUtils.newInstance();

    private final float mHorizontalPadding;
    private final float mVerticalPadding;
    private final float mRoundRadius;
    private final int mTextHeight;
    private final int mPreviewTextOffset;
    private final int mBaseTextAlpha;
    private final int mBaseBackgroundAlpha;

    private int mKeyboardWidth;
    private String mText;
    private int mPreviewTextX;
    private int mPreviewTextY;
    private long mShowStartTime;

    public LanguageSwitchDrawingPreview(final TypedArray mainKeyboardViewAttr) {
        mTextPaint.setTextAlign(Align.CENTER);
        mTextPaint.setTextSize(mainKeyboardViewAttr.getDimensionPixelSize(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextSize, 0)
                * PREVIEW_SIZE_SCALE);
        final int textColor = mainKeyboardViewAttr.getColor(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextColor, Color.WHITE);
        mBaseTextAlpha = Color.alpha(textColor);
        mTextPaint.setColor(textColor);

        final int backgroundColor = mainKeyboardViewAttr.getColor(
                R.styleable.MainKeyboardView_gestureFloatingPreviewColor, 0xDD000000);
        mBaseBackgroundAlpha = Color.alpha(backgroundColor);
        mBackgroundPaint.setColor(backgroundColor);

        mPreviewTextOffset = mainKeyboardViewAttr.getDimensionPixelOffset(
                R.styleable.MainKeyboardView_gestureFloatingPreviewTextOffset, 0);
        mHorizontalPadding = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureFloatingPreviewHorizontalPadding, 0.0f)
                * PREVIEW_SIZE_SCALE;
        mVerticalPadding = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureFloatingPreviewVerticalPadding, 0.0f)
                * PREVIEW_SIZE_SCALE;
        mRoundRadius = mainKeyboardViewAttr.getDimension(
                R.styleable.MainKeyboardView_gestureFloatingPreviewRoundRadius, 0.0f)
                * PREVIEW_SIZE_SCALE;

        mTextPaint.getTextBounds("M", 0, 1, mTextBounds);
        mTextHeight = mTextBounds.height();
    }

    @Override
    public void setKeyboardViewGeometry(@Nonnull final int[] originCoords, final int width,
            final int height) {
        super.setKeyboardViewGeometry(originCoords, width, height);
        mKeyboardWidth = width;
    }

    @Override
    public void onDeallocateMemory() {
        // Nothing to do here.
    }

    public void dismissLanguageSwitchPreview() {
        mText = null;
        invalidateDrawingView();
    }

    public void showLanguageSwitchPreview(@Nonnull final PointerTracker tracker,
            @Nonnull final String text) {
        if (!isPreviewEnabled()) {
            return;
        }

        mText = text;
        mShowStartTime = SystemClock.uptimeMillis();
        tracker.getDownCoordinates(mDownCoords);
        updatePreviewPosition();
    }

    @Override
    public void drawPreview(@Nonnull final Canvas canvas) {
        if (!isPreviewEnabled() || TextUtils.isEmpty(mText)) {
            return;
        }

        final long elapsed = SystemClock.uptimeMillis() - mShowStartTime;
        if (elapsed >= PREVIEW_DURATION_MS) {
            dismissLanguageSwitchPreview();
            return;
        }

        final long fadeStart = PREVIEW_DURATION_MS - PREVIEW_FADE_MS;
        final float fadeRatio = elapsed <= fadeStart
                ? 1.0f
                : 1.0f - ((float)(elapsed - fadeStart) / (float)PREVIEW_FADE_MS);
        final int fadeAlpha = Math.max(0, Math.min(Constants.Color.ALPHA_OPAQUE,
                Math.round(Constants.Color.ALPHA_OPAQUE * fadeRatio)));

        mBackgroundPaint.setAlpha(Math.round(mBaseBackgroundAlpha
                * PREVIEW_BACKGROUND_ALPHA_SCALE * (fadeAlpha / 255.0f)));
        mTextPaint.setAlpha(Math.round(mBaseTextAlpha * (fadeAlpha / 255.0f)));
        canvas.drawRoundRect(mPreviewRectangle, mRoundRadius, mRoundRadius, mBackgroundPaint);
        canvas.drawText(mText, mPreviewTextX, mPreviewTextY, mTextPaint);

        invalidateDrawingView();
    }

    @Override
    public void setPreviewPosition(@Nonnull final PointerTracker tracker) {
        if (!isPreviewEnabled() || TextUtils.isEmpty(mText)) {
            return;
        }

        tracker.getDownCoordinates(mDownCoords);
        updatePreviewPosition();
    }

    private void updatePreviewPosition() {
        if (TextUtils.isEmpty(mText)) {
            invalidateDrawingView();
            return;
        }

        final float textWidth = mTextPaint.measureText(mText);
        final float rectWidth = textWidth + mHorizontalPadding * 2.0f;
        final float rectHeight = mTextHeight + mVerticalPadding * 2.0f;
        final float anchorX = mKeyboardWidth > 0
                ? mKeyboardWidth / 2.0f
                : CoordinateUtils.x(mDownCoords);
        final float rectX = Math.max(0.0f, Math.min(anchorX - rectWidth / 2.0f,
                Math.max(0.0f, mKeyboardWidth - rectWidth)));
        final float rectY = CoordinateUtils.y(mDownCoords) - mPreviewTextOffset - rectHeight;

        mPreviewRectangle.set(rectX, rectY, rectX + rectWidth, rectY + rectHeight);
        mPreviewTextX = (int)(rectX + mHorizontalPadding + textWidth / 2.0f);
        mPreviewTextY = (int)(rectY + mVerticalPadding) + mTextHeight;
        invalidateDrawingView();
    }
}
