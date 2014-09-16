package info.staticfree.android.twentyfourhour;

import android.content.Context;


public class TwentyFourHourClockWidget extends TwentyFourHourClockWidgetResizable {

	@Override
	protected float getSize(Context context) {
        return 196f * getDisplayDensity(context);
	}
}
