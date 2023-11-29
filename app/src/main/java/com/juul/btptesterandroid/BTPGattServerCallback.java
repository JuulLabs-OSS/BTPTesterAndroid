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

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.util.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import no.nordicsemi.android.ble.BleManagerCallbacks;

import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE;
import static com.juul.btptesterandroid.Utils.BT_BASE_UUID_STR;
import static com.juul.btptesterandroid.Utils.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID;
import static com.juul.btptesterandroid.Utils.stringToUUID;

public class BTPGattServerCallback extends BluetoothGattServerCallback {

    private final static String TAG = "GATT";
    private final BleManagerCallbacks managerCallbacks;
    private boolean peripheral = false;
    private BluetoothGattServer gattServer;
    private IGattServerCallbacks valueChangedCb;
    private PrepWriteContext<BluetoothGattCharacteristic> prepWriteCharContext;
    private PrepWriteContext<BluetoothGattDescriptor> prepWriteDescContext;

    public BTPGattServerCallback(BleManagerCallbacks managerCallbacks) {
        super();
        this.managerCallbacks = managerCallbacks;
    }

    public void isPeripheral() {
        peripheral = true;
    }

    public void isCentral() {
        peripheral = false;
    }

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);
        if (!peripheral) {
            return;
        }

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                this.managerCallbacks.onDeviceReady(device);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                this.managerCallbacks.onDeviceDisconnected(device);
            }
        }
    }

    public static final String PTS_DB_UUID_FMT = "0000%s-8C26-476F-89A7-A108033A69C7";
    public static final String PTS_SVC = String.format(PTS_DB_UUID_FMT, "0001");
    public static final String PTS_CHR_READ_WRITE = String.format(PTS_DB_UUID_FMT, "0006");
    public static final String PTS_DSC_READ_WRITE = String.format(PTS_DB_UUID_FMT, "000B");
    public static final String PTS_CHR_READ_WRITE_LONG = String.format(PTS_DB_UUID_FMT, "0015");
    public static final String PTS_DSC_READ_WRITE_LONG = String.format(PTS_DB_UUID_FMT, "001B");
    public static final String PTS_CHR_NOTIFY = String.format(PTS_DB_UUID_FMT, "0025");
    public static final String CCCD_UUID = BT_BASE_UUID_STR.replace("00000000",
            "00002902");
    public static final String PTS_INC_SVC = BT_BASE_UUID_STR.replace("00000000",
            "0000001E");

    byte[] shortValue = new byte[1];
    byte[] longValue = new byte[100];

    private void addTestIncludeService() {
        BluetoothGattService incSvc = new BluetoothGattService(stringToUUID(PTS_INC_SVC),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        gattServer.addService(incSvc);
    }

    private void addTestService(BluetoothGattService incService) {
        BluetoothGattService svc = new BluetoothGattService(stringToUUID(PTS_SVC),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        svc.addService(incService);

        BluetoothGattCharacteristic chrReadWrite = new BluetoothGattCharacteristic(
                stringToUUID(PTS_CHR_READ_WRITE), PROPERTY_READ | PROPERTY_WRITE,
                PERMISSION_READ | PERMISSION_WRITE);
        chrReadWrite.setValue(shortValue);

        BluetoothGattDescriptor dscReadWrite = new BluetoothGattDescriptor(
                stringToUUID(PTS_DSC_READ_WRITE), PERMISSION_READ | PERMISSION_WRITE);
        dscReadWrite.setValue(shortValue);

        chrReadWrite.addDescriptor(dscReadWrite);


        BluetoothGattDescriptor dscReadWriteLong = new BluetoothGattDescriptor(
                stringToUUID(PTS_DSC_READ_WRITE_LONG),
                PERMISSION_READ | PERMISSION_WRITE);
        dscReadWriteLong.setValue(longValue);

        chrReadWrite.addDescriptor(dscReadWriteLong);
        svc.addCharacteristic(chrReadWrite);

        BluetoothGattCharacteristic chrReadWriteLong = new BluetoothGattCharacteristic(
                stringToUUID(PTS_CHR_READ_WRITE_LONG), PROPERTY_READ | PROPERTY_WRITE,
                PERMISSION_READ | PERMISSION_WRITE);
        chrReadWriteLong.setValue(longValue);
        svc.addCharacteristic(chrReadWriteLong);

        BluetoothGattCharacteristic chrNotify = new BluetoothGattCharacteristic(
                stringToUUID(PTS_CHR_NOTIFY),
                PROPERTY_NOTIFY | PROPERTY_INDICATE, PERMISSION_READ);
        chrNotify.setValue(shortValue);

        BluetoothGattDescriptor dscCCC = new BluetoothGattDescriptor(
                stringToUUID(CCCD_UUID),
                PERMISSION_READ | PERMISSION_WRITE);
        chrNotify.addDescriptor(dscCCC);

        svc.addCharacteristic(chrNotify);

        gattServer.addService(svc);
    }

    public void setGattServer(BluetoothGattServer server) {
        gattServer = server;

        addTestIncludeService();
    }

    public void setGattAttributeValueChangedCallback(IGattServerCallbacks valueChangedCb) {
        this.valueChangedCb = valueChangedCb;
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Log.d(TAG, "onServiceAdded");

        if (service.getUuid().equals(stringToUUID(PTS_INC_SVC))) {
            addTestService(service);
        }
    }

    class PrepWriteContext<Attribute> {
        BluetoothDevice device;
        Attribute attr;
        byte[] value = {};

        PrepWriteContext(BluetoothDevice device,
                         Attribute attr) {
            this.device = device;
            this.attr = attr;
        }

        public void update(int offset, byte[] newVal) {
            int newLen = newVal.length + offset;
            byte[] tempVal = new byte[newLen];
            System.arraycopy(value, 0, tempVal, 0, offset);
            System.arraycopy(newVal, 0, tempVal, offset, newVal.length);
            value = tempVal;
        }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        Log.d(TAG, String.format("onCharacteristicReadRequest %d %d", requestId, offset));
        byte[] value = characteristic.getValue();
        if ((value.length - offset) <= 0) {
            gattServer.sendResponse(device, requestId, 0, offset, null);
            return;
        }

        /* TODO: Fix copying of almost whole array */
        byte[] newval = Arrays.copyOfRange(value, offset, value.length);
        gattServer.sendResponse(device, requestId, 0, offset, newval);
    }

    @SuppressLint("Assert")
    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic,
                preparedWrite, responseNeeded, offset, value);
        Log.d(TAG, String.format("onCharacteristicWriteRequest reqId %d offset %d " +
                        "prepWrite %b rsp %b", requestId, offset,
                preparedWrite, responseNeeded));

        // Verify that offset is 0 when using normal write
        assert(offset == 0 || preparedWrite);

        if (preparedWrite) {
            if (this.prepWriteCharContext == null) {
                prepWriteCharContext = new PrepWriteContext<>(device, characteristic);
            }

            prepWriteCharContext.update(offset, value);
            gattServer.sendResponse(device, requestId, 0, offset, value);
            return;
        }

        characteristic.setValue(value);

        if (responseNeeded) {
            gattServer.sendResponse(device, requestId, 0, offset, value);
        }

        valueChangedCb.characteristicValueChanged(device, characteristic, value);
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                        int offset, BluetoothGattDescriptor descriptor) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
        Log.d(TAG, "onDescriptorReadRequest");
        byte[] value = descriptor.getValue();
        if ((value.length - offset) <= 0) {
            gattServer.sendResponse(device, requestId, 0, offset, null);
            return;
        }

        /* TODO: Fix to not copy the whole array */
        byte[] newval = Arrays.copyOfRange(value, offset, value.length);
        gattServer.sendResponse(device, requestId, 0, offset, newval);
    }

    @SuppressLint("Assert")
    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite, boolean responseNeeded,
                                         int offset, byte[] value) {
        super.onDescriptorWriteRequest(device, requestId, descriptor,
                preparedWrite, responseNeeded, offset, value);
        Log.d(TAG, String.format("onDescriptorWriteRequest reqId %d offset %d " +
                        "prepWrite %b rsp %b UUID %s", requestId, offset,
                preparedWrite, responseNeeded, descriptor.getUuid()));

        // Verify that offset is 0 when using normal write
        assert(offset == 0 || preparedWrite);

        if (preparedWrite) {
            if (this.prepWriteDescContext == null) {
                prepWriteDescContext = new PrepWriteContext<>(device, descriptor);
            }

            prepWriteDescContext.update(offset, value);
            gattServer.sendResponse(device, requestId, 0, offset, value);
            return;
        }

        int status;

        if (descriptor.getUuid().equals(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID)) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            boolean supportsNotifications = (characteristic.getProperties() &
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
            boolean supportsIndications = (characteristic.getProperties() &
                    BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;

            Log.d(TAG, String.format("cccd notifications %b indications %b",
                    supportsNotifications, supportsIndications));

            if (!(supportsNotifications || supportsIndications)) {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                Log.d(TAG, "Not supported");
            } else if (value.length != 2) {
                status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                Log.d(TAG, "invalid length");
            } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                status = notificationsDisabled(device, characteristic);
                descriptor.setValue(value);
                Log.d(TAG, "notifications disable");
            } else if (supportsNotifications &&
                    Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                status = notificationsEnabled(device, characteristic, false /* indicate */);
                descriptor.setValue(value);
                Log.d(TAG, "notifications enable");
            } else if (supportsIndications &&
                    Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                status = notificationsEnabled(device, characteristic, true /* indicate */);
                descriptor.setValue(value);
                Log.d(TAG, "indications enable");
            } else {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                Log.d(TAG, "not supported 2");
            }
        } else {
            status = BluetoothGatt.GATT_SUCCESS;
            descriptor.setValue(value);

            Log.d(TAG, "not cccd");
        }
        if (responseNeeded) {
            gattServer.sendResponse(device, requestId, status,
                    /* No need to respond with offset */ 0,
                    /* No need to respond with a value */ null);
        }

        valueChangedCb.descriptorValueChanged(device, descriptor, value);
    }

    private Map<BluetoothGattCharacteristic, Set<Pair<BluetoothDevice, Boolean>>> subscribed = new HashMap<>();

    private int notificationsEnabled(BluetoothDevice device,
                                     BluetoothGattCharacteristic characteristic,
                                     boolean indication) {
        Log.d(TAG, "notificationsEnabled");
        Set<Pair<BluetoothDevice, Boolean>> subs = subscribed.get(characteristic);
        if (subs==null) {
            subs = new HashSet<>();
            subscribed.put(characteristic, subs);
        }
        for (Pair<BluetoothDevice, Boolean> f : subs) {
            if (f.first.equals(device)) {
                return GATT_FAILURE;
            }
        }

        subs.add(new Pair<>(device, indication));

        notifyCharacteristicChanged(characteristic);

        return 0;
    }

    private int notificationsDisabled(BluetoothDevice device,
                                      BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "notificationsDisabled");
        Set<Pair<BluetoothDevice, Boolean>> subs = subscribed.get(characteristic);
        if (subs==null) {
            subs = new HashSet<>();
            subscribed.put(characteristic, subs);
        }

        for (Pair<BluetoothDevice, Boolean> f : subs) {
            if (f.first.equals(device)) {
                subs.remove(f);
                return 0;
            }
        }

        return GATT_FAILURE;
    }

    private void notifyCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "notifyCharacteristicChanged");

        Set<Pair<BluetoothDevice, Boolean>> subs = subscribed.get(characteristic);
        if (subs==null) {
            subs = new HashSet<>();
            subscribed.put(characteristic, subs);
        }

        for (Pair<BluetoothDevice, Boolean> f : subs) {
            gattServer.notifyCharacteristicChanged(f.first, characteristic, f.second);
        }
    }

    @SuppressLint("Assert")
    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        super.onExecuteWrite(device, requestId, execute);
        Log.d(TAG, "onExecuteWrite");

        if (prepWriteCharContext != null) {
            if (execute) {
                gattServer.sendResponse(device, requestId, 0, 0, null);
                valueChangedCb.characteristicValueChanged(device, prepWriteCharContext.attr,
                        prepWriteCharContext.value);
            }

            prepWriteCharContext = null;

            return;
        }

        if (prepWriteDescContext != null) {
            if (execute) {
                gattServer.sendResponse(device, requestId, 0, 0, null);
                valueChangedCb.descriptorValueChanged(device, prepWriteDescContext.attr,
                        prepWriteDescContext.value);
            }

            prepWriteCharContext = null;
        }
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
        Log.d(TAG, "onNotificationSent");
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
        Log.d(TAG, "onMtuChanged");
    }

    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(device, txPhy, rxPhy, status);
        Log.d(TAG, "onPhyUpdate");
    }

    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyRead(device, txPhy, rxPhy, status);
        Log.d(TAG, "onPhyRead");
    }

    public boolean setValue(int attrId, byte[] value) {
        int i = 0;

        for (BluetoothGattService svc : gattServer.getServices()) {
            Log.d(TAG, String.format("service UUID=%s TYPE=%d",
                    svc.getUuid(), svc.getType()));
            ++i;
            if (i == attrId) {
                return false;
            }

            for (BluetoothGattService inc : svc.getIncludedServices()) {
                Log.d(TAG, String.format("include UUID=%s TYPE=%d",
                        inc.getUuid(), inc.getType()));
                ++i;
                if (i == attrId) {
                    return false;
                }
            }

            for (BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                Log.d(TAG, String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                        chr.getUuid(), chr.getProperties(), chr.getPermissions()));
                ++i;
                if (i == attrId) {
                    chr.setValue(value);
                    notifyCharacteristicChanged(chr);
                    return true;
                }

                for (BluetoothGattDescriptor dsc : chr.getDescriptors()) {
                    Log.d(TAG, String.format("descriptor UUID=%s PERMS=%d",
                            dsc.getUuid(), dsc.getPermissions()));
                    ++i;
                    if (i == attrId) {
                        dsc.setValue(value);
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
