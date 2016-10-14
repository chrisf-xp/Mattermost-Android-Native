package com.kilogramm.mattermost.rxtest;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.ClientCfg;
import com.kilogramm.mattermost.model.entity.InitObject;
import com.kilogramm.mattermost.model.entity.Team;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.error.HttpError;
import com.kilogramm.mattermost.model.fromnet.LoginData;
import com.kilogramm.mattermost.network.ApiMethod;
import com.kilogramm.mattermost.view.authorization.ForgotPasswordActivity;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import icepick.State;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import retrofit2.adapter.rxjava.HttpException;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Evgeny on 04.10.2016.
 */
public class LoginRxPresenter extends BaseRxPresenter<LoginRxActivity> {

    private static final String TAG = "LoginRxPresenter";

    private static final int REQUEST_LOGIN = 1;
    private static final int REQUEST_INITLOAD = 2;


    private Realm mRealm;

    @State
    String mEditEmail = "";
    @State
    String mEditPassword = "";

    private ObservableBoolean isEnabledSignInButton;
    private ObservableField<String> siteName;
    private ObservableInt isVisibleProgress;


    public void onClickSignIn(View v) {
        requestLogin();
    }

    public void onForgotButtonClick(View v) {
        ForgotPasswordActivity.start(getView());
    }


    private void handleErrorLogin(Throwable e) {
        if (e instanceof HttpException) {
            HttpError error;
            try {
                error = new Gson()
                        .fromJson((((HttpException) e)
                                .response()
                                .errorBody()
                                .string()), HttpError.class);
                Log.d(TAG, error.getMessage());
                Toast.makeText(getView(), error.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (IOException e1) {
                Log.d(TAG, "Message not has body.");
                e1.printStackTrace();
            }
        } else {
            Toast.makeText(getView(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "SystemException, stackTrace: \n");
            e.printStackTrace();
        }
    }

    private boolean canClickSignIn() {
        return mEditEmail.length() != 0 &&
                mEditPassword.length() != 0 && isValidEmail(mEditEmail);
    }

    private boolean isValidEmail(String email) {
        Pattern p = Patterns.EMAIL_ADDRESS;
        Matcher m = p.matcher(email);
        return m.matches();
    }

    //======================== Network ============================================================

    private List<Team> saveDataAfterLogin(InitObject initObject) {
        mRealm.beginTransaction();
        RealmResults<ClientCfg> results = mRealm.where(ClientCfg.class).findAll();
        results.deleteAllFromRealm();
        mRealm.copyToRealmOrUpdate(initObject.getClientCfg());
        RealmList<User> directionProfiles = new RealmList<>();
        directionProfiles.addAll(initObject.getMapDerectProfile().values());
        mRealm.copyToRealmOrUpdate(directionProfiles);
        List<Team> teams = mRealm.copyToRealmOrUpdate(initObject.getTeams());
        mRealm.commitTransaction();
        return teams;
    }

    //======================== Getters for binding ================================================

    public ObservableField<String> getSiteName() {
        return siteName;
    }

    public String getmEditEmail() {
        return mEditEmail;
    }

    public String getmEditPassword() {
        return mEditPassword;
    }

    public ObservableBoolean getIsEnabledSignInButton() {
        return isEnabledSignInButton;
    }

    public ObservableInt getIsVisibleProgress() {
        return isVisibleProgress;
    }

    public TextWatcher getEmailTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditEmail = s.toString();
                isEnabledSignInButton.set(canClickSignIn());

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    public TextWatcher getPasswordTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mEditPassword = s.toString();
                isEnabledSignInButton.set(canClickSignIn());

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    //======================== Utils ==============================================================

    private Boolean isOpenChatScreen = false;

    public void requestLogin() {
        start(REQUEST_LOGIN);
    }

    public void requestInitLoad() {
        start(REQUEST_INITLOAD);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);
        Log.d(TAG, "OnCreate()");
        mRealm = Realm.getDefaultInstance();
        isEnabledSignInButton = new ObservableBoolean(false);
        siteName = new ObservableField<String>(mRealm.where(ClientCfg.class).findFirst().getSiteName());
        isVisibleProgress = new ObservableInt(View.GONE);

        restartableFirst(REQUEST_LOGIN, () -> {
            MattermostApp application = MattermostApp.getSingleton();
            ApiMethod service = application.getMattermostRetrofitService();

            getView().hideKeyboard(getView());
            isVisibleProgress.set(View.VISIBLE);
            return service.login(new LoginData(mEditEmail, mEditPassword, "")).cache()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());
        }, (loginRxActivity, user) -> {
            MattermostPreference.getInstance().setMyUserId(user.getId());
            mRealm.executeTransaction(realm -> realm.copyToRealmOrUpdate(user));
            requestInitLoad();

        }, (loginRxActivity1, throwable) -> {
            isVisibleProgress.set(View.GONE);
            handleErrorLogin(throwable);
        });

        restartableFirst(REQUEST_INITLOAD, () -> {

            MattermostApp application = MattermostApp.getSingleton();
            ApiMethod service = application.getMattermostRetrofitService();
            isVisibleProgress.set(View.VISIBLE);
            return service.initLoad()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread());
        }, (loginRxActivity, initObject) -> {
            List<Team> teams = saveDataAfterLogin(initObject);
            isOpenChatScreen = (teams.size() == 1);
            Log.d(TAG, "Complete");
            isVisibleProgress.set(View.GONE);
            if (isOpenChatScreen) {
                loginRxActivity.showChatActivity();
            } else {
                //TODO start team chose activity
            }
        }, (loginRxActivity1, throwable) -> {
            isVisibleProgress.set(View.GONE);
            handleErrorLogin(throwable);
        });

    }

}
