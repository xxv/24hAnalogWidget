package info.staticfree.android.twentyfourhour;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
    private static final float DATE_OVERLAY_OFFSET_X = 0.1875f;
    private static final float DATE_OVERLAY_TEXT_SIZE_SCALE = 0.0625f;
    public static final String PREFS_USER_WANTS_LOCATION = "location_enabled";
    private static final String TAG = WearFace.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        checkForLocationPermissionsAndMaybeLaunch();
    }

    private void checkForLocationPermissionsAndMaybeLaunch() {
        if (!hasLocationPermissions()) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (preferences.getBoolean(PREFS_USER_WANTS_LOCATION, true)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    startActivity(new Intent(this, PermissionRequestActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        }
    }

    private boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED;
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private static final int MSG_UPDATE_TIME = 100;
        private static final long INTERACTIVE_UPDATE_RATE_MS = 1000;
        private static final int SHADE_ALPHA = 60;

        private final Analog24HClock clock = new Analog24HClock(WearFace.this);
        private boolean registeredTimeZoneReceiver;
        private boolean viewSizeInvalid = true;
        private boolean isRound;

        private final SunPositionOverlay sunPositionOverlay = new SunPositionOverlay(WearFace.this);
        private final DateOverlay dateOverlay =
                new DateOverlay(DATE_OVERLAY_OFFSET_X, -DATE_OVERLAY_OFFSET_X,
                        DATE_OVERLAY_TEXT_SIZE_SCALE);
        private final HandsOverlay handsOverlayAmbient =
                new HandsOverlay(getApplicationContext(), R.drawable.wear_hour_hand_ambient,
                        R.drawable.wear_minute_hand_ambient);
        private final HandsOverlay handsOverlay =
                new HandsOverlay(getApplicationContext(), R.drawable.wear_hour_hand,
                        R.drawable.wear_minute_hand);

        Engine() {
            sunPositionOverlay.setScale(SUN_POSITION_OVERLAY_SCALE);
            sunPositionOverlay.setShadeAlpha(SHADE_ALPHA);
        }

        private final GoogleApiClient.ConnectionCallbacks connectionCallbacks =
                new GoogleApiClient.ConnectionCallbacks() {
                    @SuppressWarnings("MissingPermission")
                    @Override
                    public void onConnected(Bundle bundle) {
                        if (hasLocationPermissions()) {
                            setLocation(LocationServices.FusedLocationApi
                                    .getLastLocation(googleApiClient));
                            requestLocationUpdate();
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                };

        private final LocationListener locationCallback = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setLocation(location);
            }
        };

        private void setLocation(@Nullable Location location) {
            sunPositionOverlay.setLocation(location);
            invalidate();
        }

        private final GoogleApiClient.OnConnectionFailedListener mConnectionFailedListener =
                new GoogleApiClient.OnConnectionFailedListener() {

                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.d(TAG, "Error connecting to Google Play Services: " + connectionResult);
                    }
                };

        @Nullable
        private final GoogleApiClient googleApiClient =
                new GoogleApiClient.Builder(WearFace.this).addApi(LocationServices.API)
                        .addApi(Wearable.API).addConnectionCallbacks(connectionCallbacks)
                        .addOnConnectionFailedListener(mConnectionFailedListener).build();

        final Handler updateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS -
                                    (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            updateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /* receiver to update the time zone */
        final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra("time-zone")) {
                    clock.setTimezone(TimeZone.getTimeZone(intent.getStringExtra("time-zone")));
                }
            }
        };

        private void updateTimer() {
            updateTimeHandler.removeMessages(MSG_UPDATE_TIME);

            if (shouldTimerBeRunning()) {
                updateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void initializeOverlays() {
            clock.clearDialOverlays();
            clock.addDialOverlay(sunPositionOverlay);
            clock.addDialOverlay(dateOverlay);
            handsOverlay.setShowSeconds(true);
            handsOverlayAmbient.setShowSeconds(true);
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            initializeOverlays();

               /* configure the system UI */
            setWatchFaceStyle(new WatchFaceStyle.Builder(WearFace.this).setAcceptsTapEvents(false)
                    .setStatusBarGravity(Gravity.CENTER).setViewProtectionMode(
                            WatchFaceStyle.PROTECT_STATUS_BAR |
                                    WatchFaceStyle.PROTECT_HOTWORD_INDICATOR)

                    .build());
            setTouchEventsEnabled(false);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            isRound = insets.isRound();

            clock.setHandsOverlay(handsOverlay);
            clock.setFace(isRound ? R.drawable.round_clock_face : R.drawable.square_clock_face);
            clock.setShowSeconds(true);

            invalidate();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            if (isRound) {
                clock.setFace(inAmbientMode ? R.drawable.round_clock_face_ambient :
                        R.drawable.round_clock_face);
            } else {
                clock.setFace(inAmbientMode ? R.drawable.square_clock_face_ambient :
                        R.drawable.square_clock_face);
            }

            clock.setHandsOverlay(inAmbientMode ? handsOverlayAmbient : handsOverlay);

            sunPositionOverlay.setShowHighNoon(!inAmbientMode);
            sunPositionOverlay.setShowTwilight(!inAmbientMode);

            if (inAmbientMode) {
                clock.removeDialOverlay(dateOverlay);
                sunPositionOverlay.setShadeAlpha(255);
            } else {
                clock.addDialOverlay(dateOverlay);
                sunPositionOverlay.setShadeAlpha(SHADE_ALPHA);
            }

            invalidate();
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            clock.setTime(System.currentTimeMillis());

            if (viewSizeInvalid) {
                clock.measure(bounds.width(), bounds.height());
                clock.layout(bounds.left, bounds.top, bounds.right, bounds.bottom);
                viewSizeInvalid = false;
            }

            clock.draw(canvas);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerTimeZoneReceiver();

                // Update time zone in case it changed while we weren't visible.
                clock.setTimezone(TimeZone.getDefault());
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
            if (registeredTimeZoneReceiver) {
                return;
            }

            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            registerReceiver(timeZoneReceiver, filter);
            registeredTimeZoneReceiver = true;
        }

        private void unregisterTimeZoneReceiver() {
            if (!registeredTimeZoneReceiver) {
                return;
            }

            registeredTimeZoneReceiver = false;
            unregisterReceiver(timeZoneReceiver);
        }

        private void connectLocationService() {
            if (googleApiClient != null) {
                googleApiClient.connect();
            }
        }

        @SuppressWarnings("MissingPermission")
        private void requestLocationUpdate() {
            LocationRequest locationRequest =
                    LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER)
                            .setNumUpdates(1);

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(googleApiClient, locationRequest, locationCallback);
        }

        private void disconnectLocationService() {
            if (googleApiClient != null && googleApiClient.isConnected()) {
                LocationServices.FusedLocationApi
                        .removeLocationUpdates(googleApiClient, locationCallback);
                googleApiClient.disconnect();
            }
        }
    }
}
