package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public interface IGattServerCallbacks {

    void characteristicValueChanged(BluetoothDevice device,
                                    BluetoothGattCharacteristic characteristic,
                                    byte[] value);

    void descriptorValueChanged(BluetoothDevice device,
                                BluetoothGattDescriptor descriptor,
                                byte[] value);
}
