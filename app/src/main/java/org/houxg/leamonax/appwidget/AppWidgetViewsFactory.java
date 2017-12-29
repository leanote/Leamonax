package org.houxg.leamonax.appwidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.houxg.leamonax.R;
import org.houxg.leamonax.database.NoteDataStore;
import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Note;
import org.houxg.leamonax.ui.NotePreviewActivity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context ctxt=null;
    private int appWidgetId;
    private List<Note> allNotes;

    public AppWidgetViewsFactory(Context ctxt, Intent intent) {
        this.ctxt=ctxt;
        appWidgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                        AppWidgetManager.INVALID_APPWIDGET_ID);

        allNotes = NoteDataStore.getAllNotes(Account.getCurrent().getUserId());
        Collections.sort(allNotes, new Comparator<Note>() {
            @Override
            public int compare(Note o1, Note o2) {
                String temp1=o1.getUpdatedTimeVal()+"";
                String temp2=o2.getUpdatedTimeVal()+"";
                return temp2.compareTo(temp1);
            }
        });
        if(allNotes.size()>30){
            allNotes=allNotes.subList(0,30);
        }
    }
    

    @Override
    public void onCreate() {
        // no-op
    }

    @Override
    public void onDestroy() {
        // no-op
    }

    @Override
    public int getCount() {
        return(allNotes.size());
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row=new RemoteViews(ctxt.getPackageName(),
                                     R.layout.note_app_widget_row);
        row.setTextViewText(R.id.appwidget_row_title, allNotes.get(position).getTitle());
        String textTemp="";
        if(allNotes.get(position).getContent().length()>200){
            textTemp=allNotes.get(position).getContent().substring(0,200)+"...";
        }else {
            textTemp=allNotes.get(position).getContent();

        }
        row.setTextViewText(R.id.appwidget_row_text, textTemp);

        Intent intent=new Intent();

        Bundle extras=new Bundle();
        extras.putLong(NotePreviewActivity.EXT_NOTE_LOCAL_ID, allNotes.get(position).getId());
        intent.putExtras(extras);

        row.setOnClickFillInIntent(R.id.widget_note, intent);
        return(row);
    }

    @Override
    public RemoteViews getLoadingView() {
        return(null);
    }

    @Override
    public int getViewTypeCount() {
        return(1);
    }

    @Override
    public long getItemId(int position) {
        return(position);
    }

    @Override
    public boolean hasStableIds() {
        return(true);
    }

    @Override
    public void onDataSetChanged() {
        // no-op
    }
}