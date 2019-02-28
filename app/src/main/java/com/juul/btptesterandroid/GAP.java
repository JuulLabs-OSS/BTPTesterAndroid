package com.juul.btptesterandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.ble.DisconnectRequest;
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
import static com.juul.btptesterandroid.BTP.BTP_STATUS_FAILED;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_SUCCESS;
import static com.juul.btptesterandroid.BTP.BTP_STATUS_UNKNOWN_CMD;
import static com.juul.btptesterandroid.BTP.GAP_CONNECT;
import static com.juul.btptesterandroid.BTP.GAP_DISCONNECT;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_CONNECTED;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_DISCONNECTED;
import static com.juul.btptesterandroid.BTP.GAP_EV_DEVICE_FOUND;
import static com.juul.btptesterandroid.BTP.GAP_GENERAL_DISCOVERABLE;
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
import static com.juul.btptesterandroid.BTP.GAP_START_ADVERTISING;
import static com.juul.btptesterandroid.BTP.GAP_START_DISCOVERY;
import static com.juul.btptesterandroid.BTP.GAP_STOP_ADVERTISING;
import static com.juul.btptesterandroid.BTP.GAP_STOP_DISCOVERY;
import static com.juul.btptesterandroid.Utils.btAddrToBytes;
import static com.juul.btptesterandroid.Utils.clearBit;
import static com.juul.btptesterandroid.Utils.setBit;

public class GAP implements BleManagerCallbacks {

    private BTTester tester = null;
    private BluetoothAdapter bleAdapter = null;
    private BTPBleManager bleManager = null;
    private static final byte CONTROLLER_INDEX = 0;
    private byte[] supportedSettings = new byte[4];
    private byte[] currentSettings = new byte[4];
    private static final int SCAN_TIMEOUT = 1000;
    private BluetoothLeScannerCompat scanner;
    private ScanConnectCallback scanCallback;
    private BluetoothLeAdvertiser advertiser;
    private BTPAdvertiseCallback advertiseCallback;

    public void supportedCommands(ByteBuffer data) {
        byte[] cmds = new byte[3];

        setBit(cmds, GAP_READ_SUPPORTED_COMMANDS);
        setBit(cmds, GAP_READ_CONTROLLER_INDEX_LIST);
        setBit(cmds, GAP_READ_CONTROLLER_INFO);

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

        try {
            Thread.sleep(SCAN_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        scanner.stopScan(scanCallback);

        if (scanCallback.getErrorCode() != 0) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        BluetoothDevice device = scanCallback.findDiscoveredDevice(addr);
        if (device == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }

        ConnectRequest req = bleManager.connect(device);
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

        DisconnectRequest req = bleManager.disconnect();
        req.enqueue();

        tester.response(BTP_SERVICE_ID_GAP, GAP_DISCONNECT,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }


    public void deviceFound(@NonNull ScanResult result) {
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

    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        BTP.GapDeviceConnectedEv ev = new BTP.GapDeviceConnectedEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_CONNECTED,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {
        BTP.GapDeviceDisconnectedEv ev = new BTP.GapDeviceDisconnectedEv();

        ev.addressType = 0x01; /* assume random */

        byte[] addr = btAddrToBytes(device.getAddress());
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_DISCONNECTED,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void onLinkLossOccurred(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onServicesDiscovered(@NonNull BluetoothDevice device,
                                     boolean optionalServicesFound) {

    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingRequired(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBonded(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onBondingFailed(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onError(@NonNull BluetoothDevice device, @NonNull String message, int errorCode) {

    }

    @Override
    public void onDeviceNotSupported(@NonNull BluetoothDevice device) {

    }


    public void handleGAP(byte opcode, byte index, ByteBuffer data) {
        switch(opcode) {
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

        switch(opcode) {
            case GAP_READ_SUPPORTED_COMMANDS:
                supportedCommands(data);
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
            default:
                tester.response(BTP_SERVICE_ID_GAP, opcode, index, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }

    public byte init(BTTester tester, BluetoothAdapter bleAdapter, BTPBleManager bleManager) {
        this.tester = tester;
        this.bleAdapter = bleAdapter;
        this.bleManager = bleManager;
        this.bleManager.setGattCallbacks(this);
        this.scanner = BluetoothLeScannerCompat.getScanner();
        this.scanCallback = new ScanConnectCallback();
        this.advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        return BTP_STATUS_SUCCESS;
    }

    public void cleanup() {
        if (this.bleManager.isConnected()) {
            try {
                this.bleManager.disconnect().await();
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
        }

        if (advertiseCallback != null) {
            this.advertiser.stopAdvertising(advertiseCallback);
        }
    }

    public byte unregister() {
        return BTP_STATUS_SUCCESS;
    }
}
