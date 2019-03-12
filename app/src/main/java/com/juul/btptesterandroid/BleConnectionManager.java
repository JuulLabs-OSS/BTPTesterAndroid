package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.juul.btptesterandroid.gatt.GattDBCharacteristic;
import com.juul.btptesterandroid.gatt.GattDBDescriptor;
import com.juul.btptesterandroid.gatt.GattDBIncludeService;
import com.juul.btptesterandroid.gatt.GattDBService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.Request;

public class BleConnectionManager extends BleManager  {

    public List<GattDBService> mServices;

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

        mServices = new ArrayList<>();
    }

    public void initializeGattDB(BluetoothGatt gatt) {
        mServices.clear();
        int curHandle = 1;

        for (BluetoothGattService svc : gatt.getServices()) {
            Log.d("GATT", String.format("service UUID=%s TYPE=%d",
                    svc.getUuid(), svc.getType()));

            GattDBService service = new GattDBService(svc);

            for (BluetoothGattService inc : svc.getIncludedServices()) {
                Log.d("GATT", String.format("include UUID=%s TYPE=%d",
                        inc.getUuid(), inc.getType()));

                service.addIncludeService(new GattDBIncludeService(new GattDBService(inc)));
            }

            for (BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                Log.d("GATT", String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                        chr.getUuid(), chr.getProperties(), chr.getPermissions()));

                GattDBCharacteristic characteristic = new GattDBCharacteristic(chr);

                for (BluetoothGattDescriptor dsc : chr.getDescriptors()) {
                    Log.d("GATT", String.format("descriptor UUID=%s PERMS=%d",
                            dsc.getUuid(), dsc.getPermissions()));

                    characteristic.addDescriptor(new GattDBDescriptor(dsc));
                }

                service.addCharacteristic(characteristic);
            }

            curHandle = service.setHandles(curHandle);
            mServices.add(service);
        }
    }

    public List<GattDBService> getAllPrimaryServices() {
        List<GattDBService> allPrimSvcs = new ArrayList<>();


        for (GattDBService btGattSvc : mServices) {
            if (!btGattSvc.isPrimary()) {
                continue;
            }

            allPrimSvcs.add(btGattSvc);
        }


        return  allPrimSvcs;
    }

    public List<GattDBService> getPrimaryServiceByUUID(UUID uuid) {
        List<GattDBService> primSvcsUuid = new ArrayList<>();


        for (GattDBService btGattSvc : mServices) {
            if (!btGattSvc.isPrimary()) {
                continue;
            }

            if (!btGattSvc.getService().getUuid().equals(uuid)) {
                continue;
            }

            primSvcsUuid.add(btGattSvc);
        }

        return  primSvcsUuid;
    }

    public List<GattDBCharacteristic> getAllCharacteristics(int startHandle,
                                                            int endHandle) {
        List<GattDBCharacteristic> allChrcs = new ArrayList<>();

        for (GattDBService svc : mServices) {
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {

                BluetoothGattCharacteristic chrcs = chr.getCharacteristic();
                Log.d("GATT", String.format("characteristic defHandle=%d valueHandle=%d " +
                                "UUID=%s PROPS=%d PERMS=%d",
                        chr.getDefHandle(), chr.getValHandle(),
                        chrcs.getUuid(), chrcs.getProperties(), chrcs.getPermissions()));
                if (chr.getDefHandle() >= startHandle && chr.getDefHandle() <= endHandle) {
                    allChrcs.add(chr);
                }
            }
        }

        return  allChrcs;
    }

    /*******************************************************************/

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
            Log.d("GATT", String.format("isRequiredServiceSupported %s", gatt));
            initializeGattDB(gatt);
            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
            Log.d("GAP", "onDeviceDisconnected");
        }
    };
}
