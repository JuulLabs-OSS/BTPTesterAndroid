package com.juul.btptesterandroid;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ScanConnectCallback extends ScanCallback {

    private Map<String, ScanResult> results;
    private int errorCode;

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
    }

    @Override
    public void onScanResult(int callbackType, @NonNull ScanResult result) {
        super.onScanResult(callbackType, result);
        Log.d("GAP", String.format("onScanResult %s", result.toString()));
        deviceDiscovered(result);
    }

    @Override
    public void onBatchScanResults(@NonNull List<ScanResult> results) {
        super.onBatchScanResults(results);
        for (ScanResult result : results) {
            Log.d("GAP", String.format("onBatchScanResult %s", result.toString()));
            deviceDiscovered(result);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        super.onScanFailed(errorCode);
        Log.d("GAP", String.format("onScanFailed %d", errorCode));
        this.errorCode = errorCode;
    }
}
