package com.kilogramm.mattermost.view.direct;

import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.ActivityWholeDirectListBinding;
import com.kilogramm.mattermost.model.entity.Channel;
import com.kilogramm.mattermost.model.entity.User;
import com.kilogramm.mattermost.presenter.WholeDirectListPresenter;
import com.kilogramm.mattermost.view.BaseActivity;

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmResults;
import nucleus.factory.RequiresPresenter;

/**
 * Created by melkshake on 14.09.16.
 */
@RequiresPresenter(WholeDirectListPresenter.class)
public class WholeDirectListActivity extends BaseActivity<WholeDirectListPresenter> {

    private ActivityWholeDirectListBinding binding;
    private WholeDirectListAdapter adapter;
    private OnDirectItemClickListener directItemClickListener;
    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.realm = Realm.getDefaultInstance();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_whole_direct_list);
        View view = binding.getRoot();
        init();
        //setRecycleView();
    }

    private void init() {
        setupToolbar(getString(R.string.title_direct_list), true);
        setColorScheme(R.color.colorPrimary, R.color.colorPrimaryDark);
        getPresenter().getProfilesForDirectMessage();
    }

    public void setRecycleView() {
        RealmResults<User> users = realm.where(User.class).isNotNull("id").findAllSorted("username");

        ArrayList<String> usersIds = new ArrayList<>();
        for (User user : users){
            usersIds.add(user.getId());
        }

        // TODO правильно провести логику нажатий и раскомментировать + конструктор в адаптере
        adapter = new WholeDirectListAdapter(this, users, true, usersIds, getPresenter());
//                (itemId, name) -> {
//                    directItemClickListener.onDirectClick(itemId, name);
//                });

        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        binding.recViewDirect.setLayoutManager(manager);
        binding.recViewDirect.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

    //==========================MVP methods==================================================

    public void finishActivity() {
        finish();
    }

    public void setDirectItemClickListener(OnDirectItemClickListener listener) {
        this.directItemClickListener = listener;
    }

    public interface OnDirectItemClickListener {
        void onDirectClick(String itemId, String name);
    }

    public Drawable getStatusDrawable(String status) {
        if(status == null) {
            return getResources().getDrawable(R.drawable.status_offline_drawable);
        }

        switch (status) {
            case Channel.ONLINE:
                return getResources().getDrawable(R.drawable.status_online_drawable);
            case Channel.OFFLINE:
                return getResources().getDrawable(R.drawable.status_offline_drawable);
            case Channel.AWAY:
                return getResources().getDrawable(R.drawable.status_away_drawable);
            case Channel.REFRESH:
                return getResources().getDrawable(R.drawable.status_refresh_drawable);
            default:
                return getResources().getDrawable(R.drawable.status_offline_drawable);
        }
    }

    public String getImageUrl(String userId) {
        if (userId != null) {
            return "https://"
                    + MattermostPreference.getInstance().getBaseUrl()
                    + "/api/v3/users/"
                    + userId
                    + "/image";
        } else {
            return "";
        }
    }

}
