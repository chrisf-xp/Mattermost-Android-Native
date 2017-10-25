package com.kilogramm.mattermost.firebaseNotifications;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceIdService;
import com.kilogramm.mattermost.MattermostApp;

/**
 * Created by Christian on 19.10.2017.
 */

public class MmFirebaseInstanceIDService extends FirebaseInstanceIdService{

    private final String TAG = "FIREBASE";
    @Override
    public void onTokenRefresh() {
        Log.i(TAG, "token refresh");
        MattermostApp.keepDeviceTokenUpToDate();
    }
}
