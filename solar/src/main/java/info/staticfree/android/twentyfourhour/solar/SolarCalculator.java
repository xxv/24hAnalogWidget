package info.staticfree.android.twentyfourhour.solar;

/*
 * Copyright (C) 2026 Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Computes when the sun is above a given altitude on a given day, using the NOAA solar position
 * equations (accurate to about a minute, which is far finer than a watch dial can show).
 *
 * Rather than solving for rise and set separately, this finds solar noon and then searches
 * outwards from it for the altitude crossing. That way polar day / polar night fall out naturally
 * and the result is always a single interval that contains solar noon.
 */
public final class SolarCalculator {
    /** Sunrise / sunset: the top of the sun's disc at the horizon, accounting for refraction. */
    public static final double ALTITUDE_DAYLIGHT = -0.833;
    public static final double ALTITUDE_CIVIL_TWILIGHT = -6;
    public static final double ALTITUDE_NAUTICAL_TWILIGHT = -12;
    public static final double ALTITUDE_ASTRONOMICAL_TWILIGHT = -18;

    private static final long MINUTE_MS = 60_000;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final long HALF_DAY_MS = 12 * HOUR_MS;
    private static final long SEARCH_STEP_MS = 10 * MINUTE_MS;
    private static final long SEARCH_PRECISION_MS = 1000;

    private SolarCalculator() {
    }

    /**
     * The interval of a day during which the sun is above a given altitude. All values are
     * fractional hours on the local wall clock of the requested day, so they map directly onto the
     * 24h dial. {@code start <= noon <= end} always holds; {@code start} may be negative and
     * {@code end} may exceed 24 when the interval crosses midnight.
     */
    public static final class Interval {
        public final double start;
        public final double noon;
        public final double end;

        public Interval(double start, double noon, double end) {
            this.start = start;
            this.noon = noon;
            this.end = end;
        }

        /** The sun never gets this high today. */
        public boolean isAlwaysBelow() {
            return end <= start;
        }

        /** The sun never gets this low today. */
        public boolean isAlwaysAbove() {
            return end - start >= 24;
        }

        @Override
        public String toString() {
            return "Interval{start=" + start + ", noon=" + noon + ", end=" + end + '}';
        }
    }

    public static Interval compute(LocalDate date, ZoneId zone, double latitude,
            double longitude, double altitude) {
        long noon = solarNoon(date, zone, longitude);
        double noonHour = wallClockHour(noon, zone);

        if (elevation(noon, latitude, longitude) < altitude) {
            return new Interval(noonHour, noonHour, noonHour);
        }

        long start = findCrossing(noon, -1, latitude, longitude, altitude);
        long end = findCrossing(noon, 1, latitude, longitude, altitude);

        return new Interval(noonHour + (double) (start - noon) / HOUR_MS, noonHour,
                noonHour + (double) (end - noon) / HOUR_MS);
    }

    /**
     * Walks away from solar noon in the given direction until the sun drops below the altitude,
     * then bisects to find the crossing. Gives up after half a day, returning that point.
     */
    private static long findCrossing(long noon, int direction, double latitude, double longitude,
            double altitude) {
        long above = noon;

        for (long offset = SEARCH_STEP_MS; offset <= HALF_DAY_MS; offset += SEARCH_STEP_MS) {
            long t = noon + direction * offset;

            if (elevation(t, latitude, longitude) < altitude) {
                long below = t;

                while (Math.abs(below - above) > SEARCH_PRECISION_MS) {
                    long mid = (above + below) / 2;

                    if (elevation(mid, latitude, longitude) < altitude) {
                        below = mid;
                    } else {
                        above = mid;
                    }
                }

                return (above + below) / 2;
            }

            above = t;
        }

        return noon + direction * HALF_DAY_MS;
    }

    /**
     * Finds the solar noon closest to 12:00 local time on the given date.
     */
    static long solarNoon(LocalDate date, ZoneId zone, double longitude) {
        long t = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();

        // The hour angle changes by one degree every four minutes; a few iterations converge.
        for (int i = 0; i < 3; i++) {
            t -= Math.round(hourAngle(t, longitude) * 4 * MINUTE_MS);
        }

        return t;
    }

    private static double wallClockHour(long epochMs, ZoneId zone) {
        ZonedDateTime time = Instant.ofEpochMilli(epochMs).atZone(zone);

        return time.getHour() + time.getMinute() / 60.0 + time.getSecond() / 3600.0 +
                time.getNano() / 3.6e12;
    }

    /**
     * @return the sun's elevation above the horizon in degrees, uncorrected for refraction.
     */
    static double elevation(long epochMs, double latitude, double longitude) {
        SolarPosition position = new SolarPosition(epochMs);
        double lat = Math.toRadians(latitude);
        double ha = Math.toRadians(hourAngle(epochMs, longitude, position));
        double cosZenith = Math.sin(lat) * Math.sin(position.declination) +
                Math.cos(lat) * Math.cos(position.declination) * Math.cos(ha);

        return 90 - Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosZenith))));
    }

    private static double hourAngle(long epochMs, double longitude) {
        return hourAngle(epochMs, longitude, new SolarPosition(epochMs));
    }

    /**
     * @return the hour angle in degrees, in [-180, 180). 0 is solar noon; positive is afternoon.
     */
    private static double hourAngle(long epochMs, double longitude,
            SolarPosition position) {
        double utcMinutes = Math.floorMod(epochMs, 86_400_000L) / (double) MINUTE_MS;
        double trueSolarMinutes = utcMinutes + position.equationOfTime + 4 * longitude;
        double ha = trueSolarMinutes / 4 - 180;

        return ((ha + 180) % 360 + 360) % 360 - 180;
    }

    /**
     * Sun declination and equation of time for an instant, per the NOAA solar calculator.
     */
    private static final class SolarPosition {
        /** radians */
        final double declination;
        /** minutes */
        final double equationOfTime;

        SolarPosition(long epochMs) {
            double julianDay = epochMs / 86_400_000.0 + 2440587.5;
            double t = (julianDay - 2451545) / 36525;

            double meanLongitude =
                    Math.toRadians((280.46646 + t * (36000.76983 + t * 0.0003032)) % 360);
            double meanAnomaly = Math.toRadians(357.52911 + t * (35999.05029 - 0.0001537 * t));
            double eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);

            double center = Math.sin(meanAnomaly) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                    Math.sin(2 * meanAnomaly) * (0.019993 - 0.000101 * t) +
                    Math.sin(3 * meanAnomaly) * 0.000289;
            double omega = Math.toRadians(125.04 - 1934.136 * t);
            double apparentLongitude = Math.toRadians(
                    Math.toDegrees(meanLongitude) + center - 0.00569 - 0.00478 * Math.sin(omega));

            double meanObliquity =
                    23 + (26 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60) / 60;
            double obliquity = Math.toRadians(meanObliquity + 0.00256 * Math.cos(omega));

            declination = Math.asin(Math.sin(obliquity) * Math.sin(apparentLongitude));

            double y = Math.pow(Math.tan(obliquity / 2), 2);
            equationOfTime = 4 * Math.toDegrees(y * Math.sin(2 * meanLongitude) -
                    2 * eccentricity * Math.sin(meanAnomaly) +
                    4 * eccentricity * y * Math.sin(meanAnomaly) * Math.cos(2 * meanLongitude) -
                    0.5 * y * y * Math.sin(4 * meanLongitude) -
                    1.25 * eccentricity * eccentricity * Math.sin(2 * meanAnomaly));
        }
    }
}
