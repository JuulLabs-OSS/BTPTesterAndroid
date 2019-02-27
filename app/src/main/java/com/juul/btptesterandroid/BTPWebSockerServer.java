package com.juul.btptesterandroid;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class BTPWebSockerServer extends WebSocketServer {

    private WebSocketServerAdapter adapter;

    public BTPWebSockerServer(int port, WebSocketServerAdapter adapter) {
        super(new InetSocketAddress(port));
        this.adapter = adapter;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        adapter.onOpen(conn, handshake);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        adapter.onClose(conn, code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        adapter.onMessage(conn, message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        adapter.onMessage(conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        adapter.onError(conn, ex);
    }

    @Override
    public void onStart() {
        adapter.onStart();
    }
}
