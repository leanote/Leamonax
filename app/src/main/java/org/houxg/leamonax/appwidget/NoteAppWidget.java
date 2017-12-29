package org.houxg.leamonax.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import org.houxg.leamonax.R;

public class NoteAppWidget extends AppWidgetProvider {

    private int widgetIds[];

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.i("debug!!!!!!!!!!!!!!!!","接收到广播");
//        abortBroadcast();
//        for (int i=0;i<widgetIds.length;i++){
//            Intent svcIntent=new Intent(context, WidgetService.class);
//            RemoteViews widget=new RemoteViews(context.getPackageName(),
//                    R.layout.note_app_widget);
//
//            widget.setRemoteAdapter(widgetIds[i], R.id.words,
//                    svcIntent);
//
//            Intent clickIntent=new Intent(context, RedirActivity.class);
//            PendingIntent clickPI=PendingIntent
//                    .getActivity(context, 0,
//                            clickIntent,
//                            PendingIntent.FLAG_UPDATE_CURRENT);
//
//            widget.setPendingIntentTemplate(R.id.words, clickPI);
//
//            AppWidgetManager.getInstance(context).updateAppWidget(widgetIds[i], widget);
//
//        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        widgetIds=appWidgetIds;
        for (int i=0; i<appWidgetIds.length; i++) {
            Intent svcIntent=new Intent(context, WidgetService.class);

            svcIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));

            RemoteViews widget=new RemoteViews(context.getPackageName(),
                    R.layout.note_app_widget);

            widget.setRemoteAdapter(appWidgetIds[i], R.id.words,
                    svcIntent);

            Intent clickIntent=new Intent(context, RedirActivity.class);
            PendingIntent clickPI=PendingIntent
                    .getActivity(context, 0,
                            clickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);

            widget.setPendingIntentTemplate(R.id.words, clickPI);

//            Intent i=new Intent(ctxt,NotePreviewActivity.class);
//            i.putExtra(NotePreviewActivity.EXT_NOTE_LOCAL_ID, allNotes.get(position).getId());
//            row.setOnClickFillInIntent(R.id.widget_note, i);

            appWidgetManager.updateAppWidget(appWidgetIds[i], widget);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.

    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

