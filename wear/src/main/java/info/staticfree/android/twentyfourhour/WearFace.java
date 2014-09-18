package info.staticfree.android.twentyfourhour;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;

import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;

public class WearFace extends Activity {
    private final static IntentFilter INTENT_FILTER;

    static {
        INTENT_FILTER = new IntentFilter();
        INTENT_FILTER.addAction(Intent.ACTION_TIME_TICK);
        INTENT_FILTER.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        INTENT_FILTER.addAction(Intent.ACTION_TIME_CHANGED);
    }

    private Analog24HClock mClock;
    private BroadcastReceiver mTimeInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mClock.setTime(System.currentTimeMillis());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_face);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mClock = (Analog24HClock) stub.findViewById(R.id.face);
                mClock.setShowNow(false);
                mTimeInfoReceiver.onReceive(WearFace.this, registerReceiver(null, INTENT_FILTER));    //  Here, we're just calling our onReceive() so it can set the current time.
                registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
                initializeClock();
            }
        });
    }

    private void initializeClock() {
        SunPositionOverlay sunPositionOverlay = new SunPositionOverlay(getApplicationContext());
        Location test = new Location("foo");
        test.setLatitude(42.4);
        test.setLongitude(-71.1);
        sunPositionOverlay.setLocation(test);
        mClock.addDialOverlay(sunPositionOverlay);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTimeInfoReceiver);
    }
}
