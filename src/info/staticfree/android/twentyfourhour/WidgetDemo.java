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

import android.app.Activity;
import android.os.Bundle;

public class WidgetDemo extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final Analog24HClock clock = (Analog24HClock) findViewById(R.id.clock);

		clock.addDialOverlay(new SunPositionOverlay(this));

	}
}
