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

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static com.juul.btptesterandroid.Utils.UUIDtoBTP;

public class GattDBCharacteristic {

    private BluetoothGattCharacteristic mCharacteristic;
    private List<GattDBDescriptor> mDescriptors;
    private int defHandle, valHandle;

    public GattDBCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
        mDescriptors = new ArrayList<>();
        defHandle = 0;
        valHandle = 0;
    }

    public void addDescriptor(GattDBDescriptor descriptor) {
        mDescriptors.add(descriptor);
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }

    public int setHandles(int curHandle) {
        defHandle = curHandle;
        ++curHandle;
        valHandle = curHandle;

        for (GattDBDescriptor dsc : mDescriptors) {
            ++curHandle;
            dsc.setHandle(curHandle);
        }

        return curHandle;
    }

    public int getDefHandle() {
        return defHandle;
    }

    public int getValHandle() {
        return valHandle;
    }

    public List<GattDBDescriptor> getDescriptors() {
        return mDescriptors;
    }

    public byte[] toBTPDefinition() {
        byte[] uuid = UUIDtoBTP(getCharacteristic().getUuid());
        ByteBuffer buf = ByteBuffer.allocate(1 + 2 + uuid.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put((byte) getCharacteristic().getProperties());
        buf.putShort((short) getValHandle());
        buf.put(uuid);

        return buf.array();
    }

    public byte[] toBTPValue() {
        return getCharacteristic().getValue();
    }

}
