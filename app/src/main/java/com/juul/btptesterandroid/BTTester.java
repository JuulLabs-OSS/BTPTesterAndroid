package com.juul.btptesterandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.juul.btptesterandroid.BTP.BTP_INDEX_NONE;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_CORE;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_GAP;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_GATT;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_FAILED;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_SUCCESS;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_UNKNOWN_CMD;
import static com.juul.btptesterandroid.BTP.CORE_EV_IUT_READY;
import static com.juul.btptesterandroid.BTP.CORE_READ_SUPPORTED_COMMANDS;
import static com.juul.btptesterandroid.BTP.CORE_READ_SUPPORTED_SERVICES;
import static com.juul.btptesterandroid.BTP.CORE_REGISTER_SERVICE;
import static com.juul.btptesterandroid.BTP.CORE_UNREGISTER_SERVICE;

public class BTTester {
    private static final int PORT = 8765;
    private WebSocketServer wsServer = null;
    private WebSocket socket = null;

    private Context context;
    private BluetoothAdapter bleAdapter;
    private BluetoothManager bleManager;
    private BleConnectionManager bleConnectionManager;

    private GAP gap = null;

    /*****************************************************************************************/

    public BTTester(Context context, BluetoothAdapter bleAdapter, BluetoothManager bleManager) {
        this.context = context;
        this.bleAdapter = bleAdapter;
        this.bleManager = bleManager;
    }

    public void init() {
        wsServer = new BTPWebSockerServer(PORT, adapter);
        wsServer.setReuseAddr(true);
        wsServer.start();
    }

    private WebSocketServerAdapter adapter = new WebSocketServerAdapter() {
        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            Log.d("TAG", "onOpen");
            if (socket == null) {
                socket = conn;
                sendMessage(BTP_SERVICE_ID_CORE, CORE_EV_IUT_READY, BTP_INDEX_NONE, null);
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            Log.d("TAG", "onClose");
            if (socket == conn) {
                cleanup();
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            Log.d("TAG", "onStringMessage");
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            Log.d("TAG", "onByteBufferMessage");
            if (socket == conn) {
                messageHandler(message.array());
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            Log.d("TAG", "onError");
            Log.d("TAG", ex.getMessage());
            Log.d("TAG", Arrays.toString(ex.getStackTrace()));
            // some errors like port binding failed may not be assignable to a specific websocket
            if (conn == null) {
                socket = null;
            } else {
                conn.close();
            }
        }

        @Override
        public void onStart() {
            Log.d("TAG", "onStart");
        }
    };

    public void supportedCommands(ByteBuffer data) {
        byte supported = 0;
        supported = Utils.setBit(supported, CORE_READ_SUPPORTED_COMMANDS);
        supported = Utils.setBit(supported, CORE_READ_SUPPORTED_SERVICES);
        supported = Utils.setBit(supported, CORE_REGISTER_SERVICE);
        supported = Utils.setBit(supported, CORE_UNREGISTER_SERVICE);

        sendMessage(BTP_SERVICE_ID_CORE, CORE_READ_SUPPORTED_COMMANDS, BTP_INDEX_NONE,
                new byte[]{supported});
    }

    public void supportedServices(ByteBuffer data) {
        byte supported = 0;

        supported = Utils.setBit(supported, BTP_SERVICE_ID_CORE);
        supported = Utils.setBit(supported, BTP_SERVICE_ID_GAP);
        supported = Utils.setBit(supported, BTP_SERVICE_ID_GATT);

        sendMessage(BTP_SERVICE_ID_CORE, CORE_READ_SUPPORTED_SERVICES, BTP_INDEX_NONE,
                new byte[]{supported});
    }

    public void registerService(ByteBuffer data) {
        byte id = data.get();
        byte status;

        switch (id) {
            case BTP_SERVICE_ID_GAP:
                if (gap != null) {
                    status = BTP_STATUS_FAILED;
                } else {
                    gap = new GAP();
                    status = gap.init(context, this, bleAdapter, bleManager);
                }
                break;
            case BTP_SERVICE_ID_GATT:
                status = BTP_STATUS_SUCCESS;
                break;
            default:
                status = BTP_STATUS_FAILED;
                break;
        }

        response(BTP_SERVICE_ID_CORE, CORE_REGISTER_SERVICE, BTP_INDEX_NONE, status);
    }

    public void unregisterService(ByteBuffer data) {
        byte id = data.get();
        byte status;

        switch (id) {
            case BTP_SERVICE_ID_GAP:
                if (gap == null) {
                    status = BTP_STATUS_FAILED;
                } else {
                    status = gap.unregister();
                    gap = null;
                }
                break;
            case BTP_SERVICE_ID_GATT:
                status = BTP_STATUS_SUCCESS;
                break;
            default:
                status = BTP_STATUS_FAILED;
                break;
        }

        response(BTP_SERVICE_ID_CORE, CORE_UNREGISTER_SERVICE, BTP_INDEX_NONE, status);
    }

    public void handleCore(byte opcode, byte index, ByteBuffer data) {
        if (index != BTP_INDEX_NONE) {
            response(BTP_SERVICE_ID_CORE, opcode, index, BTP_STATUS_FAILED);
            return;
        }

        switch (opcode) {
            case CORE_READ_SUPPORTED_COMMANDS:
                supportedCommands(data);
                break;
            case CORE_READ_SUPPORTED_SERVICES:
                supportedServices(data);
                break;
            case CORE_REGISTER_SERVICE:
                registerService(data);
                break;
            case CORE_UNREGISTER_SERVICE:
                unregisterService(data);
                break;
            default:
                response(BTP_SERVICE_ID_CORE, opcode, BTP_INDEX_NONE, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }

    public void messageHandler(byte[] bytes) {
        Log.d("TAG", String.format("messageHandler %s", Utils.bytesToHex(bytes)));
        BTPMessage msg = BTPMessage.parse(bytes);

        if (msg == null) {
            /* Ignore wrongly formatted message */
            return;
        }

        switch (msg.service) {
            case BTP_SERVICE_ID_CORE:
                handleCore(msg.opcode, msg.index, msg.data);
                break;
            case BTP_SERVICE_ID_GAP:
                gap.handleGAP(msg.opcode, msg.index, msg.data);
                break;
            case BTP_SERVICE_ID_GATT:
                gap.handleGATT(msg.opcode, msg.index, msg.data);
                break;
            default:
                response(msg.service, msg.opcode, msg.index, BTP_STATUS_FAILED);
        }
    }

    public void sendMessage(byte service, byte opcode, byte index, byte[] data) {
        Log.d("TAG", String.format("sendMessage 0x%02x 0x%02x 0x%02x %s",
                service, opcode, index, Utils.bytesToHex(data)));
        BTPMessage message = new BTPMessage(service, opcode, index, data);
        byte[] bytes = message.toByteArray();

        if (socket != null && socket.isOpen()) {
            socket.send(bytes);
        } else {
            Log.e("TAG", "WebSocket is closed");
        }
    }

    public void response(byte service, byte opcode, byte index, byte status) {
        if (status == BTP_STATUS_SUCCESS) {
            sendMessage(service, opcode, index, null);
            return;
        }

        sendMessage(service, opcode, index, new byte[]{status});
    }

    public void cleanup() {
        if (gap != null) {
            gap.cleanup();
            gap = null;
        }

        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    public void close() {
        if (wsServer != null) {
            try {
                cleanup();
                wsServer.stop();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            wsServer = null;
        }
    }

}
