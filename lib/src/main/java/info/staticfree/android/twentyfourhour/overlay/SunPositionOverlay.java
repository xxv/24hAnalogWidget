package info.staticfree.android.twentyfourhour.overlay;

/*
 * Copyright (C) 2011-2026 Steve Pomeroy <steve@staticfree.info>
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
 *
 * 20130315 - modified to add Civil, Nautical, Astronomical twilight
 * times by Rob Prior <android@b4.ca>
 *
 * 2026 - replaced jSunTimes with SolarCalculator (NOAA solar position equations), shared with
 * the Wear OS companion app. See SolarCalculator's javadoc for accuracy notes.
 */

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;

import info.staticfree.android.twentyfourhour.lib.R;
import info.staticfree.android.twentyfourhour.solar.SolarCalculator;

public class SunPositionOverlay implements DialOverlay {
    private static final float HIGH_NOON_ARC_ANGLE = 2;
    private static final float DEGREE_CIRCLE = 360;

    private final RectF inset = new RectF();

    private double latitude;
    private double longitude;
    private boolean hasLocation;

    private static final Paint OVERLAY_NO_INFO_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final Paint OVERLAY_SUN = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint OVERLAY_SUNSET = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        OVERLAY_SUN.setARGB(127, 255, 201, 14); // Orange for Sun
        OVERLAY_SUN.setStyle(Paint.Style.FILL);

        OVERLAY_SUNSET.setARGB(20, 0, 0, 0); // Sunrise/Sunset
        OVERLAY_SUNSET.setStyle(Paint.Style.FILL);
    }

    private float scale = 0.5f;
    private boolean showTwilight = true;
    private boolean showHighNoon = true;

    public SunPositionOverlay(@NonNull Context context) {
        OVERLAY_NO_INFO_PAINT.setShader(new BitmapShader(BitmapFactory
                .decodeResource(context.getResources(), R.drawable.no_sunrise_sunset_tile),
                Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setShadeAlpha(int alpha) {
        OVERLAY_SUNSET.setAlpha(alpha);
    }

    public void setShowTwilight(boolean showTwilight) {
        this.showTwilight = showTwilight;
    }

    public void setShowHighNoon(boolean showHighNoon) {
        this.showHighNoon = showHighNoon;
    }

    public void setLocation(@Nullable Location location) {
        hasLocation = location != null;

        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    /**
     * @return the {@link Canvas#drawArc} angle (0 = 3 o'clock, clockwise) for an hour-of-day.
     * {@link HandsOverlay#getHourHandAngle} uses a 12-o'clock-relative convention instead (as does
     * the hand image itself), hence the 270 degree (-90 degree) shift between the two. Takes the
     * fractional, possibly out-of-range hours-of-day that a {@link SolarCalculator.Interval} can
     * have (negative, or beyond 24, when the interval crosses midnight).
     */
    private static float getHourArcAngle(double hourOfDay) {
        double handAngle = (hourOfDay + 12) % 24;

        if (handAngle < 0) {
            handAngle += 24;
        }

        return (float) ((handAngle * 15 + 270) % DEGREE_CIRCLE);
    }

    private void drawPlaceholder(@NonNull Canvas canvas) {
        canvas.drawArc(inset, 0, DEGREE_CIRCLE / 2, true, OVERLAY_NO_INFO_PAINT);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, int cX, int cY, int w, int h,
            @NonNull Calendar calendar, boolean sizeChanged) {
        int insetW = (int) (w / 2.0f * scale);
        int insetH = (int) (h / 2.0f * scale);
        inset.set(cX - insetW, cY - insetH, cX + insetW, cY + insetH);

        if (!hasLocation) {
            // not much we can do if we don't have a location
            drawPlaceholder(canvas);

            return;
        }

        LocalDate date = LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
        ZoneId zone = calendar.getTimeZone().toZoneId();

        SolarCalculator.Interval daylight =
                SolarCalculator.compute(date, zone, latitude, longitude, SolarCalculator.ALTITUDE_DAYLIGHT);

        drawInsetArc(canvas, getHourArcAngle(daylight.end), getHourArcAngle(daylight.start),
                OVERLAY_SUNSET);

        if (showTwilight) {
            drawTwilightArc(canvas, date, zone, SolarCalculator.ALTITUDE_CIVIL_TWILIGHT);
            drawTwilightArc(canvas, date, zone, SolarCalculator.ALTITUDE_NAUTICAL_TWILIGHT);
            drawTwilightArc(canvas, date, zone, SolarCalculator.ALTITUDE_ASTRONOMICAL_TWILIGHT);
        }

        if (showHighNoon && !daylight.isAlwaysBelow()) {
            float highNoon = getHourArcAngle(daylight.noon);
            canvas.drawArc(inset, highNoon - HIGH_NOON_ARC_ANGLE / 2, HIGH_NOON_ARC_ANGLE, true,
                    OVERLAY_SUN);
        }
    }

    private void drawTwilightArc(@NonNull Canvas canvas, @NonNull LocalDate date,
            @NonNull ZoneId zone, double altitude) {
        SolarCalculator.Interval interval =
                SolarCalculator.compute(date, zone, latitude, longitude, altitude);

        drawInsetArc(canvas, getHourArcAngle(interval.end), getHourArcAngle(interval.start),
                OVERLAY_SUNSET);
    }

    private void drawInsetArc(@NonNull Canvas canvas, float startAngle, float endAngle,
            @NonNull Paint paint) {
        canvas.drawArc(inset, startAngle, (DEGREE_CIRCLE + (endAngle - startAngle)) % DEGREE_CIRCLE,
                true, paint);
    }
}
