/*
 * Copyright (c) 2019 JUUL Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
