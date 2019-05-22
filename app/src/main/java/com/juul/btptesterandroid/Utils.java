package com.juul.btptesterandroid;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.juul.btptesterandroid.gatt.GattDBCharacteristic;
import com.juul.btptesterandroid.gatt.GattDBDescriptor;
import com.juul.btptesterandroid.gatt.GattDBIncludeService;
import com.juul.btptesterandroid.gatt.GattDBService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Utils {

    static void setBit(byte[] data, int bit)
    {
        int byteIdx = (bit / 8);

        data[byteIdx] = setBit(data[byteIdx], bit % 8);
    }

    static byte testBit(final byte[] data, int bit)
    {
        int byteIdx = (bit / 8);

        return testBit(data[byteIdx], bit % 8);
    }

    static void clearBit(final byte[] data, int bit)
    {
        int byteIdx = (bit / 8);

        data[byteIdx] = clearBit(data[byteIdx], bit % 8);
    }

    static byte setBit(byte b, int bit) {
       return b |= 1 << bit;
    }

    static byte testBit(byte b, int bit) {
        return (byte) (b & 1 << bit);
    }

    static byte clearBit(byte b, int bit) {
        return (byte) (b & ~(1 << bit));
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static void reverseBytes(byte[] bytes) {
        for(int i = 0; i < bytes.length / 2; i++)
        {
            byte temp = bytes[i];
            bytes[i] = bytes[bytes.length - i - 1];
            bytes[bytes.length - i - 1] = temp;
        }
    }

    public static byte[] btAddrToBytes(String address) {
        String addrStr = address.replaceAll(":", "");
        byte[] addr = hexStringToByteArray(addrStr);
        reverseBytes(addr);
        return addr;
    }

    public static String btpToBdAddr(byte[] bytes) {
        String hexAddr = Utils.bytesToHex(bytes);
        return hexAddr.replaceAll("..(?!$)", "$0:");
    }

    public static final String BT_BASE_UUID_STR = "00000000-0000-1000-8000-00805f9b34fb";
    public static final byte[] BT_BASE_UUID_BYTE = hexStringToByteArray(
            BT_BASE_UUID_STR.replace("-", ""));

    public static UUID btpToUUID(byte[] bytes) {
        byte[] uuidBytes;

        if (bytes.length != 2 && bytes.length != 4 && bytes.length != 16) {
            return null;
        }

        if (bytes.length == 16) {
            uuidBytes = bytes;
        } else if (bytes.length == 2) {
            byte[] uuid = BT_BASE_UUID_BYTE.clone();
            uuid[2] = bytes[0];
            uuid[3] = bytes[1];
            uuidBytes = uuid;
        } else {
            byte[] uuid = BT_BASE_UUID_BYTE.clone();
            uuid[0] = bytes[0];
            uuid[1] = bytes[1];
            uuid[2] = bytes[2];
            uuid[3] = bytes[3];
            uuidBytes = uuid;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(uuidBytes);
        return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
    }

    public static boolean isBluetoothSIGUuid(UUID uuid) {
        String uuidStr = uuid.toString();
        String normalizedStr = "00000000" + uuidStr.substring(8);
        return normalizedStr.equals(BT_BASE_UUID_STR);
    }

    public static byte[] UUIDtoBTP(UUID uuid) {
        if (isBluetoothSIGUuid(uuid)) {
            String uuidStr = uuid.toString();
            String substr = uuidStr.substring(0, 8);
            byte[] byteuuid = hexStringToByteArray(substr);
            if (byteuuid[0] == 0 && byteuuid[1] == 0) {
                return new byte[] {byteuuid[3], byteuuid[2]};
            } else {
                reverseBytes(byteuuid);
                return byteuuid;
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.putLong(uuid.getLeastSignificantBits());
        byteBuffer.putLong(uuid.getMostSignificantBits());
        return byteBuffer.array();
    }

    public static List<GattDBService> initializeGattDB(List<BluetoothGattService> services) {
        Log.d("GATT", "initializeGattDB");
        ArrayList<GattDBService> dbServices = new ArrayList<>();
        int curHandle = 1;

        for (BluetoothGattService svc : services) {
            Log.d("GATT", String.format("service UUID=%s TYPE=%d",
                    svc.getUuid(), svc.getType()));

            GattDBService service = new GattDBService(svc);

            for (BluetoothGattService inc : svc.getIncludedServices()) {
                Log.d("GATT", String.format("include UUID=%s TYPE=%d",
                        inc.getUuid(), inc.getType()));

                service.addIncludeService(new GattDBIncludeService(new GattDBService(inc)));
            }

            for (BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                Log.d("GATT", String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                        chr.getUuid(), chr.getProperties(), chr.getPermissions()));

                GattDBCharacteristic characteristic = new GattDBCharacteristic(chr);

                for (BluetoothGattDescriptor dsc : chr.getDescriptors()) {
                    Log.d("GATT", String.format("descriptor UUID=%s PERMS=%d",
                            dsc.getUuid(), dsc.getPermissions()));

                    characteristic.addDescriptor(new GattDBDescriptor(dsc));
                }

                service.addCharacteristic(characteristic);
            }

            curHandle = service.setHandles(curHandle) + 1;
            dbServices.add(service);
        }

        return dbServices;
    }

    public static List<BluetoothGattService> getCoreGattServices() {
        /* TODO: Fix properties and permissions */
        List<BluetoothGattService> svcs = new ArrayList<>();

        UUID gattSvcUUID = Utils.btpToUUID(new byte[]{ 0x18, 0x01 });
        BluetoothGattService gattSvc = new BluetoothGattService(gattSvcUUID, 0);
        svcs.add(gattSvc);

        UUID svcChgCharUUID = Utils.btpToUUID(new byte[]{ 0x2a, 0x05 });
        BluetoothGattCharacteristic svcChgChar =
                new BluetoothGattCharacteristic(svcChgCharUUID, 1, 0);
        gattSvc.addCharacteristic(svcChgChar);

        UUID cccdUUID = Utils.btpToUUID(new byte[]{ 0x29, 0x02 });
        BluetoothGattDescriptor cccd =
                new BluetoothGattDescriptor(cccdUUID, 1);
        svcChgChar.addDescriptor(cccd);

        UUID gapSvcUUID = Utils.btpToUUID(new byte[]{ 0x18, 0x00 });
        BluetoothGattService gapSvc = new BluetoothGattService(gapSvcUUID, 0);
        svcs.add(gapSvc);

        UUID devNameChrUUID = Utils.btpToUUID(new byte[]{ 0x2a, 0x00 });
        BluetoothGattCharacteristic devNameChr =
                new BluetoothGattCharacteristic(devNameChrUUID, 1, 0);
        gapSvc.addCharacteristic(devNameChr);

        UUID appearanceChrUUID = Utils.btpToUUID(new byte[]{ 0x2a, 0x01 });
        BluetoothGattCharacteristic appearanceChr =
                new BluetoothGattCharacteristic(appearanceChrUUID, 1, 0);
        gapSvc.addCharacteristic(appearanceChr);

        UUID carChrUUID = Utils.btpToUUID(new byte[]{ 0x2a, (byte) 0xa6});
        BluetoothGattCharacteristic carChr =
                new BluetoothGattCharacteristic(carChrUUID, 1, 0);
        gapSvc.addCharacteristic(carChr);

        return svcs;
    }

    public static final byte[] BT_PRI_SVC_TYPE_UUID_BYTES = new byte[]{ 0x00, 0x28 };
    public static final byte[] BT_SEC_SVC_TYPE_UUID_BYTES = new byte[]{ 0x01, 0x28 };
    public static final byte[] BT_INC_SVC_TYPE_UUID_BYTES = new byte[]{ 0x02, 0x28 };
    public static final byte[] BT_CHRC_TYPE_UUID_BYTES = new byte[]{ 0x03, 0x28 };

    public static List<BTP.GattAttribute> getGATTAttributes(List<BluetoothGattService> services,
                                                            int startHandle, int endHandle,
                                                            UUID typeUUID) {
        List<GattDBService> dbServices = Utils.initializeGattDB(services);
        List<BTP.GattAttribute> attrs = new ArrayList<>();

        for (GattDBService svc : dbServices) {
            Log.d("GATT", String.format("service UUID=%s TYPE=%d",
                    svc.getService().getUuid(), svc.getService().getType()));
            attrs.add(new BTP.GattAttribute(svc));

            for (GattDBIncludeService inc : svc.getIncludedServices()) {
                Log.d("GATT", String.format("include UUID=%s TYPE=%d",
                        inc.getService().getService().getUuid(),
                        inc.getService().getService().getType()));
                attrs.add(new BTP.GattAttribute(inc));
            }

            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                Log.d("GATT", String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                        chr.getCharacteristic().getUuid(),
                        chr.getCharacteristic().getProperties(),
                        chr.getCharacteristic().getPermissions()));
                attrs.add(new BTP.GattAttribute(chr));

                for (GattDBDescriptor dsc : chr.getDescriptors()) {
                    Log.d("GATT", String.format("descriptor UUID=%s PERMS=%d",
                            dsc.getDescriptor().getUuid(),
                            dsc.getDescriptor().getPermissions()));
                    attrs.add(new BTP.GattAttribute(dsc));
                }
            }
        }

        return attrs;
    }

    public static byte[] gattAttrValToBTP(List<BluetoothGattService> services,
                                          int handle) {
        List<GattDBService> dbServices = Utils.initializeGattDB(services);

        for (GattDBService svc : dbServices) {
            Log.d("GATT", String.format("service UUID=%s TYPE=%d",
                    svc.getService().getUuid(), svc.getService().getType()));

            if (svc.getStartHandle() == handle) {
                return svc.toBTP();
            }

            for (GattDBIncludeService inc : svc.getIncludedServices()) {
                Log.d("GATT", String.format("include UUID=%s TYPE=%d",
                        inc.getService().getService().getUuid(),
                        inc.getService().getService().getType()));

                if (inc.getHandle() == handle) {
                    return inc.toBTP();
                }
            }

            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                Log.d("GATT", String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                        chr.getCharacteristic().getUuid(),
                        chr.getCharacteristic().getProperties(),
                        chr.getCharacteristic().getPermissions()));

                if (chr.getDefHandle() == handle) {
                    return chr.toBTPDefinition();
                }

                if (chr.getValHandle() == handle) {
                    return chr.toBTPValue();
                }

                for (GattDBDescriptor dsc : chr.getDescriptors()) {
                    Log.d("GATT", String.format("descriptor UUID=%s PERMS=%d",
                            dsc.getDescriptor().getUuid(),
                            dsc.getDescriptor().getPermissions()));

                    if (dsc.getHandle() == handle) {
                        return dsc.toBTP();
                    }
                }
            }
        }

        return null;
    }

}
