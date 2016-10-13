package com.kilogramm.mattermost;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.facebook.stetho.Stetho;
import com.kilogramm.mattermost.network.ApiMethod;
import com.kilogramm.mattermost.network.MattermostRetrofitService;
import com.kilogramm.mattermost.network.PicassoService;
import com.kilogramm.mattermost.network.TestApiGuthubMethod;
import com.kilogramm.mattermost.network.TestGithubRetrofitService;
import com.kilogramm.mattermost.tools.FileUtil;
import com.uphyca.stetho_realm.RealmInspectorModulesProvider;

import org.java_websocket.client.WebSocketClient;

import java.util.regex.Pattern;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.Scheduler;

/**
 * Created by Evgeny on 25.07.2016.
 */
// TODO тут почти ни один класс не используется. Их можно удалить? (Kepar)
public class MattermostApp extends Application{

    public static final String URL_WEB_SOCKET = "wss://mattermost.kilograpp.com/api/v3/users/websocket";

    private static MattermostApp singleton = null;

    private ApiMethod mattermostRetrofitService;
    private TestApiGuthubMethod testApiGuthubMethod;
    private Scheduler defaultSubscribeSchedulder;
    private WebSocketClient mWebSocketClient;

    public static MattermostApp get(Context context){
        return (MattermostApp) context.getApplicationContext();
    }

    public static MattermostApp getSingleton(){
        return singleton;
    }
    public void refreshMattermostRetrofitService(){
        mattermostRetrofitService = MattermostRetrofitService.refreshRetrofitService();
    }

    public TestApiGuthubMethod getGithubService(){
        if(testApiGuthubMethod == null){
            testApiGuthubMethod = TestGithubRetrofitService.create();
        }
        return testApiGuthubMethod;
    }
    public ApiMethod getMattermostRetrofitService() throws IllegalArgumentException{
        if(mattermostRetrofitService == null){
            mattermostRetrofitService = MattermostRetrofitService.create();
        }
        return mattermostRetrofitService;
    }

    public Scheduler getDefaultSubscribeSchedulder() {
        if(defaultSubscribeSchedulder == null){
            defaultSubscribeSchedulder = rx.schedulers.Schedulers.io();
        }
        return defaultSubscribeSchedulder;
    }

    public void setDefaultSubscribeSchedulder(Scheduler scheduler){
        defaultSubscribeSchedulder = scheduler;
    }

    @Override
    public void onCreate() {
        MultiDex.install(getApplicationContext());
        super.onCreate();
        if(!BuildConfig.DEBUG){
            Fabric.with(this, new Crashlytics());
        }
        singleton = this;
        FileUtil.createInstance(getApplicationContext());
        RealmConfiguration configuration = new RealmConfiguration.Builder(getApplicationContext())
                .name("mattermostDb.realm")
                .build();
        Realm.compactRealm(configuration);
        Realm.removeDefaultConfiguration();
        Realm.setDefaultConfiguration(configuration);

        PicassoService.create(getApplicationContext());
        MattermostPreference.createInstance(getApplicationContext());

        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this)
                                .databaseNamePattern(Pattern.compile(".+\\.realm"))
                                .build())
                        .build());
    }

    public void compact() {
        RealmConfiguration configuration = new RealmConfiguration.Builder(getApplicationContext())
                .name("mattermostDb.realm")
                .build();
        Realm.compactRealm(configuration);
    }
}
