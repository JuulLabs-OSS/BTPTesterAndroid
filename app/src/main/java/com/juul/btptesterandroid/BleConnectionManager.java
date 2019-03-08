package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.List;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.Request;

public class BleConnectionManager extends BleManager  {

    public List<BluetoothGattService> mServices = null;

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

    @NonNull
    @Override
    protected Request createBond(){
        return super.createBond();
    }

    @NonNull
    @Override
    protected Request removeBond() {
        return super.removeBond();
    }

    @Override
    protected Request refreshDeviceCache() {
        return super.refreshDeviceCache();
    }

    @Override
    protected String pairingVariantToString(int variant) {
        return super.pairingVariantToString(variant);
    }

    @Override
    protected String bondStateToString(int state) {
        return super.bondStateToString(state);
    }

    @Override
    protected void onPairingRequestReceived(@NonNull BluetoothDevice device, int variant) {
        super.onPairingRequestReceived(device, variant);
        Log.d("GAP", String.format("onPairingRequestReceived %s %s", device,
                pairingVariantToString(variant)));
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
            Log.d("GAP", String.format("isRequiredServiceSupported %s", gatt));
            mServices = gatt.getServices();

            for (BluetoothGattService svc : gatt.getServices()) {
                Log.d("GAP", String.format("service UUID=%s TYPE=%d",
                        svc.getUuid(), svc.getType()));

                for (BluetoothGattService inc : svc.getIncludedServices()) {
                    Log.d("GAP", String.format("include UUID=%s TYPE=%d",
                            inc.getUuid(), inc.getType()));
                }

                for (BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                    Log.d("GAP", String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                            chr.getUuid(), chr.getProperties(), chr.getPermissions()));

                    for (BluetoothGattDescriptor dsc : chr.getDescriptors()) {
                        Log.d("GAP", String.format("descriptor UUID=%s PERMS=%d",
                                dsc.getUuid(), dsc.getPermissions()));

                    }
                }
            }

            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
            Log.d("GAP", "onDeviceDisconnected");
        }
    };
}
