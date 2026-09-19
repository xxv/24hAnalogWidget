package info.staticfree.android.twentyfourhour.sun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Sun times are expressed in local wall-clock hours, so they need recomputing when the time zone
 * (e.g. travel) or clock changes rather than waiting for the next periodic update.
 */
public class TimeChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LocationStore.requestComplicationUpdates(context);
    }
}
