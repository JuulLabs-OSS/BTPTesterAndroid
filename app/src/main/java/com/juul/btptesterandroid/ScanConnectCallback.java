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

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ScanConnectCallback extends ScanCallback {
    private final static String TAG = "GAP";
    private Map<String, ScanResult> results;
    private int errorCode;
    private IDeviceDiscovered deviceDiscoveredCb;

    public interface IDeviceDiscovered {
        void report(ScanResult result);
    }

    ScanConnectCallback() {
        results = new HashMap<>();
        errorCode = 0;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public BluetoothDevice findDiscoveredDevice(String address) {
        ScanResult result = results.getOrDefault(address, null);
        if (result == null) {
            return null;
        }

        return result.getDevice();
    }

    private void deviceDiscovered(@NonNull ScanResult result) {
        results.put(result.getDevice().getAddress(), result);
        if (deviceDiscoveredCb != null) {
            deviceDiscoveredCb.report(result);
        }
    }
    public void setDeviceDiscoveredCb(IDeviceDiscovered cb) {
        this.deviceDiscoveredCb = cb;
    }

    public void clearCache() {
        this.deviceDiscoveredCb = null;
        results.clear();
        errorCode = 0;
    }

    @Override
    public void onScanResult(int callbackType, @NonNull ScanResult result) {
        super.onScanResult(callbackType, result);
        Log.d(TAG, String.format("onScanResult %s", result.toString()));
        deviceDiscovered(result);
    }

    @Override
    public void onBatchScanResults(@NonNull List<ScanResult> results) {
        super.onBatchScanResults(results);
        for (ScanResult result : results) {
            Log.d(TAG, String.format("onBatchScanResult %s", result.toString()));
            deviceDiscovered(result);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.d(TAG, String.format("onScanFailed %d", errorCode));
        this.errorCode = errorCode;
    }
}
