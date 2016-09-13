package com.kilogramm.mattermost.presenter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.kilogramm.mattermost.BuildConfig;
import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.model.entity.ClientCfg;
import com.kilogramm.mattermost.model.entity.InitObject;
import com.kilogramm.mattermost.model.error.HttpError;
import com.kilogramm.mattermost.network.ApiMethod;
import com.kilogramm.mattermost.view.authorization.MainActivity;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.Realm;
import io.realm.RealmResults;
import nucleus.presenter.Presenter;
import retrofit2.adapter.rxjava.HttpException;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by kraftu on 13.09.16.
 */
public class MainPresenter extends Presenter<MainActivity> {

    private static final String TAG = "MainPresenter";

    //TODO pattern url null fix
    private static Pattern mPatternUrl = Patterns.WEB_URL;

    private Subscription mSubscription;

    private Realm mRealm;

    private MattermostApp mMattermostApp;


    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        super.onCreate(savedState);
        mMattermostApp = MattermostApp.getSingleton();
        mRealm = Realm.getDefaultInstance();

    }

    @Override
    protected void onTakeView(MainActivity mainActivity) {
        super.onTakeView(mainActivity);

        //TODO FIX logic check login
        if(MattermostPreference.getInstance().getAuthToken()!=null &&
                MattermostPreference.getInstance().getCookies()!=null){
            getView().showChatActivity();
        }
        getView().setShowProgress(false);

        if(BuildConfig.DEBUG) getView().setTextUrl("https://mattermost.kilograpp.com");
    }

    public void checkConnetHost(String editTextUrl){
        URI url = URI.create(editTextUrl);
        String s = url.getAuthority();
        if(s == null){
            s = url.toString();
        }

        //TODO FIX logic
        MattermostPreference.getInstance().setBaseUrl(s);
        mMattermostApp.refreshMattermostRetrofitService();

        if(mSubscription!=null && !mSubscription.isUnsubscribed()){
            mSubscription.unsubscribe();
        }

        //TODO FIX logic
        ApiMethod service = null;
        try{
            service = mMattermostApp.getMattermostRetrofitService();
        } catch (IllegalArgumentException e){
            e.printStackTrace();
            getView().showErrorText("Url is not valid https://");
            return;
        }

        getView().setShowProgress(true);

        mSubscription = service.initLoad()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<InitObject>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Complete");
                        getView().setShowProgress(false);
                        getView().showLoginActivity();
                    }
                    @Override
                    public void onError(Throwable e) {
                        getView().setShowProgress(false);
                        //TODO make error handling logic
                        handleErrorLogin(e);
                    }
                    @Override
                    public void onNext(InitObject initObject) {
                        getView().showErrorText("Url is not valid https://");

                        //TODO FIX logic save state config
                        mRealm.executeTransaction(realm -> {
                            RealmResults<ClientCfg> results = realm.where(ClientCfg.class).findAll();
                            results.deleteAllFromRealm();
                        });
                        mRealm.executeTransaction(realm1 -> {
                            ClientCfg cfg = realm1.copyToRealm(initObject.getClientCfg());
                            realm1.copyToRealm(cfg);
                        });
                    }
                });

    }

    public void checkEnterUrl(String url){
        //TODO check logic url
       //getView().setShowNextButton(isValidUrl(url));
    }

    private boolean isValidUrl(String url) {
        Matcher m = mPatternUrl.matcher(url);
        return m.matches();
    }

   private void handleErrorLogin(Throwable e) {
        if(e instanceof HttpException){
            HttpError error;
            try {
                error = new Gson()
                        .fromJson((((HttpException) e)
                                .response()
                                .errorBody()
                                .string()), HttpError.class);
                Log.d(TAG, error.getMessage());
                getView().showErrorText(error.getMessage());
            } catch (IOException e1) {
                Log.d(TAG, "Message not has body.");
                e1.printStackTrace();
            }
        } else if(e instanceof JsonSyntaxException) {
            getView().showErrorText("invalid response from the server");
            e.printStackTrace();
        } else {
            getView().showErrorText( e.getMessage());
            Log.d(TAG, "SystemException, stackTrace: \n");
            e.printStackTrace();
        }
    }

}
