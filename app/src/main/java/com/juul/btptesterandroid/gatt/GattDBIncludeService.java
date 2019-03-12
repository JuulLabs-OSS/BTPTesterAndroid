package com.juul.btptesterandroid.gatt;

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
}
