package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;

public class BleConnectionManager extends BleManager  {

    /**
     * The manager constructor.
     * <p>
     * After constructing the manager, the callbacks object must be set with
     * {@link #setGattCallbacks(BleManagerCallbacks)}.
     * <p>
     * To connect a device, call {@link #connect(BluetoothDevice)}.
     *
     * @param context the context.
     */
    public BleConnectionManager(@NonNull Context context) {
        super(context);
    }

    @Override
    public void setGattCallbacks(@NonNull BleManagerCallbacks callbacks) {
        super.setGattCallbacks(callbacks);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }

    /**
     * BluetoothGatt callbacks object.
     */
    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {
        @Override
        protected void initialize() {
        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
        }
    };
}
