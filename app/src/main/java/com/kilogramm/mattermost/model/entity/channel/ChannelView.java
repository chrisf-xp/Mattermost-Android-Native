package com.kilogramm.mattermost.model.entity.channel;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Christian on 13.11.2017.
 */

public class ChannelView{

    @SerializedName("channel_id")
    @Expose
    public String channelId;
    @SerializedName("prev_channel_id")
    @Expose
    public String prevChannelId;

    public ChannelView(String channelId, String prevChannelId){
        this.channelId = channelId;
        this.prevChannelId = prevChannelId;
    }

    public ChannelView(String channelId){
        this.channelId = channelId;
        this.prevChannelId = "";
    }
}
