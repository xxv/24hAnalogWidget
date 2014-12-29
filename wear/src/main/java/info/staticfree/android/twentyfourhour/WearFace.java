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
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.overlay.DateOverlay;
import info.staticfree.android.twentyfourhour.overlay.HandsOverlay;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.wear.R;

public class WearFace extends CanvasWatchFaceService {
    /**
     * This is scaled based on the background design.
     */
    public static final float SUN_POSITION_OVERLAY_SCALE = 0.61345f;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 100;
        private static final long INTERACTIVE_UPDATE_RATE_MS = 60 * 1000;

        private final Analog24HClock mClock = new Analog24HClock(WearFace.this);
        private boolean mRegisteredTimeZoneReceiver;
        private boolean mViewSizeInvalid = true;
        private final SunPositionOverlay mSunPositionOverlay =
                new SunPositionOverlay(WearFace.this);

        private final GoogleApiClient.ConnectionCallbacks mConnectionCallbacks =
                new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(final Bundle bundle) {
                        setLocation(LocationServices.FusedLocationApi
                                .getLastLocation(mGoogleApiClient));
                        requestLocationUpdate();
                    }

                    @Override
                    public void onConnectionSuspended(final int i) {

                    }
                };

        private final LocationListener mLocationCallback = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {
                setLocation(location);
            }
        };
        private boolean mIsRound;

        private void setLocation(@Nullable final Location location) {
            mSunPositionOverlay.setLocation(location);
            invalidate();
        }

        private final GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener =
                new GoogleApiClient.OnConnectionFailedListener() {

                    @Override
                    public void onConnectionFailed(final ConnectionResult connectionResult) {
                        Log.d("WearFace", "Error connecting to Google Play Services");
                    }
                };

        @Nullable
        private final GoogleApiClient mGoogleApiClient =
                new GoogleApiClient.Builder(WearFace.this).addApi(LocationServices.API)
                        .addApi(Wearable.API).addConnectionCallbacks(mConnectionCallbacks)
                        .addOnConnectionFailedListener(mConnectionFailedListener).build();

        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(final Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            final long timeMs = System.currentTimeMillis();
                            final long delayMs = INTERACTIVE_UPDATE_RATE_MS -
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
            public void onReceive(final Context context, final Intent intent) {
                if (intent.hasExtra("time-zone")) {
                    mClock.setTimezone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                }
            }
        };

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
            mClock.clearDialOverlays();
            mSunPositionOverlay.setScale(SUN_POSITION_OVERLAY_SCALE);
            mSunPositionOverlay.setShadeAlpha(60);
            mClock.addDialOverlay(mSunPositionOverlay);

            final DateOverlay dateOverlay = new DateOverlay(0.1875f, -0.1875f, 0.0625f);
            mClock.addDialOverlay(dateOverlay);
        }

        @Override
        public void onCreate(final SurfaceHolder holder) {
            super.onCreate(holder);
            initializeClock();

               /* configure the system UI */
            setWatchFaceStyle(new WatchFaceStyle.Builder(WearFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false).setHotwordIndicatorGravity(Gravity.CENTER)
                    .setStatusBarGravity(Gravity.CENTER).setViewProtection(
                            WatchFaceStyle.PROTECT_STATUS_BAR |
                                    WatchFaceStyle.PROTECT_HOTWORD_INDICATOR)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT).build());
            setTouchEventsEnabled(false);
        }

        @Override
        public void onApplyWindowInsets(final WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();

            if (mIsRound) {
                mClock.setHandsOverlay(
                        new HandsOverlay(getApplicationContext(), R.drawable.round_hour_hand,
                                R.drawable.round_minute_hand));
                mClock.setFace(R.drawable.round_clock_face);
            } else {
                mClock.setHandsOverlay(
                        new HandsOverlay(getApplicationContext(), R.drawable.square_hour_hand,
                                R.drawable.square_minute_hand));
                mClock.setFace(R.drawable.square_clock_face);
            }

            invalidate();
        }

        @Override
        public void onPropertiesChanged(final Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(final boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (mIsRound) {
                mClock.setFace(inAmbientMode ? R.drawable.round_clock_face_ambient :
                        R.drawable.round_clock_face);
            } else {
                mClock.setFace(inAmbientMode ? R.drawable.square_clock_face_ambient :
                        R.drawable.square_clock_face);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(final Canvas canvas, final Rect bounds) {
            super.onDraw(canvas, bounds);
            mClock.setTime(System.currentTimeMillis());

            if (mViewSizeInvalid) {
                mClock.measure(bounds.width(), bounds.height());
                mClock.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
                mViewSizeInvalid = false;
            }

            mClock.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(final boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeZoneReceiver();

                // Update time zone in case it changed while we weren't visible.
                mClock.setTimezone(TimeZone.getDefault());
                connectLocationService();
            } else {
                unregisterTimeZoneReceiver();
                disconnectLocationService();
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

        private void connectLocationService() {
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        }

        private void requestLocationUpdate() {
            final LocationRequest locationRequest =
                    LocationRequest.create().setPriority(LocationRequest.PRIORITY_NO_POWER)
                            .setNumUpdates(1);

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationCallback);

        }

        private void disconnectLocationService() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi
                        .removeLocationUpdates(mGoogleApiClient, mLocationCallback);
                mGoogleApiClient.disconnect();
            }
        }
    }
}
