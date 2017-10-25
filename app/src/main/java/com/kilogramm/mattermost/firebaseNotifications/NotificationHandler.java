package com.kilogramm.mattermost.firebaseNotifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kilogramm.mattermost.ApplicationLifecycleManager;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.rxtest.MainRxActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Christian on 23.10.2017.
 */

public class NotificationHandler {

    private static NotificationHandler singleton = null;

    private static int pendingNotificationsCount = 0;
    private Service service;

    private static final String TAG = "NOTIFICATION-HANDLER";

    private static final int MESSAGE_ID = 77698383;
    private static final int INFO_ID = 73787079;
    private static final int REQUEST_CODE = 1722;

    private static final String TYPE_MESSAGE = "message";
    private static final String TYPE_CLEAR = "clear";
    private static final String TYPE_INFO = "info";

    private static final String TYPE = "type";
    private static final String BADGE = "badge";
    private static final String MESSAGE = "message";
    private static final String CHANNEL_ID = "channel_id";
    private static final String CHANNEL_NAME = "channel_name";
    private static final String TEAM_ID = "team_id";
    private static final String POST_ID = "post_id";
    private static final String ROOT_ID = "root_id";
    private static final String SENDER_ID = "sender_id";
    private static final String OR_USERNAME = "override_username";
    private static final String OR_ICON = "override_icon_url";
    private static final String FROM_WEBHOOK = "from_webhook";

    private static final String DELIM = "in";
    private static final int TICKER_MESSAGE_LENGTH_MAX = 256;
    private static final int TICKER_MESSAGE_LENGTH_CUT = 64;

    private NotificationHandler(Service service){
        //pendingNotificationsCount = 0;
        this.service = service;
    }

    public static NotificationHandler getInstance(Service service){
        if (singleton == null){
            singleton = new NotificationHandler(service);
        }
        return singleton;
    }

    public static void clearNotificationsIfAny(){
        if (singleton != null){
            singleton.handleClearNotification();
        }
    }

    public void handleNotification(Map<String, String> data){
        //debug
        Log.d(TAG, "handle a Notification");
        Log.d(TAG, data.get(MESSAGE));
        //end debug

        if (data.containsKey(TYPE) && !ApplicationLifecycleManager.isAppVisible()){
            String type = data.get(TYPE);
            if (type.equals(TYPE_MESSAGE)){
                Log.i(TAG, "handle a Message Notification");
                handleMessageNotification(data);
            }
            else if (type.equals(TYPE_CLEAR)){
                handleClearNotification();
            }
            else if (type.equals(TYPE_INFO)){ // only for self-created notifications
                handleInfoNotification(data);
            }
        }
    }

    private void handleMessageNotification(Map<String, String> data){
        if (!data.containsKey(MESSAGE)){
            return;
        }
        String message = data.get(MESSAGE);
        String title = service.getResources().getString(R.string.notification_new_message_single);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service.getApplicationContext());
        // ### Number of Notifications pending
        if (pendingNotificationsCount> 0) {
            title = Integer.toString(pendingNotificationsCount+1) + " " + service.getResources().getString(R.string.notification_new_message_multiple);
            builder.setNumber(pendingNotificationsCount+1);
        }
        // ### Appearance of Notfication
        builder.setSmallIcon(R.drawable.ic_mm_noti);
        builder.setLargeIcon(BitmapFactory.decodeResource( service.getResources(), R.drawable.notification_icon));
        int color = service.getResources().getColor(R.color.colorPrimary);
        builder.setColor(color);
        builder.setLights(color, 1000, 2000);
        builder.setAutoCancel(true);
        // ### Text of the Notification
        builder.setContentTitle(title);
        builder.setContentText(message);
        String tickerMes = message.substring(0, (message.length()>TICKER_MESSAGE_LENGTH_MAX ? TICKER_MESSAGE_LENGTH_CUT : message.length()) );
        if(message.length() > TICKER_MESSAGE_LENGTH_MAX){
            tickerMes += "...";
        }
        builder.setTicker(tickerMes);
        // ### Intent
        Intent notificationIntent = new Intent(service.getApplicationContext(), MainRxActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent intent = PendingIntent.getActivity(service.getApplicationContext(), REQUEST_CODE,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent); //add intent to notification
        // ## Build
        Notification notification = builder.build();
        // ## Alarm set to defaults: TODO: menu to disable/enable alarms
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_SOUND;

        //todo show picture of sender


        // notify:
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(MESSAGE_ID, notification);
        incrementPendingNotificationsCount();

        //request Channel
        if (data.containsKey(CHANNEL_ID)){
            Log.i(TAG, data.get(CHANNEL_ID));
            MattermostPreference.getInstance().setLastChannelId(data.get(CHANNEL_ID));
        }

    }

    private void handleClearNotification(){
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(MESSAGE_ID);
        resetPendingNotificationsCount();
    }

    private void handleInfoNotification(Map<String, String> data){
        if (!data.containsKey(MESSAGE)){
            return;
        }
        String message = data.get(MESSAGE);
        String title =  service.getResources().getString(R.string.notification_information);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service.getApplicationContext())
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(message);
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(INFO_ID, notification);
    }

    public void incrementPendingNotificationsCount(){
        pendingNotificationsCount++;
    }
    public void resetPendingNotificationsCount(){
        pendingNotificationsCount = 0;
    }
}
