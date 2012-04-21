package info.staticfree.android.twentyfourhour;

import android.content.Context;


public class TwentyFourHourClockWidget4x extends TwentyFourHourClockWidget {

	@Override
	protected float getSize(Context context) {
		return 453f * getDisplayDensity(context);
	}
}
