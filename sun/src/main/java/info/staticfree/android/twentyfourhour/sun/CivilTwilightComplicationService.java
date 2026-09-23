package info.staticfree.android.twentyfourhour.sun;

import info.staticfree.android.twentyfourhour.solar.SolarCalculator;

public class CivilTwilightComplicationService extends SunComplicationService {
    @Override
    protected double getAltitude() {
        return SolarCalculator.ALTITUDE_CIVIL_TWILIGHT;
    }

    @Override
    protected int getLabel() {
        return R.string.slot_civil_twilight;
    }
}
