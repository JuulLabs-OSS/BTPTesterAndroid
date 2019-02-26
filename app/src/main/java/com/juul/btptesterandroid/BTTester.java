package com.juul.btptesterandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class BTTester {
    private static final String SERVER = "ws://192.168.9.111:8765";
    private static final int TIMEOUT = 5000;
    private static final int RECONNECT_TIMEOUT = 3000;

    private Context context;
    private BluetoothAdapter bleAdapter;
    private BluetoothManager bleManager;
    private WebSocket ws = null;

    private GAP gap = null;

    public static final int HDR_LEN = 5;
    public static final byte BTP_INDEX_NONE = (byte) 0xff;

    public static final byte BTP_SERVICE_ID_CORE = 0;
    public static final byte BTP_SERVICE_ID_GAP = 1;
    public static final byte BTP_SERVICE_ID_GATT = 2;
    public static final byte BTP_SERVICE_ID_L2CAP = 3;
    public static final byte BTP_SERVICE_ID_MESH = 4;

    public static final byte BTP_STATUS_SUCCESS = 0x00;
    public static final byte BTP_STATUS_FAILED = 0x01;
    public static final byte BTP_STATUS_UNKNOWN_CMD = 0x02;
    public static final byte BTP_STATUS_NOT_READY = 0x03;

    public static final byte BTP_STATUS = 0x00;
    public static final byte CORE_REA = 0x00;

    /* Core Service */
    public static final byte CORE_READ_SUPPORTED_COMMANDS = 0x01;
    public static final byte CORE_READ_SUPPORTED_SERVICES = 0x02;
    public static final byte CORE_REGISTER_SERVICE = 0x03;
    public static final byte CORE_UNREGISTER_SERVICE = 0x04;

    /* events */
    public static final byte CORE_EV_IUT_READY = (byte) 0x80;

    /* GAP Service */
    /* commands */
    public static final byte GAP_READ_SUPPORTED_COMMANDS = 0x01;
    public static final byte GAP_READ_CONTROLLER_INDEX_LIST = 0x02;

    public static final byte GAP_SETTINGS_POWERED = 0;
    public static final byte GAP_SETTINGS_CONNECTABLE = 1;
    public static final byte GAP_SETTINGS_FAST_CONNECTABLE = 2;
    public static final byte GAP_SETTINGS_DISCOVERABLE = 3;
    public static final byte GAP_SETTINGS_BONDABLE = 4;
    public static final byte GAP_SETTINGS_LINK_SEC_3 = 5;
    public static final byte GAP_SETTINGS_SSP = 6;
    public static final byte GAP_SETTINGS_BREDR = 7;
    public static final byte GAP_SETTINGS_HS = 8;
    public static final byte GAP_SETTINGS_LE = 9;
    public static final byte GAP_SETTINGS_ADVERTISING = 10;
    public static final byte GAP_SETTINGS_SC = 11;
    public static final byte GAP_SETTINGS_DEBUG_KEYS = 12;
    public static final byte GAP_SETTINGS_PRIVACY = 13;
    public static final byte GAP_SETTINGS_CONTROLLER_CONFIG = 14;
    public static final byte GAP_SETTINGS_STATIC_ADDRESS = 15;

    public static final byte GAP_READ_CONTROLLER_INFO = 0x03;

    public static class GapReadControllerInfo {
        byte[] address;
        byte[] supportedSettings;
        byte[] currentSettings;
        byte[] cod;
        byte[] name;
        byte[] shortName;

        public GapReadControllerInfo() {
            this.address = new byte[6];
            this.supportedSettings = new byte[4];
            this.currentSettings = new byte[4];
            this.cod = new byte[3];
            this.name = new byte[249];
            this.shortName = new byte[11];
        }

        byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(6 + 4 + 4 + 3 + 249 + 11);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            buf.put(address);
            buf.put(supportedSettings);
            buf.put(currentSettings);
            buf.put(cod);
            buf.put(name);
            buf.put(shortName);

            return buf.array();
        }
    }

    public BTTester(Context context, BluetoothAdapter bleAdapter, BluetoothManager bleManager) {
        this.context = context;
        this.bleAdapter = bleAdapter;
        this.bleManager = bleManager;
    }

    public void init() {
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
            sleep(RECONNECT_TIMEOUT);
            ws = ws.recreate().connectAsynchronously();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private WebSocketAdapter adapter = new WebSocketAdapter() {
        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) {
            Log.d("TAG", "onConnected");
            try {
                sendMessage(BTP_SERVICE_ID_CORE, CORE_EV_IUT_READY, BTP_INDEX_NONE, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
            cleanup();
            tryReconnect();
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) {
            Log.d("TAG", "onBinaryMessage");
            try {
                messageHandler(binary);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) {
            Log.d("TAG", "onUnexpectedError");
        }
    };

    public void supportedCommands(ByteBuffer data) throws Exception {
        byte supported = 0;

        supported = Utils.setBit(supported, CORE_READ_SUPPORTED_COMMANDS);
        supported = Utils.setBit(supported, CORE_READ_SUPPORTED_SERVICES);
        supported = Utils.setBit(supported, CORE_REGISTER_SERVICE);
        supported = Utils.setBit(supported, CORE_UNREGISTER_SERVICE);

        sendMessage(BTP_SERVICE_ID_CORE, CORE_READ_SUPPORTED_COMMANDS, BTP_INDEX_NONE,
                new byte[]{supported});
    }

    public void supportedServices(ByteBuffer data) throws Exception {
        byte supported = 0;

        supported = Utils.setBit(supported, BTP_SERVICE_ID_CORE);
        supported = Utils.setBit(supported, BTP_SERVICE_ID_GAP);
        supported = Utils.setBit(supported, BTP_SERVICE_ID_GATT);

        sendMessage(BTP_SERVICE_ID_CORE, CORE_READ_SUPPORTED_SERVICES, BTP_INDEX_NONE,
                new byte[]{supported});
    }

    public void registerService(ByteBuffer data) throws Exception {
        byte id = data.get();
        byte status;

        switch (id) {
            case BTP_SERVICE_ID_GAP:
                if (gap != null) {
                    status = BTP_STATUS_FAILED;
                } else {
                    gap = new GAP();
                    status = gap.init(this, bleAdapter);
                }
                break;
            case BTP_SERVICE_ID_GATT:
                status = BTP_STATUS_FAILED;
                break;
            default:
                status = BTP_STATUS_FAILED;
                break;
        }

        response(BTP_SERVICE_ID_CORE, CORE_REGISTER_SERVICE, BTP_INDEX_NONE, status);
    }

    public void unregisterService(ByteBuffer data) throws Exception {
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

    public void handleCore(byte opcode, byte index, ByteBuffer data) throws Exception {
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
                break;
            default:
                response(msg.service, msg.opcode, msg.index, BTP_STATUS_FAILED);
        }
    }

    public void sendMessage(byte service, byte opcode, byte index, byte[] data) throws Exception {
        Log.d("TAG", String.format("sendMessage 0x%02x 0x%02x 0x%02x %s",
                service, opcode, index, Arrays.toString(data)));
        if (!ws.isOpen()) {
            throw new Exception("WebSocket is closed");
        }

        BTPMessage message = new BTPMessage(service, opcode, index, data);
        byte[] bytes = message.toByteArray();
        ws.sendBinary(bytes);
    }

    public void response(byte service, byte opcode, byte index, byte status) throws Exception {
        if (status == BTP_STATUS_SUCCESS) {
            sendMessage(service, opcode, index, null);
            return;
        }

        sendMessage(service, opcode, index, new byte[]{status});
    }

    public void cleanup() {
        gap = null;
    }

    public void close() {
        if (ws != null) {
            ws.disconnect();
            ws = null;
        }
    }

}
