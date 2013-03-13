package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2011-2012 Steve Pomeroy <steve@staticfree.info>
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

import android.app.Activity;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class WidgetDemo extends Activity {

	private static final String PREF_DISABLED_ALT_SIZES = "disabled_alt_sizes";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Analog24HClock clock = (Analog24HClock) findViewById(R.id.clock);

		clock.addDialOverlay(new SunPositionOverlay(this));
		new DisableWidgetTask().execute();
	}

	/**
	 * Disables the fixed-sized widgets for Android 3.0+, otherwise disables resizable one.
	 */
	private void disableWidgetSizes() {

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean isDisabled = prefs.getBoolean(PREF_DISABLED_ALT_SIZES, false);

		if (isDisabled) {
			return;
		}

		final PackageManager pm = getPackageManager();
		// for versions 3.0+ we use the resizable widget instead of having multiple pre-defined
		// sizes.
		final boolean supportsResize = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

		// resizable widget

		pm.setComponentEnabledSetting(new ComponentName(this, TwentyFourHourClockWidget.class),
				supportsResize ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
						: PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
				PackageManager.DONT_KILL_APP);

		// fixed size widgets

		pm.setComponentEnabledSetting(new ComponentName(this, TwentyFourHourClockWidget2x.class),
				supportsResize ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
						: PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
				PackageManager.DONT_KILL_APP);

		pm.setComponentEnabledSetting(new ComponentName(this, TwentyFourHourClockWidget3x.class),
				supportsResize ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED
						: PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
				PackageManager.DONT_KILL_APP);

		prefs.edit().putBoolean(PREF_DISABLED_ALT_SIZES, true).commit();
	}

	private class DisableWidgetTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			disableWidgetSizes();
			return true;
		}
	}
}
