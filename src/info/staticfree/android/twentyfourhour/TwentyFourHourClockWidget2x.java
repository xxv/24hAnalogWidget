package info.staticfree.android.twentyfourhour;

import android.content.Context;


public class TwentyFourHourClockWidget2x extends TwentyFourHourClockWidget {

	@Override
	protected float getSize(Context context) {
        return 196f * getDisplayDensity(context);
	}
}
