package com.juul.btptesterandroid.gatt;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.ArrayList;
import java.util.List;

public class GattDBCharacteristic {

    private BluetoothGattCharacteristic mCharacteristic;
    private List<GattDBDescriptor> mDescriptors;
    private int defHandle, valHandle;

    public GattDBCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
        mDescriptors = new ArrayList<>();
        defHandle = 0;
        valHandle = 0;
    }

    public void addDescriptor(GattDBDescriptor descriptor) {
        mDescriptors.add(descriptor);
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    public int setHandles(int curHandle) {
        defHandle = curHandle;
        ++curHandle;
        valHandle = curHandle;

        for (GattDBDescriptor dsc : mDescriptors) {
            ++curHandle;
            dsc.setHandle(curHandle);
        }

        return curHandle;
    }

    public int getDefHandle() {
        return defHandle;
    }

    public int getValHandle() {
        return valHandle;
    }
}
