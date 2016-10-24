package com.kilogramm.mattermost.presenter.settings;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.NotifyProps;
import com.kilogramm.mattermost.model.entity.NotifyUpdate;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.network.ApiMethod;
import com.kilogramm.mattermost.rxtest.BaseRxPresenter;
import com.kilogramm.mattermost.view.settings.NotificationActivity;

import icepick.State;
import io.realm.Realm;
import nucleus.presenter.delivery.Delivery;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ngers on 18.10.16.
 */

public class NotificationPresenter extends BaseRxPresenter<NotificationActivity> {

    private static final String channelMentions = "\"@channel\",\"@all\",";

    private static final String TAG = "NotificationPresenter";
    private static final int REQUEST_UPDATE_NOTIFY = 1;

    @State
    NotifyProps notifyProps;
    @State
    User user;

    private ApiMethod service;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        MattermostApp mMattermostApp = MattermostApp.getSingleton();
        Realm realm = Realm.getDefaultInstance();
        this.notifyProps = new NotifyProps(realm.where(NotifyProps.class).findFirst());
        this.user = realm.where(User.class).equalTo("id", MattermostPreference.getInstance().getMyUserId()).findFirst();
        service = mMattermostApp.getMattermostRetrofitService();
        initRequests();
    }

    private void initRequests() {
        initDeletePost();
    }

    private void initDeletePost() {
        restartableFirst(REQUEST_UPDATE_NOTIFY, () ->
                        service.updateNotify(new NotifyUpdate(notifyProps, user.getId()))
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io()),
                (settingsActivity, user) -> {
                    UserRepository.update(user);
                    Toast.makeText(settingsActivity, "Saved successfully", Toast.LENGTH_SHORT).show();
                }, (settingsActivity, throwable) -> {
                    sendError(throwable);
                    Log.d(TAG, "Error update notification " + throwable.getMessage());
                });
    }

    private void sendError(Throwable throwable) {
        createTemplateObservable(throwable.getMessage())
                .subscribe(split((notificationActivity, s) -> Toast.makeText(notificationActivity, s, Toast.LENGTH_SHORT).show()));
    }

    public void requestUpdateNotify() {
        start(REQUEST_UPDATE_NOTIFY);
    }


    public String getMentionsAll() {
        String result = "";
        if (isFirstNameTrigger()) {
            result = "\"" + getFirstName() + "\",";
        }
        if (notifyProps != null) {
            String[] mentions = notifyProps.getMentionKeys().split(",");
            for (String s : mentions) {
                if (s.equals(getUserName()) || s.equals(getUserNameMentioned()))
                    if (result.length() != 0)
                        result = result + ",\"" + s + "\"";
                    else
                        result = "\"" + s + "\"";
            }
            if (notifyProps.getChannel().equals("true"))
                if (result.length() != 0)
                    result = result + ","  +channelMentions;
                else
                    result = channelMentions;

            for (String s : mentions) {
                if (!s.equals(getUserName()) && !s.equals(getUserNameMentioned()))
                    if (result.length() != 0)
                        result = result + "\"," + s + "\"";
                    else
                        result = "\"" + s + "\"";
            }
            return result;
        }
        return null;
    }

    public String getPushSetting() {
        return notifyProps.getPush();
    }

    public void setPushSetting(String push) {
        notifyProps.setPush(push);
    }

    public String getPushStatusSetting() {
        return notifyProps.getPushStatus();
    }

    public void setPushStatusSetting(String pushStatus) {
        notifyProps.setPushStatus(pushStatus);
    }

    public boolean isChannelTrigger() {
        return notifyProps.getChannel().equals("true") ? true : false;
    }

    public void setChannelTrigger(boolean channel) {
        notifyProps.setChannel(channel ? "true" : "false");
    }

    public boolean isFirstNameTrigger() {
        if (notifyProps.getFirstName() != null)
            return notifyProps.getFirstName().equals("true") ? true : false;
        return false;
    }

    public void setFirstNameTrigger(boolean firsName) {
        notifyProps.setFirstName(firsName ? "true" : "false");
    }

    public String getMentionsKeys() {
        return notifyProps.getMentionKeys();
    }

    public String getOtherMentionsKeys() {
        String otherMention = "";
        if (getMentionsKeys() != null) {
            String[] mention_key = getMentionsKeys().split(",");
            for (String key : mention_key) {
                if (!key.equals(getUserName()) && !key.equals(getUserNameMentioned())) {
                    if (otherMention.length() != 0)
                        otherMention = otherMention + "," + key;
                    else
                        otherMention = key;
                }
            }
        }
        return otherMention;
    }

    public void setMentionsKeys(String mentions) {
        String otherMention = "";
        String[] mention_key = mentions.split(",");
        int i = 1;
        for (String key : mention_key) {
            if (key.trim().length() != 0) {
                otherMention = otherMention + key.trim();
                if (i < mention_key.length)
                    otherMention = otherMention + ",";
            }
            i++;
        }
        notifyProps.setMentionKeys(otherMention);
    }

    public boolean isUserName() {
        if (getMentionsKeys() != null) {
            String[] mention_key = getMentionsKeys().split(",");
            for (String m : mention_key) {
                if (m.equals(getUserName()))
                    return true;
            }
        }
        return false;
    }

    public String getUserName() {
        return user.getUsername();
    }

    public String getUserNameMentioned() {
        return "@" + user.getUsername();
    }

    public boolean isUserNameMentioned() {
        if (getMentionsKeys() != null) {
            String[] mention_key = getMentionsKeys().split(",");
            for (String m : mention_key) {
                if (m.equals(getUserNameMentioned()))
                    return true;
            }
        }
        return false;
    }

    public String getFirstName() {
        return user.getFirstName();
    }


    public <T> Observable<Delivery<NotificationActivity, T>> createTemplateObservable(T obj) {
        return Observable.just(obj)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .compose(deliverFirst());
    }
}
