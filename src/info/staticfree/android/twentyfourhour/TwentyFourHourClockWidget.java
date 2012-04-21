package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2011-2012 Steve Pomeroy <steve@staticfree.info>
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

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;

public class TwentyFourHourClockWidget extends AppWidgetProvider {

	private static final String TAG = TwentyFourHourClockWidget.class.getSimpleName();

	private Analog24HClock clock;

	/**
	 * Sending this broadcast intent will cause the clock widgets to update.
	 */
	public static String ACTION_CLOCK_UPDATE = "info.staticfree.android.twentyfourhour.ACTION_CLOCK_UPDATE";

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);

		startTicking(context);
		clock = new Analog24HClock(context);
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);

		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		alarmManager.cancel(createUpdate(context));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		final String action = intent.getAction();

		if (ACTION_CLOCK_UPDATE.equals(action) ||
				Intent.ACTION_TIME_CHANGED.equals(action) ||
				Intent.ACTION_TIMEZONE_CHANGED.equals(action)){
			final ComponentName appWidgets = new ComponentName(context.getPackageName(), getClass().getName());
			final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			final int ids[] = appWidgetManager.getAppWidgetIds(appWidgets);
			if (ids.length > 0){
				onUpdate(context, appWidgetManager, ids);
			}
		}
	}

	protected float getSize(Context context) {
		return 453f * getDisplayDensity(context);
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

		if (clock == null){
			clock = new Analog24HClock(context);
			clock.setShowSeconds(false);
			clock.addDialOverlay(new SunPositionOverlay(context));

			final int s = (int) getSize(context);
			clock.onSizeChanged(s, s, 0, 0);
			clock.measure(s, s);
			clock.layout(0, 0, s, s);
			Log.d(TAG, "size is " + s);
			clock.setDrawingCacheEnabled(true);

			final PendingIntent intent = ClockUtil.getClockIntent(context);
			if (intent != null){
				rv.setOnClickPendingIntent(R.id.clock, intent);
			}
		}

		final Bitmap cached = clock.getDrawingCache(true);
		if (cached != null){
			rv.setImageViewBitmap(R.id.clock, cached);
		}

		appWidgetManager.updateAppWidget(appWidgetIds, rv);
	}

	protected float getDisplayDensity(Context context) {
		final DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
				.getMetrics(dm);
		return dm.density;
	}

	/**
	 * Schedules an alarm to update the clock every minute, at the top of the minute.
	 *
	 * @param context
	 */
	private void startTicking(Context context){
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		// schedules updates so they occur on the top of the minute
		final Calendar c = Calendar.getInstance();
		c.setTimeInMillis(System.currentTimeMillis());
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		c.add(Calendar.MINUTE, 1);
		alarmManager.setRepeating(AlarmManager.RTC, c.getTimeInMillis(), 1000 * 60, createUpdate(context));
	}

	/**
	 * Creates an intent to update the clock(s).
	 *
	 * @param context
	 * @return
	 */
	private PendingIntent createUpdate(Context context){
		return PendingIntent.getBroadcast(context, 0,
				new Intent(ACTION_CLOCK_UPDATE), PendingIntent.FLAG_UPDATE_CURRENT);
	}
}
