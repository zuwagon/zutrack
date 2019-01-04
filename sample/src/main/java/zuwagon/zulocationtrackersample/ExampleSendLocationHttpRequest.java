package zuwagon.zulocationtrackersample;

import android.location.Location;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import zuwagon.zulocationtrackelib.ZWSendLocationHttpRequest;


/**
 * Location HTTP Sender with certain settings.
 */
public class ExampleSendLocationHttpRequest extends ZWSendLocationHttpRequest {

    final String _userId;
    final String _apiKey;

    /**
     * Good place to set permanent parameters.
     * You can do it differently.
     * @param userId
     * @param apiKey
     */
    public ExampleSendLocationHttpRequest(String userId, String apiKey) {
        _userId = userId;
        _apiKey = apiKey;
    }

    @Override
    public String provideUrl(Location location) {
        return "http://mytrackingsite.org/user/location.php?lat=" + location.getLatitude() +
                "&lon=" + location.getLongitude();
    }

    @Override
    public String provideRequestMethod() {
        // Return request type
        return GET;
    }

    @Override
    public List<Pair<String, String>> provideHeaders() {

        // Add any headers need.

        ArrayList<Pair<String, String>> ret = new ArrayList<>();

        ret.add(new Pair<>("user_id", _userId));
        ret.add(new Pair<>("api_key", _apiKey));

        return ret;
    }

    // Since request type is GET, we're omitted optional provideContent() method implementation.

    @Override
    public boolean processResponse(int responseCode, String responseMessage, String responseContent) {
        // If response satisfies conditions, return true
        return responseCode == 200;
    }
}
