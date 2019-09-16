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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import com.juul.btptesterandroid.gatt.GattDBCharacteristic;
import com.juul.btptesterandroid.gatt.GattDBDescriptor;
import com.juul.btptesterandroid.gatt.GattDBIncludeService;
import com.juul.btptesterandroid.gatt.GattDBService;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.exception.BluetoothDisabledException;
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException;
import no.nordicsemi.android.ble.exception.InvalidRequestException;
import no.nordicsemi.android.ble.exception.RequestFailedException;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;
import static android.content.ContentValues.TAG;
import static com.juul.btptesterandroid.BTP.BTP_INDEX_NONE;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_GAP;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_GATT;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_FAILED;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_SUCCESS;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_UNKNOWN_CMD;
import static com.juul.btptesterandroid.BTP.GAP_CONNECT;
import static com.juul.btptesterandroid.BTP.GAP_DISCONNECT;
import static com.juul.btptesterandroid.BTP.GAP_EV_CONN_PARAM_UPDATE;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_CONNECTED;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_DISCONNECTED;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_FOUND;
import static com.juul.btptesterandroid.BTP.GAP_EV_PAIRING_CONSENT;
import static com.juul.btptesterandroid.BTP.GAP_EV_PASSKEY_CONFIRM_REQ;
import static com.juul.btptesterandroid.BTP.GAP_EV_PASSKEY_DISPLAY;
import static com.juul.btptesterandroid.BTP.GAP_EV_PASSKEY_ENTRY_REQ;
import static com.juul.btptesterandroid.BTP.GAP_EV_SEC_LEVEL_CHANGED;
import static com.juul.btptesterandroid.BTP.GAP_GENERAL_DISCOVERABLE;
import static com.juul.btptesterandroid.BTP.GAP_PAIR;
import static com.juul.btptesterandroid.BTP.GAP_PASSKEY_CONFIRM;
import static com.juul.btptesterandroid.BTP.GAP_PASSKEY_ENTRY;
import static com.juul.btptesterandroid.BTP.GAP_READ_CONTROLLER_INDEX_LIST;
import static com.juul.btptesterandroid.BTP.GAP_READ_CONTROLLER_INFO;
import static com.juul.btptesterandroid.BTP.GAP_READ_SUPPORTED_COMMANDS;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_ADVERTISING;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_BONDABLE;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_CONNECTABLE;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_DISCOVERABLE;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_LE;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_POWERED;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_PRIVACY;
import static com.juul.btptesterandroid.BTP.GAP_SETTINGS_STATIC_ADDRESS;
import static com.juul.btptesterandroid.BTP.GAP_SET_CONNECTABLE;
import static com.juul.btptesterandroid.BTP.GAP_SET_DISCOVERABLE;
import static com.juul.btptesterandroid.BTP.GAP_SET_IO_CAP;
import static com.juul.btptesterandroid.BTP.GAP_START_ADVERTISING;
import static com.juul.btptesterandroid.BTP.GAP_START_DISCOVERY;
import static com.juul.btptesterandroid.BTP.GAP_STOP_ADVERTISING;
import static com.juul.btptesterandroid.BTP.GAP_STOP_DISCOVERY;
import static com.juul.btptesterandroid.BTP.GAP_UNPAIR;
import static com.juul.btptesterandroid.BTP.GATT_ADD_CHARACTERISTIC;
import static com.juul.btptesterandroid.BTP.GATT_ADD_DESCRIPTOR;
import static com.juul.btptesterandroid.BTP.GATT_ADD_INCLUDED_SERVICE;
import static com.juul.btptesterandroid.BTP.GATT_ADD_SERVICE;
import static com.juul.btptesterandroid.BTP.GATT_CFG_INDICATE;
import static com.juul.btptesterandroid.BTP.GATT_CFG_NOTIFY;
import static com.juul.btptesterandroid.BTP.GATT_DISC_ALL_CHRC;
import static com.juul.btptesterandroid.BTP.GATT_DISC_ALL_DESC;
import static com.juul.btptesterandroid.BTP.GATT_DISC_ALL_PRIM_SVCS;
import static com.juul.btptesterandroid.BTP.GATT_DISC_CHRC_UUID;
import static com.juul.btptesterandroid.BTP.GATT_DISC_FIND_INCLUDED;
import static com.juul.btptesterandroid.BTP.GATT_DISC_PRIM_UUID;
import static com.juul.btptesterandroid.BTP.GATT_EV_ATTR_VALUE_CHANGED;
import static com.juul.btptesterandroid.BTP.GATT_EV_NOTIFICATION;
import static com.juul.btptesterandroid.BTP.GATT_GET_ATTRIBUTES;
import static com.juul.btptesterandroid.BTP.GATT_GET_ATTRIBUTE_VALUE;
import static com.juul.btptesterandroid.BTP.GATT_READ;
import static com.juul.btptesterandroid.BTP.GATT_READ_LONG;
import static com.juul.btptesterandroid.BTP.GATT_READ_SUPPORTED_COMMANDS;
import static com.juul.btptesterandroid.BTP.GATT_SET_VALUE;
import static com.juul.btptesterandroid.BTP.GATT_START_SERVER;
import static com.juul.btptesterandroid.BTP.GATT_WRITE;
import static com.juul.btptesterandroid.BTP.GATT_WRITE_LONG;
import static com.juul.btptesterandroid.Utils.btAddrToBytes;
import static com.juul.btptesterandroid.Utils.bytesToHex;
import static com.juul.btptesterandroid.Utils.clearBit;
import static com.juul.btptesterandroid.Utils.setBit;
import static com.juul.btptesterandroid.Utils.testBit;

public class GAP implements BleManagerCallbacks, IGattServerCallbacks {

    private Context context;
    private BTTester tester = null;
    private BluetoothAdapter bleAdapter = null;
    private BluetoothManager bleManager = null;

    private Map<String, BleConnectionManager> connectionsMap;
    private Set<BluetoothDevice> processedDevices;

    private BluetoothGattServer gattServer;
    private BTPGattServerCallback gattServerCallback;
    private BluetoothLeScannerCompat scanner;
    private ScanConnectCallback scanCallback;
    private BluetoothLeAdvertiser advertiser;
    private BTPAdvertiseCallback advertiseCallback;

    private int attributeCount;
    private List<BluetoothGattService> addedServices;

    private BluetoothGattService lastAddedService = null;
    private BluetoothGattCharacteristic lastAddedCharacteristic;
    private BluetoothGattDescriptor lastAddedDescriptor;

    private static final byte CONTROLLER_INDEX = 0;
    private byte[] supportedSettings = new byte[4];
    private byte[] currentSettings = new byte[4];
    private int pairingVariant;

    public byte init(Context context, BTTester tester, BluetoothAdapter bleAdapter,
                     BluetoothManager bleManager) {
        this.context = context;
        this.tester = tester;
        this.bleAdapter = bleAdapter;
        this.bleManager = bleManager;

        this.gattServerCallback = new BTPGattServerCallback(this);
        this.gattServer = this.bleManager.openGattServer(context, gattServerCallback);
        this.gattServerCallback.setGattServer(this.gattServer);
        this.gattServerCallback.setGattAttributeValueChangedCallback(this);

        if (this.gattServer == null) {
            return BTP_STATUS_FAILED;
        }

        attributeCount = 0;
        addedServices = new ArrayList<>();
        processedDevices = new HashSet<>();

        this.scanner = BluetoothLeScannerCompat.getScanner();
        this.scanCallback = new ScanConnectCallback();

        this.advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        connectionsMap = new HashMap<>();

        pairingVariant = PAIRING_VARIANT_CONSENT;

        this.context.registerReceiver(incomingPairRequestReceiver,
                new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));

        this.context.registerReceiver(bondStateChangedReceiver,
                new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        return BTP_STATUS_SUCCESS;
    }

    public BleConnectionManager findConnection(String addr) {
        return connectionsMap.getOrDefault(addr, null);
    }

    private void unpairDevice(BluetoothDevice dev) {
        try {
            Method m = dev.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(dev, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void cleanup() {
        try {
            for (Map.Entry<String, BleConnectionManager> entry : connectionsMap.entrySet()) {
                BleConnectionManager mng = entry.getValue();
                if (mng == null) {
                    continue;
                }

                if (mng.isConnected()) {
                    mng.disconnect().await();
                }

                mng.removeBond().enqueue();
            }

            for (BluetoothDevice dev : processedDevices) {
                unpairDevice(dev);
            }

        } catch (RequestFailedException e) {
            e.printStackTrace();
        } catch (DeviceDisconnectedException e) {
            e.printStackTrace();
        } catch (BluetoothDisabledException e) {
            e.printStackTrace();
        } catch (InvalidRequestException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (gattServer != null) {
            gattServer.clearServices();
            gattServer.close();
        }

        if (advertiseCallback != null) {
            this.advertiser.stopAdvertising(advertiseCallback);
        }

        this.context.unregisterReceiver(incomingPairRequestReceiver);
        this.context.unregisterReceiver(bondStateChangedReceiver);
    }

    public byte unregister() {
        return BTP_STATUS_SUCCESS;
    }

    public void supportedCommandsGAP(ByteBuffer data) {
        byte[] cmds = new byte[3];

        setBit(cmds, GAP_READ_SUPPORTED_COMMANDS);
        setBit(cmds, GAP_READ_CONTROLLER_INDEX_LIST);
        setBit(cmds, GAP_READ_CONTROLLER_INFO);
        setBit(cmds, GAP_SET_CONNECTABLE);
        setBit(cmds, GAP_SET_DISCOVERABLE);
        setBit(cmds, GAP_START_ADVERTISING);
        setBit(cmds, GAP_STOP_ADVERTISING);
        setBit(cmds, GAP_START_DISCOVERY);
        setBit(cmds, GAP_STOP_DISCOVERY);
        setBit(cmds, GAP_CONNECT);
        setBit(cmds, GAP_DISCONNECT);
        setBit(cmds, GAP_SET_IO_CAP);
        setBit(cmds, GAP_PAIR);
        setBit(cmds, GAP_UNPAIR);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_READ_SUPPORTED_COMMANDS, CONTROLLER_INDEX,
                cmds);
    }

    public void controllerIndexList(ByteBuffer data) {
        ByteBuffer rp = ByteBuffer.allocate(2);
        rp.order(ByteOrder.LITTLE_ENDIAN);
        int len = 1;

        rp.put((byte) len);
        rp.put(CONTROLLER_INDEX);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_READ_CONTROLLER_INDEX_LIST,
                CONTROLLER_INDEX, rp.array());
    }

    public void controllerInfo(ByteBuffer data) {
        BTP.GapReadControllerInfoRp rp = new BTP.GapReadControllerInfoRp();

        Log.d("GAP", String.format("device address '%s'", bleAdapter.getAddress()));
        byte[] addr = btAddrToBytes(bleAdapter.getAddress());
        System.arraycopy(addr, 0, rp.address, 0, rp.address.length);

        byte[] name = bleAdapter.getName().getBytes();
        System.arraycopy(name, 0, rp.name, 0, name.length);

        setBit(supportedSettings, GAP_SETTINGS_POWERED);
        setBit(supportedSettings, GAP_SETTINGS_BONDABLE);
        setBit(supportedSettings, GAP_SETTINGS_LE);
        setBit(supportedSettings, GAP_SETTINGS_CONNECTABLE);
        setBit(supportedSettings, GAP_SETTINGS_DISCOVERABLE);
        setBit(supportedSettings, GAP_SETTINGS_ADVERTISING);
        setBit(supportedSettings, GAP_SETTINGS_STATIC_ADDRESS);
        setBit(supportedSettings, GAP_SETTINGS_PRIVACY);

        setBit(currentSettings, GAP_SETTINGS_POWERED);
        setBit(currentSettings, GAP_SETTINGS_BONDABLE);
        setBit(currentSettings, GAP_SETTINGS_LE);
        setBit(currentSettings, GAP_SETTINGS_DISCOVERABLE);
        setBit(currentSettings, GAP_SETTINGS_PRIVACY);

        System.arraycopy(supportedSettings, 0, rp.supportedSettings,
                0, rp.supportedSettings.length);

        System.arraycopy(currentSettings, 0, rp.currentSettings,
                0, rp.currentSettings.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_READ_CONTROLLER_INFO,
                CONTROLLER_INDEX, rp.toBytes());
    }

    public class BTPAdvertiseCallback extends AdvertiseCallback {
        byte opcode;

        public BTPAdvertiseCallback(byte opcode) {
            this.opcode = opcode;
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

            Log.d("GAP", "Advertise onStartSuccess");

            if (opcode == GAP_START_ADVERTISING) {
                setBit(currentSettings, GAP_SETTINGS_ADVERTISING);
            } else {
                clearBit(currentSettings, GAP_SETTINGS_ADVERTISING);
            }

            BTP.GapStartAdvertisingRp rp = new BTP.GapStartAdvertisingRp();
            System.arraycopy(currentSettings, 0,
                    rp.currentSettings, 0, currentSettings.length);
            tester.sendMessage(BTP_SERVICE_ID_GAP, opcode, CONTROLLER_INDEX, rp.toBytes());
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);

            Log.d("GAP", "Advertise onStartSuccess");

            tester.response(BTP_SERVICE_ID_GAP, GAP_START_ADVERTISING, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
        }
    }

    public void setConnectable(ByteBuffer data) {
        BTP.GapSetConnectableCmd cmd = BTP.GapSetConnectableCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_SET_CONNECTABLE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        Log.d("GAP", String.format("setConnectable 0x%02x", cmd.connectable));

        if (cmd.connectable > 0) {
            setBit(currentSettings, GAP_SETTINGS_CONNECTABLE);
        } else {
            clearBit(currentSettings, GAP_SETTINGS_CONNECTABLE);
        }

        BTP.GapSetConnectableRp rp = new BTP.GapSetConnectableRp();
        System.arraycopy(currentSettings, 0, rp.currentSettings, 0, currentSettings.length);
        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_SET_CONNECTABLE, CONTROLLER_INDEX,
                rp.toBytes());
    }

    public void setDiscoverable(ByteBuffer data) {
        BTP.GapSetDiscoverableCmd cmd = BTP.GapSetDiscoverableCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_SET_DISCOVERABLE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        Log.d("GAP", String.format("setDiscoverable 0x%02x", cmd.discoverable));

        if (cmd.discoverable != GAP_GENERAL_DISCOVERABLE) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_SET_DISCOVERABLE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BTP.GapSetDiscoverableRp rp = new BTP.GapSetDiscoverableRp();
        System.arraycopy(currentSettings, 0, rp.currentSettings, 0, currentSettings.length);
        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_SET_DISCOVERABLE, CONTROLLER_INDEX,
                rp.toBytes());
    }


    public void startAdvertising(ByteBuffer data) {
        BTP.GapStartAdvertisingCmd cmd = BTP.GapStartAdvertisingCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_START_ADVERTISING, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        Log.d("GAP", String.format("startAdvertising 0x%02x 0x%02x", cmd.advDataLen,
                cmd.scanRspDataLen));

        gattServerCallback.isPeripheral();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(testBit(currentSettings, GAP_SETTINGS_CONNECTABLE) > 0)
                .build();

        AdvertiseData.Builder adBuilder = new AdvertiseData.Builder();
        AdvertisingDataParser.parse(adBuilder, cmd.advData);
        AdvertiseData advertiseData = adBuilder.build();

        AdvertiseData.Builder sdBuilder = new AdvertiseData.Builder();
        AdvertisingDataParser.parse(sdBuilder, cmd.scanRspData);
        AdvertiseData scanrspData = sdBuilder.build();

        advertiseCallback = new BTPAdvertiseCallback(GAP_START_ADVERTISING);
        advertiser.startAdvertising(settings, advertiseData, scanrspData, advertiseCallback);
    }

    public void stopAdvertising(ByteBuffer data) {
        Log.d("GAP", "stopAdvertising 0x%02x 0x%02x");
        advertiser.stopAdvertising(advertiseCallback);
    }

    public void startDiscovery(ByteBuffer data) {
        BTP.GapStartDiscoveryCmd cmd = BTP.GapStartDiscoveryCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_START_DISCOVERY, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        Log.d("GAP", String.format("startDiscovery 0x%02x", cmd.flags));

        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(true)
                .build();

        scanCallback.clearCache();
        scanCallback.setDeviceDiscoveredCb(this::deviceFound);
        scanner.startScan(null, settings, scanCallback);

        if (scanCallback.getErrorCode() != 0) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_START_DISCOVERY, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        tester.response(BTP_SERVICE_ID_GAP, GAP_START_DISCOVERY, CONTROLLER_INDEX,
                BTP_STATUS_SUCCESS);
    }

    public void stopDiscovery(ByteBuffer data) {
        Log.d("GAP", "stopDiscovery");
        scanner.stopScan(scanCallback);
        if (scanCallback.getErrorCode() != 0) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_STOP_DISCOVERY, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        tester.response(BTP_SERVICE_ID_GAP, GAP_STOP_DISCOVERY, CONTROLLER_INDEX,
                BTP_STATUS_SUCCESS);
    }

    public void connect(ByteBuffer data) {
        BTP.GapConnectCmd cmd = BTP.GapConnectCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String bdAddr = Utils.btpToBdAddr(cmd.address);
        Log.d("GAP", String.format("connect %d %s", cmd.addressType,
                Utils.bytesToHex(cmd.address)));

        BluetoothDevice device = scanCallback.findDiscoveredDevice(bdAddr);
        if (device == null) {
            Log.d("GAP", "Connect: device not found");
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BleConnectionManager mng = new BleConnectionManager(this.context);
        mng.setGattCallbacks(this);
        gattServerCallback.isCentral();
        ConnectRequest req = mng.connect(device);
        req.enqueue();

        connectionsMap.put(bdAddr, mng);

        tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    public void disconnect(ByteBuffer data) {
        BTP.GapDisconnectCmd cmd = BTP.GapDisconnectCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_DISCONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GAP", String.format("disconnect %d %s", cmd.addressType, addr));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GAP, GAP_DISCONNECT,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        mng.disconnect().enqueue();

        tester.response(BTP_SERVICE_ID_GAP, GAP_DISCONNECT,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    private void setIOCap(ByteBuffer data) {
        BTP.GapSetIOCapCmd cmd = BTP.GapSetIOCapCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_SET_IO_CAP, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        Log.d("GAP", String.format("set io cap %d", cmd.ioCap));

        tester.response(BTP_SERVICE_ID_GAP, GAP_SET_IO_CAP,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    private void pair(ByteBuffer data) {
        BTP.GapPairCmd cmd = BTP.GapPairCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_PAIR, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GAP", String.format("pair %d %s", cmd.addressType,
                addr));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GAP, GAP_PAIR,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        mng.createBond().enqueue();

        tester.response(BTP_SERVICE_ID_GAP, GAP_PAIR,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    private void unpair(ByteBuffer data) {
        BTP.GapUnpairCmd cmd = BTP.GapUnpairCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_UNPAIR, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GAP", String.format("unpair %d %s", cmd.addressType,
                addr));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GAP, GAP_UNPAIR,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        mng.removeBond().enqueue();

        tester.response(BTP_SERVICE_ID_GAP, GAP_UNPAIR,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }


    private void passkeyEntry(ByteBuffer data) {
        BTP.GapPasskeyEntryCmd cmd = BTP.GapPasskeyEntryCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_PASSKEY_ENTRY, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        String passkey = String.format("%06d", cmd.passkey);

        Log.d("GAP", String.format("passkeyEntry %d %s %s", cmd.addressType, addr, passkey));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GAP, GAP_PASSKEY_ENTRY,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        mng.getBluetoothDevice().setPin(passkey.getBytes());

        tester.response(BTP_SERVICE_ID_GAP, GAP_PASSKEY_ENTRY,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    private void passkeyConfirm(ByteBuffer data) {
        BTP.GapPasskeyConfirmCmd cmd = BTP.GapPasskeyConfirmCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_PASSKEY_CONFIRM, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);

        Log.d("GAP", String.format("passkeyConfirm %d %s %d",
                cmd.addressType, addr, cmd.match));

        /* Requires BLUETOOTH_PRIVILEGED, so this doesn't work */
        /* mng.getBluetoothDevice().setPairingConfirmation(true); */

        tester.response(BTP_SERVICE_ID_GAP, GAP_PASSKEY_CONFIRM,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    public void deviceFound(@NonNull ScanResult result) {
        Log.d("GAP", String.format("deviceFound %s", result));

        BTP.GapDeviceFoundEv ev = new BTP.GapDeviceFoundEv();
        BluetoothDevice device = result.getDevice();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        ev.rssi = (byte) result.getRssi();
        if (result.getScanRecord() != null) {
            ev.flags = (byte) result.getScanRecord().getAdvertiseFlags();
        }

        if (result.getScanRecord() != null &&
                result.getScanRecord().getBytes() != null) {
            byte[] data = result.getScanRecord().getBytes();
            ev.eirDataLen = (short) data.length;
            ev.eirData = Arrays.copyOf(data, data.length);
        }

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_FOUND,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onDeviceConnecting %s", device));
    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onDeviceConnected %s", device));
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onDeviceDisconnecting %s", device));
    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onDeviceDisconnected %s", device));
        BTP.GapDeviceDisconnectedEv ev = new BTP.GapDeviceDisconnectedEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_DISCONNECTED,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onLinkLossOccured %s", device));
    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device,
                                     boolean optionalServicesFound) {
        Log.d("GAP", String.format("onServicesDiscovered %s %b", device,
                optionalServicesFound));
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onDeviceReady %s", device));
        processedDevices.add(device);

        BTP.GapDeviceConnectedEv ev = new BTP.GapDeviceConnectedEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_CONNECTED,
                CONTROLLER_INDEX, ev.toBytes());
    }

    protected static final int PAIRING_VARIANT_PIN = 0;
    protected static final int PAIRING_VARIANT_PASSKEY = 1;
    protected static final int PAIRING_VARIANT_PASSKEY_CONFIRMATION = 2;
    protected static final int PAIRING_VARIANT_CONSENT = 3;
    protected static final int PAIRING_VARIANT_DISPLAY_PASSKEY = 4;
    protected static final int PAIRING_VARIANT_DISPLAY_PIN = 5;
    protected static final int PAIRING_VARIANT_OOB_CONSENT = 6;

    public void securityLevelChanged(BluetoothDevice device, int bondState) {
        Log.d("GAP", String.format("securityLevelChanged %s %s", device, pairingVariant));
        BTP.GapSecLevelChangedEv ev = new BTP.GapSecLevelChangedEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);
        ev.level = 1;

        switch (pairingVariant) {
            case PAIRING_VARIANT_PIN:
            case PAIRING_VARIANT_PASSKEY_CONFIRMATION:
            case PAIRING_VARIANT_DISPLAY_PASSKEY:
                /* Assume this is LE Secure Connections pairing with encryption
                using a 128-bit strength encryption key */
                ev.level = 3;
                break;
        }

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_SEC_LEVEL_CHANGED,
                CONTROLLER_INDEX, ev.toBytes());
    }


    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onBondingRequired %s", device));
    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onBonded %s", device));
        securityLevelChanged(device, device.getBondState());
    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onBondingFailed %s", device));
    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {
        Log.d("GAP", String.format("onError %s %s %d", device, message, errorCode));
    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onDeviceNotSupported %s", device));
    }

    private final BroadcastReceiver bondStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.ERROR);

                int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                        BluetoothDevice.ERROR);

                Log.d("GAP", String.format("bondStateChangedReceiver %s %d %d", dev, bondState, prevBondState));

                if (bondState == BOND_BONDED) {
                    securityLevelChanged(dev, bondState);
                }
            }
        }
    };

    public void passkeyEntryRequestEv(BluetoothDevice device) {
        Log.d("GAP", String.format("passkeyEntryRequestEv %s", device));
        BTP.GapPasskeyEntryEv ev = new BTP.GapPasskeyEntryEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_PASSKEY_ENTRY_REQ,
                CONTROLLER_INDEX, ev.toBytes());
    }

    public void passkeyDisplayEv(BluetoothDevice device, int passkey) {
        Log.d("GAP", String.format("passkeyDisplayEv %s %d", device, passkey));
        BTP.GapPasskeyDisplayEv ev = new BTP.GapPasskeyDisplayEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        ev.passkey = passkey;

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_PASSKEY_DISPLAY,
                CONTROLLER_INDEX, ev.toBytes());
    }

    public void passkeyConfirmEv(BluetoothDevice device, int passkey) {
        Log.d("GAP", String.format("passkeyConfirmEv %s %d", device, passkey));
        BTP.GapPasskeyConfirmEv ev = new BTP.GapPasskeyConfirmEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        ev.passkey = passkey;

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_PASSKEY_CONFIRM_REQ,
                CONTROLLER_INDEX, ev.toBytes());
    }

    public void pairingConsentEv(BluetoothDevice device) {
        Log.d("GAP", String.format("pairingConsentEv %s", device));
        BTP.GapPairingConsentEv ev = new BTP.GapPairingConsentEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_PAIRING_CONSENT,
                CONTROLLER_INDEX, ev.toBytes());
    }

    private final BroadcastReceiver incomingPairRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {

                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.ERROR);
                int passkey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                        BluetoothDevice.ERROR);

                Log.d("GAP", String.format("ActionPairingRequest %s %d %d", dev,
                        pairingVariant, passkey));

                switch (pairingVariant) {
                    case PAIRING_VARIANT_PIN:
                        passkeyEntryRequestEv(dev);
                        abortBroadcast();
                        break;
                    case PAIRING_VARIANT_DISPLAY_PASSKEY:
                        passkeyDisplayEv(dev, passkey);
                        abortBroadcast();
                        break;
                    case PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                        passkeyConfirmEv(dev, passkey);
                        break;
                    case PAIRING_VARIANT_CONSENT:
                        pairingConsentEv(dev);
                        break;
                    default:
                        Log.e("GAP", "Unsupported pairing variant");
                        break;
                }
            }
        }
    };

    private void sendConnParamUpdateEv(BluetoothDevice device, int interval,
                                       int latency, int timeout, int status) {
        Log.d("GAP", String.format("sendConnParamUpdateEv addr=%s interval=%d latency=%d " +
                "timeout=%d status=%d", device, interval, latency, timeout, status));
        BTP.GapConnParamUpdateEv ev = new BTP.GapConnParamUpdateEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        ev.connItvl = (short) interval;
        ev.connLatency = (short) latency;
        ev.supervisionTimeout = (short) timeout;

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_CONN_PARAM_UPDATE,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void connectionParamUpdate(BluetoothDevice device, int interval, int latency,
                                      int timeout, int status) {
        sendConnParamUpdateEv(device, interval, latency, timeout, status);
    }

    public void handleGAP(byte opcode, byte index, ByteBuffer data) {
        switch (opcode) {
            case GAP_READ_SUPPORTED_COMMANDS:
            case GAP_READ_CONTROLLER_INDEX_LIST:
                if (index != BTP_INDEX_NONE) {
                    tester.response(BTP_SERVICE_ID_GAP, opcode, index, BTP_STATUS_FAILED);
                    return;
                }
                break;
            default:
                if (index != CONTROLLER_INDEX) {
                    tester.response(BTP_SERVICE_ID_GAP, opcode, index, BTP_STATUS_FAILED);
                    return;
                }
                break;
        }

        switch (opcode) {
            case GAP_READ_SUPPORTED_COMMANDS:
                supportedCommandsGAP(data);
                break;
            case GAP_READ_CONTROLLER_INDEX_LIST:
                controllerIndexList(data);
                break;
            case GAP_READ_CONTROLLER_INFO:
                controllerInfo(data);
                break;
            case GAP_SET_CONNECTABLE:
                setConnectable(data);
                break;
            case GAP_SET_DISCOVERABLE:
                setDiscoverable(data);
                break;
            case GAP_START_ADVERTISING:
                startAdvertising(data);
                break;
            case GAP_STOP_ADVERTISING:
                stopAdvertising(data);
                break;
            case GAP_START_DISCOVERY:
                startDiscovery(data);
                break;
            case GAP_STOP_DISCOVERY:
                stopDiscovery(data);
                break;
            case GAP_CONNECT:
                connect(data);
                break;
            case GAP_DISCONNECT:
                disconnect(data);
                break;
            case GAP_SET_IO_CAP:
                setIOCap(data);
                break;
            case GAP_PAIR:
                pair(data);
                break;
            case GAP_UNPAIR:
                unpair(data);
                break;
            case GAP_PASSKEY_ENTRY:
                passkeyEntry(data);
                break;
            case GAP_PASSKEY_CONFIRM:
                passkeyConfirm(data);
                break;
            default:
                tester.response(BTP_SERVICE_ID_GAP, opcode, index, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }

    public void supportedCommandsGATT(ByteBuffer data) {
        byte[] cmds = new byte[4];

        setBit(cmds, GATT_READ_SUPPORTED_COMMANDS);
        setBit(cmds, GATT_ADD_SERVICE);
        setBit(cmds, GATT_ADD_CHARACTERISTIC);
        setBit(cmds, GATT_ADD_DESCRIPTOR);
        setBit(cmds, GATT_ADD_INCLUDED_SERVICE);
        setBit(cmds, GATT_SET_VALUE);
        setBit(cmds, GATT_START_SERVER);
        setBit(cmds, GATT_DISC_ALL_PRIM_SVCS);
        setBit(cmds, GATT_DISC_PRIM_UUID);
        setBit(cmds, GATT_DISC_FIND_INCLUDED);
        setBit(cmds, GATT_DISC_ALL_CHRC);
        setBit(cmds, GATT_DISC_CHRC_UUID);
        setBit(cmds, GATT_DISC_ALL_DESC);
        setBit(cmds, GATT_READ);
        setBit(cmds, GATT_READ_LONG);
        setBit(cmds, GATT_WRITE);
        setBit(cmds, GATT_WRITE_LONG);
        setBit(cmds, GATT_CFG_NOTIFY);
        setBit(cmds, GATT_CFG_INDICATE);
        setBit(cmds, GATT_GET_ATTRIBUTES);
        setBit(cmds, GATT_GET_ATTRIBUTE_VALUE);

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_READ_SUPPORTED_COMMANDS, CONTROLLER_INDEX,
                cmds);
    }

    private void addService(ByteBuffer data) {
        Log.d("GATT", "addService");
        BTP.GattAddServiceCmd cmd = BTP.GattAddServiceCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GATT_ADD_SERVICE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        UUID uuid = Utils.btpToUUID(cmd.uuid);

        Log.d("GATT", String.format("type %d uuid %s", cmd.type, uuid));

        if (lastAddedService != null) {
            if (!gattServer.addService(lastAddedService)) {
                Log.e("GATT", "Couldn't add service");
                tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_SERVICE, CONTROLLER_INDEX,
                        BTP_STATUS_FAILED);
                return;
            }
            lastAddedService = null;
        }

        lastAddedService = new BluetoothGattService(uuid, cmd.type);
        ++attributeCount;

        BTP.GattAddServiceRp rp = new BTP.GattAddServiceRp();
        rp.svcId = (short) attributeCount;
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_ADD_SERVICE, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void addCharacteristic(ByteBuffer data) {
        Log.d("GATT", "addCharacteristic");
        BTP.GattAddCharacteristicCmd cmd = BTP.GattAddCharacteristicCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GATT_ADD_CHARACTERISTIC, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        UUID uuid = Utils.btpToUUID(cmd.uuid);

        Log.d("GATT", String.format("id %d perm %d prop %d uuid %s",
                Short.toUnsignedInt(cmd.svcId), cmd.permissions, cmd.properties, uuid));

        if (lastAddedService == null) {
            Log.e("GATT", "LastAddedService is null");
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_CHARACTERISTIC, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(uuid,
                cmd.properties, cmd.permissions);

        lastAddedService.addCharacteristic(characteristic);
        lastAddedCharacteristic = characteristic;
        ++attributeCount;

        BTP.GattAddCharacteristicRp rp = new BTP.GattAddCharacteristicRp();
        rp.chrId = (short) attributeCount;
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_ADD_CHARACTERISTIC, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void addDescriptor(ByteBuffer data) {
        Log.d("GATT", "addDescriptor");
        BTP.GattAddDescriptorCmd cmd = BTP.GattAddDescriptorCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GATT_ADD_DESCRIPTOR, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        UUID uuid = Utils.btpToUUID(cmd.uuid);

        Log.d("GATT", String.format("id %d perm %d uuid %s",
                Short.toUnsignedInt(cmd.chrId), cmd.permissions, uuid));

        if (lastAddedCharacteristic == null) {
            Log.e("GATT", "LastAddedCharacteristic is null");
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_DESCRIPTOR, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(uuid, cmd.permissions);

        lastAddedCharacteristic.addDescriptor(descriptor);
        lastAddedDescriptor = descriptor;
        ++attributeCount;

        BTP.GattAddDescriptorRp rp = new BTP.GattAddDescriptorRp();
        rp.dscId = (short) attributeCount;
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_ADD_DESCRIPTOR, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void addIncludedService(ByteBuffer data) {
        Log.d("GATT", "addIncludedService");
        BTP.GattAddIncludedServiceCmd cmd = BTP.GattAddIncludedServiceCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_INCLUDED_SERVICE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        Log.d("GATT", String.format("id %d ", Short.toUnsignedInt(cmd.svcId)));

        if (lastAddedService == null) {
            Log.e("GATT", "LastAddedService is null");
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_INCLUDED_SERVICE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BluetoothGattService inc = Utils.findServiceByHandle(gattServer.getServices(),
                Short.toUnsignedInt(cmd.svcId));

        if (inc == null) {
            Log.e("GATT", String.format("Couldn't find service id %d ",
                    Short.toUnsignedInt(cmd.svcId)));
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_INCLUDED_SERVICE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        if (!lastAddedService.addService(inc)) {
            Log.e("GATT", "Couldn't add included service");
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_INCLUDED_SERVICE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        ++attributeCount;

        BTP.GattAddIncludedServiceRp rp = new BTP.GattAddIncludedServiceRp();
        rp.includedServiceId = (short) attributeCount;
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_ADD_INCLUDED_SERVICE, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void setValue(ByteBuffer data) {
        Log.d("GATT", "setValue");

        BTP.GattSetValueCmd cmd = BTP.GattSetValueCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GATT_SET_VALUE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        Log.d("GATT", String.format("id %d value %s", cmd.attrId, bytesToHex(cmd.value)));

        boolean success = gattServerCallback.setValue(cmd.attrId, cmd.value);
        byte status = success ? BTP_STATUS_SUCCESS : BTP_STATUS_FAILED;

        tester.response(BTP_SERVICE_ID_GATT, GATT_SET_VALUE, CONTROLLER_INDEX, status);
    }

    private void startServer(ByteBuffer data) {
        Log.d("GATT", "startServer");

        if (lastAddedService != null && !gattServer.addService(lastAddedService)) {
            Log.e("GATT", "Couldn't add service");
            tester.response(BTP_SERVICE_ID_GATT, GATT_ADD_SERVICE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BTP.GattStartServerRp rp = new BTP.GattStartServerRp();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_START_SERVER, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void discAllPrimSvcs(ByteBuffer data) {
        Log.d("GATT", "discAllPrimSvcs");
        BTP.GattDiscAllPrimSvcsCmd cmd = BTP.GattDiscAllPrimSvcsCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GATT_DISC_ALL_PRIM_SVCS, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s", cmd.addressType, addr));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_PRIM_SVCS,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        List<BTP.GattService> services = new ArrayList<>();
        for (GattDBService btGattSvc : mng.getAllPrimaryServices()) {
            services.add(new BTP.GattService(btGattSvc));
        }

        BTP.GattDiscAllPrimSvcsRp rp = new BTP.GattDiscAllPrimSvcsRp();
        rp.services = services.toArray(new BTP.GattService[0]);
        rp.servicesCount = (byte) services.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_PRIM_SVCS, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void discPrimUuid(ByteBuffer data) {
        Log.d("GATT", "discPrimUuid");
        BTP.GattDiscPrimUuidCmd cmd = BTP.GattDiscPrimUuidCmd.parse(data);
        if (cmd == null || (cmd.uuidLen != 2 && cmd.uuidLen != 16)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_PRIM_UUID, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        UUID uuid = Utils.btpToUUID(cmd.uuid);
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s UUID=%s", cmd.addressType, addr,
                String.valueOf(uuid)));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_PRIM_UUID,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        List<BTP.GattService> services = new ArrayList<>();
        for (GattDBService btGattSvc : mng.getPrimaryServiceByUUID(uuid)) {
                services.add(new BTP.GattService(btGattSvc));
        }

        BTP.GattDiscPrimUuidRp rp = new BTP.GattDiscPrimUuidRp();
        rp.services = services.toArray(new BTP.GattService[0]);
        rp.servicesCount = (byte) services.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_PRIM_UUID, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void findIncluded(ByteBuffer data) {
        Log.d("GATT", "findIncluded");
        BTP.GattFindIncludedCmd cmd = BTP.GattFindIncludedCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_FIND_INCLUDED, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s", cmd.addressType, addr));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_FIND_INCLUDED,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }
        List<GattDBIncludeService> includes = mng.getIncludedServices(
                Short.toUnsignedInt(cmd.startHandle),
                Short.toUnsignedInt(cmd.endHandle));

        List<BTP.GattIncluded> services = new ArrayList<>();
        for (GattDBIncludeService inc : includes) {
            services.add(new BTP.GattIncluded(inc));
        }

        BTP.GattFindIncludedRp rp = new BTP.GattFindIncludedRp();
        rp.included = services.toArray(new BTP.GattIncluded[0]);
        rp.servicesCount = (byte) services.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_FIND_INCLUDED, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void discAllChrc(ByteBuffer data) {
        Log.d("GATT", "discAllChrc");
        BTP.GattDiscAllChrcCmd cmd = BTP.GattDiscAllChrcCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_CHRC, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s startHandle=0x%04x endHandle=0x%04x",
                cmd.addressType, addr, cmd.startHandle, cmd.endHandle));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_CHRC,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        List<BTP.GattCharacteristic> characteristics = new ArrayList<>();
        for (GattDBCharacteristic btGattChr :
                mng.getAllCharacteristics(Short.toUnsignedInt(cmd.startHandle),
                        Short.toUnsignedInt(cmd.endHandle))) {
            characteristics.add(new BTP.GattCharacteristic(btGattChr));
        }

        BTP.GattDiscChrcRp rp = new BTP.GattDiscChrcRp();
        rp.characteristics = characteristics.toArray(new BTP.GattCharacteristic[0]);
        rp.characteristicsCount = (byte) characteristics.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_CHRC, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void discChrcUuid(ByteBuffer data) {
        Log.d("GATT", "discChrcUuid");
        BTP.GattDiscChrcUuidCmd cmd = BTP.GattDiscChrcUuidCmd.parse(data);
        if (cmd == null || (cmd.uuidLen != 2 && cmd.uuidLen != 16)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_CHRC_UUID, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        UUID uuid = Utils.btpToUUID(cmd.uuid);
        Log.d("GATT", String.format("%d %s startHandle=0x%04x endHandle=0x%04x UUID=%s",
                cmd.addressType, addr, cmd.startHandle, cmd.endHandle, String.valueOf(uuid)));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_CHRC_UUID,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        List<BTP.GattCharacteristic> characteristics = new ArrayList<>();
        for (GattDBCharacteristic btGattChr :
                mng.getCharacteristicByUUID(Short.toUnsignedInt(cmd.startHandle),
                        Short.toUnsignedInt(cmd.endHandle), uuid)) {
            characteristics.add(new BTP.GattCharacteristic(btGattChr));
        }

        BTP.GattDiscChrcRp rp = new BTP.GattDiscChrcRp();
        rp.characteristics = characteristics.toArray(new BTP.GattCharacteristic[0]);
        rp.characteristicsCount = (byte) characteristics.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_CHRC_UUID, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void discAllDesc(ByteBuffer data) {
        Log.d("GATT", "discAllDesc");
        BTP.GattDiscAllDescCmd cmd = BTP.GattDiscAllDescCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_DESC, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s startHandle=0x%04x endHandle=0x%04x",
                cmd.addressType, addr, cmd.startHandle, cmd.endHandle));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_DESC,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        List<BTP.GattDescriptor> descriptors = new ArrayList<>();
        for (GattDBDescriptor btGattDsc :
                mng.getAllDescriptors(Short.toUnsignedInt(cmd.startHandle),
                        Short.toUnsignedInt(cmd.endHandle))) {
            descriptors.add(new BTP.GattDescriptor(btGattDsc));
        }

        BTP.GattDiscAllDescRp rp = new BTP.GattDiscAllDescRp();
        rp.descriptors = descriptors.toArray(new BTP.GattDescriptor[0]);
        rp.descriptorCount = (byte) descriptors.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_DESC, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void onReadResponse(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        BTP.GattReadRp rp = new BTP.GattReadRp();
        byte[] readData = data.getValue();
        if (readData == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        rp.dataLen = (short) readData.length;
        rp.data = readData;

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_READ, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void read(ByteBuffer data) {
        Log.d("GATT", "read");
        BTP.GattReadCmd cmd = BTP.GattReadCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s handle=0x%04x", cmd.addressType, addr, cmd.handle));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ, CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        if (!mng.gattRead(Short.toUnsignedInt(cmd.handle), this::onReadResponse)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
        }
    }

    private void onReadLongResponse(@NonNull final BluetoothDevice device,
                                    @NonNull final Data data) {
        BTP.GattReadRp rp = new BTP.GattReadRp();
        byte[] readData = data.getValue();
        if (readData == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ_LONG, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        rp.dataLen = (short) readData.length;
        rp.data = readData;

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_READ_LONG, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void readLong(ByteBuffer data) {
        Log.d("GATT", "readLong");
        BTP.GattReadLongCmd cmd = BTP.GattReadLongCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ_LONG, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s handle=0x%04x offset=0x%04x", cmd.addressType,
                addr, cmd.handle, cmd.offset));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ_LONG,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        if (!mng.gattRead(Short.toUnsignedInt(cmd.handle), this::onReadLongResponse)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ_LONG, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
        }
    }

    private void onWriteResponse(@NonNull final BluetoothDevice device, @NonNull final Data data) {
        if (data.getValue() == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_WRITE, CONTROLLER_INDEX,
                new byte[] {BTP_STATUS_SUCCESS});
    }

    private void write(ByteBuffer data) {
        Log.d("GATT", "write");
        BTP.GattWriteCmd cmd = BTP.GattWriteCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s handle=0x%04x len %d", cmd.addressType, addr,
                Short.toUnsignedInt(cmd.handle), cmd.data.length));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE, CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        if (!mng.gattWrite(Short.toUnsignedInt(cmd.handle), cmd.data,
                this::onWriteResponse)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
        }
    }

    private void onWriteLongResponse(@NonNull final BluetoothDevice device,
                                     @NonNull final Data data) {
        if (data.getValue() == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE_LONG, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_WRITE_LONG, CONTROLLER_INDEX,
                new byte[] {BTP_STATUS_SUCCESS});
    }

    private void writeLong(ByteBuffer data) {
        Log.d("GATT", "writeLong");
        BTP.GattWriteLongCmd cmd = BTP.GattWriteLongCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE_LONG, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s handle=0x%04x handle=0x%04x len %d",
                cmd.addressType, addr, Short.toUnsignedInt(cmd.handle),
                Short.toUnsignedInt(cmd.offset), cmd.data.length));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE_LONG,
                    CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        if (!mng.gattWriteLong(Short.toUnsignedInt(cmd.handle), Short.toUnsignedInt(cmd.offset),
                cmd.data, this::onWriteLongResponse)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_WRITE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
        }
    }

    class NotificationReceivedCallback implements DataReceivedCallback  {
        int handle = 0;
        byte type = 0;

        @Override
        public void onDataReceived(@NonNull final BluetoothDevice device,
                                   @NonNull final Data data) {
            BTP.GattNotificationEv ev = new BTP.GattNotificationEv();

            ev.addressType = 0x01; /* assume random */
            ev.address = btAddrToBytes(device.getAddress());
            ev.type = this.type;
            ev.handle = (short) this.handle;
            byte[] bytes = data.getValue();
            if (bytes == null) {
                ev.dataLen = 0;
                ev.data = null;
            } else {
                ev.dataLen = (short) bytes.length;
                ev.data = bytes;
            }

            tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_EV_NOTIFICATION, CONTROLLER_INDEX,
                    ev.toBytes());
        }
    }

    private void configSubscription(ByteBuffer data, byte opcode) {
        Log.d("GATT", "configSubscription");
        BTP.GattCfgNotifyCmd cmd = BTP.GattCfgNotifyCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, opcode, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        String addr = Utils.btpToBdAddr(cmd.address);
        Log.d("GATT", String.format("%d %s cccdHandle=0x%04x opcode=0x%02x enable=%d",
                cmd.addressType, addr, cmd.cccdHandle, opcode, cmd.enable));

        BleConnectionManager mng = findConnection(addr);
        if (mng == null) {
            Log.e("GATT", "Connection not found");
            tester.response(BTP_SERVICE_ID_GATT, opcode, CONTROLLER_INDEX, BTP_STATUS_FAILED);
            return;
        }

        if (!mng.configSubscription(
                Short.toUnsignedInt(cmd.cccdHandle),
                opcode, cmd.enable, new NotificationReceivedCallback())) {
            tester.response(BTP_SERVICE_ID_GATT, opcode, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        tester.response(BTP_SERVICE_ID_GATT, opcode, CONTROLLER_INDEX,
                BTP_STATUS_SUCCESS);
    }

    private void getAttributes(ByteBuffer data) {
        Log.d("GATT", "getAttributes");
        BTP.GattGetAttributesCmd cmd = BTP.GattGetAttributesCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_GET_ATTRIBUTES, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        Log.d("GATT", String.format("startHandle=0x%04x endHandle=0x%04x typeLen=%d",
                Short.toUnsignedInt(cmd.startHandle), Short.toUnsignedInt(cmd.endHandle),
                cmd.typeLen));

        UUID typeUUID = null;
        if (cmd.typeLen > 0) {
            typeUUID = Utils.btpToUUID(cmd.type);

        }
        List<BluetoothGattService> svcs = Utils.getCoreGattServices();
        svcs.addAll(gattServer.getServices());

        List<BTP.GattAttribute> attributes = Utils.getGATTAttributes(svcs,
                cmd.startHandle, cmd.endHandle, typeUUID);

        BTP.GattGetAttributtesRp rp = new BTP.GattGetAttributtesRp();
        rp.attributesCount = (byte) attributes.size();
        rp.attributes = attributes.toArray(new BTP.GattAttribute[0]);

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_GET_ATTRIBUTES, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void getAttributeValue(ByteBuffer data) {
        Log.d("GATT", "getAttributeValue");
        BTP.GattGetAttributeValueCmd cmd = BTP.GattGetAttributeValueCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_GET_ATTRIBUTE_VALUE, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        Log.d("GATT", String.format("handl=0x%04x", Short.toUnsignedInt(cmd.handle)));

        List<BluetoothGattService> svcs = Utils.getCoreGattServices();
        svcs.addAll(gattServer.getServices());

        BTP.GattGetAttributeValueRp rp = new BTP.GattGetAttributeValueRp();

        byte[] val = Utils.gattAttrValToBTP(svcs, Short.toUnsignedInt(cmd.handle));

        if (val != null) {
            rp.valueLength = (short) val.length;
            rp.value = val;
        }

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_GET_ATTRIBUTE_VALUE, CONTROLLER_INDEX,
                rp.toBytes());
    }

    private void sendAttrValueChangedEv(byte[] value) {
        Log.d("GATT", String.format("sendAttrValueChangedEv %s", bytesToHex(value)));
        BTP.GattAttrValueChangedEv ev = new BTP.GattAttrValueChangedEv();

        if (value != null) {
            ev.dataLen = (short) value.length;
            ev.data = value;
        }

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_EV_ATTR_VALUE_CHANGED,
                CONTROLLER_INDEX, ev.toBytes());

    }

    @Override
    public void characteristicValueChanged(BluetoothDevice device,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        sendAttrValueChangedEv(value);
    }

    @Override
    public void descriptorValueChanged(BluetoothDevice device,
                                       BluetoothGattDescriptor descriptor,
                                       byte[] value) {
        sendAttrValueChangedEv(value);
    }

    public void handleGATT(byte opcode, byte index, ByteBuffer data) {
        switch (opcode) {
            case GATT_READ_SUPPORTED_COMMANDS:
                supportedCommandsGATT(data);
                break;
            case GATT_ADD_SERVICE:
                addService(data);
                break;
            case GATT_ADD_CHARACTERISTIC:
                addCharacteristic(data);
                break;
            case GATT_ADD_DESCRIPTOR:
                addDescriptor(data);
                break;
            case GATT_ADD_INCLUDED_SERVICE:
                addIncludedService(data);
                break;
            case GATT_SET_VALUE:
                setValue(data);
                break;
            case GATT_START_SERVER:
                startServer(data);
                break;
            case GATT_DISC_ALL_PRIM_SVCS:
                discAllPrimSvcs(data);
                break;
            case GATT_DISC_PRIM_UUID:
                discPrimUuid(data);
                break;
            case GATT_DISC_FIND_INCLUDED:
                findIncluded(data);
                break;
            case GATT_DISC_ALL_CHRC:
                discAllChrc(data);
                break;
            case GATT_DISC_CHRC_UUID:
                discChrcUuid(data);
                break;
            case GATT_DISC_ALL_DESC:
                discAllDesc(data);
                break;
            case GATT_READ:
                read(data);
                break;
            case GATT_READ_LONG:
                readLong(data);
                break;
            case GATT_WRITE:
                write(data);
                break;
            case GATT_WRITE_LONG:
                writeLong(data);
                break;
            case GATT_CFG_NOTIFY:
            case GATT_CFG_INDICATE:
                configSubscription(data, opcode);
                break;
            case GATT_GET_ATTRIBUTES:
                getAttributes(data);
                break;
            case GATT_GET_ATTRIBUTE_VALUE:
                getAttributeValue(data);
                break;
            default:
                tester.response(BTP_SERVICE_ID_GATT, opcode, index, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }
}
