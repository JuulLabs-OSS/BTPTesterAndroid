package com.juul.btptesterandroid;

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
import static com.juul.btptesterandroid.Utils.setBit;

public class GAP {

    private BTTester tester = null;
    private static final byte CONTROLLER_INDEX = 0;

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

    public byte init(BTTester tester) {
        this.tester = tester;
        return BTP_STATUS_SUCCESS;
    }

    public byte unregister() {
        return BTP_STATUS_SUCCESS;
    }
}
