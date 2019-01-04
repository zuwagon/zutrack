package zuwagon.zulocationtrackelib;

import android.content.Context;

/**
 * Framework status codes.
 */
public class ZWStatus {

    /**
     * No status.
     */
    public static final int NONE = -1;

    /**
     * Indicates that service started successfully
     */
    public static final int SERVICE_STARTED = 0;

    /**
     * Indicates that specified incorrect location request parameters.
     * This value is exceptionally rare, in case Google change something so extremely and
     * current settings will become incompatible.
     */
    public static final int INCORRECT_LOCATION_REQUEST_PARAMETERS = 1;

    /**
     * Framework requested phone settings adjustment from user and that request was rejected by user.
     */
    public static final int HARDWARE_RESOLUTION_FAILED = 2;

    /**
     * Framework requested permission to use location services from user and user has prohibited that action.
     */
    public static final int PERMISSION_REQUEST_FAILED = 3;

    /**
     * Service stopped by user ({@link Zuwagon#stopTrack(Context) Zuwagon.stopTrack} called).
     */
    public static final int SERIVCE_STOPPED = 4;

    /**
     * Http request by {@link ZWSendLocationHttpRequest ZWSendLocationHttpRequest} failed.
     */
    public static final int HTTP_REQUEST_FAILED = 5;

    /**
     * Warning, when no location data received long time.
     */
    public static final int WARNING_NO_LOCATION_LONG_TIME = 6;

    /**
     * Message about location feed restored. Revoking {@link #WARNING_NO_LOCATION_LONG_TIME WARNING_NO_LOCATION_LONG_TIME}.
     */
    public static final int LOCATION_RESTORED = 7;
}
