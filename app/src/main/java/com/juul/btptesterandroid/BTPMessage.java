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



