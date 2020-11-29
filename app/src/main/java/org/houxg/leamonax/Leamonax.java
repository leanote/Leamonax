package org.houxg.leamonax;


import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.database.CursorWindow;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.facebook.stetho.Stetho;
import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.loader.glide.GlideImageLoader;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.yuyh.library.imgsel.ISNav;
import com.yuyh.library.imgsel.common.ImageLoader;

import net.danlew.android.joda.JodaTimeAndroid;

import org.greenrobot.eventbus.EventBus;
import org.houxg.leamonax.ui.MainActivity;

import java.lang.reflect.Field;

public class Leamonax extends Application {

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        XLog.init(BuildConfig.DEBUG ? LogLevel.ALL : LogLevel.NONE);
        if (!TextUtils.isEmpty(BuildConfig.BUGLY_KEY)) {
            initBugly();
        }
        BigImageViewer.initialize(GlideImageLoader.with(this));
        EventBus.builder()
                .logNoSubscriberMessages(false)
                .sendNoSubscriberEvent(false)
                .throwSubscriberException(true)
                .installDefaultEventBus();
        FlowManager.init(new FlowConfig.Builder(this).build());
        JodaTimeAndroid.init(this);
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }
        ISNav.getInstance().init(new ImageLoader() {
            @Override
            public void displayImage(Context context, String path, ImageView imageView) {
                Glide.with(context).load(path).into(imageView);
            }
        });
        // Increase allowed size of notes.
        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 30 * 1024 * 1024); // Allow 30MB notes
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initBugly() {
        Beta.canShowUpgradeActs.add(MainActivity.class);
        Beta.upgradeCheckPeriod = 60 * 1000; // 1 minute

        Resources res = getResources();
        Beta.strToastYourAreTheLatestVersion =res.getString(R.string.your_are_the_latest_version);
        Beta.strToastCheckUpgradeError =res.getString(R.string.check_upgrade_error);
        Beta.strToastCheckingUpgrade =res.getString(R.string.checking_upgrade);
        Beta.strNotificationDownloading =res.getString(R.string.downloading);
        Beta.strNotificationClickToView =res.getString(R.string.click_to_view);
        Beta.strNotificationClickToInstall =res.getString(R.string.click_to_install);
        Beta.strNotificationClickToRetry =res.getString(R.string.click_to_retry);
        Beta.strNotificationClickToContinue =res.getString(R.string.continue_download);
        Beta.strNotificationDownloadSucc =res.getString(R.string.download_successful);
        Beta.strNotificationDownloadError =res.getString(R.string.download_error);
        Beta.strNotificationHaveNewVersion =res.getString(R.string.have_new_version);
        Beta.strNetworkTipsMessage =res.getString(R.string.should_continue_download);
        Beta.strNetworkTipsTitle =res.getString(R.string.network_prompt);
        Beta.strNetworkTipsConfirmBtn =res.getString(R.string.continue_download);
        Beta.strNetworkTipsCancelBtn =res.getString(R.string.cancel);
        Beta.strUpgradeDialogVersionLabel =res.getString(R.string.version);
        Beta.strUpgradeDialogFileSizeLabel =res.getString(R.string.file_size);
        Beta.strUpgradeDialogUpdateTimeLabel =res.getString(R.string.update_time);
        Beta.strUpgradeDialogFeatureLabel =res.getString(R.string.what_s_new);
        Beta.strUpgradeDialogUpgradeBtn =res.getString(R.string.upgrade_now);
        Beta.strUpgradeDialogInstallBtn =res.getString(R.string.install);
        Beta.strUpgradeDialogRetryBtn =res.getString(R.string.retry);
        Beta.strUpgradeDialogContinueBtn =res.getString(R.string.continue_text);
        Beta.strUpgradeDialogCancelBtn =res.getString(R.string.next_time);
        
        Bugly.init(this, BuildConfig.BUGLY_KEY, BuildConfig.DEBUG);
    }
}
