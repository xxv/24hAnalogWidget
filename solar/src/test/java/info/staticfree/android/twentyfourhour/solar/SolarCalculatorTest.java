package info.staticfree.android.twentyfourhour.solar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

public class SolarCalculatorTest {
    /** Tolerance in hours: 3 minutes, well under what's visible on the dial. */
    private static final double TOLERANCE = 3 / 60.0;

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private static double hm(int hours, int minutes) {
        return hours + minutes / 60.0;
    }

    @Test
    public void newYorkSummerSolstice() {
        LocalDate date = LocalDate.of(2026, 6, 21);

        // Reference: NOAA solar calculator.
        SolarCalculator.Interval day =
                SolarCalculator.compute(date, NEW_YORK, 40.7128, -74.0060,
                        SolarCalculator.ALTITUDE_DAYLIGHT);
        assertEquals(hm(5, 25), day.start, TOLERANCE);
        assertEquals(hm(20, 31), day.end, TOLERANCE);
        assertEquals(hm(12, 58), day.noon, TOLERANCE);

        SolarCalculator.Interval civil =
                SolarCalculator.compute(date, NEW_YORK, 40.7128, -74.0060,
                        SolarCalculator.ALTITUDE_CIVIL_TWILIGHT);
        assertEquals(hm(4, 53), civil.start, TOLERANCE);
        assertEquals(hm(21, 3), civil.end, TOLERANCE);
    }

    @Test
    public void greenwichEquinox() {
        SolarCalculator.Interval day =
                SolarCalculator.compute(LocalDate.of(2026, 3, 20), LONDON, 51.4779, 0,
                        SolarCalculator.ALTITUDE_DAYLIGHT);

        assertEquals(hm(6, 3), day.start, TOLERANCE);
        assertEquals(hm(18, 14), day.end, TOLERANCE);
    }

    @Test
    public void tromsoMidnightSun() {
        SolarCalculator.Interval day =
                SolarCalculator.compute(LocalDate.of(2026, 6, 21), OSLO, 69.6492, 18.9553,
                        SolarCalculator.ALTITUDE_DAYLIGHT);

        assertTrue(day.isAlwaysAbove());
        assertFalse(day.isAlwaysBelow());
        assertEquals(24, day.end - day.start, 1e-6);
    }

    @Test
    public void tromsoPolarNight() {
        LocalDate date = LocalDate.of(2026, 12, 21);

        SolarCalculator.Interval day = SolarCalculator.compute(date, OSLO, 69.6492, 18.9553,
                SolarCalculator.ALTITUDE_DAYLIGHT);
        assertTrue(day.isAlwaysBelow());
        assertEquals(day.noon, day.start, 0);
        assertEquals(day.noon, day.end, 0);

        // The sun still gets within about 3 degrees of the horizon at noon: civil twilight.
        SolarCalculator.Interval civil = SolarCalculator.compute(date, OSLO, 69.6492, 18.9553,
                SolarCalculator.ALTITUDE_CIVIL_TWILIGHT);
        assertFalse(civil.isAlwaysBelow());
        assertFalse(civil.isAlwaysAbove());
    }

    @Test
    public void farFromZoneMeridianCrossesMidnight() {
        // Kashgar runs on Beijing time, so solar noon is around 15:00 and, in summer,
        // astronomical dusk is after midnight.
        LocalDate date = LocalDate.of(2026, 6, 21);
        SolarCalculator.Interval day = SolarCalculator.compute(date, SHANGHAI, 39.4704, 75.9898,
                SolarCalculator.ALTITUDE_DAYLIGHT);
        assertEquals(hm(15, 0), day.noon, 0.25);

        SolarCalculator.Interval astronomical = SolarCalculator.compute(date, SHANGHAI, 39.4704,
                75.9898, SolarCalculator.ALTITUDE_ASTRONOMICAL_TWILIGHT);
        assertTrue(astronomical.end > 24);
        assertFalse(astronomical.isAlwaysAbove());
    }

    /**
     * The watch face stacks one wedge per altitude, so the intervals must nest, and a
     * RANGED_VALUE complication needs min <= value <= max.
     */
    @Test
    public void intervalsNestAndContainNoon() {
        double[] altitudes = {
                SolarCalculator.ALTITUDE_DAYLIGHT,
                SolarCalculator.ALTITUDE_CIVIL_TWILIGHT,
                SolarCalculator.ALTITUDE_NAUTICAL_TWILIGHT,
                SolarCalculator.ALTITUDE_ASTRONOMICAL_TWILIGHT,
        };
        ZoneId[] zones = {NEW_YORK, LONDON, OSLO, SHANGHAI, ZoneId.of("Pacific/Auckland")};

        for (ZoneId zone : zones) {
            for (int latitude = -85; latitude <= 85; latitude += 17) {
                for (int longitude = -180; longitude < 180; longitude += 45) {
                    for (int dayOfYear = 1; dayOfYear <= 365; dayOfYear += 30) {
                        LocalDate date = LocalDate.ofYearDay(2026, dayOfYear);
                        SolarCalculator.Interval previous = null;

                        for (double altitude : altitudes) {
                            SolarCalculator.Interval interval = SolarCalculator.compute(date,
                                    zone, latitude, longitude, altitude);
                            String where = zone + " " + latitude + "," + longitude + " " + date +
                                    " " + altitude + " " + interval;

                            assertTrue(where, interval.start <= interval.noon);
                            assertTrue(where, interval.noon <= interval.end);
                            assertTrue(where, interval.end - interval.start <= 24 + 1e-6);
                            assertTrue(where, interval.noon >= 0 && interval.noon < 24);

                            if (previous != null) {
                                assertTrue(where, interval.start <= previous.start + 1e-6);
                                assertTrue(where, interval.end >= previous.end - 1e-6);
                            }

                            previous = interval;
                        }
                    }
                }
            }
        }
    }
}
