package zuwagon.zulocationtrackelib;

import android.annotation.SuppressLint;
import android.location.Location;
import android.util.Log;
import android.util.Pair;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static zuwagon.zulocationtrackelib.Constants.TAG;

/**
 * Base class for implementing transferring location data over HTTP.
 */
public abstract class ZWSendLocationHttpRequest implements ZWProcessLocationCallback {

    /**
     * GET method specifier constant.
     */
    public static final String GET = "GET";

    /**
     * POST method specifier constant.
     */
    public static final String POST = "POST";


    /**
     * Implement this method to provide well-formed URL link to your backend.
     * @param location Last location received.
     * @return URL, which will be requested.
     */
    public abstract String provideUrl(Location location);

    /**
     * Implement this method to provide your HTTP request method required.
     * Only two options available: {@link #GET GET} and {@link #POST POST}.
     * @return GET or {@link #POST POST}. Any other value will cause request error.
     */
    public abstract String provideRequestMethod();

    /**
     * Implement this method to provide headers for your HTTP request.
     * @return List of header records.
     */
    public List<Pair<String, String>> provideHeaders() {
        return null;
    }

    /**
     * Provide content for {@link #POST POST} request. If method is {@link #GET GET}, result of this
     * function is ignored.
     * @return Content for {@link #POST POST} request.
     */
    public String provideContent() {
        return null;
    }

    /**
     * Implement this method to control, wheither request succeeded or not.
     * If false returned, {@link ZWStatus#HTTP_REQUEST_FAILED HTTP_REQUEST_FAILED} status will be fired.
     * @param responseCode HTTP response code
     * @param responseMessage HTTP response message
     * @param responseContent HTTP response content (may be null or empty)
     * @return true, if request is succeeded/, false otherwise.
     */
    public abstract boolean processResponse(int responseCode, String responseMessage, String responseContent);

    @Override
    public void onNewLocation(final Location newLocation) {

        new Thread(new Runnable() {
            @SuppressLint("AllowAllHostnameVerifier")
            @Override
            public void run() {

                boolean responseResult = false;

                try {
                    final String url = provideUrl(newLocation);

                    HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();

                    if (conn instanceof HttpsURLConnection) {
                        HttpsURLConnection sconn = (HttpsURLConnection) conn;
                        sconn.setHostnameVerifier(new AllowAllHostnameVerifier());
                    }

                    List<Pair<String, String>> headers = provideHeaders();
                    if (headers != null && headers.size() > 0) {
                        for(Pair<String, String> header : headers) {
                            if (header.first != null && header.second != null) {
                                conn.addRequestProperty(header.first, header.second);
                            }
                        }
                    }

                    conn.setReadTimeout(Constants.HTTP_READ_TIMEOUT_MS);
                    conn.setConnectTimeout(Constants.HTTP_CONNECT_TIMEOUT_MS);
                    conn.setUseCaches(false);
                    conn.setDoInput(true);

                    String requestMethod = provideRequestMethod();

                    conn.setRequestMethod(requestMethod);

                    if (POST.equals(requestMethod)) {
                        String content = provideContent();
                        if (content != null && !"".equals(content)) {
                            conn.setDoOutput(true);
                            OutputStream outputStream = conn.getOutputStream();
                            BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(outputStream, Constants.CHARSET));
                            writer.write(content);
                            writer.close();
                        }
                    } else if (GET.equals(requestMethod)) {
                        conn.getOutputStream().close();
                    } else throw new Exception("Wrong request method. POST or GET should be.");

                    int responseCode = conn.getResponseCode();
                    String responseMessage = conn.getResponseMessage();
                    StringBuilder sbResponseContent = new StringBuilder();

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String s;
                    while ((s = br.readLine()) != null) sbResponseContent.append(s);

                    responseResult = processResponse(responseCode, responseMessage, sbResponseContent.toString());

                } catch (Exception ex) {
                    Log.d(TAG, "Http request error", ex);
                }

                if (!responseResult) Zuwagon.postStatus(ZWStatus.HTTP_REQUEST_FAILED);
            }
        }).start();
    }
}
