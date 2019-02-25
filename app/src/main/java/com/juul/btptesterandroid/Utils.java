package com.juul.btptesterandroid;

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

    static byte setBit(byte b, int bit) {
       return b |= 1 << bit;
    }

    static byte testBit(byte b, int bit) {
        return (byte) (b & 1 << bit);
    }

    static byte clearBit(byte b, int bit) {
        return (byte) (b & ~(1 << bit));
    }
}
