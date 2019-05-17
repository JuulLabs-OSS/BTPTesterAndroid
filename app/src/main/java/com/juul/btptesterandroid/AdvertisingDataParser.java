package com.juul.btptesterandroid;

import android.bluetooth.le.AdvertiseData;
import android.os.ParcelUuid;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

public class AdvertisingDataParser {
    public static final byte AD_UUID16_SOME = 0x02;
    public static final byte AD_UUID16_FULL = 0x03;
    public static final byte AD_UUID32_SOME = 0x04;
    public static final byte AD_UUID32_FULL = 0x05;
    public static final byte AD_UUID128_SOME = 0x06;
    public static final byte AD_UUID128_FULL = 0x07;
    public static final byte AD_NAME_SHORT = 0x08;
    public static final byte AD_NAME_FULL = 0x09;
    public static final byte AD_TX_POWER = 0x0a;
    public static final byte AD_UUID16_SVC_DATA = 0x16;
    public static final byte AD_UUID32_SVC_DATA = 0x20;
    public static final byte AD_UUID128_SVC_DATA = 0x21;
    public static final byte AD_MANUFACTURER_DATA = (byte) 0xff;

    public static void parseUUIDs(ArrayList<UUID> uuids, byte[] uuidBytes, int uuidLen) {
        ByteBuffer buf = ByteBuffer.wrap(uuidBytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int length = uuidBytes.length;
        byte[] bytes = new byte[uuidLen];

        while(length > 0) {
            buf.get(bytes, 0, uuidLen);
            Utils.reverseBytes(bytes);
            length -= uuidLen;
            UUID u = Utils.btpToUUID(bytes);
            if (u == null) {
                assert(false);
            }

            uuids.add(u);
        }
    }

    public static void parseAdUUIDs(AdvertiseData.Builder builder, byte[] data,
                                    int uuidLen) {
        ArrayList<UUID> uuids = new ArrayList<>();
        parseUUIDs(uuids, data, uuidLen);

        for (UUID u : uuids) {
            builder.addServiceUuid(new ParcelUuid(u));
        }
    }

    public static void parseAdServiceData(AdvertiseData.Builder builder, byte[] data,
                                          int uuidLen) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int length = data.length;
        byte[] bytes = new byte[uuidLen];

        buf.get(bytes, 0, uuidLen);
        Utils.reverseBytes(bytes);
        length -= uuidLen;
        UUID u = Utils.btpToUUID(bytes);
        if (u == null) {
            assert(false);
        }

        byte[] additionalData = new byte[length];
        buf.get(additionalData, 0, length);

        builder.addServiceData(new ParcelUuid(u), additionalData);
    }

    public static void parseAdManufacturerData(AdvertiseData.Builder builder, byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int length = data.length;

        int mid = buf.getShort();
        length -= 2;

        byte[] additionalData = new byte[length];
        buf.get(additionalData, 0, length);

        builder.addManufacturerData(mid, additionalData);
    }

    public static void parseAdTxPower(AdvertiseData.Builder builder, byte[] data) {
        /* TODO: Add support for setting tx power level */
        builder.setIncludeTxPowerLevel(true);
    }

    public static void parseAdName(AdvertiseData.Builder builder, byte[] data) {
        builder.setIncludeDeviceName(true);
    }

    public static boolean parse(AdvertiseData.Builder builder, byte[] data) {
        if (data == null || data.length == 0) {
            return true;
        }

        ByteBuffer buf = ByteBuffer.wrap(data);

        try {
            while (true) {
                byte adType = buf.get();
                int length = buf.get();

                byte[] adStruct = new byte[length];
                buf.get(adStruct, 0, adStruct.length);

                switch (adType) {
                    case AD_UUID16_SOME:
                        parseAdUUIDs(builder, adStruct, 2);
                        break;
                    case AD_UUID16_FULL:
                        parseAdUUIDs(builder, adStruct, 2);
                        break;
                    case AD_UUID32_SOME:
                        parseAdUUIDs(builder, adStruct, 4);
                        break;
                    case AD_UUID32_FULL:
                        parseAdUUIDs(builder, adStruct, 4);
                        break;
                    case AD_UUID128_SOME:
                        parseAdUUIDs(builder, adStruct, 16);
                        break;
                    case AD_UUID128_FULL:
                        parseAdUUIDs(builder, adStruct, 16);
                        break;
                    case AD_NAME_SHORT:
                        parseAdName(builder, adStruct);
                        break;
                    case AD_NAME_FULL:
                        parseAdName(builder, adStruct);
                        break;
                    case AD_TX_POWER:
                        parseAdTxPower(builder, adStruct);
                        break;
                    case AD_UUID16_SVC_DATA:
                        parseAdServiceData(builder, adStruct, 2);
                        break;
                    case AD_UUID32_SVC_DATA:
                        parseAdServiceData(builder, adStruct, 4);
                        break;
                    case AD_UUID128_SVC_DATA:
                        parseAdServiceData(builder, adStruct, 16);
                        break;
                    case AD_MANUFACTURER_DATA:
                        parseAdManufacturerData(builder, adStruct);
                        break;
                    default:
                        return false;

                }
            }
        } catch(BufferUnderflowException e) { }

        return true;
    }
}
