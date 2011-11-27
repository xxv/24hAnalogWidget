package info.staticfree.android.twentyfourhour;

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
