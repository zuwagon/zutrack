package zuwagon.zulocationtrackersample;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import zuwagon.zulocationtrackelib.ZWInstantLocationCallback;
import zuwagon.zulocationtrackelib.ZWProcessLocationCallback;
import zuwagon.zulocationtrackelib.ZWStatus;
import zuwagon.zulocationtrackelib.ZWStatusCallback;
import zuwagon.zulocationtrackelib.Zuwagon;

public class MainActivity extends AppCompatActivity {

    TextView tvStatus, tvLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tvStatus);
        tvLocation = findViewById(R.id.tvLocation);

        findViewById(R.id.bEnableService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Zuwagon.startTrack(MainActivity.this);
            }
        });

        findViewById(R.id.bDisableService).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Zuwagon.stopTrack(MainActivity.this);
            }
        });

        findViewById(R.id.bInstantLocation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Zuwagon.instantLocation(MainActivity.this, new ZWInstantLocationCallback() {
                    @Override
                    public void onResult(final int result, final Location location) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String text = "Unknown result: " + result;
                                switch (result) {
                                    case ZWInstantLocationCallback.OK: text = "Current location: " + location; break;
                                    case ZWInstantLocationCallback.PERMISSION_REQUEST_NEED: text = "Location permission request need"; break;
                                    case ZWInstantLocationCallback.LOCATION_NOT_AWAILABLE: text = "Location data not available"; break;
                                }

/*
The location object may be null in the following situations:

Location is turned off in the device settings. The result could be null even if the last location
was previously retrieved because disabling location also clears the cache.

The device never recorded its location, which could be the case of a new device or a device that has
been restored to factory settings.

Google Play services on the device has restarted, and there is no active Fused Location Provider
client that has requested location after the services restarted. To avoid this situation you can
create a new client and request location updates yourself. For more information, see Receiving
Location Updates.
*/

                                new AlertDialog.Builder(MainActivity.this)
                                        .setMessage(text)
                                        .setPositiveButton("GOT IT", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();

                            }
                        });
                    }
                });
            }
        });

//        findViewById(R.id.bCauseAppCrash).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Encouraging application crash with division by zero.
//                int a = 22 / 0;
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Add activity scope callbacks
        Zuwagon.addStatusCallback(zwStatusCallback, true);
        Zuwagon.addLocationProcessor(showLocationInTextView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove activity scope callbacks
        Zuwagon.removeStatusCallback(zwStatusCallback);
        Zuwagon.removeLocationProcessor(showLocationInTextView);
    }

    /**
     * Status listener implementation.
     */
    final ZWStatusCallback zwStatusCallback = new ZWStatusCallback() {
        @Override
        public void onStatus(int code) {

            int statusRes = R.string.zw_status_unknown;

            switch (code) {
                case ZWStatus.SERVICE_STARTED: statusRes = R.string.zw_status_service_started; break;
                case ZWStatus.INCORRECT_LOCATION_REQUEST_PARAMETERS: statusRes = R.string.zw_status_incorrect_request_parameters; break;
                case ZWStatus.HARDWARE_RESOLUTION_FAILED: statusRes = R.string.zw_status_hardware_resolution_failed; break;
                case ZWStatus.PERMISSION_REQUEST_FAILED: statusRes = R.string.zw_status_permission_request_failed; break;
                case ZWStatus.SERIVCE_STOPPED: statusRes = R.string.zw_status_service_stopped; break;
                case ZWStatus.HTTP_REQUEST_FAILED: statusRes = R.string.zw_status_http_request_failed; break;
                case ZWStatus.WARNING_NO_LOCATION_LONG_TIME: statusRes = R.string.zw_status_warning_no_location_long_time; break;
            }

            tvStatus.setText(statusRes);
            tvLocation.setText("");
        }
    };

    /**
     * The simplest location listener ever.
     */
    final ZWProcessLocationCallback showLocationInTextView = new ZWProcessLocationCallback() {
        @Override
        public void onNewLocation(final Location newLocation) {
            tvLocation.post(new Runnable() {
                @Override
                public void run() {
                    tvLocation.setText("" + newLocation);
                }
            });
        }
    };
}
