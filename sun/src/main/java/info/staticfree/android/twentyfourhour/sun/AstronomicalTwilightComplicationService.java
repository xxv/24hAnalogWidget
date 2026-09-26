package info.staticfree.android.twentyfourhour.sun;

import info.staticfree.android.twentyfourhour.solar.SolarCalculator;

public class AstronomicalTwilightComplicationService extends SunComplicationService {
    @Override
    protected double getAltitude() {
        return SolarCalculator.ALTITUDE_ASTRONOMICAL_TWILIGHT;
    }

    @Override
    protected int getLabel() {
        return R.string.slot_astronomical_twilight;
    }
}
