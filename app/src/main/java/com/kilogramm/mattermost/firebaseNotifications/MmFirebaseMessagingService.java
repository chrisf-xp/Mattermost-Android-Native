package com.kilogramm.mattermost.firebaseNotifications;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by Christian on 19.10.2017.
 */

public class MmFirebaseMessagingService extends FirebaseMessagingService {

    NotificationHandler notificationHandler;

    private final String TAG = "NOTIFICATION";

    @Override
    public void onCreate(){
        super.onCreate();
        notificationHandler = NotificationHandler.getInstance(this);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i(TAG, "received");

        Map<String, String> data = remoteMessage.getData();
        notificationHandler.handleNotification(data);
    }

    //TODO onMessageDelete()

}
