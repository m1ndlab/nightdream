package com.firebirdberlin.nightdream;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import java.util.Map;

public abstract class BillingHelperActivity extends Activity {
    static final String TAG = "BillingActivity";

    IInAppBillingService mService;
    Map<String, Boolean> purchases;
    ServiceConnection mServiceConn = new ServiceConnection() {

        BillingHelper billingHelper;

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "IIAB service disconnected");
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "IIAB service connected");
            mService = IInAppBillingService.Stub.asInterface(service);
            billingHelper = new BillingHelper(getApplicationContext(), mService);
            purchases = billingHelper.getPurchases();
            if (billingHelper.isPurchased(BillingHelper.ITEM_WEB_RADIO)) {
                Log.i(TAG, "Web Radio is purchased");
            } else {
                Log.i(TAG, "Web Radio is NOT purchased");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // bind the in-app billing service
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
}
