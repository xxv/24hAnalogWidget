package info.staticfree.android.twentyfourhour.sun;

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

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationText;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.NoDataComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.RangedValueComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

import info.staticfree.android.twentyfourhour.solar.SolarCalculator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Publishes the part of today when the sun is above some altitude as a RANGED_VALUE complication:
 *
 * <ul>
 * <li>min: hour of day the interval starts (e.g. sunrise), possibly negative</li>
 * <li>max: hour of day the interval ends (e.g. sunset), possibly over 24</li>
 * <li>value: hour of day of solar noon</li>
 * </ul>
 *
 * The 24h Analog watch face (Watch Face Format, so it can't compute this itself) turns these into
 * shaded wedges on its dial. A complication data source can't tell which slot it's filling, so
 * each altitude threshold is its own service, and the watch face uses a slot per threshold.
 */
public abstract class SunComplicationService extends ComplicationDataSourceService {
    private static final String TAG = SunComplicationService.class.getSimpleName();

    /** @return the sun altitude, in degrees, that this complication describes */
    protected abstract double getAltitude();

    /** @return a name for the interval, e.g. "Daylight" */
    @StringRes
    protected abstract int getLabel();

    @Override
    public void onComplicationRequest(@NonNull ComplicationRequest request,
            @NonNull ComplicationRequestListener listener) {
        LocationStore locationStore = new LocationStore(this);
        locationStore.refreshIfStale();

        double[] location = locationStore.get();
        ComplicationData data;

        if (location == null || request.getComplicationType() != ComplicationType.RANGED_VALUE) {
            data = new NoDataComplicationData();
        } else {
            data = createData(SolarCalculator.compute(LocalDate.now(), ZoneId.systemDefault(),
                    location[0], location[1], getAltitude()));
        }

        try {
            listener.onComplicationData(data);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not send complication data", e);
        }
    }

    @Nullable
    @Override
    public ComplicationData getPreviewData(@NonNull ComplicationType type) {
        if (type != ComplicationType.RANGED_VALUE) {
            return null;
        }

        // A nice equinox-ish day for the editor's preview.
        double halfDay = 6 - getAltitude() / 15;
        return createData(new SolarCalculator.Interval(12 - halfDay, 12, 12 + halfDay));
    }

    @NonNull
    private ComplicationData createData(@NonNull SolarCalculator.Interval interval) {
        String text;

        if (interval.isAlwaysBelow()) {
            text = getString(R.string.sun_always_below);
        } else if (interval.isAlwaysAbove()) {
            text = getString(R.string.sun_always_above);
        } else {
            text = formatHour(interval.start) + "–" + formatHour(interval.end);
        }

        ComplicationText label = new PlainComplicationText.Builder(getText(getLabel())).build();
        ComplicationText description = new PlainComplicationText.Builder(
                getString(R.string.sun_content_description, getText(getLabel()), text)).build();

        return new RangedValueComplicationData.Builder((float) interval.noon,
                (float) interval.start, (float) interval.end, description)
                .setTitle(label)
                .setText(new PlainComplicationText.Builder(text).build())
                .build();
    }

    @NonNull
    static String formatHour(double hour) {
        int minutes = (int) Math.round(hour * 60);
        minutes = Math.floorMod(minutes, 24 * 60);

        return String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60);
    }
}
