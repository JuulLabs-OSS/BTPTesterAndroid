package com.juul.btptesterandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import com.juul.btptesterandroid.gatt.GattDBCharacteristic;
import com.juul.btptesterandroid.gatt.GattDBDescriptor;
import com.juul.btptesterandroid.gatt.GattDBService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.DisconnectRequest;
import no.nordicsemi.android.ble.Request;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.exception.BluetoothDisabledException;
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException;
import no.nordicsemi.android.ble.exception.InvalidRequestException;
import no.nordicsemi.android.ble.exception.RequestFailedException;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static com.juul.btptesterandroid.BTP.BTP_INDEX_NONE;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_GAP;
import static com.juul.btptesterandroid.BTP.BTP_SERVICE_ID_GATT;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_FAILED;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_SUCCESS;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_UNKNOWN_CMD;
import static com.juul.btptesterandroid.BTP.GAP_CONNECT;
import static com.juul.btptesterandroid.BTP.GAP_DISCONNECT;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_CONNECTED;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_DISCONNECTED;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_FOUND;
import static com.juul.btptesterandroid.BTP.GAP_GENERAL_DISCOVERABLE;
import static com.juul.btptesterandroid.BTP.GAP_PAIR;
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
import static com.juul.btptesterandroid.BTP.GATT_CFG_INDICATE;
import static com.juul.btptesterandroid.BTP.GATT_CFG_NOTIFY;
import static com.juul.btptesterandroid.BTP.GATT_DISC_ALL_CHRC;
import static com.juul.btptesterandroid.BTP.GATT_DISC_ALL_DESC;
import static com.juul.btptesterandroid.BTP.GATT_DISC_ALL_PRIM_SVCS;
import static com.juul.btptesterandroid.BTP.GATT_DISC_CHRC_UUID;
import static com.juul.btptesterandroid.BTP.GATT_DISC_PRIM_UUID;
import static com.juul.btptesterandroid.BTP.GATT_EV_NOTIFICATION;
import static com.juul.btptesterandroid.BTP.GATT_READ;
import static com.juul.btptesterandroid.BTP.GATT_READ_SUPPORTED_COMMANDS;
import static com.juul.btptesterandroid.BTP.GATT_WRITE;
import static com.juul.btptesterandroid.Utils.btAddrToBytes;
import static com.juul.btptesterandroid.Utils.clearBit;
import static com.juul.btptesterandroid.Utils.setBit;

public class GAP implements BleManagerCallbacks {

    private static final int SCAN_TIMEOUT = 1000;

    private Context context;
    private BTTester tester = null;
    private BluetoothAdapter bleAdapter = null;
    private BluetoothManager bleManager = null;
    private BleConnectionManager bleConnectionManager = null;

    private BluetoothGattServer gattServer;
    private BTPGattServerCallback gattServerCallback;
    private BluetoothLeScannerCompat scanner;
    private ScanConnectCallback scanCallback;
    private BluetoothLeAdvertiser advertiser;
    private BTPAdvertiseCallback advertiseCallback;

    private static final byte CONTROLLER_INDEX = 0;
    private byte[] supportedSettings = new byte[4];
    private byte[] currentSettings = new byte[4];

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

    private static final String BTP_TESTER_UUID = "0000CDAB-0000-1000-8000-00805F9B34FB";

    public class BTPAdvertiseCallback extends AdvertiseCallback {
        byte opcode;

        public BTPAdvertiseCallback(byte opcode) {
            this.opcode = opcode;
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);

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

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build();
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        advertiseCallback = new BTPAdvertiseCallback(GAP_START_ADVERTISING);
        advertiser.startAdvertising(settings, advertiseData, advertiseCallback);
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
        Log.d("GAP", String.format("connect %d %s", cmd.addressType,
                Utils.bytesToHex(cmd.address)));

        ScanSettings settings = new ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(true)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BTP_TESTER_UUID)).build());
        String addr = Utils.bytesToHex(cmd.address).replaceAll("..(?!$)", "$0:");

        scanCallback.clearCache();
        scanner.startScan(filters, settings, scanCallback);

        if (scanCallback.getErrorCode() != 0) {
            Log.d("GAP", String.format("Connect: scanCallback error %d",
                    scanCallback.getErrorCode()));
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        try {
            Thread.sleep(SCAN_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        scanner.stopScan(scanCallback);

        if (scanCallback.getErrorCode() != 0) {
            Log.d("GAP", String.format("Connect: scanCallback error %d",
                    scanCallback.getErrorCode()));
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BluetoothDevice device = scanCallback.findDiscoveredDevice(addr);
        if (device == null) {
            Log.d("GAP", "Connect: device not found");
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        ConnectRequest req = bleConnectionManager.connect(device);
        req.enqueue();

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
        Log.d("GAP", String.format("disconnect %d %s", cmd.addressType,
                Utils.bytesToHex(cmd.address)));

        DisconnectRequest req = bleConnectionManager.disconnect();
        req.enqueue();

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
        Log.d("GAP", String.format("pair %d %s", cmd.addressType,
                Utils.bytesToHex(cmd.address)));

        Request request = bleConnectionManager.createBond();
        request.enqueue();

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
        Log.d("GAP", String.format("unpair %d %s", cmd.addressType,
                Utils.bytesToHex(cmd.address)));

        Request request = bleConnectionManager.removeBond();
        request.enqueue();

        tester.response(BTP_SERVICE_ID_GAP, GAP_UNPAIR,
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
        BTP.GapDeviceConnectedEv ev = new BTP.GapDeviceConnectedEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_CONNECTED,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onBondingRequired %s", device));
    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {
        Log.d("GAP", String.format("onBonded %s", device));
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

    private final BroadcastReceiver incomingPairRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("GAP", String.format("ActionPairingRequest %s", dev));
                // FIXME: This does not work
                // dev.setPairingConfirmation(true);
            }
        }
    };

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
            default:
                tester.response(BTP_SERVICE_ID_GAP, opcode, index, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }

    public void supportedCommandsGATT(ByteBuffer data) {
        byte[] cmds = new byte[4];

        setBit(cmds, GATT_READ_SUPPORTED_COMMANDS);
        setBit(cmds, GATT_DISC_ALL_PRIM_SVCS);

        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_READ_SUPPORTED_COMMANDS, CONTROLLER_INDEX,
                cmds);
    }

    private void discAllPrimSvcs(ByteBuffer data) {
        Log.d("GATT", "discAllPrimSvcs");

        List<BTP.GattService> services = new ArrayList<>();

        for (GattDBService btGattSvc : bleConnectionManager.getAllPrimaryServices()) {
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
        Log.d("GATT", String.format("UUID=%s", String.valueOf(uuid)));

        List<BTP.GattService> services = new ArrayList<>();

        for (GattDBService btGattSvc : bleConnectionManager.getPrimaryServiceByUUID(uuid)) {
                services.add(new BTP.GattService(btGattSvc));
        }

        BTP.GattDiscPrimUuidRp rp = new BTP.GattDiscPrimUuidRp();
        rp.services = services.toArray(new BTP.GattService[0]);
        rp.servicesCount = (byte) services.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_PRIM_UUID, CONTROLLER_INDEX,
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
        Log.d("GATT", String.format("startHandle=0x%04x endHandle=0x%04x",
                cmd.startHandle, cmd.endHandle));

        List<BTP.GattCharacteristic> characteristics = new ArrayList<>();

        for (GattDBCharacteristic btGattChr :
                bleConnectionManager.getAllCharacteristics(Short.toUnsignedInt(cmd.startHandle),
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
        UUID uuid = Utils.btpToUUID(cmd.uuid);
        Log.d("GATT", String.format("startHandle=0x%04x endHandle=0x%04x UUID=%s",
                cmd.startHandle, cmd.endHandle, String.valueOf(uuid)));

        List<BTP.GattCharacteristic> characteristics = new ArrayList<>();

        for (GattDBCharacteristic btGattChr :
                bleConnectionManager.getCharacteristicByUUID(Short.toUnsignedInt(cmd.startHandle),
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
        Log.d("GATT", String.format("startHandle=0x%04x endHandle=0x%04x",
                cmd.startHandle, cmd.endHandle));

        List<BTP.GattDescriptor> descriptors = new ArrayList<>();

        for (GattDBDescriptor btGattDsc :
                bleConnectionManager.getAllDescriptors(Short.toUnsignedInt(cmd.startHandle),
                        Short.toUnsignedInt(cmd.endHandle))) {
            descriptors.add(new BTP.GattDescriptor(btGattDsc));
        }

        BTP.GattDiscAllDescRp rp = new BTP.GattDiscAllDescRp();
        rp.descriptors = descriptors.toArray(new BTP.GattDescriptor[0]);
        rp.descriptorCount = (byte) descriptors.size();
        tester.sendMessage(BTP_SERVICE_ID_GATT, GATT_DISC_ALL_DESC, CONTROLLER_INDEX,
                rp.toBytes());
    }

    void onReadResponse(@NonNull final BluetoothDevice device, @NonNull final Data data) {
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
        Log.d("GATT", String.format("handle=0x%04x", cmd.handle));

        if (!bleConnectionManager.gattRead(Short.toUnsignedInt(cmd.handle), this::onReadResponse)) {
            tester.response(BTP_SERVICE_ID_GATT, GATT_READ, CONTROLLER_INDEX,
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
        Log.d("GATT", String.format("handle=0x%04x", cmd.handle));

        if (!bleConnectionManager.gattWrite(Short.toUnsignedInt(cmd.handle), cmd.data,
                this::onWriteResponse)) {
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
        Log.d("GATT", String.format("cccdHandle=0x%04x opcode=0x%02x enable=%d", cmd.cccdHandle,
                opcode, cmd.enable));

        if (!bleConnectionManager.configSubscription(
                Short.toUnsignedInt(cmd.cccdHandle),
                opcode, cmd.enable, new NotificationReceivedCallback())) {
            tester.response(BTP_SERVICE_ID_GATT, opcode, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        tester.response(BTP_SERVICE_ID_GATT, opcode, CONTROLLER_INDEX,
                BTP_STATUS_SUCCESS);
    }

    public void handleGATT(byte opcode, byte index, ByteBuffer data) {
        switch (opcode) {
            case GATT_READ_SUPPORTED_COMMANDS:
                supportedCommandsGATT(data);
                break;
            case GATT_DISC_ALL_PRIM_SVCS:
                discAllPrimSvcs(data);
                break;
            case GATT_DISC_PRIM_UUID:
                discPrimUuid(data);
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
            case GATT_WRITE:
                write(data);
                break;
            case GATT_CFG_NOTIFY:
            case GATT_CFG_INDICATE:
                configSubscription(data, opcode);
                break;
            default:
                tester.response(BTP_SERVICE_ID_GATT, opcode, index, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }

    public byte init(Context context, BTTester tester, BluetoothAdapter bleAdapter,
                     BluetoothManager bleManager) {
        this.context = context;
        this.tester = tester;
        this.bleAdapter = bleAdapter;
        this.bleManager = bleManager;

        this.gattServerCallback = new BTPGattServerCallback(this);
        this.gattServer = this.bleManager.openGattServer(context, gattServerCallback);
        if (this.gattServer == null) {
            return BTP_STATUS_FAILED;
        }

        this.scanner = BluetoothLeScannerCompat.getScanner();
        this.scanCallback = new ScanConnectCallback();

        this.advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        this.bleConnectionManager = new BleConnectionManager(this.context);
        this.bleConnectionManager.setGattCallbacks(this);

        this.context.registerReceiver(incomingPairRequestReceiver,
                new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));

        return BTP_STATUS_SUCCESS;
    }

    public void cleanup() {
        try {
            if (this.bleConnectionManager.isConnected()) {
                this.bleConnectionManager.disconnect().await();
            }

            this.bleConnectionManager.removeBond().enqueue();
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
    }

    public byte unregister() {
        return BTP_STATUS_SUCCESS;
    }
}
