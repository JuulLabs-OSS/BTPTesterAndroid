package com.juul.btptesterandroid.gatt;

import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;

public class GattDBService {

    private BluetoothGattService mService;
    private List<GattDBCharacteristic> mCharacteristics;
    private List<GattDBIncludeService> mIncludes;
    private int startHandle, endHandle;

    public GattDBService(BluetoothGattService service) {
        mService = service;
        mCharacteristics = new ArrayList<>();
        mIncludes = new ArrayList<>();
        startHandle = 0;
        endHandle = 0;
    }

    public void addCharacteristic(GattDBCharacteristic characteristic) {
        mCharacteristics.add(characteristic);
    }

    public void addIncludeService(GattDBIncludeService includeService) {
        mIncludes.add(includeService);
    }

    public BluetoothGattService getService() {
        return mService;
    }

    public int setHandles(int curHandle) {
        startHandle = curHandle;

        for (GattDBIncludeService inc : mIncludes) {
            ++curHandle;
            inc.setHandle(curHandle);
        }

        for (GattDBCharacteristic chr : mCharacteristics) {
            ++curHandle;
            curHandle = chr.setHandles(curHandle);
        }

        endHandle = curHandle;

        return  endHandle;
    }

    public int getStartHandle() {
        return startHandle;
    }

    public int getEndHandle() {
        return endHandle;
    }

    public boolean isPrimary() {
        return mService.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY;
    }
}
