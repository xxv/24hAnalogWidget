package info.staticfree.android.twentyfourhour.overlay;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.util.Calendar;

import info.staticfree.android.twentyfourhour.lib.R;

public class HandsOverlay implements DialOverlay {
    private final Drawable hour;
    private final Drawable minute;

    private float mHourRot;
    private float mMinRot;
    private boolean mShowSeconds;
    private final boolean useLargeFace;

    public HandsOverlay(Context context, boolean useLargeFace) {
        Resources r = context.getResources();

        this.useLargeFace = useLargeFace;

        hour = r.getDrawable(this.useLargeFace ? R.drawable.hour_hand_large : R.drawable.hour_hand);
        minute = r.getDrawable(
                this.useLargeFace ? R.drawable.minute_hand_large : R.drawable.minute_hand);
    }

    public HandsOverlay(@NonNull Drawable hourHand, @NonNull Drawable minuteHand) {
        useLargeFace = false;

        hour = hourHand;
        minute = minuteHand;
    }

    public HandsOverlay(@NonNull Context context, int hourHandRes, int minuteHandRes) {
        Resources r = context.getResources();

        useLargeFace = false;

        hour = r.getDrawable(hourHandRes);
        minute = r.getDrawable(minuteHandRes);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, int cX, int cY, int width, int height,
            @NonNull Calendar calendar, boolean sizeChanged) {

        updateHands(calendar);

        drawHand(canvas, minute, mMinRot, cX, cY, sizeChanged);
        drawHand(canvas, hour, mHourRot, cX, cY, sizeChanged);
    }

    private void drawHand(@NonNull Canvas canvas, @NonNull Drawable hand, float angle, int cX,
            int cY, boolean sizeChanged) {
        canvas.save();
        canvas.rotate(angle, cX, cY);

        if (sizeChanged) {
            setDrawableBounds(hand, cX, cY);
        }
        hand.draw(canvas);
        canvas.restore();
    }

    private void setDrawableBounds(@NonNull Drawable drawable, int cX, int cY) {
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        drawable.setBounds(cX - (w / 2), cY - (h / 2), cX + (w / 2), cY + (h / 2));
    }

    public void setShowSeconds(boolean showSeconds) {
        mShowSeconds = showSeconds;
    }

    private void updateHands(Calendar calendar) {
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        int m = calendar.get(Calendar.MINUTE);
        int s = calendar.get(Calendar.SECOND);

        mHourRot = getHourHandAngle(h, m);
        mMinRot = (m / 60.0f) * 360 + (mShowSeconds ? ((s / 60.0f) * 360 / 60.0f) : 0);
    }

    public static float getHourHandAngle(int h, int m) {
        return ((12 + h) / 24.0f * 360) % 360 + (m / 60.0f) * 360 / 24.0f;
    }
}
