package info.staticfree.android.twentyfourhour.sun;

public class DaylightComplicationService extends SunComplicationService {
    @Override
    protected double getAltitude() {
        return SolarCalculator.ALTITUDE_DAYLIGHT;
    }

    @Override
    protected int getLabel() {
        return R.string.slot_daylight;
    }
}
