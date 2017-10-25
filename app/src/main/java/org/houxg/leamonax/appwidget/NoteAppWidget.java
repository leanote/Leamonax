package org.houxg.leamonax.appwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import org.houxg.leamonax.R;
import org.houxg.leamonax.ui.NoteFragment;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link NoteAppWidgetConfigureActivity NoteAppWidgetConfigureActivity}
 */
public class NoteAppWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        //获取从设置的activity 里保存的信息
        CharSequence widgetText = NoteAppWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        // 构建小部件的ui
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.note_app_widget);
//        views.setTextViewText(R.id.appwidget_text, widgetText);
        NoteFragment mNoteFragment;

        // 设置界面

        // 指示小部件管理器更新小部件
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            NoteAppWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
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

