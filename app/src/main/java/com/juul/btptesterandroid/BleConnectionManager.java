/*
 * Copyright (c) 2019 JUUL Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import androidx.annotation.Nullable;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.ReadRequest;
import no.nordicsemi.android.ble.Request;
import no.nordicsemi.android.ble.ValueChangedCallback;
import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.callback.DataSentCallback;

public class BleConnectionManager extends BleManager  {

    public static final String GATT_TAG = "GATT";
    public static final String GAP_TAG = "GAP";
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

    public List<GattDBService> getAllPrimaryServices() {
        List<GattDBService> allPrimSvcs = new ArrayList<>();


        for (GattDBService btGattSvc : mServices) {
            if (!btGattSvc.isPrimary()) {
                continue;
            }

            allPrimSvcs.add(btGattSvc);
        }


        return allPrimSvcs;
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

        return primSvcsUuid;
    }

    public List<GattDBIncludeService> getIncludedServices(int startHandle,
                                                          int endHandle) {
        List<GattDBIncludeService> inclSvcsUuid = new ArrayList<>();


        for (GattDBService btGattSvc : mServices) {
            List<GattDBIncludeService> includes = btGattSvc.getIncludedServices();

            for (GattDBIncludeService inc : includes) {
                if (inc.getHandle() >= startHandle && inc.getHandle() <= endHandle) {
                    inclSvcsUuid.add(inc);
                }
            }
        }

        return inclSvcsUuid;
    }

    public List<GattDBCharacteristic> getAllCharacteristics(int startHandle,
                                                            int endHandle) {
        List<GattDBCharacteristic> allChrcs = new ArrayList<>();

        for (GattDBService svc : mServices) {
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                if (chr.getDefHandle() >= startHandle && chr.getDefHandle() <= endHandle) {
                    allChrcs.add(chr);
                }
            }
        }

        return allChrcs;
    }

    public List<GattDBCharacteristic> getCharacteristicByUUID(int startHandle, int endHandle,
                                                              UUID uuid) {
        List<GattDBCharacteristic> allChrcs = new ArrayList<>();

        for (GattDBService svc : mServices) {
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                if (chr.getDefHandle() >= startHandle && chr.getDefHandle() <= endHandle &&
                        chr.getCharacteristic().getUuid().equals(uuid)) {
                    allChrcs.add(chr);
                }
            }
        }

        return allChrcs;
    }

    public List<GattDBDescriptor> getAllDescriptors(int startHandle, int endHandle) {
        List<GattDBDescriptor> allDescs = new ArrayList<>();

        for (GattDBService svc : mServices) {
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                for (GattDBDescriptor dsc : chr.getDescriptors()) {
                    if (dsc.getHandle() >= startHandle && dsc.getHandle() <= endHandle) {
                        allDescs.add(dsc);
                    }
                }
            }
        }

        return allDescs;
    }

    public GattDBCharacteristic findCharacteristic(int handle) {
        for (GattDBService svc : mServices) {
            Log.d(GATT_TAG, String.format("service UUID=%s TYPE=%d START_HDL=%d END_HDL=%d",
                    svc.getService().getUuid(), svc.getService().getType(),
                    svc.getStartHandle(), svc.getEndHandle()));
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                Log.d(GATT_TAG, String.format("characteristic UUID=%s PROPS=%d PERMS=%d" +
                                "DEF_HDL=%d VAL_HDL=%d",
                        chr.getCharacteristic().getUuid(), chr.getCharacteristic().getProperties(),
                        chr.getCharacteristic().getPermissions(),
                        chr.getDefHandle(), chr.getValHandle()));

                if (chr.getValHandle() == handle) {
                    return chr;
                }
            }
        }

        return null;
    }

    public GattDBDescriptor findDescriptor(int handle) {
        for (GattDBService svc : mServices) {
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                for (GattDBDescriptor dsc : chr.getDescriptors()) {
                    Log.d(GATT_TAG, String.format("descriptor UUID=%s PERMS=%d HDL=%d",
                            dsc.getDescriptor().getUuid(),
                            dsc.getDescriptor().getPermissions(),
                            dsc.getHandle()));
                    if (dsc.getHandle() == handle) {
                        return dsc;
                    }
                }
            }
        }

        return null;
    }

    public GattDBCharacteristic findCCCDCharacteristic(int cccdHandle) {
        for (GattDBService svc : mServices) {
            for (GattDBCharacteristic chr : svc.getCharacteristics()) {
                for (GattDBDescriptor dsc : chr.getDescriptors()) {
                    Log.d(GATT_TAG, String.format("descriptor UUID=%s PERMS=%d HDL=%d",
                            dsc.getDescriptor().getUuid(),
                            dsc.getDescriptor().getPermissions(),
                            dsc.getHandle()));
                    if (dsc.getHandle() == cccdHandle) {
                        return chr;
                    }
                }
            }
        }

        return null;
    }

    public boolean gattRead(int handle, DataReceivedCallback cb) {
        GattDBCharacteristic chr = findCharacteristic(handle);
        if (chr != null) {
            ReadRequest req = readCharacteristic(chr.getCharacteristic());
            req.with(cb).enqueue();
            return true;
        }

        GattDBDescriptor dsc = findDescriptor(handle);
        if (dsc != null) {
            ReadRequest req = readDescriptor(dsc.getDescriptor());
            req.with(cb).enqueue();
            return true;
        }

        return false;
    }

    public boolean gattWrite(int handle, byte[] data, DataSentCallback cb) {
        GattDBCharacteristic chr = findCharacteristic(handle);
        if (chr != null) {
            WriteRequest req = writeCharacteristic(chr.getCharacteristic(), data);
            req.with(cb).enqueue();
            return true;
        }

        GattDBDescriptor dsc = findDescriptor(handle);
        if (dsc != null) {
            WriteRequest req = writeDescriptor(dsc.getDescriptor(), data);
            req.with(cb).enqueue();
            return true;
        }

        return false;
    }

    public boolean gattWriteLong(int handle, int offset, byte[] data, DataSentCallback cb) {
        GattDBCharacteristic chr = findCharacteristic(handle);
        if (chr != null) {
            WriteRequest req = writeCharacteristic(chr.getCharacteristic(), data,
                    offset, data.length);
            req.with(cb).enqueue();
            return true;
        }

        GattDBDescriptor dsc = findDescriptor(handle);
        if (dsc != null) {
            WriteRequest req = writeDescriptor(dsc.getDescriptor(), data, offset, data.length);
            req.with(cb).enqueue();
            return true;
        }

        return false;
    }


    public boolean configSubscription(int cccdHandle, byte opcode, int enable,
                                      GAP.NotificationReceivedCallback cb) {
        GattDBCharacteristic chr = findCCCDCharacteristic(cccdHandle);
        WriteRequest req;
        if (chr == null) {
            return false;
        }

        if (enable == 0){
            if (opcode == BTP.GATT_CFG_NOTIFY) {
                req = disableNotifications(chr.getCharacteristic());
            } else {
                req = disableIndications(chr.getCharacteristic());
            }
            req.enqueue();
            return true;
        }


        if (opcode == BTP.GATT_CFG_NOTIFY) {
            cb.type = 0x01;
            cb.handle = chr.getValHandle();

            req = enableNotifications(chr.getCharacteristic());

            ValueChangedCallback valueChangedCallback =
                    setNotificationCallback(chr.getCharacteristic());
            valueChangedCallback.with(cb);
        } else {
            cb.type = 0x02;
            cb.handle = chr.getValHandle();

            req = enableIndications(chr.getCharacteristic());

            ValueChangedCallback valueChangedCallback =
                    setIndicationCallback(chr.getCharacteristic());
            valueChangedCallback.with(cb);
        }
        req.enqueue();
        return true;
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

    @NonNull
    @Override
    protected ValueChangedCallback setNotificationCallback(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.setNotificationCallback(characteristic);
    }

    @NonNull
    @Override
    protected ValueChangedCallback setIndicationCallback(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.setIndicationCallback(characteristic);
    }

    @NonNull
    @Override
    protected WriteRequest enableNotifications(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.enableNotifications(characteristic);
    }

    @NonNull
    @Override
    protected WriteRequest disableNotifications(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.disableNotifications(characteristic);
    }

    @NonNull
    @Override
    protected WriteRequest enableIndications(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.enableIndications(characteristic);
    }

    @NonNull
    @Override
    protected WriteRequest disableIndications(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.disableIndications(characteristic);
    }

    @NonNull
    @Override
    protected ReadRequest readCharacteristic(@Nullable BluetoothGattCharacteristic characteristic) {
        return super.readCharacteristic(characteristic);
    }

    @NonNull
    @Override
    protected WriteRequest writeCharacteristic(@Nullable BluetoothGattCharacteristic characteristic, @Nullable byte[] data) {
        return super.writeCharacteristic(characteristic, data);
    }

    @NonNull
    @Override
    protected ReadRequest readDescriptor(@Nullable BluetoothGattDescriptor descriptor) {
        return super.readDescriptor(descriptor);
    }

    @NonNull
    @Override
    protected WriteRequest writeDescriptor(@Nullable BluetoothGattDescriptor descriptor, @Nullable byte[] data) {
        return super.writeDescriptor(descriptor, data);
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
        Log.d(GAP_TAG, String.format("onPairingRequestReceived %s %s", device,
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
            Log.d(GATT_TAG, String.format("isRequiredServiceSupported %s", gatt));
            mServices = Utils.initializeGattDB(gatt.getServices());
            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
            Log.d(GAP_TAG, "onDeviceDisconnected");
        }
    };
}
