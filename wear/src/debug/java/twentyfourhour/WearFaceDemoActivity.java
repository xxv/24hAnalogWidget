package twentyfourhour;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;

import java.util.Calendar;
import java.util.TimeZone;

import info.staticfree.android.twentyfourhour.Analog24HClock;
import info.staticfree.android.twentyfourhour.overlay.DateOverlay;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.wear.R;

public class WearFaceDemoActivity extends Activity {
    /**
     * This is scaled based on the background design.
     */
    public static final float SUN_POSITION_OVERLAY_SCALE = 0.61345f;


    private SunPositionOverlay mSunPositionOverlay;
    private Analog24HClock mClock;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_face);

        ((WatchViewStub) findViewById(R.id.watch_view_stub))
                .setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
                    @Override
                    public void onLayoutInflated(final WatchViewStub watchViewStub) {
                        mClock = (Analog24HClock) findViewById(R.id.face);
                        mSunPositionOverlay = new SunPositionOverlay(WearFaceDemoActivity.this);
                        initializeClock();
                        setDemoData();
                    }
                });
    }

    private void initializeClock() {
        mSunPositionOverlay.setScale(SUN_POSITION_OVERLAY_SCALE);
        mSunPositionOverlay.setShadeAlpha(60);
        mClock.addDialOverlay(mSunPositionOverlay);

        final DateOverlay dateOverlay = new DateOverlay(0.1875f, -0.1875f, 0.0625f);
        mClock.addDialOverlay(dateOverlay);
    }

    private void setDemoData() {
        final Calendar time = Calendar.getInstance(TimeZone.getTimeZone("America/Guatemala"));
        // A time that will put the hands at "10:10", is near the equinox,
        // and shows the month rollover.
        time.set(2014, 8, 30, 8, 10, 0);
        mClock.setTime(time);
        final Location location = new Location("demo");
        // Somewhere in Guatemala. This puts solar noon at actual noon.
        location.setLatitude(15.26);
        location.setLongitude(-92.13);
        mSunPositionOverlay.setLocation(location);
    }
}
