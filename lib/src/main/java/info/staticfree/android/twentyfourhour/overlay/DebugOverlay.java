package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import java.util.Calendar;

public class DebugOverlay implements DialOverlay {
    private final Paint p;
    private final Paint p2;

    public DebugOverlay() {
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.CYAN);
        p.setAlpha(127);

        p2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        p2.setColor(Color.RED);
        p2.setAlpha(127);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, int cX, int cY, int w, int h,
            @NonNull Calendar calendar, boolean sizeChanged) {

        canvas.drawRect(cX - w / 2, cY - h / 2, cX + w / 2, cY + h / 2, p);
        canvas.drawCircle(cX, cY, 50, p2);
    }
}
