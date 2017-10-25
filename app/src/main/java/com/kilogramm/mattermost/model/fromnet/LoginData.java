package com.kilogramm.mattermost.model.fromnet;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Evgeny on 26.07.2016.
 */
public class LoginData {
    @SerializedName("login_id")
    private String login;
    @SerializedName("password")
    private String password;
    @SerializedName("token")
    private String token;
    @SerializedName("device_id")
    private String device_id;

    public LoginData(String login, String password, String token, String device_id) {
        this.login = login;
        this.password = password;
        this.token = token;
        this.device_id = device_id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
