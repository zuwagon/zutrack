package zuwagon.zulocationtrackelib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static zuwagon.zulocationtrackelib.Constants.TAG;

/**
 * Boot receiver, which "watchdogs" {@link ZWAlarmReceiver ZWAlarmReceiver} started after boot.
 * For case of device reboot with tracking started.
 */
public class ZWBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "ZWBootReceiver.onReceive(..., " + intent + ")");
            if (Zuwagon._needServiceStarted) Zuwagon.startTrack(context);
        }
    }
}
