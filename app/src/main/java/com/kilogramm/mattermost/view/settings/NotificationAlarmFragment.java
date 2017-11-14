package com.kilogramm.mattermost.view.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;

import android.databinding.DataBindingUtil;
import android.view.ViewGroup;

import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.FragmentAlarmNotificationBinding;
import com.kilogramm.mattermost.presenter.settings.NotificationPresenter;
import com.kilogramm.mattermost.view.fragments.BaseFragment;

/**
 * Created by Christian on 26.10.2017.
 */
public class NotificationAlarmFragment extends BaseFragment implements View.OnClickListener {

    private FragmentAlarmNotificationBinding mBinding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm_notification,
                container, false);

        initSetting();
        initOnClick();
        mBinding.textViewDescription.setText(getText(R.string.alarm_description));
        return mBinding.getRoot();
    }

    @Override
    public void onClick(View view) {
        String tagClicked = view.getTag().toString();
        if (tagClicked.equals("Sound")){
            getPresenter().toggleSoundSetting();
        }
        if (tagClicked.equals("Vibration")){
            getPresenter().toggleVibrationSetting();
        }
        initSetting();
    }

    @Override
    public NotificationPresenter getPresenter() {
        return ((NotificationActivity) getActivity()).getPresenter();
    }

    private void initOnClick() {
        mBinding.cardViewVibration.setOnClickListener(this);
        mBinding.cardViewSound.setOnClickListener(this);
    }

    private void initSetting() {
        notifyAlarmSetting();
        if(getPresenter().getSoundSetting().equals(getString(R.string.alarm_sound))){
            mBinding.imageViewSelectSound.setVisibility(View.VISIBLE);
        }
        if(getPresenter().getVibrationSetting().equals(getString(R.string.alarm_vibration))){
            mBinding.imageViewSelectVibration.setVisibility(View.VISIBLE);
        }
    }

    private void notifyAlarmSetting() {
        mBinding.imageViewSelectVibration.setVisibility(View.INVISIBLE);
        mBinding.imageViewSelectSound.setVisibility(View.INVISIBLE);
    }
}
