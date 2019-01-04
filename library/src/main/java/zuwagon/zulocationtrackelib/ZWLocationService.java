package zuwagon.zulocationtrackelib;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static zuwagon.zulocationtrackelib.Constants.TAG;

/**
 * Location flow service. Main part of framework. 90% functionality is here.
 */
public class ZWLocationService extends Service {

    private Thread serviceThread = null;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private Location _lastLocation = null;
    private long _lastLocationProcessingTime = 0;

    private boolean _reportedNoGps = false;

    public static final void start(Context context) {
        Log.d(TAG, "ZWLocationService.start(...)");
        Intent serviceIntent = new Intent(context, ZWLocationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Zuwagon._needForegroundService) {
            Log.d(TAG, "startForegroundService");
            context.startForegroundService(serviceIntent);
        } else {
            Log.d(TAG, "startTrack");
            context.startService(serviceIntent);
        }
    }

    public static final void stop(Context context) {
        Intent serviceIntent = new Intent(context, ZWLocationService.class);
        context.stopService(serviceIntent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ZWLocationService.onCreate()");

        try {

            if (Zuwagon._needForegroundService) {
                Log.d(TAG, "ZWLocationService.startForeground(" + Constants.FOREGROUND_NOTIFICATION_ID + ", ...)");
                startForeground(Constants.FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
            } else {
                Log.d(TAG, "ZWLocationService.stopForeground(true)");
                stopForeground(true);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) throw new SecurityException();


            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            final LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(Constants.DEFAULT_LOCATION_UPDATE_INTERVAL_MS);
            locationRequest.setFastestInterval(Constants.DEFAULT_LOCATION_UPDATE_FASTEST_INTERVAL_MS);
            locationRequest.setPriority(Constants.DEFAULT_LOCATION_PRIORITY);

            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    Zuwagon.postStatus(ZWStatus.SERVICE_STARTED);
                }
            });

            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "Check location settings failure", e);

                    fusedLocationProviderClient = null;
                    Zuwagon.setNeedServiceStarted(false);
                    stopSelf();

                    if (e instanceof ResolvableApiException) {
                        PendingIntent pendingIntent = ((ResolvableApiException)e).getResolution();
                        Intent intent = new Intent(ZWLocationService.this, ZWResolutionActivity.class);
                        intent.putExtra("option", Constants.RESOLUTION_OPTION_HARDWARE);
                        intent.putExtra("resolution", pendingIntent);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            });

            Zuwagon._currentServiceInstance = this;

            // Happy end
            return;

        } catch (SecurityException sex) {
            Log.d(TAG, "SecurityException in onCreate", sex);

            Intent intent = new Intent(ZWLocationService.this, ZWResolutionActivity.class);
            intent.putExtra("option", Constants.RESOLUTION_OPTION_PERMISSIONS);
            intent.putExtra("start_tracking", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } catch (Exception ex) {
            Log.d(TAG, "Exception in onCreate", ex);

            fusedLocationProviderClient = null;
            Zuwagon.postStatus(ZWStatus.INCORRECT_LOCATION_REQUEST_PARAMETERS);
        }


        // Here we know that error obviously happened.
        // All error cases handled. It's time to end up.

        Zuwagon.setNeedServiceStarted(false);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "ZWLocationService.onStartCommand(" + intent + ", " + flags + ", " + startId);

        if (serviceThread == null) serviceThread = new ServiceThread();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ZWLocationService.onDestroy");

        if (serviceThread != null) {
            try {
                serviceThread.interrupt();
            } catch (Exception ignored) { }
        }

        if (fusedLocationProviderClient != null) {
            try {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            } catch (Exception ignored) { }
            fusedLocationProviderClient = null;
        }

        if (Zuwagon._needServiceStarted) {
            ZWAlarmReceiver.setupAlarm(this, Constants.RECREATE_SERVICE_ON_DESTROY_DELAY_MS);
        }

        if (Zuwagon._currentServiceInstance == this) Zuwagon._currentServiceInstance = null;

        Zuwagon.postStatus(ZWStatus.SERIVCE_STOPPED);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult == null) return;
            final Location location = locationResult.getLastLocation();

            if (_lastLocation == null || _lastLocation.getAltitude() != location.getAltitude() ||
                    _lastLocation.getLongitude() != location.getLongitude() ||
                    _lastLocationProcessingTime < System.currentTimeMillis() - Constants.MAXIMUM_LOCATION_PROCESS_INTERVAL_MS) {

                Log.d(TAG, "ZWLocationService$locationCallback.onLocationResult NEW location = " + location);

                _lastLocation = location;
                _lastLocationProcessingTime = System.currentTimeMillis();

                if (_reportedNoGps) {
                    Zuwagon.postStatus(ZWStatus.LOCATION_RESTORED);
                    _reportedNoGps = false;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (ZWProcessLocationCallback callback : Zuwagon.processLocationCallbacks) {
                            try {
                                callback.onNewLocation(location);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }).start();
            } else {
                Log.d(TAG, "ZWLocationService$locationCallback.onLocationResult TWICE location = " + location);
            }
        }
    };

    private Notification buildForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel nc = new NotificationChannel(
                    Constants.FOREGROUND_NOTIFICATION_CHANNEL_ID,
                    Zuwagon._notificationChannelTitle, NotificationManager.IMPORTANCE_DEFAULT);

            nm.createNotificationChannel(nc);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(this,
                Constants.FOREGROUND_NOTIFICATION_CHANNEL_ID);

        b.setOngoing(true);
        if (Zuwagon._notificationSmallIconRes != 0) b.setSmallIcon(Zuwagon._notificationSmallIconRes);
        if (Zuwagon._notificationTitle != null) b.setContentTitle(Zuwagon._notificationTitle);
        if (Zuwagon._notificationText != null) b.setContentText(Zuwagon._notificationText);
        if (Zuwagon._notificationTicker != null) b.setTicker(Zuwagon._notificationTicker);
        b.setOnlyAlertOnce(true);
        b.setPriority(NotificationCompat.PRIORITY_HIGH);

        return b.build();
    }


    class ServiceThread extends Thread {
        long threadId = System.currentTimeMillis();

        ServiceThread() {
            Log.d(TAG, "ZWLocationService$ServiceThread #" + threadId + " created");
            start();
        }

        @Override
        public void run() {

            try {
                while (true) {
                    // Watching if was no location updates long time and sends warning every Constants.NO_LOCATION_WARNING_INTERVAL_MS milliseconds.
                    if (!ZWResolutionActivity.isRunning() && _lastLocationProcessingTime <
                            System.currentTimeMillis() - Constants.NO_LOCATION_WARNING_TIMEOUT_MS) {

                        // Checking, if location permission removed
                        if (ActivityCompat.checkSelfPermission(ZWLocationService.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED) {

                            // Requesting permission without restarting service
                            Zuwagon._uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(ZWLocationService.this, ZWResolutionActivity.class);
                                    intent.putExtra("option", Constants.RESOLUTION_OPTION_PERMISSIONS);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                            });

                        } else {
                            if (!_reportedNoGps) {
                                Zuwagon.postStatus(ZWStatus.WARNING_NO_LOCATION_LONG_TIME);
                                _reportedNoGps = true;
                            }
                        }
                    }
                    Thread.sleep(Constants.NO_LOCATION_WARNING_INTERVAL_MS);
                }
            } catch (InterruptedException ignored) { }

            if (serviceThread == this) serviceThread = null;
            Log.d(TAG, "ZWLocationService$ServiceThread #" + threadId + " destroyed");
        }
    }

}
