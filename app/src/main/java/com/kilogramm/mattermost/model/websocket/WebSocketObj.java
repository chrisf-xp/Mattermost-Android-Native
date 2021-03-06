package com.kilogramm.mattermost.model.websocket;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.kilogramm.mattermost.model.entity.Broadcast;
import com.kilogramm.mattermost.model.entity.Data;
import com.kilogramm.mattermost.model.entity.Preference.Preferences;
import com.kilogramm.mattermost.model.entity.post.Post;

import java.util.Map;

import io.realm.annotations.Ignore;

/**
 * Created by Evgeny on 31.08.2016.
 */
public class WebSocketObj implements Parcelable {

    public static final String TEAM_ID = "team_id";
    public static final String CHANNEL_ID = "channel_id";
    public static final String USER_ID = "user_id";
    public static final String EVENT = "event";
    public static final String DATA = "data";

    public static final String EVENT_POSTED = "posted";
    public static final String EVENT_CHANNEL_VIEWED = "channel_viewed";
    public static final String OMIT_USERS = "omit_users";
    public static final String EVENT_NEW_USER = "new_user";
    public static final String EVENT_USER_ADDED = "user_added";
    public static final String EVENT_USER_REMOVE= "user_removed";
    public static final String EVENT_DIRECT_ADDED = "direct_added";
    public static final String EVENT_CHANNEL_DELETED = "channel_deleted";
    public static final String EVENT_TYPING = "typing";
    public static final String EVENT_POST_EDITED = "post_edited";
    public static final String EVENT_POST_DELETED = "post_deleted";
    public static final String EVENT_STATUS_CHANGE = "status_change";
    public static final String EVENT_PREFERENCE_CHANGED = "preference_changed";

    //Posted
    public static final String CHANNEL_DISPLAY_NAME = "channel_display_name";
    public static final String CHANNEL_TYPE = "channel_type";
    public static final String CHANNEL_POST = "post";
    public static final String SENDER_NAME = "sender_name";
    public static final String MENTIONS = "mentions";

    //Typing
    public static final String PARENT_ID = "parent_id";
    public static final String STATE = "state";

    //Status
    public static final String STATUS = "status";
    public static final String SEQ_REPLAY = "seq_reply";
    public static final String ALL_USER_STATUS = "all_user_status";
    public static final String PREFERENCES = "preference";
    public static final String BROADCAST = "broadcast";
    @SerializedName("team_id")
    @Expose
    private String teamId;
    @SerializedName("channel_id")
    @Expose
    private String channelId;
    @SerializedName("user_id")
    @Expose
    private String userId;
    @Ignore
    @Expose
    private String postId;
    @SerializedName("event")
    @Expose
    private String event;
    private transient String dataJSON;
    private transient String broadcastJSON;
    @SerializedName("data")
    @Expose
    private transient Data data;
    @SerializedName("broadcast")
    @Expose
    private Broadcast broadcast;
    @SerializedName("seq_reply")
    @Expose
    private Integer seqReplay;

    @SerializedName("seq")
    @Expose
    private Integer seq;


    //Posted
    @SerializedName("channel_display_name")
    @Expose
    private String channelDisplayName;
    @SerializedName("channel_type")
    @Expose
    private String channelType;
    @SerializedName("mentions")
    @Expose
    private String mentions;
    @SerializedName("post")
    @Expose
    private Post post;
    @SerializedName("sender_name")
    @Expose
    private String senderName;

    public Broadcast getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(Broadcast broadcast) {
        this.broadcast = broadcast;
    }

    public Integer getSeq() {
        return seq;
    }

    public void setDataJSON(String dataJSON) {
        this.dataJSON = dataJSON;
    }

    public Integer getSeqReplay() {
        return seqReplay;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
        if(data!=null) userId = data.getUserId();
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public WebSocketObj() {
    }

    public void setBroadcastJSON(String broadcastJSON) {
        this.broadcastJSON = broadcastJSON;
    }

    public static class BuilderData {
        private String channelDisplayName;
        private String channelType;
        private String mentions;
        private String userId;
        private Post post;
        private String senderName;
        private String teamId;
        private String status;
        private String parentId;
        private Preferences preferences;
        private Map<String, String> mapUserStatus;
        private String channelId;

        public BuilderData setChannelDisplayName(String channelDisplayName) {
            this.channelDisplayName = channelDisplayName;
            return this;
        }

        public BuilderData setChannelType(String channelType) {
            this.channelType = channelType;
            return this;
        }

        public BuilderData setMentions(String mentions) {
            this.mentions = mentions;
            return this;
        }

        public BuilderData setPreference(Preferences preference){
            this.preferences = preference;
            return this;
        }

        public BuilderData setPost(Post post) {
            this.post = post;
            return this;
        }

        public BuilderData setSenderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public BuilderData setTeamId(String teamId){
            this.teamId = teamId;
            return this;
        }

        public BuilderData setParentId(String parentId){
            this.parentId = parentId;
            return this;
        }

        public BuilderData setUser(String userId) {
            this.userId = userId;
            return this;
        }

        public BuilderData setMapUserStatus(Map<String, String> mapUserStatus) {
            this.mapUserStatus = mapUserStatus;
            return this;
        }

        public Data build(){
            return new Data(channelDisplayName,
                    channelType,
                    mentions,
                    post,
                    senderName,
                    teamId,
                    status,
                    mapUserStatus,
                    preferences,
                    userId,
                    channelId);
        }

        public BuilderData setStatus(String status) {
            this.status = status;
            return this;
        }

        public BuilderData setChannelId(String channelId) {
            this.channelId = channelId;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.teamId);
        dest.writeString(this.channelId);
        dest.writeString(this.userId);
        dest.writeString(this.postId);
        dest.writeString(this.event);
        dest.writeParcelable(this.broadcast, flags);
        dest.writeValue(this.seqReplay);
        dest.writeValue(this.seq);
        dest.writeString(this.channelDisplayName);
        dest.writeString(this.channelType);
        dest.writeString(this.mentions);
        dest.writeParcelable(this.post, flags);
        dest.writeString(this.senderName);
    }

    protected WebSocketObj(Parcel in) {
        this.teamId = in.readString();
        this.channelId = in.readString();
        this.userId = in.readString();
        this.postId = in.readString();
        this.event = in.readString();
        this.broadcast = in.readParcelable(Broadcast.class.getClassLoader());
        this.seqReplay = (Integer) in.readValue(Integer.class.getClassLoader());
        this.seq = (Integer) in.readValue(Integer.class.getClassLoader());
        this.channelDisplayName = in.readString();
        this.channelType = in.readString();
        this.mentions = in.readString();
        this.post = in.readParcelable(Post.class.getClassLoader());
        this.senderName = in.readString();
    }

    public static final Creator<WebSocketObj> CREATOR = new Creator<WebSocketObj>() {
        @Override
        public WebSocketObj createFromParcel(Parcel source) {
            return new WebSocketObj(source);
        }

        @Override
        public WebSocketObj[] newArray(int size) {
            return new WebSocketObj[size];
        }
    };
}
