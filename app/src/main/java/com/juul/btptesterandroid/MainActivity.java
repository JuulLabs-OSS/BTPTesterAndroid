package com.juul.btptesterandroid;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;

public class MainActivity extends AppCompatActivity {
    private Socket socket = null;
    private Activity activity = MainActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            socket = IO.socket("http://192.168.9.111:8765");

            socket.on(Socket.EVENT_CONNECT, onConnect);
            socket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            socket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.on("newMessage", newMessageListner);
//            socket.on(Manager.EVENT_TRANSPORT, onTransport);
            socket.connect();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

//    private Emitter.Listener onTransport = new Emitter.Listener() {
//        @Override
//        public void call(final Object... args) {
//            Log.d("TAG", "onTransport: " + Arrays.toString(args));
//
//            Transport transport = (Transport) args[0];
//            // Adding headers when EVENT_REQUEST_HEADERS is called
//            transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
//                @Override
//                public void call(Object... args) {
//                    Log.v("TAG", "Caught EVENT_REQUEST_HEADERS after EVENT_TRANSPORT, adding headers");
//                    Map<String, List<String>> mHeaders = (Map<String, List<String>>)args[0];
//                    mHeaders.put("Authorization", Arrays.asList("Basic bXl1c2VyOm15cGFzczEyMw=="));
//                }
//            });
//        }
//    };


    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("TAG", "onConnectError: " + Arrays.toString(args));
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (activity != null)
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d("TAG", "onDisconnect: " + Arrays.toString(args));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("TAG", "onConnect: " + Arrays.toString(args));
            sendMessage();
        }
    };

    private Emitter.Listener newMessageListner = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("TAG", "newMessageListner: " + Arrays.toString(args));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }

    public void sendMessage() {
        if (socket.connected()) {
            socket.emit("newMessage", "Message from Android!");
        }
    }
}
