package com.kilogramm.mattermost.view.menu;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;

import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.databinding.ActivityRightMenuAboutAppBinding;
import com.kilogramm.mattermost.view.BaseActivity;

/**
 * Created by melkshake on 02.11.16.
 */

public class RightMenuAboutAppActivity extends BaseActivity {

    private ActivityRightMenuAboutAppBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_right_menu_about_app);

        setupToolbar(getApplicationContext().getResources().getString(R.string.about_mattermost), true);
        setColorScheme(R.color.colorPrimary, R.color.colorPrimaryDark);
        
        mBinding.textViewMattermostOrg.setMovementMethod(LinkMovementMethod.getInstance());
        mBinding.textViewKilograppTeam.setMovementMethod(LinkMovementMethod.getInstance());

        mBinding.textViewVesrion.setText("Version: " + getString(R.string.app_version));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, RightMenuAboutAppActivity.class);
        context.startActivity(starter);
    }
}
