package com.juul.btptesterandroid;

public class Utils {
    static byte setBit(byte b, int bit) {
       return b |= 1 << bit;
    }

    byte clearBit(byte b, int bit) {
        return b &= ~(1 << bit);
    }
}
