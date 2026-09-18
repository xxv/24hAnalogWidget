package info.staticfree.android.twentyfourhour.sun;

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
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Keeps the last known location, since complication requests need an answer right away and the
 * app often can't get a fresh fix while it's in the background.
 */
final class LocationStore {
    private static final String TAG = LocationStore.class.getSimpleName();

    private static final String PREFS_NAME = "location";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LAST_ATTEMPT = "last_attempt";

    /** Complication requests come in bursts (one per slot), so don't ask for a fix every time. */
    private static final long MIN_REFRESH_INTERVAL_MS = 15 * 60 * 1000;
    private static final long MAX_LOCATION_AGE_MS = 60 * 60 * 1000;

    /**
     * Roughly 5km. Sun times shift by about 4 minutes per degree of longitude, so smaller moves
     * aren't worth redrawing for.
     */
    private static final double SIGNIFICANT_CHANGE_DEGREES = 0.05;

    static final Class<?>[] SUN_COMPLICATIONS = {
            DaylightComplicationService.class,
            CivilTwilightComplicationService.class,
            NauticalTwilightComplicationService.class,
            AstronomicalTwilightComplicationService.class,
    };

    private final Context context;
    private final SharedPreferences prefs;

    LocationStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * @return {latitude, longitude} or null if no location has been stored yet.
     */
    @Nullable
    double[] get() {
        if (!prefs.contains(KEY_LATITUDE) || !prefs.contains(KEY_LONGITUDE)) {
            return null;
        }

        return new double[] {
                Double.longBitsToDouble(prefs.getLong(KEY_LATITUDE, 0)),
                Double.longBitsToDouble(prefs.getLong(KEY_LONGITUDE, 0)),
        };
    }

    boolean hasPermission() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Asks for a location fix unless one was requested recently. If the location has moved
     * meaningfully, it's stored and all the sun complications are asked to update.
     */
    void refreshIfStale() {
        long now = System.currentTimeMillis();

        if (now - prefs.getLong(KEY_LAST_ATTEMPT, 0) < MIN_REFRESH_INTERVAL_MS) {
            return;
        }

        refresh();
    }

    @SuppressLint("MissingPermission")
    void refresh() {
        if (!hasPermission()) {
            return;
        }

        prefs.edit().putLong(KEY_LAST_ATTEMPT, System.currentTimeMillis()).apply();

        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
                .build();

        LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(request, null)
                .addOnSuccessListener(this::onLocation)
                .addOnFailureListener(e -> Log.w(TAG, "Could not get location", e));
    }

    private void onLocation(@Nullable Location location) {
        if (location == null) {
            Log.d(TAG, "No location available");
            return;
        }

        double[] previous = get();

        if (previous != null &&
                Math.abs(previous[0] - location.getLatitude()) < SIGNIFICANT_CHANGE_DEGREES &&
                Math.abs(previous[1] - location.getLongitude()) < SIGNIFICANT_CHANGE_DEGREES) {
            return;
        }

        prefs.edit()
                .putLong(KEY_LATITUDE, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(KEY_LONGITUDE, Double.doubleToRawLongBits(location.getLongitude()))
                .apply();

        requestComplicationUpdates(context);
    }

    static void requestComplicationUpdates(@NonNull Context context) {
        for (Class<?> service : SUN_COMPLICATIONS) {
            ComplicationDataSourceUpdateRequester
                    .create(context, new ComponentName(context, service))
                    .requestUpdateAll();
        }
    }
}
