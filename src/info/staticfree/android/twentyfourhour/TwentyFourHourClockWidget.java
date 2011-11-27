package info.staticfree.android.twentyfourhour;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.widget.RemoteViews;

public class TwentyFourHourClockWidget extends AppWidgetProvider {

	/**
	 * Sending this broadcast intent will cause the clock widgets to update.
	 */
	public static String ACTION_CLOCK_UPDATE = "info.staticfree.android.twentyfourhour.ACTION_CLOCK_UPDATE";

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);

		startTicking(context);
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

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

		final Analog24HClock clock = new Analog24HClock(context);
		clock.setShowSeconds(false);

		clock.addDialOverlay(new SunPositionOverlay(context));

		final int s = clock.getSuggestedMinimumHeight();
		clock.measure(s, s);
		clock.layout(0, 0, s, s);

		final Bitmap bmp = Bitmap.createBitmap(s, s, Config.ARGB_8888);
		final Canvas c = new Canvas(bmp);
		clock.draw(c);

		final Bitmap immutable = Bitmap.createBitmap(bmp);
		bmp.recycle();

		rv.setImageViewBitmap(R.id.clock, immutable);

		final PendingIntent intent = ClockUtil.getClockIntent(context);
		if (intent != null){
			rv.setOnClickPendingIntent(R.id.clock, intent);
		}

		appWidgetManager.updateAppWidget(appWidgetIds, rv);
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
