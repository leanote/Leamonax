package org.houxg.leamonax.ui.edit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.elvishew.xlog.XLog;

import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.R;
import org.houxg.leamonax.ReadableException;
import org.houxg.leamonax.database.NoteDataStore;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Tag;
import org.houxg.leamonax.service.NoteFileService;
import org.houxg.leamonax.service.NoteService;
import org.houxg.leamonax.ui.BaseActivity;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.DialogDisplayer;
import org.houxg.leamonax.utils.NetworkUtils;
import org.houxg.leamonax.utils.ToastUtils;
import org.houxg.leamonax.widget.LeaViewPager;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

//TODO: hide action bar
public class NoteEditActivity extends BaseActivity implements EditorFragment.EditorFragmentListener, SettingFragment.SettingFragmentListener {

    private static final String TAG = "NoteEditActivity:";
    public static final int RESULT_CONFLICT = 525;
    public static final String EXT_NOTE_LOCAL_ID = "ext_note_local_id";
    public static final String EXT_IS_NEW_NOTE = "ext_is_new_note";
    public static final String TAG_EDITOR = "tag_editor_tag";
    public static final String TAG_SETTING = "tag_setting_tag";
    public static final int FRAG_EDITOR = 0;
    public static final int FRAG_SETTINGS = 1;

    private EditorFragment mEditorFragment;
    private SettingFragment mSettingsFragment;
    private Wrapper mOriginal;
    private Wrapper mModified;
    private boolean mIsNewNote;

    private LeaViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        initToolBar((Toolbar) findViewById(R.id.toolbar), true);
        mPager = (LeaViewPager) findViewById(R.id.pager);
        mPager.setPagingEnabled(false);
        mPager.setAdapter(new SectionAdapter(getSupportFragmentManager()));
        mPager.setOffscreenPageLimit(2);

        if (savedInstanceState != null) {
            mEditorFragment = (EditorFragment) getSupportFragmentManager().findFragmentByTag(savedInstanceState.getString(TAG_EDITOR));
            mSettingsFragment = (SettingFragment) getSupportFragmentManager().findFragmentByTag(savedInstanceState.getString(TAG_SETTING));
        }

        long noteLocalId = getIntent().getLongExtra(EXT_NOTE_LOCAL_ID, -1);
        if (noteLocalId == -1) {
            finish();
            return;
        }
        mIsNewNote = getIntent().getBooleanExtra(EXT_IS_NEW_NOTE, false);
        mOriginal = new Wrapper(NoteDataStore.getByLocalId(noteLocalId));
        mModified = new Wrapper(NoteDataStore.getByLocalId(noteLocalId));
        setResult(RESULT_CANCELED);
    }

    public static Intent getOpenIntent(Context context, long noteLocalId, boolean isNewNote) {
        Intent intent = new Intent(context, NoteEditActivity.class);
        intent.putExtra(EXT_NOTE_LOCAL_ID, noteLocalId);
        intent.putExtra(EXT_IS_NEW_NOTE, isNewNote);
        return intent;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO:save note state
        outState.putString(TAG_EDITOR, mEditorFragment.getTag());
        outState.putString(TAG_SETTING, mSettingsFragment.getTag());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_note, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent();
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        this.sendBroadcast(intent);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                filterUnchanged()
                        .doOnNext(new Action1<Wrapper>() {
                            @Override
                            public void call(Wrapper wrapper) {
                                saveAsDraft(wrapper);
                                setResult(RESULT_OK);
                                NetworkUtils.checkNetwork();
                            }
                        })
                        .flatMap(new Func1<Wrapper, Observable<Long>>() {
                            @Override
                            public Observable<Long> call(Wrapper wrapper) {
                                return uploadToServer(wrapper.note.getId());
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Action0() {
                            @Override
                            public void call() {
                                DialogDisplayer.showProgress(NoteEditActivity.this, R.string.saving_note);
                            }
                        })
                        .subscribe(new Observer<Long>() {
                            @Override
                            public void onCompleted() {
                                DialogDisplayer.dismissProgress();
                                finish();
                            }

                            @Override
                            public void onError(Throwable e) {
                                DialogDisplayer.dismissProgress();
                                ToastUtils.show(Leamonax.getContext(), e.getMessage());
                                if (e instanceof NetworkUtils.NetworkUnavailableException) {
                                    finish();
                                } else if (e instanceof ReadableException) {
                                    setResult(RESULT_CONFLICT);
                                    finish();
                                }
                            }

                            @Override
                            public void onNext(Long noteLocalId) {
                                Note localNote = NoteDataStore.getByLocalId(noteLocalId);
                                localNote.setIsDirty(false);
                                localNote.save();
                            }
                        });
                return true;
            case R.id.action_settings:
                mPager.setCurrentItem(FRAG_SETTINGS);
                return true;
            case android.R.id.home:
                if (mPager.getCurrentItem() > FRAG_EDITOR) {
                    mPager.setCurrentItem(FRAG_EDITOR);
                } else {
                    filterUnchanged()
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnCompleted(new Action0() {
                                @Override
                                public void call() {
                                    NoteEditActivity.super.onOptionsItemSelected(item);
                                }
                            })
                            .subscribe(new Action1<Wrapper>() {
                                @Override
                                public void call(Wrapper wrapper) {
                                    setResult(RESULT_OK);
                                    XLog.i(TAG + wrapper.toString());

                                    if (mIsNewNote && isTitleContentEmpty(wrapper.note)) {
                                        XLog.i(TAG + "remove empty note, id=" + wrapper.note.getId());
                                        wrapper.note.delete();
                                    } else {
                                        saveAsDraft(wrapper);
                                    }
                                }
                            });
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Observable<Long> uploadToServer(final long noteLocalId) {
        return Observable.create(
                new Observable.OnSubscribe<Long>() {
                    @Override
                    public void call(Subscriber<? super Long> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            NoteService.saveNote(noteLocalId);
                            subscriber.onNext(noteLocalId);
                            subscriber.onCompleted();
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() > FRAG_EDITOR) {
            mPager.setCurrentItem(FRAG_EDITOR);
        } else {
            filterUnchanged()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnCompleted(new Action0() {
                        @Override
                        public void call() {
                            NoteEditActivity.super.onBackPressed();
                        }
                    })
                    .subscribe(new Action1<Wrapper>() {
                        @Override
                        public void call(Wrapper wrapper) {
                            setResult(RESULT_OK);
                            XLog.i(TAG + wrapper.toString());

                            if (mIsNewNote && isTitleContentEmpty(wrapper.note)) {
                                XLog.i(TAG + "remove empty note, id=" + wrapper.note.getId());
                                wrapper.note.delete();
                            } else {
                                saveAsDraft(wrapper);
                            }
                        }
                    });
        }
    }

    private Observable<Wrapper> filterUnchanged() {
        return Observable.create(
                new Observable.OnSubscribe<Wrapper>() {
                    @Override
                    public void call(Subscriber<? super Wrapper> subscriber) {
                        if (!subscriber.isUnsubscribed()) {
                            updateNote();
                            if (mOriginal.note.hasChanges(mModified.note)
                                    || isTitleContentEmpty(mModified.note)
                                    || !CollectionUtils.isTheSame(mOriginal.tags, mModified.tags)
                                    || mModified.note.isDirty()
                                    || isLocalNote(mModified.note)) {
                                subscriber.onNext(mModified);
                            }
                            subscriber.onCompleted();
                        }
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    private boolean isTitleContentEmpty(Note note) {
        return TextUtils.isEmpty(note.getTitle()) && TextUtils.isEmpty(note.getContent());
    }

    private boolean isLocalNote(Note note) {
        return TextUtils.isEmpty(note.getNoteId());
    }

    private void updateNote() {
        String title = mEditorFragment.getTitle();
        String content = mEditorFragment.getContent();
        mModified.note.setTitle(title);
        mModified.note.setContent(content);
        mModified.note.setNoteBookId(mSettingsFragment.getNotebookId());
        List<String> tagTexts = mSettingsFragment.getTags();
        mModified.tags = new ArrayList<>();
        for (String tagText : tagTexts) {
            mModified.tags.add(tagText);
        }
        mModified.note.setIsPublicBlog(mSettingsFragment.shouldPublic());
    }

    private void saveAsDraft(Wrapper wrapper) {
        Note modifiedNote = wrapper.note;
        XLog.i(TAG + "saveAsDraft(), local id=" + modifiedNote.getId());
        Note noteFromDb = NoteDataStore.getByLocalId(modifiedNote.getId());
        noteFromDb.setContent(modifiedNote.getContent());
        noteFromDb.setTitle(modifiedNote.getTitle());
        noteFromDb.setNoteBookId(modifiedNote.getNoteBookId());
        noteFromDb.setIsPublicBlog(modifiedNote.isPublicBlog());
        noteFromDb.setIsDirty(true);
        long updateTime = System.currentTimeMillis();
        noteFromDb.setUpdatedTimeVal(updateTime);
        if (mIsNewNote) {
            noteFromDb.setCreatedTimeVal(updateTime);
        }
        noteFromDb.update();

        NoteService.updateTagsToLocal(modifiedNote.getId(), wrapper.tags);
    }

    @Override
    public Uri createImage(String filePath) {
        return NoteFileService.createImageFile(mModified.note.getId(), filePath);
    }

    @Override
    public Uri createAttach(String filePath) {
        return null;
    }

    @Override
    public void onInitialized() {
        mEditorFragment.setTitle(mModified.note.getTitle());
        mEditorFragment.setContent(mModified.note.getContent());
    }

    @Override
    public void onFragmentInitialized() {
        mSettingsFragment.setNotebookId(mModified.note.getNoteBookId());
        mSettingsFragment.setShouldPublic(mModified.note.isPublicBlog());
        List<String> tagTexts = new ArrayList<>();
        for (String tag : mModified.tags) {
            tagTexts.add(tag);
        }
        mSettingsFragment.setTags(tagTexts);
    }

    private class SectionAdapter extends FragmentPagerAdapter {


        public SectionAdapter(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return EditorFragment.getNewInstance(mModified.note.isMarkDown(), true);
                case 1:
                    return SettingFragment.getNewInstance();
                default:
                    return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            switch (position) {
                case 0:
                    mEditorFragment = (EditorFragment) fragment;
                    break;
                case 1:
                    mSettingsFragment = (SettingFragment) fragment;
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private static class Wrapper {
        Note note;
        List<String> tags;

        public Wrapper(Note note) {
            this.note = note;
            List<Tag> tags = Tag.getByNoteLocalId(note.getId());
            this.tags = new ArrayList<>();
            for (Tag tag : tags) {
                this.tags.add(tag.getText());
            }
        }
    }
}
