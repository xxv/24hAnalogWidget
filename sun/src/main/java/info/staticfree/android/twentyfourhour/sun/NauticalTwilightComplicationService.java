package info.staticfree.android.twentyfourhour.sun;

public class NauticalTwilightComplicationService extends SunComplicationService {
    @Override
    protected double getAltitude() {
        return SolarCalculator.ALTITUDE_NAUTICAL_TWILIGHT;
    }

    @Override
    protected int getLabel() {
        return R.string.slot_nautical_twilight;
    }
}
