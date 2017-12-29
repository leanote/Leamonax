package org.houxg.leamonax.ui;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.Toast;

import org.houxg.leamonax.R;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.network.ApiProvider;
import org.houxg.leamonax.service.AccountService;

public class LaunchActivity extends Activity {
    final int PERMISSIONS_REQUEST_CODE=19919;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_launch);
        if(Build.VERSION.SDK_INT >= 23){
            if(hasPermission(
                    Manifest.permission.CAMERA ,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE ,
                    Manifest.permission.READ_EXTERNAL_STORAGE ,
                    Manifest.permission.READ_PHONE_STATE
            )){
                doAfterGetPermission();
            }else {
                AlertDialog.Builder builder=new AlertDialog.Builder(LaunchActivity.this);
                builder.setMessage(getString(R.string.permission_get_description));
                builder.setTitle(getString(R.string.permission_get));
                builder.setPositiveButton(getString(R.string.allow), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermission(PERMISSIONS_REQUEST_CODE,
                                Manifest.permission.CAMERA ,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE ,
                                Manifest.permission.READ_EXTERNAL_STORAGE ,
                                Manifest.permission.READ_PHONE_STATE
                        );
                    }
                });
                builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                builder.setCancelable(false).create().show();
            }
        }else {
            doAfterGetPermission();
        }

    }

    @Override
    public void onBackPressed() {

    }

    @SuppressLint("WrongViewCast")
    private void doAfterGetPermission(){
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

    public boolean hasPermission(String... permissions){
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestPermission(int requestCode, String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if(hasPermission(Manifest.permission.CAMERA ,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE ,
                            Manifest.permission.READ_EXTERNAL_STORAGE ,
                            Manifest.permission.READ_PHONE_STATE
                    )){
                        doAfterGetPermission();
                    }else {
                        Toast.makeText(LaunchActivity.this,getString(R.string.permission_get_error),Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }else {
                    Toast.makeText(LaunchActivity.this,getString(R.string.permission_get_error),Toast.LENGTH_SHORT).show();
                    this.finish();
                }
            default:
                break;
        }
    }

}
