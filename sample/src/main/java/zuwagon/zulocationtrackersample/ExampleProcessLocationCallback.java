package zuwagon.zulocationtrackersample;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import zuwagon.zulocationtrackelib.ZWProcessLocationCallback;

public class ExampleProcessLocationCallback implements ZWProcessLocationCallback {

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onNewLocation(final Location newLocation) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(App.appContext(), "New location: " + newLocation, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
