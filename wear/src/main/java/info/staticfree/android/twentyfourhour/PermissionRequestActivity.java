package info.staticfree.android.twentyfourhour;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Locale;

public class PermissionRequestActivity extends Activity {
    private static final String TAG = PermissionRequestActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        maybeRequestPermissions();
    }

    public void maybeRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (PERMISSION_REQUEST_CODE == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permissions[i])) {
                    setUserWantsLocation(grantResults[i] == PackageManager.PERMISSION_GRANTED);
                }
            }

            finish();
        }
    }

    public void setUserWantsLocation(boolean userWantsLocation) {
        Log.d(TAG, String.format(Locale.US, "set user wants location: %s", userWantsLocation));

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putBoolean(WearFace.PREFS_USER_WANTS_LOCATION, userWantsLocation)
                .apply();
    }
}
