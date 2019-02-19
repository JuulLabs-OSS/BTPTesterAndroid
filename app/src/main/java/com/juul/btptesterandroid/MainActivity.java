package com.juul.btptesterandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    /**
     * The echo server on websocket.org.
     */
    private static final String SERVER = "ws://192.168.9.111:8765";

    /**
     * The timeout value in milliseconds for socket connection.
     */
    private static final int TIMEOUT = 5000;

    WebSocket ws = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a WebSocket factory and set 5000 milliseconds as a timeout
        // value for socket connection.
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(TIMEOUT);

        // Create a WebSocket. The timeout value set above is used.
        try {
            ws = factory.createSocket(SERVER);

            ws.addListener(adapter);

            ws.connectAsynchronously();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    WebSocketAdapter adapter = new WebSocketAdapter() {
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            Log.d("TAG", "onConnected");
            super.onConnected(websocket, headers);
            sendMessage();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            Log.d("TAG", "onConnectError");
            super.onConnectError(websocket, exception);
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            Log.d("TAG", "onDisconnected");
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            // Create a new WebSocket instance and connect to the same endpoint.
            ws = ws.recreate().connect();
        }

        @Override
        public void onTextMessage(WebSocket websocket, String message) {
            Log.d("TAG", "onTextMessage: " + message);
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            Log.d("TAG", "onBinaryMessage");
            super.onBinaryMessage(websocket, binary);
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            Log.d("TAG", "onUnexpectedError");
            super.onUnexpectedError(websocket, cause);
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
    }

    public void sendMessage() {
        if (ws.isOpen()) {
            ws.sendText("Message from Android!");
        }
    }
}
