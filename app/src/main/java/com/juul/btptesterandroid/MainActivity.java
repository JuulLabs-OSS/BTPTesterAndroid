package com.juul.btptesterandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

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
    private static final int RECONNECT_TIMEOUT = 3000;

    WebSocket ws = null;
    TextView statusTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("TAG", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.statusText);
        statusTextView.setText("Disconnected");

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

    private void tryReconnect() {
        try {
            Thread.sleep(RECONNECT_TIMEOUT);
            ws = ws.recreate().connectAsynchronously();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    WebSocketAdapter adapter = new WebSocketAdapter() {
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
            Log.d("TAG", "onConnected");
            sendMessage();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) {
            Log.d("TAG", "onConnectError");
            tryReconnect();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
                                   WebSocketFrame clientCloseFrame, boolean closedByServer) {
            Log.d("TAG", "onDisconnected");
            tryReconnect();
        }

        @Override
        public void onTextMessage(WebSocket websocket, String message) {
            Log.d("TAG", "onTextMessage: " + message);
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) {
            Log.d("TAG", "onBinaryMessage");
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) {
            Log.d("TAG", "onUnexpectedError");
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
