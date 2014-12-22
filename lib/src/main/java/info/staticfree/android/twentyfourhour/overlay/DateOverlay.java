package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Calendar;
import java.util.Locale;

/**
 * An overlay that shows the date.
 */
public class DateOverlay implements DialOverlay {
    private final float mOffsetY;
    private final float mOffsetX;
    private float rectRatio = 1.61828f;
    private float textSize = 20;
    private float tomorrowScale = 0.5f;
    private RectF bgrect = new RectF();
    private RectF todayRect = new RectF();
    private Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Calendar mTomorrow = Calendar.getInstance();

    public DateOverlay(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mBgPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void onDraw(Canvas canvas, int cX, int cY, int w, int h, Calendar calendar, boolean sizeChanged) {
        todayRect.set(cX + mOffsetX, cY + mOffsetY, cX + mOffsetX + textSize * rectRatio, cX + mOffsetY + textSize);

        bgrect.set(todayRect);
        bgrect.inset(3, 3);
        bgrect.offset(todayRect.width() - 8, 0);
        // Tomorrow
        mTextPaint.setColor(Color.argb(96, 255, 255, 255));
        mBgPaint.setColor(Color.argb(40, 255, 255, 255));
        mTomorrow.setTime(calendar.getTime());

        mTomorrow.add(Calendar.DAY_OF_MONTH, 1);
        boolean showNextMonth = mTomorrow.get(Calendar.MONTH) != calendar.get(Calendar.MONTH);
        drawDay(canvas, mTomorrow, bgrect, mBgPaint, mTextPaint, showNextMonth);

        // Main date.
        mTextPaint.setColor(Color.argb(192, 255, 255, 255));
        mBgPaint.setColor(Color.argb(255, 80, 80, 80));

        drawDay(canvas, calendar, todayRect, mBgPaint, mTextPaint, true);
    }

    private void drawDay(Canvas canvas, Calendar when, RectF bg, Paint bgPaint, Paint textPaint, boolean showMonth) {
        drawTextRectBg(canvas, String.valueOf(when.get(Calendar.DAY_OF_MONTH)), bg, bgPaint, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);

        if (showMonth) {
            textPaint.setTextSize(bg.height() * 0.6f);
            textPaint.setColor(Color.argb(127, 255, 255, 255));
            canvas.drawText(when.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                    bg.left + bg.width() * 0.05f, bg.top, textPaint);
        }

    }

    private void drawTextRectBg(Canvas canvas, CharSequence text, RectF location, Paint bgPaint, Paint textPaint) {
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawRoundRect(location, 2f, 2f, bgPaint);
        textPaint.setTextSize(location.height());
        canvas.drawText(text, 0, text.length(), location.centerX(), location.bottom - location.height() * 0.15f, textPaint);
    }
}
