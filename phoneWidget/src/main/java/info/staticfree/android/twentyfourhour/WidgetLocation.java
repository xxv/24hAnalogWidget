package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2026 Steve Pomeroy <steve@staticfree.info>
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the last known approximate location for the sunrise / sunset shading.
 *
 * The widget redraws every minute but can't wait for a location fix, so it draws with the stored
 * location and, now and then, picks up whatever location the system already has cached. A fresh
 * fix is only requested while the app's activity is open.
 */
final class WidgetLocation {
    private static final String TAG = WidgetLocation.class.getSimpleName();

    private static final String PREFS_NAME = "location";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LAST_CHECK = "last_check";

    /** The widget updates every minute; the location doesn't need checking nearly that often. */
    private static final long MIN_CHECK_INTERVAL_MS = 15 * 60 * 1000;

    /**
     * Roughly 5km. Sun times shift by about 4 minutes per degree of longitude, so smaller moves
     * aren't worth storing.
     */
    private static final double SIGNIFICANT_CHANGE_DEGREES = 0.05;

    private final Context context;
    private final SharedPreferences prefs;

    WidgetLocation(@NonNull Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    boolean hasPermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    boolean hasBackgroundPermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @return the stored location, or null if there isn't one yet
     */
    @Nullable
    Location get() {
        if (!prefs.contains(KEY_LATITUDE) || !prefs.contains(KEY_LONGITUDE)) {
            return null;
        }

        Location location = new Location("stored");
        location.setLatitude(Double.longBitsToDouble(prefs.getLong(KEY_LATITUDE, 0)));
        location.setLongitude(Double.longBitsToDouble(prefs.getLong(KEY_LONGITUDE, 0)));

        return location;
    }

    /**
     * Called from the widget's minute updates. Picks up the system's cached location if it's
     * been a while since the last check. This doesn't power up any location hardware.
     *
     * @return true if the stored location changed
     */
    boolean checkFromBackground() {
        long now = System.currentTimeMillis();

        if (!hasPermission() || now - prefs.getLong(KEY_LAST_CHECK, 0) < MIN_CHECK_INTERVAL_MS) {
            return false;
        }

        prefs.edit().putLong(KEY_LAST_CHECK, now).apply();

        return store(getLastKnown());
    }

    /**
     * Requests a fresh location fix. Only call while the app is in the foreground.
     *
     * @param onChanged run on the main thread if the stored location changes
     */
    void requestFresh(@NonNull Runnable onChanged) {
        if (!hasPermission()) {
            return;
        }

        if (store(getLastKnown())) {
            onChanged.run();
        }

        LocationManager locationManager = context.getSystemService(LocationManager.class);
        List<String> providers = getFreshProviders(locationManager);

        if (providers.isEmpty()) {
            Log.d(TAG, "No location provider available for a fresh fix");
            return;
        }

        requestFrom(locationManager, providers, 0, onChanged);
    }

    /**
     * Asks each provider in turn, moving to the next when one times out without a location.
     */
    @SuppressLint("MissingPermission")
    private void requestFrom(@NonNull LocationManager locationManager,
            @NonNull List<String> providers, int index, @NonNull Runnable onChanged) {
        if (index >= providers.size()) {
            return;
        }

        String provider = providers.get(index);

        try {
            // Times out (after about 30 seconds) with a null location.
            locationManager.getCurrentLocation(provider, null, context.getMainExecutor(),
                    location -> {
                        if (location == null) {
                            requestFrom(locationManager, providers, index + 1, onChanged);
                        } else if (store(location)) {
                            onChanged.run();
                        }
                    });
        } catch (SecurityException | IllegalArgumentException e) {
            Log.w(TAG, "Could not request location from " + provider, e);
            requestFrom(locationManager, providers, index + 1, onChanged);
        }
    }

    /**
     * @return enabled providers to ask for a fresh fix, lowest power first
     */
    @NonNull
    private static List<String> getFreshProviders(@NonNull LocationManager locationManager) {
        List<String> providers = new ArrayList<>();

        for (String provider : new String[] {LocationManager.FUSED_PROVIDER,
                LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
            if (locationManager.isProviderEnabled(provider)) {
                providers.add(provider);
            }
        }

        return providers;
    }

    /**
     * @return the most recent location any provider has cached, or null
     */
    @SuppressLint("MissingPermission")
    @Nullable
    private Location getLastKnown() {
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        Location newest = null;

        try {
            for (String provider : locationManager.getProviders(true)) {
                Location location = locationManager.getLastKnownLocation(provider);

                if (location != null && (newest == null || location.getTime() > newest.getTime())) {
                    newest = location;
                }
            }
        } catch (SecurityException e) {
            // Without background access, this can be refused while the app isn't visible.
            Log.d(TAG, "Location not available right now", e);
        }

        return newest;
    }

    private boolean store(@Nullable Location location) {
        if (location == null) {
            return false;
        }

        Location previous = get();

        if (previous != null &&
                Math.abs(previous.getLatitude() - location.getLatitude()) <
                        SIGNIFICANT_CHANGE_DEGREES &&
                Math.abs(previous.getLongitude() - location.getLongitude()) <
                        SIGNIFICANT_CHANGE_DEGREES) {
            return false;
        }

        prefs.edit()
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(location.getLongitude()))
                .apply();

        return true;
    }
}
