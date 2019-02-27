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

public class BTTester {
    private static final int PORT = 8765;
    private WebSocketServer wsServer = null;
    private WebSocketServerAdapter wsAdapter = null;
    private WebSocket socket = null;

    private Context context;
    private BluetoothAdapter bleAdapter;
    private BluetoothManager bleManager;
    private BTPBleManager btpBleManager;

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

    public static class GapReadControllerInfoRp {
        byte[] address;
        byte[] supportedSettings;
        byte[] currentSettings;
        byte[] cod;
        byte[] name;
        byte[] shortName;

        public GapReadControllerInfoRp() {
            this.address = new byte[6];
            this.supportedSettings = new byte[4];
            this.currentSettings = new byte[4];
            this.cod = new byte[3];
            this.name = new byte[249];
            this.shortName = new byte[11];
        }

        byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(6 + 4 + 4 + 3 + 249 + 11);

            buf.put(address);
            buf.put(supportedSettings);
            buf.put(currentSettings);
            buf.put(cod);
            buf.put(name);
            buf.put(shortName);

            return buf.array();
        }
    }

    public static final byte GAP_CONNECT = 0x0e;

    public static class GapConnectCmd {
        byte addressType;
        byte[] address;

        private GapConnectCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);
        }

        public static GapConnectCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 7) {
                return null;
            }

            return new GapConnectCmd(byteBuffer);
        }
    }

    public static final byte GAP_EV_DEVICE_CONNECTED = (byte) 0x82;

    public static class GapDeviceConnectedEv {
        byte addressType;
        byte[] address;

        public GapDeviceConnectedEv() {
            addressType = 0;
            address = new byte[6];
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(6 + 1);

            byteBuffer.put(addressType);
            byteBuffer.put(address);

            return byteBuffer.array();
        }
    }


    /*****************************************************************************************/

    public BTTester(Context context, BluetoothAdapter bleAdapter, BluetoothManager bleManager) {
        this.context = context;
        this.bleAdapter = bleAdapter;
        this.bleManager = bleManager;
        this.btpBleManager = new BTPBleManager(this.context);
    }

    public void init() {
        wsServer = new BTPWebSockerServer(PORT, adapter);
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
                    status = gap.init(this, bleAdapter, btpBleManager);
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
                break;
            default:
                response(msg.service, msg.opcode, msg.index, BTP_STATUS_FAILED);
        }
    }

    public void sendMessage(byte service, byte opcode, byte index, byte[] data) {
        Log.d("TAG", String.format("sendMessage 0x%02x 0x%02x 0x%02x %s",
                service, opcode, index, Utils.bytesToHex(data)));
        if (!socket.isOpen()) {
            try {
                throw new Exception("WebSocket is closed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        BTPMessage message = new BTPMessage(service, opcode, index, data);
        byte[] bytes = message.toByteArray();
        socket.send(bytes);
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
//            gap.cleanup();
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
