package info.staticfree.android.twentyfourhour.overlay;

import android.graphics.Canvas;
import android.support.annotation.NonNull;

import java.util.Calendar;

/**
 * An overlay for a clock dial.
 *
 * @author <a href="mailto:steve@staticfree.info">Steve Pomeroy</a>
 */
public interface DialOverlay {

    /**
     * Subclasses should implement this to draw the overlay.
     *
     * @param canvas the canvas onto which you must draw
     * @param cX the x coordinate of the center
     * @param cY the y coordinate of the center
     * @param w the width of the canvas
     * @param h the height of the canvas
     * @param calendar the desired date/time
     */
    void onDraw(@NonNull Canvas canvas, int cX, int cY, int w, int h, @NonNull Calendar calendar,
            boolean sizeChanged);
}
