package org.houxg.leamonax.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.houxg.leamonax.R;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.toolbar));
        }

    }

    protected void initToolBar(Toolbar toolbar) {
        initToolBar(toolbar, false);
    }

    protected void initToolBar(Toolbar toolbar, boolean hasBackArrow) {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setTitleTextColor(0xffFAFAFA);
            toolbar.setTitle(getTitle());
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null && hasBackArrow) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            ActivityCompat.finishAfterTransition(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
