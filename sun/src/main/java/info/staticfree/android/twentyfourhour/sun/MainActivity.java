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
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import info.staticfree.android.twentyfourhour.solar.SolarCalculator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Asks for location permission (needed to compute sun times) and shows what's being published.
 */
public class MainActivity extends Activity {
    private static final int REQUEST_FOREGROUND_LOCATION = 1;
    private static final int REQUEST_BACKGROUND_LOCATION = 2;

    private LocationStore locationStore;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationStore = new LocationStore(this);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        status = new TextView(this);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setPadding(padding, padding, padding, padding * 2);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(status);
        setContentView(scrollView);

        if (!locationStore.hasPermission()) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_FOREGROUND_LOCATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // While in the foreground a fresh fix is always allowed, so take the opportunity.
        locationStore.refresh();
        LocationStore.requestComplicationUpdates(this);
        showStatus();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_FOREGROUND_LOCATION && locationStore.hasPermission()) {
            locationStore.refresh();

            // Without background access, the location only updates when this app is opened.
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_BACKGROUND_LOCATION);
            }
        }

        showStatus();
    }

    private void showStatus() {
        StringBuilder text = new StringBuilder(getString(R.string.app_name)).append("\n\n");

        if (!locationStore.hasPermission()) {
            text.append(getString(R.string.status_no_permission));
            status.setText(text);
            return;
        }

        double[] location = locationStore.get();

        if (location == null) {
            text.append(getString(R.string.status_no_location));
            status.setText(text);
            return;
        }

        text.append(String.format(Locale.getDefault(), "%.2f, %.2f\n\n", location[0],
                location[1]));

        appendInterval(text, R.string.slot_daylight, SolarCalculator.ALTITUDE_DAYLIGHT, location);
        appendInterval(text, R.string.slot_civil_twilight,
                SolarCalculator.ALTITUDE_CIVIL_TWILIGHT, location);
        appendInterval(text, R.string.slot_nautical_twilight,
                SolarCalculator.ALTITUDE_NAUTICAL_TWILIGHT, location);
        appendInterval(text, R.string.slot_astronomical_twilight,
                SolarCalculator.ALTITUDE_ASTRONOMICAL_TWILIGHT, location);

        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            text.append('\n').append(getString(R.string.status_no_background));
        }

        status.setText(text);
    }

    private void appendInterval(@NonNull StringBuilder text, int label, double altitude,
            @NonNull double[] location) {
        SolarCalculator.Interval interval = SolarCalculator.compute(LocalDate.now(),
                ZoneId.systemDefault(), location[0], location[1], altitude);

        text.append(getString(label)).append('\n');

        if (interval.isAlwaysBelow()) {
            text.append(getString(R.string.sun_always_below));
        } else if (interval.isAlwaysAbove()) {
            text.append(getString(R.string.sun_always_above));
        } else {
            text.append(SunComplicationService.formatHour(interval.start)).append(" – ")
                    .append(SunComplicationService.formatHour(interval.end));
        }

        text.append("\n\n");
    }
}
