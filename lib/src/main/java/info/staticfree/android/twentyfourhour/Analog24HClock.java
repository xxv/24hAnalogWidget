package info.staticfree.android.twentyfourhour;
/*
 * Copyright (C) 2011 Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Some portions Copyright (C) 2006 The Android Open Source Project
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


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.lib.R;
import info.staticfree.android.twentyfourhour.overlay.DialOverlay;
import info.staticfree.android.twentyfourhour.overlay.HandsOverlay;

/**
 * A widget that displays the time as a 12-at-the-top 24 hour analog clock. By
 * default, it will show the current time in the current timezone. The displayed
 * time can be set using {@link #setTime(long)} and and
 * {@link #setTimezone(TimeZone)}.
 *
 * @author <a href="mailto:steve@staticfree.info">Steve Pomeroy</a>
 */
public class Analog24HClock extends View {

    private boolean mShowNow = true;

    private Calendar mCalendar;
    private Drawable mFace;

    private int mDialWidth;
    private int mDialHeight;

    private int mBottom;
    private int mTop;
    private int mLeft;
    private int mRight;
    private boolean mSizeChanged;
    private boolean mUseLargeFace = false;

    private HandsOverlay mHandsOverlay;

    private final ArrayList<DialOverlay> mDialOverlay = new ArrayList<DialOverlay>();

    public Analog24HClock(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public Analog24HClock(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Analog24HClock(Context context) {
        super(context);

        init(context);
    }

    private void init(Context context) {
        setFace(mUseLargeFace ? R.drawable.clock_face_large : R.drawable.clock_face);

        mCalendar = Calendar.getInstance();

        mHandsOverlay = new HandsOverlay(context, mUseLargeFace);
    }

    public void setFace(int drawableRes){
        final Resources r = getResources();
        mFace = r.getDrawable(drawableRes);
        mDialHeight = mFace.getIntrinsicHeight();
        mDialWidth = mFace.getIntrinsicWidth();
    }

    /**
     * Sets the currently displayed time in {@link System#currentTimeMillis()}
     * time. This will clear {@link #setShowNow(boolean)}.
     *
     * @param time the time to display on the clock
     */
    public void setTime(long time) {
        setShowNow(false);
        mCalendar.setTimeInMillis(time);

        invalidate();
    }

    /**
     * Sets the currently displayed time. This will clear {@link #setShowNow(boolean)}.
     *
     * @param calendar The time to display on the clock
     */
    public void setTime(Calendar calendar) {
        setShowNow(false);
        mCalendar = calendar;

        invalidate();
    }

    /**
     * When set, the current time in the current timezone will be displayed.
     *
     * @param showNow
     */
    public void setShowNow(boolean showNow) {
        mShowNow = showNow;
    }

    /**
     * When set, the minute hand will move slightly based on the current number
     * of seconds. If false, the minute hand will snap to the minute ticks.
     * Note: there is no second hand, this only affects the minute hand.
     *
     * @param showSeconds
     */
    public void setShowSeconds(boolean showSeconds) {
        mHandsOverlay.setShowSeconds(showSeconds);
    }

    /**
     * Sets the timezone to use when displaying the time.
     *
     * @param timezone
     */
    public void setTimezone(TimeZone timezone) {
        mCalendar = Calendar.getInstance(timezone);
    }

    public void setHandsOverlay(HandsOverlay handsOverlay) {
        mHandsOverlay = handsOverlay;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        final boolean prevUseLargeFace = mUseLargeFace;

        mUseLargeFace = w > mDialWidth || h > mDialHeight;

        // reinitialize if we need to switch face images
        if (prevUseLargeFace != mUseLargeFace) {
            //init(getContext());
        }

        mSizeChanged = true;
    }

    // some parts from AnalogClock.java
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final boolean sizeChanged = mSizeChanged;
        mSizeChanged = false;

        if (mShowNow) {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
        }

        final int availW = mRight - mLeft;
        final int availH = mBottom - mTop;

        final int cX = availW / 2;
        final int cY = availH / 2;

        final int w = mDialWidth;
        final int h = mDialHeight;

        boolean scaled = false;

        if (availW < w || availH < h) {
            scaled = true;
            final float scale = Math.min((float) availW / (float) w,
                    (float) availH / (float) h);
            canvas.save();
            canvas.scale(scale, scale, cX, cY);
        }

        if (sizeChanged) {
            mFace.setBounds(cX - (w / 2), cY - (h / 2), cX + (w / 2), cY
                    + (h / 2));
        }

        mFace.draw(canvas);

        for (final DialOverlay overlay : mDialOverlay) {
            overlay.onDraw(canvas, cX, cY, w, h, mCalendar, sizeChanged);
        }

        mHandsOverlay.onDraw(canvas, cX, cY, w, h, mCalendar, sizeChanged);

        if (scaled) {
            canvas.restore();
        }
    }

    // from AnalogClock.java
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float) heightSize / (float) mDialHeight;
        }

        final float scale = Math.min(hScale, vScale);

        setMeasuredDimension(
                getDefaultSize((int) (mDialWidth * scale), widthMeasureSpec),
                getDefaultSize((int) (mDialHeight * scale), heightMeasureSpec));
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return mDialHeight;
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return mDialWidth;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // because we don't have access to the actual protected fields
        mRight = right;
        mLeft = left;
        mTop = top;
        mBottom = bottom;
    }


    public void addDialOverlay(DialOverlay dialOverlay) {
        mDialOverlay.add(dialOverlay);
    }

    public void removeDialOverlay(DialOverlay dialOverlay) {
        mDialOverlay.remove(dialOverlay);
    }

    private final BroadcastReceiver mClockChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // borrowed from AnalogClock.java
            if (Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                final String tz = intent.getStringExtra("time-zone");
                mCalendar = Calendar.getInstance(TimeZone.getTimeZone(tz));
            }

            invalidate();
        }

    };
}
