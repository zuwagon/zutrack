package zuwagon.zulocationtrackersample;

import android.app.Application;
import android.content.Context;

import zuwagon.zulocationtrackelib.Zuwagon;

public class App extends Application {

    private static Context _appContext;

    public static final Context appContext() {
        return _appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        _appContext = getApplicationContext();

        // Framework initialization. Should be done once, before Zuwagon.startTrack called.
        Zuwagon.configure(getApplicationContext(),
                83838383,
                "sample-auth-token",
                0,
                "Channel title",
                "Notification title",
                "Notification text",
                "Notification ticker",
                0, 0
                );

        // Add any location processors you want

        // Http send implementation.
        // You should check ExampleSendLocationHttpRequest and uncomment following string.
        // Zuwagon.addLocationProcessor(new ExampleSendLocationHttpRequest("my user id", "my api key"));

        // Just shows toast
        Zuwagon.addLocationProcessor(new ExampleProcessLocationCallback());
    }
}
