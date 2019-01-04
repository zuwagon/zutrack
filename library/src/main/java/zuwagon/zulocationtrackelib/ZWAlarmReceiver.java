package zuwagon.zulocationtrackelib;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import static zuwagon.zulocationtrackelib.Constants.TAG;

/**
 * Alarm receiver, which "watchdogs" {@link ZWLocationService ZWLocationService} running.
 * It is completely internal.
 */
public class ZWAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ZWAlarmReceiver.onReceive(..., " + intent + ") " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(System.currentTimeMillis())));

        if (Zuwagon._needServiceStarted) {
            ZWLocationService.start(context);
            setupAlarm(context, Constants.ALARM_RECEIVER_INTERVAL_MS);
        }
    }

    protected static final void setupAlarm(Context context, long delayMs) {
        Log.d(TAG, "ZWAlarmReceiver.setupAlarm(...)");
        Intent intent = new Intent(context, ZWAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Log.d(TAG, "alarmManager = " + alarmManager);
            alarmManager.cancel(pendingIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delayMs,
                        pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delayMs,
                        pendingIntent);
            }
        }
    }

    protected static final void clearAlarm(Context context) {
        Log.d(TAG, "ZWAlarmReceiver.stop(...)");
        ZWLocationService.stop(context);
        Intent intent = new Intent(context, ZWAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
    }
}
