package com.juul.btptesterandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidx.annotation.NonNull;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.ConnectRequest;

import static com.juul.btptesterandroid.BTTester.BTP_INDEX_NONE;
import static com.juul.btptesterandroid.BTTester.BTP_SERVICE_ID_GAP;
import static com.juul.btptesterandroid.BTTester.BTP_STATUS_FAILED;
import static com.juul.btptesterandroid.BTTester.BTP_STATUS_SUCCESS;
import static com.juul.btptesterandroid.BTTester.BTP_STATUS_UNKNOWN_CMD;
import static com.juul.btptesterandroid.BTTester.GAP_CONNECT;
import static com.juul.btptesterandroid.BTTester.GAP_EV_DEVICE_CONNECTED;
import static com.juul.btptesterandroid.BTTester.GAP_READ_CONTROLLER_INDEX_LIST;
import static com.juul.btptesterandroid.BTTester.GAP_READ_CONTROLLER_INFO;
import static com.juul.btptesterandroid.BTTester.GAP_READ_SUPPORTED_COMMANDS;
import static com.juul.btptesterandroid.BTTester.GAP_SETTINGS_ADVERTISING;
import static com.juul.btptesterandroid.BTTester.GAP_SETTINGS_BONDABLE;
import static com.juul.btptesterandroid.BTTester.GAP_SETTINGS_CONNECTABLE;
import static com.juul.btptesterandroid.BTTester.GAP_SETTINGS_LE;
import static com.juul.btptesterandroid.BTTester.GAP_SETTINGS_POWERED;
import static com.juul.btptesterandroid.BTTester.GAP_SETTINGS_STATIC_ADDRESS;
import static com.juul.btptesterandroid.Utils.setBit;

public class GAP implements BleManagerCallbacks {

    private BTTester tester = null;
    private BluetoothAdapter bleAdapter = null;
    private BTPBleManager bleManager = null;
    private static final byte CONTROLLER_INDEX = 0;
    private byte[] supportedSettings = new byte[4];
    private byte[] currentSettings = new byte[4];

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
        BTTester.GapReadControllerInfoRp rp = new BTTester.GapReadControllerInfoRp();

        byte[] addr = bleAdapter.getAddress().getBytes();
        System.arraycopy(addr, 0, rp.address, 0, rp.address.length);

        byte[] name = bleAdapter.getName().getBytes();
        System.arraycopy(name, 0, rp.name, 0, name.length);

        setBit(supportedSettings, GAP_SETTINGS_POWERED);
        setBit(supportedSettings, GAP_SETTINGS_BONDABLE);
        setBit(supportedSettings, GAP_SETTINGS_LE);
        setBit(supportedSettings, GAP_SETTINGS_CONNECTABLE);
        setBit(supportedSettings, GAP_SETTINGS_ADVERTISING);
        setBit(supportedSettings, GAP_SETTINGS_STATIC_ADDRESS);

        setBit(currentSettings, GAP_SETTINGS_POWERED);
        setBit(currentSettings, GAP_SETTINGS_BONDABLE);
        setBit(currentSettings, GAP_SETTINGS_LE);

        System.arraycopy(supportedSettings, 0, rp.supportedSettings,
                0, rp.supportedSettings.length);

        System.arraycopy(currentSettings, 0, rp.currentSettings,
                0, rp.currentSettings.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_READ_CONTROLLER_INFO,
                CONTROLLER_INDEX, rp.toBytes());
    }

    public void connect(ByteBuffer data) {
        BTTester.GapConnectCmd cmd = BTTester.GapConnectCmd.parse(data);
        if (cmd == null) {
            tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT, CONTROLLER_INDEX,
                    BTP_STATUS_FAILED);
            return;
        }
        Log.d("GAP", String.format("connect %d %s", cmd.addressType,
                Utils.bytesToHex(cmd.address)));

        BluetoothDevice device = bleAdapter.getRemoteDevice(cmd.address);

        ConnectRequest req = bleManager.connect(device);
        req.enqueue();

        tester.response(BTP_SERVICE_ID_GAP, GAP_CONNECT,
                CONTROLLER_INDEX, BTP_STATUS_SUCCESS);
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {
        BTTester.GapDeviceConnectedEv ev = new BTTester.GapDeviceConnectedEv();

        ev.addressType = 0x01; /* random */
        byte[] addr = device.getAddress().getBytes();
        System.arraycopy(addr, 0, ev.address, 0, ev.address.length);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_EV_DEVICE_CONNECTED,
                CONTROLLER_INDEX, ev.toBytes());
    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device) {

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
            case GAP_CONNECT:
                connect(data);
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
        return BTP_STATUS_SUCCESS;
    }

    public byte unregister() {
        return BTP_STATUS_SUCCESS;
    }
}
