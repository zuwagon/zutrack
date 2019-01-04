package zuwagon.zulocationtrackelib;

import com.google.android.gms.location.LocationRequest;

public class Constants {

    /**
     * Http read timeout (ms)
     */
    public static final int HTTP_READ_TIMEOUT_MS = 5000;

    /**
     * Http connection timeout (ms)
     */
    public static final int HTTP_CONNECT_TIMEOUT_MS = 10000;

    /**
     * Http connection charset.
     */
    public static final String CHARSET = "UTF-8";

    /**
     * Interval (ms) of starting watchdog alarm receiver.
     */
    public static final long ALARM_RECEIVER_INTERVAL_MS = 60000;

    /**
     * Delay before recreating service after it dropped by system.
     * Intended for Android 7.0- and background services.
     */
    public static final long RECREATE_SERVICE_ON_DESTROY_DELAY_MS = 5000;

    /**
     * Interval from time of last location received, when framework starts to send warning status {@link ZWStatus#WARNING_NO_LOCATION_LONG_TIME WARNING_NO_LOCATION_LONG_TIME}
     */
    public static final long NO_LOCATION_WARNING_TIMEOUT_MS = 60000;

    /**
     * Interval between sending warning status {@link ZWStatus#WARNING_NO_LOCATION_LONG_TIME WARNING_NO_LOCATION_LONG_TIME}
     */
    public static final long NO_LOCATION_WARNING_INTERVAL_MS = 10000;

    /**
     * Maximum interval between location requests.
     */
    public static final long MAXIMUM_LOCATION_PROCESS_INTERVAL_MS = 30000;

    /**
     * Desired interval for active location updates.
     */
    public static final long DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 10000;

    /**
     * Fastest interval for location updates.
     */
    public static final long DEFAULT_LOCATION_UPDATE_FASTEST_INTERVAL_MS = 5000;

    /**
     * Location priority setting.
     */
    public static final int DEFAULT_LOCATION_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;


    //
    // Some internal constants which makes no behavior impact.
    //

    public static final int FOREGROUND_NOTIFICATION_ID = 1;

    public static final String FOREGROUND_NOTIFICATION_CHANNEL_ID = "zulocationtrackerlib_channel";

    public static final class CONFIG {
        public static final String NEED_SERVICE_STARTED = CONFIG.class.getName() + ".NEED_SERVICE_STARTED";
    }

    public static final int RESOLUTION_OPTION_HARDWARE = 1;
    public static final int RESOLUTION_OPTION_PERMISSIONS = 2;

    public static final String TAG = "zuwagon";
}
