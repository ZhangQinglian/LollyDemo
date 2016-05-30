package com.zqlite.android.lollydemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.zqlite.android.lolly.Lolly;
import com.zqlite.android.lollydemo.artphelper.ARTPHelper;

public class MainActivity extends AppCompatActivity {

    private Button mShowWindowBtn ;
    private Button mHideWindowBtn;
    private Button mCollectLog;
    private Button mSaveLog;
    final ARTPHelper artpHelper = new ARTPHelper(true);


    public static final int OVERLAY_PERMISSION_REQ_CODE  = 370 ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        artpHelper.writeExternalStorage();
        artpHelper.requestPermissions(this);

        mShowWindowBtn = (Button) findViewById(R.id.show_window);
        mShowWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        Lolly.try2StartLolly(MainActivity.this, new String[]{"scott"});
                    }else{
                        artpHelper.tryToDropZone(MainActivity.this);
                    }
                }else{
                    Lolly.try2StartLolly(MainActivity.this, new String[]{"scott"});
                }

            }
        });

        mHideWindowBtn = (Button) findViewById(R.id.hide_window);
        mHideWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lolly.stopLolly(MainActivity.this);
            }
        });

        mCollectLog = (Button) findViewById(R.id.collect_log);
        mCollectLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lolly.justStartCollectLog(MainActivity.this);
            }
        });

        mSaveLog = (Button) findViewById(R.id.save_log);
        mSaveLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lolly.stopLollyAndSaveLog(MainActivity.this);
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        artpHelper.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }
}
