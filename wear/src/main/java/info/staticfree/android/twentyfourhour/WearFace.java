package info.staticfree.android.twentyfourhour;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.overlay.DateOverlay;
import info.staticfree.android.twentyfourhour.overlay.HandsOverlay;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.wear.R;

public class WearFace extends CanvasWatchFaceService {

    public static final float SUN_POSITION_OVERLAY_SCALE = 0.61345f;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 100;
        private static final long INTERACTIVE_UPDATE_RATE_MS = 60 * 1000;
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS -
                                    (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };
        /* receiver to update the time zone */
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("time-zone")) {
                    mClock.setTimezone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                }
            }
        };
        private boolean mViewSizeInvalid = true;
        private Analog24HClock mClock;
        private boolean mRegisteredTimeZoneReceiver;

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void initializeClock() {
            mClock = new Analog24HClock(getApplicationContext());
            SunPositionOverlay sunPositionOverlay = new SunPositionOverlay(getApplicationContext());
            Location test = new Location("manual");
            test.setLatitude(42.4);
            test.setLongitude(-71.1);
            sunPositionOverlay.setLocation(test);
            sunPositionOverlay.setScale(SUN_POSITION_OVERLAY_SCALE);
            sunPositionOverlay.setShadeAlpha(60);
            mClock.addDialOverlay(sunPositionOverlay);

            DateOverlay dateOverlay = new DateOverlay(30, -30);
            mClock.addDialOverlay(dateOverlay);
            mClock.setHandsOverlay(
                    new HandsOverlay(getApplicationContext(), R.drawable.round_hour_hand,
                            R.drawable.round_minute_hand));
            mClock.setFace(R.drawable.round_clock_face);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            initializeClock();

               /* configure the system UI */
            setWatchFaceStyle(new WatchFaceStyle.Builder(WearFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false).setHotwordIndicatorGravity(Gravity.CENTER)
                    .setStatusBarGravity(Gravity.CENTER)
                    .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT).build());
            setTouchEventsEnabled(false);

            updateTimer();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mClock.setTime(System.currentTimeMillis());

            if (mViewSizeInvalid) {
                mClock.measure(bounds.width(), bounds.height());
                mClock.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
                mViewSizeInvalid = false;
            }

            mClock.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeZoneReceiver();

                // Update time zone in case it changed while we weren't visible.
                mClock.setTimezone(TimeZone.getDefault());
            } else {
                unregisterTimeZoneReceiver();
            }

            // Whether the timer should be running depends on whether we're visible and
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerTimeZoneReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }

            mRegisteredTimeZoneReceiver = true;
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            unregisterReceiver(mTimeZoneReceiver);
        }
    }
}
