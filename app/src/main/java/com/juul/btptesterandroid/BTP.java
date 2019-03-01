package com.juul.btptesterandroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class BTP {
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

    public static final byte GAP_SET_CONNECTABLE = 0x06;

    public static class GapSetConnectableCmd {
        byte connectable = 0;

        private GapSetConnectableCmd(ByteBuffer byteBuffer) {
            connectable = byteBuffer.get();
        }

        public static GapSetConnectableCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 1) {
                return null;
            }

            return new GapSetConnectableCmd(byteBuffer);
        }
    }

    public static class GapSetConnectableRp {
        byte[] currentSettings;

        public GapSetConnectableRp() {
            this.currentSettings = new byte[4];
        }

        byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.put(currentSettings);
            return buf.array();
        }
    }

    public static final byte GAP_NON_DISCOVERABLE = 0x00;
    public static final byte GAP_GENERAL_DISCOVERABLE = 0x01;
    public static final byte GAP_LIMITED_DISCOVERABLE = 0x02;

    public static final byte GAP_SET_DISCOVERABLE = 0x08;

    public static class GapSetDiscoverableCmd {
        byte discoverable = 0;

        private GapSetDiscoverableCmd(ByteBuffer byteBuffer) {
            discoverable = byteBuffer.get();
        }

        public static GapSetDiscoverableCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 1) {
                return null;
            }

            return new GapSetDiscoverableCmd(byteBuffer);
        }
    }

    public static class GapSetDiscoverableRp {
        byte[] currentSettings;

        public GapSetDiscoverableRp() {
            this.currentSettings = new byte[4];
        }

        byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.put(currentSettings);
            return buf.array();
        }
    }

    public static final byte GAP_START_ADVERTISING = 0x0a;

    public static class GapStartAdvertisingCmd {
        byte advDataLen = 0;
        byte scanRspDataLen = 0;
        byte[] advData = null;
        byte[] scanRspData = null;

        private GapStartAdvertisingCmd(ByteBuffer byteBuffer) {
            advDataLen = byteBuffer.get();
            scanRspDataLen = byteBuffer.get();
            if (advDataLen > 0) {
                advData = new byte[advDataLen];
                byteBuffer.get(advData);
            }
            if (scanRspDataLen > 0) {
                scanRspData = new byte[scanRspDataLen];
                byteBuffer.get(scanRspData);
            }
        }

        public static GapStartAdvertisingCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 2) {
                return null;
            }

            return new GapStartAdvertisingCmd(byteBuffer);
        }
    }

    public static class GapStartAdvertisingRp {
        byte[] currentSettings;

        public GapStartAdvertisingRp() {
            this.currentSettings = new byte[4];
        }

        byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.put(currentSettings);
            return buf.array();
        }
    }

    public static final byte GAP_STOP_ADVERTISING = 0x0b;
    public static final byte GAP_START_DISCOVERY = 0x0c;

    public static class GapStartDiscoveryCmd {
        byte flags;

        private GapStartDiscoveryCmd(ByteBuffer byteBuffer) {
            flags = byteBuffer.get();
        }

        public static GapStartDiscoveryCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 1) {
                return null;
            }

            return new GapStartDiscoveryCmd(byteBuffer);
        }
    }

    public static final byte GAP_STOP_DISCOVERY = 0x0d;

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

    public static final byte GAP_DISCONNECT = 0x0f;

    public static class GapDisconnectCmd {
        byte addressType;
        byte[] address;

        private GapDisconnectCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);
        }

        public static GapDisconnectCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 7) {
                return null;
            }

            return new GapDisconnectCmd(byteBuffer);
        }
    }

    public static final byte GAP_IO_CAP_DISPLAY_ONLY = 0x00;
    public static final byte GAP_IO_CAP_DISPLAY_YESNO = 0x01;
    public static final byte GAP_IO_CAP_KEYBOARD_ONLY = 0x02;
    public static final byte GAP_IO_CAP_NO_INPUT_OUTPUT = 0x03;
    public static final byte GAP_IO_CAP_KEYBOARD_DISPLAY = 0x04;

    public static final byte GAP_SET_IO_CAP = 0x10;

    public static class GapSetIOCapCmd {
        byte ioCap;

        private GapSetIOCapCmd(ByteBuffer byteBuffer) {
            ioCap = byteBuffer.get();
        }

        public static GapSetIOCapCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 1) {
                return null;
            }

            return new GapSetIOCapCmd(byteBuffer);
        }
    }

    public static final byte GAP_PAIR = 0x11;

    public static class GapPairCmd {
        byte addressType;
        byte[] address;

        private GapPairCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);
        }

        public static GapPairCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 7) {
                return null;
            }

            return new GapPairCmd(byteBuffer);
        }
    }

    public static final byte GAP_UNPAIR = 0x12;

    public static class GapUnpairCmd {
        byte addressType;
        byte[] address;

        private GapUnpairCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);
        }

        public static GapUnpairCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 7) {
                return null;
            }

            return new GapUnpairCmd(byteBuffer);
        }
    }


    public static final byte GAP_EV_DEVICE_FOUND = (byte) 0x81;

    public static class GapDeviceFoundEv {
        byte addressType;
        byte[] address;
        byte rssi;
        byte flags;
        short eirDataLen;
        byte[] eirData;

        public GapDeviceFoundEv() {
            addressType = 0;
            address = new byte[6];
            rssi = 0;
            flags = 0;
            eirDataLen = 0;
            eirData = null;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(6 + 1 + 1 + 1 + 2 + eirDataLen);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.put(addressType);
            byteBuffer.put(address);
            byteBuffer.put(rssi);
            byteBuffer.put(flags);
            byteBuffer.putShort(eirDataLen);
            if (eirDataLen != 0 && eirData != null) {
                byteBuffer.put(eirData);
            }

            return byteBuffer.array();
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

    public static final byte GAP_EV_DEVICE_DISCONNECTED = (byte) 0x83;

    public static class GapDeviceDisconnectedEv {
        byte addressType;
        byte[] address;

        public GapDeviceDisconnectedEv() {
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
}
