package com.kilogramm.mattermost.rxtest;

import android.os.Bundle;

import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.fromnet.ListInviteObj;
import com.kilogramm.mattermost.network.ServerMethod;

import icepick.State;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Evgeny on 18.10.2016.
 */
public class InviteUserRxPresenter extends BaseRxPresenter<InviteUserRxActivity> {

    private static final int REQUEST_INVITE = 1;

    @State
    ListInviteObj listInvite;


    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        initRequest();
    }

    private void initRequest() {
        initInviteRequest();
    }

    private void initInviteRequest() {
        restartableFirst(REQUEST_INVITE,
                () -> ServerMethod.getInstance()
                        .invite(MattermostPreference.getInstance().getTeamId(), listInvite)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()),
                (inviteUserRxActivity, o) -> sendInviteOk(),
                (inviteUserRxActivity1, throwable) -> sendError(parceError(throwable, null)));
    }

    public void requestInvite(ListInviteObj inviteObj){
        this.listInvite = inviteObj;
        start(REQUEST_INVITE);
    }

    private void sendError(String message) {
        createTemplateObservable(message)
                .subscribe(split(InviteUserRxActivity::showError));
    }

    private void sendInviteOk() {
        createTemplateObservable(new Object())
                .subscribe(split((inviteUserRxActivity, o) -> inviteUserRxActivity.onOkInvite()));
    }
}