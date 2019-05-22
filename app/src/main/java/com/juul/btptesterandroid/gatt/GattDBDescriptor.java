package com.juul.btptesterandroid.gatt;

import android.bluetooth.BluetoothGattDescriptor;

public class GattDBDescriptor {

    private final BluetoothGattDescriptor mDescriptor;
    private int handle;

    public GattDBDescriptor(BluetoothGattDescriptor descriptor) {
        mDescriptor = descriptor;
        handle = 0;
    }

    public void setHandle(int curHandle) {
        handle = curHandle;
    }

    public int getHandle() {
        return handle;
    }

    public BluetoothGattDescriptor getDescriptor() {
        return mDescriptor;
    }

    public byte[] toBTP() {
        return getDescriptor().getValue();
    }
}
