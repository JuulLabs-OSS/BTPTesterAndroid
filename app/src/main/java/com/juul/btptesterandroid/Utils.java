package com.juul.btptesterandroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    public static final String BT_BASE_UUID_STR = "0000000000001000800000805F9B34FB";
    public static final byte[] BT_BASE_UUID_BYTE = hexStringToByteArray(BT_BASE_UUID_STR);

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

    public static byte[] UUIDtoBTP(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.putLong(uuid.getLeastSignificantBits());
        byteBuffer.putLong(uuid.getMostSignificantBits());
        return byteBuffer.array();
    }
}
