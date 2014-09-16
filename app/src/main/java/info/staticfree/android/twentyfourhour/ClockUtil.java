package info.staticfree.android.twentyfourhour;

/*
 * Copyright (C) 2011 Steve Pomeroy <steve@staticfree.info>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class ClockUtil {

    /**
     * @param context
     * @return an intent that will show the native clock app
     * @see <a
     * href="http://stackoverflow.com/questions/3590955/intent-to-launch-the-clock-application-on-android">Stack
     * Overflow post</a>
     */
    public static final PendingIntent getClockIntent(Context context) {
        final Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN).addCategory(
                Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Verify clock implementation
        final String clockImpls[][] = {
                {"Alarm Klock", "com.angrydoughnuts.android.alarmclock",
                        "com.angrydoughnuts.android.alarmclock.ActivityAlarmClock"},

                {"HTC Alarm Clock", "com.htc.android.worldclock",
                        "com.htc.android.worldclock.WorldClockTabControl"},

                {"Standar Alarm Clock", "com.android.deskclock",
                        "com.android.deskclock.AlarmClock"},

                {"Froyo Nexus Alarm Clock", "com.google.android.deskclock",
                        "com.android.deskclock.DeskClock"},

                {"Moto Blur Alarm Clock", "com.motorola.blur.alarmclock",
                        "com.motorola.blur.alarmclock.AlarmClock"},

                {"Samsung Galaxy S", "com.sec.android.app.clockpackage",
                        "com.sec.android.app.clockpackage.ClockPackage"}

        };

        boolean foundClockImpl = false;

        final PackageManager packageManager = context.getPackageManager();

        for (int i = 0; i < clockImpls.length; i++) {
            // final String vendor = clockImpls[i][0];
            final String packageName = clockImpls[i][1];
            final String className = clockImpls[i][2];
            try {
                final ComponentName cn = new ComponentName(packageName, className);
                packageManager.getActivityInfo(cn, PackageManager.GET_META_DATA);
                alarmClockIntent.setComponent(cn);
                // debug("Found " + vendor + " --> " + packageName + "/" + className);
                foundClockImpl = true;
                break;
            } catch (final NameNotFoundException e) {
                // haven't found it yet
            }
        }

        if (foundClockImpl) {

            return PendingIntent.getActivity(context, 0, alarmClockIntent, 0);
        } else {
            return null;
        }
    }
}
