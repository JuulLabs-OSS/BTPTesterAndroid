package com.juul.btptesterandroid;

import com.juul.btptesterandroid.gatt.GattDBCharacteristic;
import com.juul.btptesterandroid.gatt.GattDBDescriptor;
import com.juul.btptesterandroid.gatt.GattDBIncludeService;
import com.juul.btptesterandroid.gatt.GattDBService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static com.juul.btptesterandroid.Utils.UUIDtoBTP;

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

    /* GATT Service */
    /* commands */
    public static final byte GATT_READ_SUPPORTED_COMMANDS = 0x01;

    public static final byte GATT_SERVICE_PRIMARY = 0x00;
    public static final byte GATT_SERVICE_SECONDARY = 0x01;

    public static final byte GATT_ADD_SERVICE = 0x02;

    public static class GattAddServiceCmd {
        byte type;
        byte uuidLen;
        byte[] uuid;

        public GattAddServiceCmd(ByteBuffer byteBuffer) {
            type = byteBuffer.get();
            uuidLen = byteBuffer.get();
            uuid = new byte[uuidLen];

            byteBuffer.get(uuid, 0, uuidLen);
            Utils.reverseBytes(uuid);
        }

        public static GattAddServiceCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 4) {
                return null;
            }

            return new GattAddServiceCmd(byteBuffer);
        }
    }

    public static class GattAddServiceRp {
        short svcId;

        public GattAddServiceRp() {
            svcId = 0;
        }

        public byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(2);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort(svcId);
            return buf.array();
        }
    }

    public static final byte GATT_ADD_CHARACTERISTIC = 0x03;

    public static class GattAddCharacteristicCmd {
        short svcId;
        byte properties;
        byte permissions;
        byte uuidLen;
        byte[] uuid;

        public GattAddCharacteristicCmd(ByteBuffer byteBuffer) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            svcId = byteBuffer.getShort();
            properties = byteBuffer.get();
            permissions = byteBuffer.get();
            uuidLen = byteBuffer.get();
            uuid = new byte[uuidLen];

            byteBuffer.get(uuid, 0, uuidLen);
            Utils.reverseBytes(uuid);
        }

        public static GattAddCharacteristicCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 7) {
                return null;
            }

            return new GattAddCharacteristicCmd(byteBuffer);
        }
    }

    public static class GattAddCharacteristicRp {
        short chrId;

        public GattAddCharacteristicRp() {
            chrId = 0;
        }

        public byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(2);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort(chrId);
            return buf.array();
        }
    }

    public static final byte GATT_ADD_DESCRIPTOR = 0x04;

    public static class GattAddDescriptorCmd {
        short chrId;
        byte permissions;
        byte uuidLen;
        byte[] uuid;

        public GattAddDescriptorCmd(ByteBuffer byteBuffer) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            chrId = byteBuffer.getShort();
            permissions = byteBuffer.get();
            uuidLen = byteBuffer.get();
            uuid = new byte[uuidLen];

            byteBuffer.get(uuid, 0, uuidLen);
            Utils.reverseBytes(uuid);
        }

        public static GattAddDescriptorCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 6) {
                return null;
            }

            return new GattAddDescriptorCmd(byteBuffer);
        }
    }

    public static class GattAddDescriptorRp {
        short dscId;

        public GattAddDescriptorRp() {
            dscId = 0;
        }

        public byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(2);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort(dscId);
            return buf.array();
        }
    }

    public static final byte GATT_START_SERVER = 0x07;

    public static class GattStartServerRp {
        short attrOff;
        byte attrCount;

        public GattStartServerRp() {
            attrOff = 0;
            attrCount = 0;
        }

        public byte[] toBytes() {
            ByteBuffer buf = ByteBuffer.allocate(3);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort(attrOff);
            buf.put(attrCount);
            return buf.array();
        }
    }

    public static class GattService {
        short startHandle;
        short endHandle;
        byte uuidLen;
        byte[] uuid;

        public GattService(GattDBService svc) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            startHandle = (short) svc.getStartHandle();
            endHandle = (short) svc.getEndHandle();

            uuid = UUIDtoBTP(svc.getService().getUuid());
            uuidLen = (byte) uuid.length;
        }

        public GattService(short startHandle, short endHandle,
                           byte[] uuid) {
            this.startHandle = startHandle;
            this.endHandle = endHandle;
            this.uuidLen = (byte) uuid.length;
            this.uuid = uuid;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 + 2 + 1 + uuidLen);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.putShort(startHandle);
            byteBuffer.putShort(endHandle);
            byteBuffer.put(uuidLen);
            byteBuffer.put(uuid);

            return byteBuffer.array();
        }
    }

    public static class GattIncluded extends GattService {
        short attHandle;

        public GattIncluded(GattDBIncludeService svc) {
            super(svc.getService());
        }

        public GattIncluded(short attHandle, short startHandle, short endHandle,
                            byte[] uuid) {
            super(startHandle, endHandle, uuid);
            this.attHandle = attHandle;
        }

        public byte[] toBytes() {
            byte[] bytes = super.toBytes();
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 + bytes.length);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.putShort(attHandle);
            byteBuffer.put(bytes);

            return byteBuffer.array();
        }
    }

    public static class GattCharacteristic {
        short definitionHandle;
        short valueHandle;
        byte properties;
        byte uuidLen;
        byte[] uuid;

        public GattCharacteristic(GattDBCharacteristic chr) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(16);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            definitionHandle = (short) chr.getDefHandle();
            valueHandle = (short) chr.getValHandle();
            properties = (byte) chr.getCharacteristic().getProperties();

            uuid = UUIDtoBTP(chr.getCharacteristic().getUuid());
            uuidLen = (byte) uuid.length;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 + 2 + 1 + 1 + uuidLen);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.putShort(definitionHandle);
            byteBuffer.putShort(valueHandle);
            byteBuffer.put(properties);
            byteBuffer.put(uuidLen);
            byteBuffer.put(uuid);

            return byteBuffer.array();
        }
    }

    public static class GattDescriptor {
        short descriptorHandle;
        byte uuidLen;
        byte[] uuid;

        public GattDescriptor(GattDBDescriptor dsc) {
            descriptorHandle = (short) dsc.getHandle();
            uuid = UUIDtoBTP(dsc.getDescriptor().getUuid());
            uuidLen = (byte) uuid.length;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 + 1 + uuidLen);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.putShort(descriptorHandle);
            byteBuffer.put(uuidLen);
            byteBuffer.put(uuid);

            return byteBuffer.array();
        }
    }

    public static final byte GATT_DISC_ALL_PRIM_SVCS = 0x0b;

    public static class GattDiscAllPrimSvcsCmd {
        byte addressType;
        byte[] address;

        public GattDiscAllPrimSvcsCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);
        }

        public static GattDiscAllPrimSvcsCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 7) {
                return null;
            }

            return new GattDiscAllPrimSvcsCmd(byteBuffer);
        }
    }

    public static class GattDiscAllPrimSvcsRp {
        byte servicesCount;
        GattService[] services;

        public GattDiscAllPrimSvcsRp() {
            this.servicesCount = 0;
            this.services = null;
        }

        public byte[] toBytes() {
            int length = 0;
            for (GattService service : services) {
                length += service.toBytes().length;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + length);

            byteBuffer.put(servicesCount);
            for (GattService service : services) {
                byteBuffer.put(service.toBytes());
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_DISC_PRIM_UUID = 0x0c;

    public static class GattDiscPrimUuidCmd {
        byte addressType;
        byte[] address;
        byte uuidLen;
        byte[] uuid;

        public GattDiscPrimUuidCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            uuidLen = byteBuffer.get();
            uuid = new byte[uuidLen];

            byteBuffer.get(uuid, 0, uuidLen);
            Utils.reverseBytes(uuid);
        }

        public static GattDiscPrimUuidCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 10) {
                return null;
            }

            return new GattDiscPrimUuidCmd(byteBuffer);
        }
    }

    public static class GattDiscPrimUuidRp {
        byte servicesCount;
        GattService[] services;

        public GattDiscPrimUuidRp() {
            this.servicesCount = 0;
            this.services = null;
        }

        public byte[] toBytes() {
            int length = 0;
            for (GattService service : services) {
                length += service.toBytes().length;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + length);

            byteBuffer.put(servicesCount);
            for (GattService service : services) {
                byteBuffer.put(service.toBytes());
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_DISC_FIND_INCLUDED = 0x0d;

    public static class GattFindIncludedCmd {
        byte addressType;
        byte[] address;
        byte uuidLen;
        byte[] uuid;

        public GattFindIncludedCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            uuidLen = byteBuffer.get();
            uuid = new byte[uuidLen];

            byteBuffer.get(uuid, 0, uuidLen);
            Utils.reverseBytes(uuid);
        }

        public static GattFindIncludedCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 10) {
                return null;
            }

            return new GattFindIncludedCmd(byteBuffer);
        }
    }

    public static class GattFindIncludedRp {
        byte servicesCount;
        GattService[] included;

        public GattFindIncludedRp() {
            this.servicesCount = 0;
            this.included = null;
        }

        public byte[] toBytes() {
            int length = 0;
            for (GattService service : included) {
                length += service.toBytes().length;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + length);

            byteBuffer.put(servicesCount);
            for (GattService service : included) {
                byteBuffer.put(service.toBytes());
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_DISC_ALL_CHRC = 0x0e;

    public static class GattDiscAllChrcCmd {
        byte addressType;
        byte[] address;
        short startHandle;
        short endHandle;

        public GattDiscAllChrcCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            startHandle = byteBuffer.getShort();
            endHandle = byteBuffer.getShort();
        }

        public static GattDiscAllChrcCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 11) {
                return null;
            }

            return new GattDiscAllChrcCmd(byteBuffer);
        }
    }

    public static class GattDiscChrcRp {
        byte characteristicsCount;
        GattCharacteristic[] characteristics;

        public GattDiscChrcRp() {
            this.characteristicsCount = 0;
            this.characteristics = null;
        }

        public byte[] toBytes() {
            int length = 0;
            for (GattCharacteristic characteristic : characteristics) {
                length += characteristic.toBytes().length;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + length);

            byteBuffer.put(characteristicsCount);
            for (GattCharacteristic characteristic : characteristics) {
                byteBuffer.put(characteristic.toBytes());
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_DISC_CHRC_UUID = 0x0f;

    public static class GattDiscChrcUuidCmd {
        byte addressType;
        byte[] address;
        short startHandle;
        short endHandle;
        byte uuidLen;
        byte[] uuid;

        public GattDiscChrcUuidCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            startHandle = byteBuffer.getShort();
            endHandle = byteBuffer.getShort();

            uuidLen = byteBuffer.get();
            uuid = new byte[uuidLen];

            byteBuffer.get(uuid, 0, uuidLen);
            Utils.reverseBytes(uuid);
        }

        public static GattDiscChrcUuidCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 14) {
                return null;
            }

            return new GattDiscChrcUuidCmd(byteBuffer);
        }
    }

    public static final byte GATT_DISC_ALL_DESC = 0x10;

    public static class GattDiscAllDescCmd {
        byte addressType;
        byte[] address;
        short startHandle;
        short endHandle;

        public GattDiscAllDescCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            startHandle = byteBuffer.getShort();
            endHandle = byteBuffer.getShort();
        }

        public static GattDiscAllDescCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 11) {
                return null;
            }

            return new GattDiscAllDescCmd(byteBuffer);
        }
    }

    public static class GattDiscAllDescRp {
        byte descriptorCount;
        GattDescriptor[] descriptors;

        public GattDiscAllDescRp() {
            this.descriptorCount = 0;
            this.descriptors = null;
        }

        public byte[] toBytes() {
            int length = 0;
            for (GattDescriptor descriptor : descriptors) {
                length += descriptor.toBytes().length;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + length);

            byteBuffer.put(descriptorCount);
            for (GattDescriptor descriptor : descriptors) {
                byteBuffer.put(descriptor.toBytes());
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_READ = 0x11;

    public static class GattReadCmd {
        byte addressType;
        byte[] address;
        short handle;

        public GattReadCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            handle = byteBuffer.getShort();
        }

        public static GattReadCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 9) {
                return null;
            }

            return new GattReadCmd(byteBuffer);
        }
    }

    public static class GattReadRp {
        byte attResponse;
        short dataLen;
        byte[] data;

        public GattReadRp() {
            this.attResponse = 0;
            this.dataLen = 0;
            this.data = null;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 2 + dataLen);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.put(attResponse);
            byteBuffer.putShort(dataLen);
            if (dataLen > 0) {
                byteBuffer.put(data);
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_READ_LONG = 0x13;

    public static class GattReadLongCmd {
        byte addressType;
        byte[] address;
        short handle;
        short offset;

        public GattReadLongCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            handle = byteBuffer.getShort();
            offset = byteBuffer.getShort();
        }

        public static GattReadLongCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 11) {
                return null;
            }

            return new GattReadLongCmd(byteBuffer);
        }
    }

    public static final byte GATT_WRITE = 0x17;

    public static class GattWriteCmd {
        byte addressType;
        byte[] address;
        short handle;
        short dataLen;
        byte[] data;

        public GattWriteCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            handle = byteBuffer.getShort();

            dataLen = byteBuffer.getShort();

            data = new byte[dataLen];
            byteBuffer.get(data, 0, dataLen);
        }

        public static GattWriteCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 11) {
                return null;
            }

            return new GattWriteCmd(byteBuffer);
        }
    }

    public static final byte GATT_WRITE_LONG = 0x18;

    public static class GattWriteLongCmd {
        byte addressType;
        byte[] address;
        short handle;
        short offset;
        short dataLen;
        byte[] data;

        public GattWriteLongCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            handle = byteBuffer.getShort();
            offset = byteBuffer.getShort();
            dataLen = byteBuffer.getShort();
            data = new byte[dataLen];
            byteBuffer.get(data, 0, dataLen);
        }

        public static GattWriteLongCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 13) {
                return null;
            }

            return new GattWriteLongCmd(byteBuffer);
        }
    }

    public static final byte GATT_CFG_NOTIFY = 0x1a;
    public static final byte GATT_CFG_INDICATE = 0x1b;

    public static class GattCfgNotifyCmd {
        byte addressType;
        byte[] address;
        byte enable;
        short cccdHandle;

        public GattCfgNotifyCmd(ByteBuffer byteBuffer) {
            address = new byte[6];

            addressType = byteBuffer.get();
            byteBuffer.get(address, 0, address.length);
            Utils.reverseBytes(address);

            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            enable = byteBuffer.get();
            cccdHandle = byteBuffer.getShort();
        }

        public static GattCfgNotifyCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 10) {
                return null;
            }

            return new GattCfgNotifyCmd(byteBuffer);
        }
    }

    public static final byte GATT_EV_NOTIFICATION = (byte) 0x80;

    public static class GattNotificationEv {
        byte addressType;
        byte[] address;
        byte type;
        short handle;
        short dataLen;
        byte[] data;

        public GattNotificationEv() {
            addressType = 0;
            address = null;
            type = 0;
            handle = 0;
            dataLen = 0;
            data = null;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 6 + 1 + 2 + 2 + dataLen);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.put(addressType);
            byteBuffer.put(address);
            byteBuffer.put(type);
            byteBuffer.putShort(handle);
            byteBuffer.putShort(dataLen);
            if (dataLen > 0) {
                byteBuffer.put(data);
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_GET_ATTRIBUTES = 0x1c;

    public static class GattGetAttributesCmd {
        short startHandle;
        short endHandle;
        byte typeLen;
        byte[] type;

        public GattGetAttributesCmd(ByteBuffer byteBuffer) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            startHandle = byteBuffer.getShort();
            endHandle = byteBuffer.getShort();
            typeLen = byteBuffer.get();

            if (typeLen > 0) {
                type = new byte[typeLen];
                byteBuffer.get(type, 0, typeLen);
                Utils.reverseBytes(type);
            }
        }

        public static GattGetAttributesCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 5) {
                return null;
            }

            return new GattGetAttributesCmd(byteBuffer);
        }
    }

    public static class GattAttribute {
        short handle;
        byte permission;
        byte typeLength;
        byte[] typeUUID;

        public GattAttribute(short handle, byte permission, UUID uuid) {
            this.handle = handle;
            this.permission = permission;
            typeUUID = UUIDtoBTP(uuid);
            typeLength = (byte) typeUUID.length;
        }

        public GattAttribute(GattDBService svc) {
            handle = (short) svc.getStartHandle();
            permission = 1;
            if (svc.getService().getType() == GATT_SERVICE_PRIMARY) {
                typeUUID = Utils.BT_PRI_SVC_TYPE_UUID_BYTES;
            } else {
                typeUUID = Utils.BT_SEC_SVC_TYPE_UUID_BYTES;
            }

            typeLength = (byte) typeUUID.length;
        }

        public GattAttribute(GattDBIncludeService svc) {
            handle = (short) svc.getHandle();
            permission = 1;
            typeUUID = Utils.BT_INC_SVC_TYPE_UUID_BYTES;
            typeLength = (byte) typeUUID.length;
        }

        public GattAttribute(GattDBCharacteristic chr) {
            handle = (short) chr.getDefHandle();
            permission = 1;
            typeUUID = Utils.BT_CHRC_TYPE_UUID_BYTES;
            typeLength = (byte) typeUUID.length;
        }

        public GattAttribute(GattDBDescriptor dsc) {
            handle = (short) dsc.getHandle();
            permission = 1;
            typeUUID = UUIDtoBTP(dsc.getDescriptor().getUuid());
            typeLength = (byte) typeUUID.length;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 + 1 + 1 + typeLength);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.putShort(handle);
            byteBuffer.put(permission);
            byteBuffer.put(typeLength);
            byteBuffer.put(typeUUID);

            return byteBuffer.array();
        }
    }

    public static class GattGetAttributtesRp {
        byte attributesCount;
        GattAttribute[] attributes;

        public GattGetAttributtesRp() {
            this.attributesCount = 0;
            this.attributes = null;
        }

        public byte[] toBytes() {
            int length = 0;
            for (GattAttribute attribute : attributes) {
                length += attribute.toBytes().length;
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + length);

            byteBuffer.put(attributesCount);
            for (GattAttribute attribute : attributes) {
                byteBuffer.put(attribute.toBytes());
            }

            return byteBuffer.array();
        }
    }

    public static final byte GATT_GET_ATTRIBUTE_VALUE = 0x1d;

    public static class GattGetAttributeValueCmd {
        short handle;

        public GattGetAttributeValueCmd(ByteBuffer byteBuffer) {
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            handle = byteBuffer.getShort();
        }

        public static GattGetAttributeValueCmd parse(ByteBuffer byteBuffer) {
            if (byteBuffer.array().length < 2) {
                return null;
            }

            return new GattGetAttributeValueCmd(byteBuffer);
        }
    }

    public static class GattGetAttributeValueRp {
        byte attResponse;
        short valueLength;
        byte[] value;

        public GattGetAttributeValueRp() {
            this.attResponse = 0;
            this.valueLength = 0;
            this.value = null;
        }

        public byte[] toBytes() {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 2 + valueLength);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            byteBuffer.put(attResponse);
            byteBuffer.putShort(valueLength);
            if (value != null) {
                byteBuffer.put(value);
            }

            return byteBuffer.array();
        }
    }
}
