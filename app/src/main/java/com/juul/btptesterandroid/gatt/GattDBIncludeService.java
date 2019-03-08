package com.juul.btptesterandroid.gatt;

import android.bluetooth.BluetoothGattService;

public class GattDBIncludeService {

    private BluetoothGattService mService;
    private int attHandle;

    public GattDBIncludeService(BluetoothGattService service) {
        mService = service;
        attHandle = 0;
    }

    public BluetoothGattService getService() {
        return mService;
    }

    public void setHandle(int handle) {
        attHandle = handle;
    }

    public int getHandle() {
        return attHandle;
    }
}
