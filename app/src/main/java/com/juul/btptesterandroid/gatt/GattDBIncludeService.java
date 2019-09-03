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

package com.juul.btptesterandroid.gatt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static com.juul.btptesterandroid.Utils.UUIDtoBTP;
import static com.juul.btptesterandroid.Utils.isBluetoothSIGUuid;

public class GattDBIncludeService {

    private GattDBService mService;
    private int attHandle;

    public GattDBIncludeService(GattDBService service) {
        mService = service;
        attHandle = 0;
    }

    public GattDBService getService() {
        return mService;
    }

    public void setHandle(int handle) {
        attHandle = handle;
    }

    public int getHandle() {
        return attHandle;
    }

    public byte[] toBTP() {
        UUID uuid = getService().getService().getUuid();
        byte[] uuidBytes = UUIDtoBTP(getService().getService().getUuid());
        int uuidLen = 0;
        if (isBluetoothSIGUuid(uuid)) {
            uuidLen = uuidBytes.length;
        }

        ByteBuffer buf = ByteBuffer.allocate(2 + 2 + uuidLen);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.putShort((short) getService().getStartHandle());
        buf.putShort((short) getService().getEndHandle());
        buf.put(uuidBytes);

        return buf.array();
    }
}
