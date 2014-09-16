package info.staticfree.android.twentyfourhour;

import android.content.Context;


public class TwentyFourHourClockWidget3x extends TwentyFourHourClockWidgetResizable {

    @Override
    protected float getSize(Context context) {
        return 196 * getDisplayDensity(context);
    }
}
