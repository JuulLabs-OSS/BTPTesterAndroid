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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.nordicsemi.android.ble.BleManagerCallbacks;

public class BTPGattServerCallback extends BluetoothGattServerCallback {

    private final BleManagerCallbacks managerCallbacks;
    private boolean peripheral = false;
    private List<BluetoothGattService> addedServices;
    private BluetoothGattServer gattServer;
    private IGattAttrValueChanged valueChangedCb;
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

    public void setGattAttributeValueChangedCallback(IGattAttrValueChanged valueChangedCb) {
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
                        "prepWrite %b rsp %b", requestId, offset,
                preparedWrite, responseNeeded));

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

        descriptor.setValue(value);

        if (responseNeeded) {
            gattServer.sendResponse(device, requestId, 0, offset, value);
        }

        valueChangedCb.descriptorValueChanged(device, descriptor, value);
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
