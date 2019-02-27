package com.juul.btptesterandroid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.juul.btptesterandroid.BTP.HDR_LEN;

public class BTPMessage {
    byte service;
    byte opcode;
    byte index;
    short len;
    ByteBuffer data;

    public BTPMessage(byte service, byte opcode, byte index, byte[] data) {
        this.service = service;
        this.opcode = opcode;
        this.index = index;
        if (data != null) {
            this.len = (short) data.length;
            this.data = ByteBuffer.wrap(data);
        }
    }

    private BTPMessage(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);

        bb.order(ByteOrder.LITTLE_ENDIAN);
        service = bb.get();
        opcode = bb.get();
        index = bb.get();
        len = bb.getShort();
        if (len > 0) {
            data = ByteBuffer.wrap(bytes, 5, len);
        }
    }

    public static BTPMessage parse(byte[] bytes) {
        if (bytes.length < HDR_LEN) {
            return null;
        }

        return new BTPMessage(bytes);
    }

    byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(HDR_LEN + this.len);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(this.service);
        buffer.put(this.opcode);
        buffer.put(this.index);
        buffer.putShort(this.len);
        if (this.len > 0 && this.data != null) {
            buffer.put(this.data);
        }

        return buffer.array();
    }
}



