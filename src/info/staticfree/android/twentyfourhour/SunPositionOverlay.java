package info.staticfree.android.twentyfourhour;

import info.staticfree.android.twentyfourhour.Analog24HClock.DialOverlay;

import java.util.Calendar;
import java.util.TimeZone;

import uk.me.jstott.coordconv.LatitudeLongitude;
import uk.me.jstott.sun.Sun;
import uk.me.jstott.sun.Time;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;

public class SunPositionOverlay implements DialOverlay {

	private final LocationManager mLm;

	private final RectF inset = new RectF();
	private final LatitudeLongitude ll = new LatitudeLongitude(0,0);
	private static Paint OVERLAY_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG) ;

	static {
		OVERLAY_PAINT.setARGB(50, 0, 0, 0);
		OVERLAY_PAINT.setStyle(Paint.Style.FILL);
	}

	public SunPositionOverlay(Context context){
		mLm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	private Location getRecentLocation(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO){
			return mLm.getLastKnownLocation("passive");

		}else{
			Location bestLoc = null;
			long mostRecent = 0;
			for (final String p : mLm.getProviders(false)){
				final Location l = mLm.getLastKnownLocation(p);
				if (l == null){
					continue;
				}
				final long fixTime = l.getTime();
				if (bestLoc == null){
					bestLoc = l;
					mostRecent = fixTime;
				}else{
					if (fixTime > mostRecent){
						bestLoc = l;
						mostRecent = fixTime;
					}
				}
			}
			return bestLoc;
		}
	}

	private float getHourArcAngle(int h, int m){
		return (Analog24HClock.getHourHandAngle(h, m) + 270) % 360.0f;
	}

	@Override
	public void onDraw(Canvas canvas, int cX, int cY, int w, int h, Calendar calendar) {
		final Location loc = getRecentLocation();
		final int insetW = (int) (w / 2.0f / 2.0f);
		final int insetH = (int) (h / 2.0f / 2.0f);
		inset.set(cX - insetW, cY - insetH, cX + insetW, cY + insetH);

		if (loc == null){
			// not much we can do if we don't have a location
			canvas.drawArc(inset, 0, 180, true, OVERLAY_PAINT);
			return;
		}
		ll.setLatitude(loc.getLatitude());
		ll.setLongitude(loc.getLongitude());

		final boolean dst = false; // TODO where can one get this info?
		final TimeZone tz = calendar.getTimeZone();

		final Time sunrise = Sun.sunriseTime(calendar, ll, tz, dst);
		final float sunriseAngle = getHourArcAngle(sunrise.getHours(), sunrise.getMinutes());

		final Time sunset = Sun.sunsetTime(calendar, ll, tz, dst);
		final float sunsetAngle = getHourArcAngle(sunset.getHours(), sunset.getMinutes());

		canvas.drawArc(inset, sunsetAngle, (360 + (sunriseAngle - sunsetAngle)) % 360, true, OVERLAY_PAINT);
	}
}
