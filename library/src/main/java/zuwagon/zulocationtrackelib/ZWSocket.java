package zuwagon.zulocationtrackelib;


import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;


import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ZWSocket {
    public static Socket mSocket = null;
    private static boolean mSocketAuthenticated;
    private static final String SOCKET_TAG = "Socket-IO";

    private static final String authToken = "Bearer " + Zuwagon._apiKey;
    private static final String serverURL = "https://api.zuwagon.com";

    private static final String imei = Zuwagon._riderId;

    private static boolean _needPing = false; // This setting from socket control logic.

    public static final void connectToServer() {
        try {
            _needPing = true;
            if(mSocket == null || !mSocket.connected()) {
                mSocket = IO.socket(serverURL);
                initSocket();
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static final void initSocket() {
        mSocket.connect();
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onTimeOutError);
        mSocket.on("authenticated", onAuthenticated);
        mSocket.on("unauthorized", onAuthFailed);
    }


    public static final void disconnectFromServer() {
        try {
            _needPing = false;
//            sendHeartbeatUpdate(0);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000); // Interval in milliseconds
                        mSocket.disconnect();
                    } catch (Exception ex) {
                    }
                }
            }).start();
        } catch (Exception ignored) {}
    }

    public static void sendHeartbeatUpdate(int status) {
        JSONObject heartbeatData = new JSONObject();
        try {
            heartbeatData.put("_id", imei);
            heartbeatData.put("status", status);
//            if(mLocation != null) {
//                JSONObject c_loc = new JSONObject();
//                c_loc.put("lat", mLocation.getLatitude());
//                c_loc.put("lon", mLocation.getLongitude());
//                c_loc.put("speed", mLocation.getSpeed());
//                c_loc.put("time", mLocation.getTime());
//                heartbeatData.put("c_loc", c_loc);
//            }
            if(mSocket != null && mSocket.connected()) {
                Log.i(SOCKET_TAG, "heartbeatData " + heartbeatData.toString());
                mSocket.emit("heartbeat", heartbeatData);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static final void ping() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3*60*1000); // Interval in milliseconds
                    if(_needPing) ZWSocket.sendHeartbeatUpdate(1);
                } catch (Exception ex) { }

                if (_needPing) ping();
            }
        }).start();
    }


    static ZWProcessLocationCallback sendLocationToServer = new ZWProcessLocationCallback() {
        @Override
        public void onNewLocation(final Location newLocation) {
            JSONObject loc = new JSONObject();
            try {
                loc.put("lat", newLocation.getLatitude());
                loc.put("lon", newLocation.getLongitude());
                loc.put("speed", Math.round(newLocation.getSpeed()));
                loc.put("time", newLocation.getTime());

                JSONObject response = new JSONObject();
                response.put("_id", imei);
                response.put("location", loc);

                Log.i(SOCKET_TAG, " Loc " + response.toString());
                if(mSocket != null && mSocket.connected())
                    mSocket.emit("tcptrip", response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private static final Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            mSocketAuthenticated = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000); // Interval in milliseconds
                        if(_needPing && mSocketAuthenticated) connectToServer();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                    errSocketSnack.show();
//                    Log.i(SOCKET_TAG, "Disconnect for error " + args[0]);
//                }
//            });
        }
    };
    private static final Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            mSocketAuthenticated = false;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.i(SOCKET_TAG, "Reason for error " + args[0]);
//                }
//            });
        }
    };
    private static final Emitter.Listener onTimeOutError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            mSocketAuthenticated = false;
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Log.i(SOCKET_TAG, "Time Out for error " + args.toString());
//                }
//            });
        }
    };

    private static final Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i(SOCKET_TAG, "OnConnect emitting authentication " + mSocket.connected());

            if (mSocketAuthenticated) {
                Log.i(SOCKET_TAG, "Socket is already authenticated ");
                return;
            }
            JSONObject token = new JSONObject();
            try {
                token.put("token", authToken);
                token.put("scope", "x-order-tracking");
                token.put("imei", imei);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mSocket.emit("authentication", token);
            Zuwagon.addLocationProcessor(sendLocationToServer);
        }
    };
    private static final Emitter.Listener onAuthenticated = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            mSocketAuthenticated = true;
            Log.i(SOCKET_TAG, " onAuthenticated");
//            sendHeartbeatUpdate(1);
            ping();
        }
    };
    private static final Emitter.Listener onAuthFailed = new Emitter.Listener() {

        @Override
        public void call(Object... args) {
            mSocketAuthenticated = false;
            Log.i(SOCKET_TAG, " onAuthFailed");

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(getApplicationContext(),
//                            "Authentication Failed", Toast.LENGTH_LONG).show();
//
//                }
//            });
        }
    };
}
