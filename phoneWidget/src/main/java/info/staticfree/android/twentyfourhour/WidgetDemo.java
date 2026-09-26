package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2011-2026 Steve Pomeroy <steve@staticfree.info>
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
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

/**
 * Previews the clock and asks for the location permission used for the sunrise / sunset shading.
 */
public class WidgetDemo extends Activity {
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_BACKGROUND_LOCATION = 2;

    private WidgetLocation widgetLocation;
    private Analog24HClock clock;
    private SunPositionOverlay sunOverlay;
    private TextView locationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        widgetLocation = new WidgetLocation(this);
        clock = (Analog24HClock) findViewById(R.id.clock);
        locationStatus = (TextView) findViewById(R.id.location_status);

        sunOverlay = new SunPositionOverlay(this);
        clock.addDialOverlay(sunOverlay);

        if (!widgetLocation.hasPermission()) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // A fresh location is only allowed while this is on screen, so take the opportunity.
        widgetLocation.requestFresh(this::onLocationChanged);
        showLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION && widgetLocation.hasPermission()) {
            widgetLocation.requestFresh(this::onLocationChanged);

            // Without background access, the widget's location only updates when this is opened.
            if (!widgetLocation.hasBackgroundPermission()) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        REQUEST_BACKGROUND_LOCATION);
            }
        }

        showLocation();
    }

    private void onLocationChanged() {
        TwentyFourHourClockWidgetResizable.updateAll(this);
        showLocation();
    }

    private void showLocation() {
        sunOverlay.setLocation(widgetLocation.get());
        clock.invalidate();

        if (!widgetLocation.hasPermission()) {
            locationStatus.setText(R.string.location_no_permission);
        } else if (widgetLocation.get() == null) {
            locationStatus.setText(R.string.location_waiting);
        } else if (!widgetLocation.hasBackgroundPermission()) {
            locationStatus.setText(R.string.location_no_background);
        } else {
            locationStatus.setText("");
        }
    }
}
