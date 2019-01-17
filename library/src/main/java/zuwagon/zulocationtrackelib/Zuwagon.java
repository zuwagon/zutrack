package zuwagon.zulocationtrackelib;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;

import static zuwagon.zulocationtrackelib.Constants.TAG;

/**
 * Entry library class. Purposed to configure and control location tracker.
 * All methods are static.
 */
public class Zuwagon {

    private static SharedPreferences _config;
    static Handler _uiHandler = new Handler(Looper.getMainLooper());

    static boolean _needServiceStarted = false;
    static int _notificationSmallIconRes = R.drawable.ic_service_notify;
    static String _notificationChannelTitle = null;
    static String _notificationTitle = null;
    static String _notificationText = null;
    static String _notificationTicker = null;
    public static String _riderId = null;
    public static String _apiKey = null;

    static int _rationaleTextRes = R.string.default_rationale_access_fine_location;
    static int _rationalePositiveButtonRes = R.string.default_rationale_positive_button;

    static boolean _needForegroundService = ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O );

    static ZWLocationService _currentServiceInstance = null;

    private static final ArrayList<ZWStatusCallback> statusCallbacks = new ArrayList<>();
    private static int lastStatus = ZWStatus.NONE;

    static final ArrayList<ZWProcessLocationCallback> processLocationCallbacks = new ArrayList<>();

    private static Thread.UncaughtExceptionHandler defaultGlobalExceptionHandler;



    /**
     * Initial framework configuration.
     * @param context Application or activity context.
     * @param notificationSmallIconRes Resource Id of small (24x24dp) notification icon. It will appear in foreground service notification and in rationale dialog.
     * @param notificationChannelTitle Notification channel title (Oreo and later).
     * @param notificationTitle Notification title.
     * @param notificationText Notification text.
     * @param notificationTicker Notification ticker. Can be null.
     * @param rationaleTextRes String resource id of location permissions rationale text.
     * @param rationalePositiveButtonRes String resource of 'OK/GOT IT' button.
     */
    public static final void configure(final Context context,
                                       String riderId,
                                       String apiKey,
                                       @DrawableRes int notificationSmallIconRes,
                                       String notificationChannelTitle,
                                       String notificationTitle,
                                       String notificationText,
                                       String notificationTicker,
                                       @StringRes int rationaleTextRes,
                                       @StringRes int rationalePositiveButtonRes
                                       )
    {
        _config = PreferenceManager.getDefaultSharedPreferences(context);
        _riderId = riderId;
        _apiKey = apiKey;
        _notificationSmallIconRes = notificationSmallIconRes != 0 ? notificationSmallIconRes : R.drawable.ic_service_notify;
        _notificationChannelTitle = notificationChannelTitle;
        _notificationTitle = notificationTitle;
        _notificationText = notificationText;
        _notificationTicker = notificationTicker;
        _rationaleTextRes = rationaleTextRes != 0 ? rationaleTextRes : R.string.default_rationale_access_fine_location;
        _rationalePositiveButtonRes = rationalePositiveButtonRes != 0 ? rationalePositiveButtonRes : R.string.default_rationale_positive_button;

        _needServiceStarted = config().getBoolean(Constants.CONFIG.NEED_SERVICE_STARTED, false);

        defaultGlobalExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();


        // Tracker self-stopping on uncaught exception and calls default exception handler
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
             @Override
             public void uncaughtException(Thread t, Throwable e) {
                 Log.d(TAG, "Stopping tracking on application exception.");
                 if (isTracking()) stopTrack(context);
                 defaultGlobalExceptionHandler.uncaughtException(t, e);
             }
         });
    }

    /**
     * Start tracking command. Launches tracking flow.
     * @param context Application or activity context.
     */
    public static final void startTrack(Context context) {
        setNeedServiceStarted(true);
        ZWLocationService.start(context);
        ZWAlarmReceiver.setupAlarm(context, Constants.RECREATE_SERVICE_ON_DESTROY_DELAY_MS);
        ZWSocket.connectToServer();
    }

    /**
     * Stop tracking command. Interrupts tracking flow.
     * @param context Application or activity context.
     */
    public static final void stopTrack(Context context) {
        setNeedServiceStarted(false);
        ZWAlarmReceiver.clearAlarm(context);
        ZWSocket.disconnectFromServer();
    }

    /**
     * Check tracking enabled and active.
     * @return true if tracking enabled and active.
     */
    public static final boolean isTracking() {
        return _currentServiceInstance != null && _needServiceStarted;
    }

    ///
    /// Status processing
    ///

    /**
     * Adds status receiver callback. Multiple callbacks can be added.
     * @param callback {@link ZWStatusCallback ZWStatusCallback} implementation
     * @param returnLastStatus true if need to return last status to current callback
     */
    public static final void addStatusCallback(final ZWStatusCallback callback, boolean returnLastStatus) {
        if (callback != null) {
            statusCallbacks.add(callback);
            if (returnLastStatus && lastStatus != ZWStatus.NONE) runStatusCallback(callback);
        }
    }

    /**
     * Removes status receiver callback from callbacks list.
     * @param callback {@link ZWStatusCallback ZWStatusCallback} reference to remove
     */
    public static final void removeStatusCallback(ZWStatusCallback callback) {
        statusCallbacks.remove(callback);
    }

    ///
    /// Received location processing
    ///

    /**
     * Adds location processor. Multiple callbacks can be added.
     * When new location data received from Fused Location Provider, all callbacks are triggered.
     * @param processLocationCallback {@link ZWProcessLocationCallback ZWProcessLocationCallback} implementation
     */
    public static final void addLocationProcessor(ZWProcessLocationCallback processLocationCallback) {
        if (!processLocationCallbacks.contains(processLocationCallback)) {
            processLocationCallbacks.add(processLocationCallback);
        }
    }

    /**
     * Removes location processor from list.
     * @param processLocationCallback {@link ZWProcessLocationCallback ZWProcessLocationCallback} reference to remove
     */
    public static final void removeLocationProcessor(ZWProcessLocationCallback processLocationCallback) {
        processLocationCallbacks.remove(processLocationCallback);
    }

    ///
    /// Getting current location
    ///

    /**
     * Getting current location from Fused Location Provider.
     * Permission checking will be performed if need.
     *
     * The location object may be null in the following situations:
     *
     * Location is turned off in the device settings. The result could be null even if the last location
     * was previously retrieved because disabling location also clears the cache.
     *
     * The device never recorded its location, which could be the case of a new device or a device that has
     * been restored to factory settings.
     *
     * Google Play services on the device has restarted, and there is no active Fused Location Provider
     * client that has requested location after the services restarted. To avoid this situation you can
     * create a new client and request location updates yourself.
     *
     * @param callback Receives location data and call status
     */
    public static final void instantLocation(final Context context, final ZWInstantLocationCallback callback) {

        // Checking location permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            callback.onResult(ZWInstantLocationCallback.PERMISSION_REQUEST_NEED, null);
        } else {
            final FusedLocationProviderClient client =
                    LocationServices.getFusedLocationProviderClient(context);
            client.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    try {
                        if (location != null) {
                            callback.onResult(ZWInstantLocationCallback.OK, location);
                        } else {
                            callback.onResult(ZWInstantLocationCallback.LOCATION_NOT_AWAILABLE, null);
                        }
                    } catch (Exception ignored) { }
                }
            });
        }

    }

    ///
    /// Internal methods.
    ///

    protected static final SharedPreferences config() {
        return _config;
    }

    static final void setNeedServiceStarted(boolean value) {
        _needServiceStarted = value;
        config().edit().putBoolean(Constants.CONFIG.NEED_SERVICE_STARTED, _needServiceStarted).commit();
    }

    static final void postStatus(final int code) {
        lastStatus = code;
        for (ZWStatusCallback callback : statusCallbacks) runStatusCallback(callback);
    }

    static final void runStatusCallback(final ZWStatusCallback callback) {
        if (callback != null) {
            _uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        callback.onStatus(lastStatus);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }
}
