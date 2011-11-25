package info.staticfree.android.twentyfourhour;

import java.util.Calendar;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * A widget that displays the time as a 12-at-the-top 24 hour analog clock. By
 * default, it will show the current time in the current timezone. The displayed
 * time can be set using {@link #setTime(long)} and and
 * {@link #setTimezone(TimeZone)}.
 *
 * @author <a href="mailto:steve@staticfree.info">Steve Pomeroy</a>
 *
 */
public class Analog24HClock extends View {

	private long mTime;
	private boolean mShowNow = true;
	private boolean mShowSeconds = true;

	private static final int UPDATE_INTERVAL = 1000 * 15;

	private Calendar c;
	private Drawable mFace;
	private Drawable mHour;
	private Drawable mMinute;

	private float mHourRot;
	private float mMinRot;

	private boolean mKeepon = false;

	public Analog24HClock(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public Analog24HClock(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Analog24HClock(Context context) {
		super(context);

		init();
	}

	private void init() {
		mFace = getResources().getDrawable(R.drawable.clock_face_fixed_sunlight);
		mHour = getResources().getDrawable(R.drawable.hour_hand);
		mMinute = getResources().getDrawable(R.drawable.minute_hand);

		c = Calendar.getInstance();
	}

	/**
	 * Sets the currently displayed time in {@link System#currentTimeMillis()}
	 * time. This will clear {@link #setShowNow(boolean)}.
	 *
	 * @param time
	 *            the time to display on the clock
	 */
	public void setTime(long time) {
		mShowNow = false;

		mTime = time;
		updateHands();
		invalidate();
	}

	/**
	 * When set, the current time in the current timezone will be displayed.
	 *
	 * @param showNow
	 */
	public void setShowNow(boolean showNow) {
		mShowNow = showNow;
	}

	/**
	 * When set, the minute hand will move slightly based on the current number
	 * of seconds. If false, the minute hand will snap to the minute ticks.
	 * Note: there is no second hand, this only affects the minute hand.
	 *
	 * @param showSeconds
	 */
	public void setShowSeconds(boolean showSeconds) {
		mShowSeconds = showSeconds;
	}

	/**
	 * Sets the timezone to use when displaying the time.
	 *
	 * @param timezone
	 */
	public void setTimezone(TimeZone timezone) {
		c = Calendar.getInstance(timezone);
	}

	@Override
	protected void onAttachedToWindow() {
		mKeepon = true;
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		mKeepon = false;
		super.onDetachedFromWindow();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mShowNow) {
			mTime = System.currentTimeMillis();
			updateHands();

			if (mKeepon) {
				postInvalidateDelayed(UPDATE_INTERVAL);
			}
		}

		final int w = getWidth();
		final int h = getHeight();

		final int s = Math.min(w, h);
		final int l = (w - s) / 2;
		final int t = (h - s) / 2;
		final int r = s + l;
		final int b = s + t;
		mFace.setBounds(l, t, r, b);

		mFace.draw(canvas);

		canvas.save();
		canvas.rotate(mHourRot, w / 2, h / 2);
		mHour.setBounds(l, t, r, b);
		mHour.draw(canvas);
		canvas.restore();

		canvas.save();
		canvas.rotate(mMinRot, w / 2, h / 2);
		mMinute.setBounds(l, t, r, b);
		mMinute.draw(canvas);
		canvas.restore();
	}

	private void updateHands() {
		c.setTimeInMillis(mTime);

		final int h = c.get(Calendar.HOUR_OF_DAY);
		final int m = c.get(Calendar.MINUTE);
		final int s = c.get(Calendar.SECOND);

		mHourRot = ((12 + h) / 24.0f * 360) % 360 + (m / 60.0f) * 360 / 24.0f;
		mMinRot = (m / 60.0f) * 360
				+ (mShowSeconds ? ((s / 60.0f) * 360 / 60.0f) : 0);
	}
}
