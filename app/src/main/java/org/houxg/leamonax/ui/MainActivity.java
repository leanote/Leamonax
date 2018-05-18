package org.houxg.leamonax.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.elvishew.xlog.XLog;
import com.tencent.bugly.crashreport.CrashReport;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.houxg.leamonax.R;
import org.houxg.leamonax.background.NoteSyncService;
import org.houxg.leamonax.component.PullToRefresh;
import org.houxg.leamonax.database.NotebookDataStore;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.model.SyncEvent;
import org.houxg.leamonax.ui.edit.NoteEditActivity;
import org.houxg.leamonax.utils.NetworkUtils;
import org.houxg.leamonax.utils.ToastUtils;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseActivity implements Navigation.Callback {

    private static final String EXT_SHOULD_RELOAD = "ext_should_reload";
    private static final String TAG_NOTE_FRAGMENT = "tag_note_fragment";

    NoteFragment mNoteFragment;

    @BindView(R.id.drawer)
    View mNavigationView;

    PullToRefresh mPullToRefresh;
    Navigation mNavigation;

    public static Intent getOpenIntent(Context context, boolean shouldReload) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXT_SHOULD_RELOAD, shouldReload);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initToolBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white);
        CrashReport.setUserId(Account.getCurrent().getUserId());

        mNavigation = new Navigation(this);
        mNavigation.init(this, mNavigationView);

        boolean shouldReload = false;
        if (savedInstanceState == null) {
            shouldReload = getIntent().getBooleanExtra(EXT_SHOULD_RELOAD, false);
            mNoteFragment = NoteFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.container, mNoteFragment, TAG_NOTE_FRAGMENT).commit();
        } else {
            mNoteFragment = (NoteFragment) getSupportFragmentManager().findFragmentByTag(TAG_NOTE_FRAGMENT);
        }

        mPullToRefresh = new PullToRefresh(
                (SwipeRefreshLayout) findViewById(R.id.swiperefresh),
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        syncNotes();
                    }
                });

        EventBus.getDefault().register(this);

        if (shouldReload) {
            mPullToRefresh.forceRefresh();
        }
    }

    private void syncNotes() {
        if (!NetworkUtils.isNetworkAvailable()) {
            ToastUtils.showNetworkUnavailable(MainActivity.this);
            mPullToRefresh.stopRefreshing();
            return;
        }
        NoteSyncService.startServiceForNote(MainActivity.this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNavigation.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            mNavigation.toggle();
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mNavigation.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (mNavigation.isOpen()) {
            mNavigation.close();
        } else {
            if (mNoteFragment.canGoBack()) {
                mNoteFragment.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @OnClick(R.id.fab)
    void clickedFab() {
        Account account = Account.getCurrent();
        Note newNote = new Note();
        newNote.setUserId(account.getUserId());
        Notebook notebook;
        NoteFragment.Mode currentMode = mNavigation.getCurrentMode();
        if (currentMode == NoteFragment.Mode.NOTEBOOK) {
            notebook = NotebookDataStore.getByLocalId(currentMode.notebookId);
        } else {
            notebook = NotebookDataStore.getRecentNoteBook(Account.getCurrent().getUserId());
        }
        if (notebook != null) {
            newNote.setNoteBookId(notebook.getNotebookId());
        } else {
            Exception exception = new IllegalStateException(
                    String.format(Locale.US, "notebook is null, mode:%s, notebookLocalId:%d", currentMode, currentMode.notebookId));
            CrashReport.postCatchedException(exception);
        }
        newNote.setIsMarkDown(account.getDefaultEditor() == Account.EDITOR_MARKDOWN);
        newNote.save();
        Intent intent = NoteEditActivity.getOpenIntent(this, newNote.getId(), true);
        startActivity(intent);
    }

    @Override
    public boolean onChangeAccount(Account account) {
        account.updateLastUseTime();
        account.update();
        mNavigation.refresh();
        mPullToRefresh.stopRefreshing();
        syncNotes();
        return true;
    }

    @Override
    public boolean onShowNotes(NoteFragment.Mode mode) {
        mNoteFragment.setMode(mode);
        return true;
    }

    @Override
    public void onClickSetting() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClickAbout() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SyncEvent event) {
        XLog.i("RequestNotes rcv: isSucceed=" + event.isSucceed());
        mPullToRefresh.stopRefreshing();
        if (event.isSucceed()) {
            mNavigation.refresh();
        } else {
            if (!TextUtils.isEmpty(event.getMsg())) {
                ToastUtils.show(this, event.getMsg());
            } else {
                ToastUtils.show(this, R.string.sync_notes_failed);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
