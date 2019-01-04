package zuwagon.zulocationtrackelib;

import android.location.Location;

/**
 * This interface must be implemented by location listeners.
 */
public interface ZWProcessLocationCallback {
    /**
     * This event fires when new location received from Fused Location Provider.
     * @param newLocation Location data
     */
    void onNewLocation(Location newLocation);
}
