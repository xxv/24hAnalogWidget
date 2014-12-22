package info.staticfree.android.twentyfourhour;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;

import info.staticfree.android.twentyfourhour.overlay.DateOverlay;
import info.staticfree.android.twentyfourhour.overlay.HandsOverlay;
import info.staticfree.android.twentyfourhour.overlay.SunPositionOverlay;
import info.staticfree.android.twentyfourhour.wear.R;

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

                initializeClock(stub);
            }
        });
    }


    private void initializeClock(View stub) {
        mClock = (Analog24HClock) stub.findViewById(R.id.face);
        mTimeInfoReceiver.onReceive(WearFace.this, registerReceiver(null, INTENT_FILTER));    //  Here, we're just calling our onReceive() so it can set the current time.
        registerReceiver(mTimeInfoReceiver, INTENT_FILTER);
        SunPositionOverlay sunPositionOverlay = new SunPositionOverlay(getApplicationContext());
        Location test = new Location("foo");
        test.setLatitude(42.4);
        test.setLongitude(-71.1);
        sunPositionOverlay.setLocation(test);
        sunPositionOverlay.setScale(0.61345f);
        sunPositionOverlay.setShadeAlpha(60);
        mClock.addDialOverlay(sunPositionOverlay);

        DateOverlay dateOverlay = new DateOverlay(30,-30);
        mClock.addDialOverlay(dateOverlay);
        mClock.setHandsOverlay(new HandsOverlay(getApplicationContext(),
                R.drawable.round_hour_hand, R.drawable.round_minute_hand));
        mClock.setFace(R.drawable.round_clock_face);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTimeInfoReceiver);
    }
}
