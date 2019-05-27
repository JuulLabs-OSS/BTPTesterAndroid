package com.juul.btptesterandroid;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.nordicsemi.android.ble.BleManagerCallbacks;

import static android.bluetooth.BluetoothGatt.GATT_FAILURE;
import static com.juul.btptesterandroid.Utils.CLIENT_CHARACTERISTIC_CONFIGURATION_UUID;

public class BTPGattServerCallback extends BluetoothGattServerCallback {

    private final BleManagerCallbacks managerCallbacks;
    private boolean peripheral = false;
    private List<BluetoothGattService> addedServices;
    private BluetoothGattServer gattServer;
    private IGattServerCallbacks valueChangedCb;
    private PrepWriteContext<BluetoothGattCharacteristic> prepWriteCharContext;
    private PrepWriteContext<BluetoothGattDescriptor> prepWriteDescContext;

    public BTPGattServerCallback(BleManagerCallbacks managerCallbacks) {
        super();
        this.managerCallbacks = managerCallbacks;
        addedServices = new ArrayList<>();
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

    public void setGattServer(BluetoothGattServer server) {
        gattServer = server;
    }

    public void setGattAttributeValueChangedCallback(IGattServerCallbacks valueChangedCb) {
        this.valueChangedCb = valueChangedCb;
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Log.d("GATT", "onServiceAdded");
        addedServices.add(service);
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
        Log.d("GATT", String.format("onCharacteristicReadRequest %d %d", requestId, offset));
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
        Log.d("GATT", String.format("onCharacteristicWriteRequest reqId %d offset %d " +
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
        Log.d("GATT", "onDescriptorReadRequest");
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
        Log.d("GATT", String.format("onDescriptorWriteRequest reqId %d offset %d " +
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

            Log.d("GATT", String.format("cccd notifications %b indications %b",
                    supportsNotifications, supportsIndications));

            if (!(supportsNotifications || supportsIndications)) {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                Log.d("GATT", "Not supported");
            } else if (value.length != 2) {
                status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
                Log.d("GATT", "invalid length");
            } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                status = notificationsDisabled(device, characteristic);
                descriptor.setValue(value);
                Log.d("GATT", "notifications disable");
            } else if (supportsNotifications &&
                    Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                status = notificationsEnabled(device, characteristic, false /* indicate */);
                descriptor.setValue(value);
                Log.d("GATT", "notifications enable");
            } else if (supportsIndications &&
                    Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                status = notificationsEnabled(device, characteristic, true /* indicate */);
                descriptor.setValue(value);
                Log.d("GATT", "indications enable");
            } else {
                status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
                Log.d("GATT", "not supported 2");
            }
        } else {
            status = BluetoothGatt.GATT_SUCCESS;
            descriptor.setValue(value);

            Log.d("GATT", "not cccd");
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
        Log.d("GATT", "notificationsEnabled");
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
        return 0;
    }

    private int notificationsDisabled(BluetoothDevice device,
                                      BluetoothGattCharacteristic characteristic) {
        Log.d("GATT", "notificationsDisabled");
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
        Log.d("GATT", "notifyCharacteristicChanged");

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
        Log.d("GATT", "onExecuteWrite");

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
        Log.d("GATT", "onNotificationSent");
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
        Log.d("GATT", "onMtuChanged");
    }

    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(device, txPhy, rxPhy, status);
        Log.d("GATT", "onPhyUpdate");
    }

    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyRead(device, txPhy, rxPhy, status);
        Log.d("GATT", "onPhyRead");
    }

    public boolean setValue(int attrId, byte[] value) {
        int i = 0;

        for (BluetoothGattService svc : addedServices) {
            Log.d("GATT", String.format("service UUID=%s TYPE=%d",
                    svc.getUuid(), svc.getType()));
            ++i;
            if (i == attrId) {
                return false;
            }

            for (BluetoothGattService inc : svc.getIncludedServices()) {
                Log.d("GATT", String.format("include UUID=%s TYPE=%d",
                        inc.getUuid(), inc.getType()));
                ++i;
                if (i == attrId) {
                    return false;
                }
            }

            for (BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                Log.d("GATT", String.format("characteristic UUID=%s PROPS=%d PERMS=%d",
                        chr.getUuid(), chr.getProperties(), chr.getPermissions()));
                ++i;
                if (i == attrId) {
                    chr.setValue(value);
                    notifyCharacteristicChanged(chr);
                    return true;
                }

                for (BluetoothGattDescriptor dsc : chr.getDescriptors()) {
                    Log.d("GATT", String.format("descriptor UUID=%s PERMS=%d",
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
