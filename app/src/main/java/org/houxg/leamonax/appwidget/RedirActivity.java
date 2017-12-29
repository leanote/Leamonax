package org.houxg.leamonax.appwidget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.houxg.leamonax.ui.NotePreviewActivity;

public class RedirActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Long word=getIntent().getLongExtra(NotePreviewActivity.EXT_NOTE_LOCAL_ID,-1);

        Intent intent=new Intent(RedirActivity.this,NotePreviewActivity.class);
        Bundle bundle=new Bundle();

        if(word==-1){
            Toast.makeText(this, "笔记编号错误", Toast.LENGTH_LONG).show();
        }else {
            bundle.putLong(NotePreviewActivity.EXT_NOTE_LOCAL_ID,word);
            intent.putExtras(bundle);
            startActivity(intent);
        }

        finish();

    }
}
