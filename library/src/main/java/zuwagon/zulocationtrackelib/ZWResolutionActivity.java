package zuwagon.zulocationtrackelib;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.AlertDialog;
import android.util.Log;

import static zuwagon.zulocationtrackelib.Constants.TAG;

/**
 * Activity, which maintains location flow dialogs, such as permission request or hardware settings adjustment.
 */
public class ZWResolutionActivity extends Activity {

    public static final int RC_RESOLUTION = Activity.RESULT_FIRST_USER;
    public static final int RC_PERMISSIONS = Activity.RESULT_FIRST_USER + 1;

    private boolean shouldStartTracking = false;

    private static ZWResolutionActivity _curInstance = null;

    public static final boolean isRunning() {
        return _curInstance != null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ZWResolutionActivity.onCreate()");
        _curInstance = this;

        Intent args = getIntent();

        int option = args.getIntExtra("option", -1);

        switch (option) {
            case Constants.RESOLUTION_OPTION_HARDWARE: {
                PendingIntent pendingIntent = args.getParcelableExtra("resolution");
                try {
                    startIntentSenderForResult(pendingIntent.getIntentSender(), RC_RESOLUTION,
                            null, 0, 0, 0);
                } catch (Exception ignored) {
                    // Just ignore this impossible case
                }
                break;
            }
            case Constants.RESOLUTION_OPTION_PERMISSIONS: {
                shouldStartTracking = args.getBooleanExtra("start_tracking", false);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

//                    ContextWrapper cv = new ContextWrapper(this);
//                    cv.setTheme(R.style.Theme_AppCompat_Dialog);
//
//                    new AlertDialog.Builder(cv)
//                            .setIcon(R.drawable.ic_service_notify)
//                            .setMessage(Zuwagon._rationaleTextRes)
//                            .setPositiveButton(Zuwagon._rationalePositiveButtonRes, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//
//                                }
//                            })
//                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
//                                @Override
//                                public void onDismiss(DialogInterface dialog) {
//                                    requestPermissionAccessFineLocation();
//                                }
//                            })
//                            .show();


                } else {
                    requestPermissionAccessFineLocation();
                }
                break;
            }
            default: finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (_curInstance == this) _curInstance = null;
    }

    void requestPermissionAccessFineLocation() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                RC_PERMISSIONS
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RC_RESOLUTION) {
            if (resultCode == RESULT_OK) {
                Zuwagon.startTrack(this);
            } else {
                Zuwagon.postStatus(ZWStatus.HARDWARE_RESOLUTION_FAILED);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);

        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == RC_PERMISSIONS && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (shouldStartTracking) Zuwagon.startTrack(this);
        } else {
            Zuwagon.postStatus(ZWStatus.PERMISSION_REQUEST_FAILED);
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        finish();
    }
}
