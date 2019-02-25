package com.juul.btptesterandroid;

import android.bluetooth.BluetoothAdapter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.juul.btptesterandroid.BTTester.BTP_INDEX_NONE;
import static com.juul.btptesterandroid.BTTester.BTP_SERVICE_ID_GAP;
import static com.juul.btptesterandroid.BTTester.BTP_STATUS_FAILED;
import static com.juul.btptesterandroid.BTTester.BTP_STATUS_SUCCESS;
import static com.juul.btptesterandroid.BTTester.BTP_STATUS_UNKNOWN_CMD;
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

public class GAP {

    private BTTester tester = null;
    private BluetoothAdapter bleAdapter = null;
    private static final byte CONTROLLER_INDEX = 0;
    private byte[] supportedSettings = new byte[4];
    private byte[] currentSettings = new byte[4];

    public void supportedCommands(ByteBuffer data) throws Exception {
        byte[] cmds = new byte[3];

        setBit(cmds, GAP_READ_SUPPORTED_COMMANDS);
        setBit(cmds, GAP_READ_CONTROLLER_INDEX_LIST);
        setBit(cmds, GAP_READ_CONTROLLER_INFO);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_READ_SUPPORTED_COMMANDS, CONTROLLER_INDEX,
                cmds);
    }

    public void controllerIndexList(ByteBuffer data) throws Exception {
        ByteBuffer rp = ByteBuffer.allocate(2);
        rp.order(ByteOrder.LITTLE_ENDIAN);
        int len = 1;

        rp.put((byte) len);
        rp.put(CONTROLLER_INDEX);

        tester.sendMessage(BTP_SERVICE_ID_GAP, GAP_READ_CONTROLLER_INDEX_LIST,
                CONTROLLER_INDEX, rp.array());
    }

    public void controllerInfo(ByteBuffer data) throws Exception {
        BTTester.GapReadControllerInfo rp = new BTTester.GapReadControllerInfo();

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

    public void handleGAP(byte opcode, byte index, ByteBuffer data) throws Exception {
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
            default:
                tester.response(BTP_SERVICE_ID_GAP, opcode, index, BTP_STATUS_UNKNOWN_CMD);
                break;
        }
    }

    public byte init(BTTester tester, BluetoothAdapter bleAdapter) {
        this.tester = tester;
        this.bleAdapter = bleAdapter;
        return BTP_STATUS_SUCCESS;
    }

    public byte unregister() {
        return BTP_STATUS_SUCCESS;
    }
}
