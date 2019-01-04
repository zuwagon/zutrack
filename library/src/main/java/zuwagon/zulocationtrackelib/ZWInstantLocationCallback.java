package zuwagon.zulocationtrackelib;

import android.content.Context;
import android.location.Location;

import java.io.Serializable;

/**
 * Result receiver from {@link Zuwagon#instantLocation(Context, ZWInstantLocationCallback) instantLocation(Context, ZWInstantLocationCallback)}.
 */
public interface ZWInstantLocationCallback extends Serializable {

    int OK = 0;
    int PERMISSION_REQUEST_NEED = 1;
    int LOCATION_NOT_AWAILABLE = 2;

    /**
     * {@link Zuwagon#instantLocation(Context, ZWInstantLocationCallback) instantLocation(Context, ZWInstantLocationCallback)} sends result of operation.
     * @param result Following: {@link #OK OK}, {@link #PERMISSION_REQUEST_NEED PERMISSION_REQUEST_NEED} or {@link #LOCATION_NOT_AWAILABLE LOCATION_NOT_AWAILABLE}
     * @param location If result is {@link #OK OK}, current location will be provided, otherwise will be null.
     */
    void onResult(int result, Location location);
}
