package org.houxg.leamonax.ui;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.service.AccountService;

public class LaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launch);
        final Intent intent;
        if (AccountService.isSignedIn()) {
            Account account = Account.getCurrent();
            ApiProvider.getInstance().init(account.getHost());
            intent = MainActivity.getOpenIntent(this, false);
        } else {
            intent = new Intent(this, SignInActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        findViewById(R.id.iv_logo)
                .animate()
                .setDuration(1000)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animator) {

                    }
                }).start();
    }

    @Override
    public void onBackPressed() {

    }
}
