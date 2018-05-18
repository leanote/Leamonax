package org.houxg.leamonax.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.houxg.leamonax.Leamonax;
import org.houxg.leamonax.R;
import org.houxg.leamonax.adapter.NoteAdapter;
import org.houxg.leamonax.database.NoteDataStore;
import org.houxg.leamonax.database.NotebookDataStore;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.model.Notebook;
import org.houxg.leamonax.service.NoteService;
import org.houxg.leamonax.utils.ActionModeHandler;
import org.houxg.leamonax.utils.CollectionUtils;
import org.houxg.leamonax.utils.NetworkUtils;
import org.houxg.leamonax.utils.SharedPreferenceUtils;
import org.houxg.leamonax.utils.ToastUtils;
import org.houxg.leamonax.widget.NoteList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static org.houxg.leamonax.R.menu.note;

public class NoteFragment extends Fragment implements NoteAdapter.NoteAdapterListener, ActionModeHandler.Callback<Note> {

    private static final String EXT_SCROLL_POSITION = "ext_scroll_position";
    private static final String SP_VIEW_TYPE = "sp_viewType";

    @BindView(R.id.recycler_view)
    RecyclerView mNoteListView;

    List<Note> mNotes;
    ActionModeHandler<Note> mActionModeHandler;
    NoteList mNoteList;
    Mode mCurrentMode;
    OnSearchFinishListener mOnSearchFinishListener;

    public void setOnSearchFinishListener(OnSearchFinishListener onSearchFinishListener) {
        this.mOnSearchFinishListener = onSearchFinishListener;
    }

    public NoteFragment() {
    }

    public static NoteFragment newInstance() {
        return new NoteFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(note, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_view_type) {
            mNoteList.toggleType();
            SharedPreferenceUtils.write(SharedPreferenceUtils.CONFIG, SP_VIEW_TYPE, mNoteList.getType());
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_note, container, false);
        ButterKnife.bind(this, view);
        mNoteList = new NoteList(container.getContext(), view, this);
        mNoteList.setType(SharedPreferenceUtils.read(SharedPreferenceUtils.CONFIG, SP_VIEW_TYPE, NoteList.DEFAULT_TYPE));
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mNoteList.setScrollPosition(savedInstanceState.getInt(EXT_SCROLL_POSITION, 0));
        }
        mActionModeHandler = new ActionModeHandler<>(getActivity(), this, R.menu.delete);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXT_SCROLL_POSITION, mNoteList.getScrollPosition());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void setMode(Mode mode) {
        mCurrentMode = mode;
        List<Note> notes;
        mNoteList.setHighlight("");
        switch (mode) {
            case RECENT_NOTES:
                notes = NoteDataStore.getAllNotes(Account.getCurrent().getUserId());
                break;
            case NOTEBOOK:
                notes = NoteDataStore.getNotesFromNotebook(Account.getCurrent().getUserId(), mode.notebookId);
                break;
            case TAG:
                notes = NoteDataStore.getByTagText(mode.tagText, Account.getCurrent().getUserId());
                break;
            case SEARCH:
                notes = NoteDataStore.searchByTitle(mode.keywords);
                mNoteList.setHighlight(mode.keywords);
                break;
            default:
                notes = new ArrayList<>();
        }
        mNotes = notes;
        Collections.sort(mNotes, new Note.UpdateTimeComparetor());
        mNoteList.render(mNotes);
        if (mNotes.size() == 0 && mOnSearchFinishListener != null) {
            mOnSearchFinishListener.doSearchFinish();
        }
    }

    public boolean canGoBack() {
        if (mCurrentMode == Mode.NOTEBOOK) {
            Notebook notebook = NotebookDataStore.getByLocalId(mCurrentMode.notebookId);
            if (!TextUtils.isEmpty(notebook.getParentNotebookId())) {
                return true;
            }
            return false;
        }
        return false;
    }

    public void goBack() {
        if (mCurrentMode == Mode.NOTEBOOK) {
            Notebook notebook = NotebookDataStore.getByLocalId(mCurrentMode.notebookId);
            long localId = NotebookDataStore.getByServerId(notebook.getParentNotebookId()).getId();
            Mode mode = Mode.NOTEBOOK;
            mode.setNotebookId(localId);
            setMode(mode);
        }
    }



    @Override
    public void onClickNote(Note note) {
        if (mActionModeHandler.isActionMode()) {
            boolean isSelected = mActionModeHandler.chooseItem(note);
            mNoteList.setSelected(note, isSelected);
        } else {
            startActivity(NotePreviewActivity.getOpenIntent(getActivity(), note.getId()));
        }
    }

    @Override
    public void onLongClickNote(final Note note) {
        boolean isSelected = mActionModeHandler.chooseItem(note);
        mNoteList.setSelected(note, isSelected);
    }

    private void deleteNote(List<Note> notes) {
        Observable.from(notes)
                .flatMap(new Func1<Note, rx.Observable<Note>>() {
                    @Override
                    public rx.Observable<Note> call(final Note note) {
                        return Observable.create(new Observable.OnSubscribe<Note>() {
                            @Override
                            public void call(Subscriber<? super Note> subscriber) {
                                if (!subscriber.isUnsubscribed()) {
                                    NoteService.trashNotesOnLocal(note);
                                    subscriber.onNext(note);
                                    subscriber.onCompleted();
                                }
                            }
                        });
                    }
                })
                .buffer(notes.size())
                .flatMap(new Func1<List<Note>, Observable<Note>>() {
                    @Override
                    public Observable<Note> call(List<Note> notes) {
                        NetworkUtils.checkNetwork();
                        return Observable.from(notes);
                    }
                })
                .flatMap(new Func1<Note, Observable<Note>>() {
                    @Override
                    public Observable<Note> call(final Note note) {
                        return Observable.create(new Observable.OnSubscribe<Note>() {
                            @Override
                            public void call(Subscriber<? super Note> subscriber) {
                                if (!subscriber.isUnsubscribed()) {
                                    NoteService.saveNote(note.getId());
                                    subscriber.onNext(note);
                                    subscriber.onCompleted();
                                }
                            }
                        });
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Note>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof NetworkUtils.NetworkUnavailableException) {
                            ToastUtils.show(Leamonax.getContext(), R.string.delete_network_error);
                        } else {
                            ToastUtils.show(Leamonax.getContext(), R.string.delete_note_failed);
                        }
                        refresh();
                    }

                    @Override
                    public void onNext(Note note) {
                        mNoteList.remove(note);
                    }
                });
    }

    private void refresh() {
        setMode(mCurrentMode);
    }

    @Override
    public boolean onAction(int actionId, List<Note> pendingItems) {
        if (CollectionUtils.isEmpty(pendingItems)) {
            ToastUtils.show(getActivity(), R.string.no_note_was_selected);
            return false;
        }
        final List<Note> waitToDelete = new ArrayList<>();
        for (int i = 0; i < pendingItems.size(); i++) {
            waitToDelete.add(pendingItems.get(i));
        }
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_note)
                .setMessage(R.string.are_you_sure_to_delete_note)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mActionModeHandler.getPendingItems().clear();
                        mActionModeHandler.dismiss();
                        deleteNote(waitToDelete);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
        return true;
    }

    @Override
    public void onDestroy(List<Note> pendingItems) {
        if (CollectionUtils.isNotEmpty(pendingItems)) {
            mNoteList.invalidateAllSelected();
        }
    }

    public enum Mode {
        RECENT_NOTES,
        NOTEBOOK,
        TAG,
        SEARCH;

        long notebookId;
        String tagText;
        String keywords;

        public void setNotebookId(long notebookId) {
            this.notebookId = notebookId;
        }

        public void setTagText(String tagText) {
            this.tagText = tagText;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }

        @Override
        public String toString() {
            return name() + "{" +
                    "notebookId=" + notebookId +
                    ", tagText='" + tagText + '\'' +
                    '}';
        }
    }

    public interface OnSearchFinishListener {
        void doSearchFinish();
    }

}
