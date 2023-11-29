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

import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.List;

import static com.juul.btptesterandroid.Utils.UUIDtoBTP;

public class GattDBService {

    private BluetoothGattService mService;
    private List<GattDBCharacteristic> mCharacteristics;
    private List<GattDBIncludeService> mIncludes;
    private int startHandle, endHandle;

    public GattDBService(BluetoothGattService service) {
        mService = service;
        mCharacteristics = new ArrayList<>();
        mIncludes = new ArrayList<>();
        startHandle = 0;
        endHandle = 0;
    }

    public void addCharacteristic(GattDBCharacteristic characteristic) {
        mCharacteristics.add(characteristic);
    }

    public void addIncludeService(GattDBIncludeService includeService) {
        mIncludes.add(includeService);
    }

    public BluetoothGattService getService() {
        return mService;
    }

    public int setHandles(int curHandle) {
        startHandle = curHandle;

        for (GattDBIncludeService inc : mIncludes) {
            ++curHandle;
            inc.setHandle(curHandle);
        }

        for (GattDBCharacteristic chr : mCharacteristics) {
            ++curHandle;
            curHandle = chr.setHandles(curHandle);
        }

        endHandle = curHandle;

        return endHandle;
    }

    public int getStartHandle() {
        return startHandle;
    }

    public int getEndHandle() {
        return endHandle;
    }

    public List<GattDBCharacteristic> getCharacteristics() {
        return mCharacteristics;
    }

    public List<GattDBIncludeService> getIncludedServices() {
        return mIncludes;
    }

    public boolean isPrimary() {
        return mService.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY;
    }

    public byte[] toBTP() {
        return UUIDtoBTP(getService().getUuid());
    }
}
