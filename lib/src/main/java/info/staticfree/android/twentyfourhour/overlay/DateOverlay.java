package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Calendar;
import java.util.Locale;

/**
 * An overlay that shows the date. Above the numeric date is the current month abbreviation.
 * When tomorrow is a new month, this shows that too.
 */
public class DateOverlay implements DialOverlay {
    public static final float ROUNDED_RECT_RADIUS = 2f;
    private final float mOffsetY;
    private final float mOffsetX;
    private static final float RECT_RATIO = 1.61828f;
    private static final float TEXT_SIZE = 20;
    private RectF tomorrowRect = new RectF();
    private RectF todayRect = new RectF();
    private Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTomorrowBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTomorrowTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mMonthTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Calendar mTomorrow = Calendar.getInstance();

    /**
     * @param offsetX the x offset, in pixels, from the center.
     * @param offsetY the y offset, in pixels, from the center.
     */
    public DateOverlay(final float offsetX, final float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setColor(Color.argb(255, 80, 80, 80));

        mTextPaint.setTextSize(TEXT_SIZE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(Color.argb(192, 255, 255, 255));

        mTomorrowTextPaint.setTextSize(TEXT_SIZE);
        mTomorrowTextPaint.setTextAlign(Paint.Align.CENTER);
        mTomorrowTextPaint.setColor(Color.argb(96, 255, 255, 255));

        mTomorrowBgPaint.setStyle(Paint.Style.FILL);
        mTomorrowBgPaint.setColor(Color.argb(40, 255, 255, 255));

        mMonthTextPaint.setTextAlign(Paint.Align.LEFT);
        mMonthTextPaint.setTextSize(TEXT_SIZE * 0.6f);
        mMonthTextPaint.setColor(Color.argb(127, 255, 255, 255));
    }

    @Override
    public void onDraw(Canvas canvas, int cX, int cY, int w, int h, Calendar calendar,
                       boolean sizeChanged) {
        todayRect.set(cX + mOffsetX, cY + mOffsetY, cX + mOffsetX + TEXT_SIZE * RECT_RATIO,
                cX + mOffsetY + TEXT_SIZE);
        // Tomorrow
        mTomorrow.setTime(calendar.getTime());
        mTomorrow.add(Calendar.DAY_OF_MONTH, 1);

        boolean showNextMonth = mTomorrow.get(Calendar.MONTH) != calendar.get(Calendar.MONTH);

        // Under-draw tomorrow
        if (showNextMonth) {
            tomorrowRect.set(todayRect);
            tomorrowRect.inset(3, 3);
            tomorrowRect.offset(todayRect.width() - 8, 0);
            drawDay(canvas, mTomorrow, tomorrowRect, mTomorrowBgPaint, mTomorrowTextPaint, true);
        }

        // Main date.
        drawDay(canvas, calendar, todayRect, mBgPaint, mTextPaint, true);
    }

    private void drawDay(final Canvas canvas, final Calendar when, final RectF bg,
                         final Paint bgPaint, final Paint textPaint, final boolean showMonth) {
        drawTextRectBg(canvas, String.valueOf(when.get(Calendar.DAY_OF_MONTH)), bg, bgPaint,
                textPaint);

        if (showMonth) {
            canvas.drawText(
                    when.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()),
                    bg.left + bg.width() * 0.05f, bg.top, mMonthTextPaint);
        }

    }

    /**
     * Draws the given text with a rounded rectangle background.
     *
     * @param canvas    the canvas to draw on.
     * @param text      the text to draw.
     * @param bgSize    the size and position of the rectangle.
     * @param bgPaint   the background color.
     * @param textPaint the text color and style; the text size will be adjusted.
     */
    private void drawTextRectBg(final Canvas canvas, final CharSequence text, final RectF bgSize,
                                final Paint bgPaint, final Paint textPaint) {
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawRoundRect(bgSize, ROUNDED_RECT_RADIUS, ROUNDED_RECT_RADIUS, bgPaint);
        textPaint.setTextSize(bgSize.height());
        canvas.drawText(text, 0, text.length(), bgSize.centerX(),
                bgSize.bottom - bgSize.height() * 0.15f, textPaint);
    }
}
