package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public interface IGattAttrValueChanged {

    public void characteristicValueChanged(BluetoothDevice device,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value);

    public void descriptorValueChanged(BluetoothDevice device,
                                       BluetoothGattDescriptor descriptor,
                                       byte[] value);
}
