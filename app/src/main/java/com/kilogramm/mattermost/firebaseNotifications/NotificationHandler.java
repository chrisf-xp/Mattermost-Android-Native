package com.kilogramm.mattermost.firebaseNotifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kilogramm.mattermost.ApplicationLifecycleManager;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.model.entity.notifyProps.NotifyProps;
import com.kilogramm.mattermost.model.entity.notifyProps.NotifyRepository;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.rxtest.MainRxActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.support.v4.app.NotificationCompat.DEFAULT_LIGHTS;
import static android.support.v4.app.NotificationCompat.DEFAULT_SOUND;
import static android.support.v4.app.NotificationCompat.DEFAULT_VIBRATE;

/**
 * Created by Christian (chrisf-xp) on 23.10.2017.
 *
 * This class handles notifications of type with data payload containing a type key with e.g.
 * "message" as value, sent by your Mattermost-Server.
 *
 * Regarding Notification-Feature:
 * The Mattermost-Server needs a Mattermost-Push-Proxy to forward the notifications to the
 * Firebase-Cloud-Messaging servers. The Mattermost-Push-Proxy has to know the API-key of this app.
 * If you compile this app yourself you have to register it to a firebase-account and insert the
 * google-services.json into the app folder of this project. You also get the API-key for the push-
 * proxy from you firebase-account.
 *
 *
 */

public class NotificationHandler {

    private static NotificationHandler singleton = null;
    private Target mTarget; //for picture update

    private static int pendingNotificationsCount = 0;
    private static List<String> lastFivePendingNotification = new LinkedList<>();
    private Service service;

    private static final String TAG = "NOTIFICATION-HANDLER";

    private static final int MESSAGE_ID = 77698383;
    private static final int UPDATE_ID = 73787079;
    private static final int REQUEST_CODE = 1722;
    private static final int UPDATE_CODE = 1723;

    private static final String TYPE_MESSAGE = "message";
    private static final String TYPE_CLEAR = "clear";

    private static final String TYPE_UPDATE = "update";
    private static final String LINK = "link";

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

    private static final int TICKER_MESSAGE_LENGTH_MAX = 256;
    private static final int TICKER_MESSAGE_LENGTH_CUT = 64;

    private NotificationHandler(Service service){
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

    /** Gets called when notification arrives and handles the Type of Notification
     * @param data Map that contains all the data fields of the arrived Notification
     */
    public void handleNotification(Map<String, String> data){
        //debug
        Log.d(TAG, "handle a Notification");
        Log.d(TAG, data.get(MESSAGE));
        //end debug

        if (!data.containsKey(TYPE))return;
        String type = data.get(TYPE);

        if (!ApplicationLifecycleManager.isAppVisible()){ // test if app in background
            if (type.equals(TYPE_MESSAGE)){
                Log.i(TAG, "handle a Message Notification");
                handleMessageNotification(data);
            }
            else if (type.equals(TYPE_CLEAR)){
                handleClearNotification();
            }
        }
        // for notifications also available when app in foreground
        if (type.equals(TYPE_UPDATE)){
            handleUpdateNotification(data);
        }
    }

    private void handleMessageNotification(Map<String, String> data){
        if (!data.containsKey(MESSAGE)){
            return;
        }
        String message = data.get(MESSAGE);
        String title = service.getResources().getString(R.string.notification_new_message_single);
        if (pendingNotificationsCount> 0) {
            title = Integer.toString(pendingNotificationsCount+1) + " " + service.getResources().getString(R.string.notification_new_message_multiple);
        }

        incrementPendingNotificationsCount();
        addPendingNotification(message);

        // notify:
        PictureUpdate picUpdate = new PictureUpdate();
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(MESSAGE_ID, buildMyNotification(title, message, data.get(SENDER_ID), picUpdate));
        picUpdate.setNotificationSent();
        //request Channel
        if (data.containsKey(CHANNEL_ID)){
            Log.i(TAG, data.get(CHANNEL_ID));
            MattermostPreference.getInstance().setLastChannelId(data.get(CHANNEL_ID));
        }

    }

    public void incrementPendingNotificationsCount(){
        pendingNotificationsCount++;
    }
    public void resetPendingNotificationsCount(){
        pendingNotificationsCount = 0;
    }
    public void addPendingNotification(String line){
        if (lastFivePendingNotification.size() < 5){
            lastFivePendingNotification.add(line);
        }else {
            lastFivePendingNotification.remove(0);
            lastFivePendingNotification.add(line);
        }
    }
    public void resetLastFivePendingNotification(){
        lastFivePendingNotification.clear();
    }

    private Notification buildMyNotification(String title, String message, String senderId, PictureUpdate picUpdate){
        int color = service.getResources().getColor(R.color.colorPrimary);
        String tickerMes = message.substring(0, (message.length()>TICKER_MESSAGE_LENGTH_MAX ? TICKER_MESSAGE_LENGTH_CUT : message.length()) );
        if(message.length() > TICKER_MESSAGE_LENGTH_MAX){
            tickerMes += "...";
        }
        // ### Intent
        Intent notificationIntent = new Intent(service.getApplicationContext(), MainRxActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent intent = PendingIntent.getActivity(service.getApplicationContext(), REQUEST_CODE,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT); // from only UPDATE_CURRENT may have fixed bug that sometimes lead to a not working intent
        // ### Alarm
        NotifyProps notifyProps = new NotifyProps(NotifyRepository.query().first());
        String vibrationString = notifyProps.getVibration();
        String soundString = notifyProps.getVibration();
        Boolean vibrationOn = false;
        Boolean soundOn = false;
        if (vibrationString != null) vibrationOn = vibrationString.equals("true");
        if (soundString != null) soundOn = soundString.equals("true");
        int alarmSetting = 0;
        if (vibrationOn) alarmSetting |= DEFAULT_VIBRATE;
        if (soundOn) alarmSetting |= DEFAULT_SOUND;
        if (vibrationOn || soundOn) alarmSetting |= DEFAULT_LIGHTS;
        // ### Big notifications: (android 4.1 and later)
        NotificationCompat.InboxStyle bigStyle = new NotificationCompat.InboxStyle();
        for (String line: lastFivePendingNotification){
            bigStyle.addLine(line);
        }
        if (pendingNotificationsCount - lastFivePendingNotification.size() > 0){
            bigStyle.setSummaryText("+" + (pendingNotificationsCount - lastFivePendingNotification.size())
                    + " " + service.getResources().getString(R.string.notification_plus_x_more) );
        }
        // ### Set
        NotificationCompat.Builder builder = new NotificationCompat.Builder(service.getApplicationContext())
                .setNumber(pendingNotificationsCount)
                .setSmallIcon(R.drawable.ic_mm_noti)
                .setLargeIcon(BitmapFactory.decodeResource( service.getResources(), R.drawable.notification_icon))
                .setColor(color)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(tickerMes)
                .setContentIntent(intent)
                .setDefaults(alarmSetting)
                .setStyle(bigStyle);
        // ### Avatar Loading
        // for future implementation: show picture of sender // maybe only with direct messages
        // picture of sender can sometimes be wrong with different requests at the same time,
        // because we can't predict which thread gets done... so implement something to
        // stop old threads when a new one comes
        if (senderId != null){
             try{
                 new Handler(Looper.getMainLooper()).post(new Runnable(){
                     @Override
                     public void run(){getUserPicture(senderId, builder, picUpdate);}
                 });
             }catch (Exception e){
                 Log.i(TAG, e.getMessage());
             }
         }

        return builder.build();
    }

    private void getUserPicture(String userId, NotificationCompat.Builder builder, PictureUpdate picUpdate){
        User user = UserRepository.query(new UserRepository.UserByIdSpecification(userId)).first();
        mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                builder.setLargeIcon(bitmap);
                // send the notification again to update it w/ the right image, only when the
                // previous notification was already sent
                for (int sleepCount = 32; sleepCount < 4000; sleepCount*=2){
                    try{
                        Thread.sleep(sleepCount);
                    }catch (InterruptedException e){
                        Log.i(TAG, e.getMessage());
                    }
                    if (picUpdate.isNotificationSent()){
                        builder.setDefaults(0);
                        ((NotificationManager) (service.getSystemService(NOTIFICATION_SERVICE)))
                                .notify(MESSAGE_ID, builder.build());
                        return;
                    }
                }
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {}

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };
        Picasso.with(service.getApplicationContext()).load(getAvatarUrl(user)).into(mTarget);
    }

    public String getAvatarUrl(User user) {
        return "https://"
                + MattermostPreference.getInstance().getBaseUrl()
                + "/api/v3/users/"
                + user.getId()
                + "/image?time="
                + user.getLastPictureUpdate();
    }

    /**Synchronized class for Notification Picture updating.
     * Used for communication between this thread and the thread that tries to get the picture.
     */
    private class PictureUpdate{
        private boolean notificationSent = false;
        public synchronized boolean isNotificationSent(){
            return notificationSent;
        }
        public synchronized void setNotificationSent(){
            notificationSent = true;
        }
    }

    private void handleClearNotification(){
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(MESSAGE_ID);
        resetPendingNotificationsCount();
        resetLastFivePendingNotification();
    }

    private void handleUpdateNotification(Map<String, String> data){
        if (!data.containsKey(LINK)){
            return;
        }
        String link = data.get(LINK);
        String title = service.getResources().getString(R.string.notification_update);

        //## Intent to open Link
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        notificationIntent.setData(Uri.parse(link));
        PendingIntent pendingIntent = PendingIntent.getActivity(service.getApplicationContext(),
                UPDATE_CODE, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service.getApplicationContext())
                .setLargeIcon(BitmapFactory.decodeResource( service.getResources(), R.drawable.notification_icon))
                .setSmallIcon(R.drawable.ic_mm_noti)
                .setContentTitle(title)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        if (data.containsKey(MESSAGE)) builder.setContentText(data.get(MESSAGE));

        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(UPDATE_ID, notification);
    }
}
