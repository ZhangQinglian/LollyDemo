package com.zqlite.android.lollydemo;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.zqlite.android.logly.Logly;
import com.zqlite.android.lolly.Lolly;
import com.zqlite.android.lollydemo.artphelper.ARTPHelper;
import com.zqlite.android.onepiece.OnePiece;
import com.zqlite.android.onepiece.ViewID;

public class MainActivity extends AppCompatActivity {

    @ViewID(R.id.show_window)
    private Button mShowWindowBtn ;
    @ViewID(R.id.hide_window)
    private Button mHideWindowBtn;
    @ViewID(R.id.save_log)
    private Button mSaveLog;
    final ARTPHelper artpHelper = new ARTPHelper(true);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        OnePiece.own().initViews(this);

        artpHelper.writeExternalStorage();
        artpHelper.requestPermissions(this);

        mShowWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        Lolly.Companion.showLolly(MainActivity.this, new String[]{"scott"});
                    }else{
                        artpHelper.tryToDropZone(MainActivity.this);
                    }
                }else{
                    Lolly.Companion.showLolly(MainActivity.this, new String[]{"scott"});
                }

            }
        });

        mHideWindowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lolly.Companion.hideLolly(MainActivity.this);
            }
        });

        mSaveLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Lolly.Companion.saveLog(MainActivity.this);
            }
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Logly.i("this is Lolly");
                Logly.d("this is Lolly");
                Logly.v("this is Lolly");
                Logly.e(new Logly.Tag(Logly.FLAG_THREAD_NAME,"scott",Logly.ERROR),"this is Lolly");
                Logly.w("this is Lolly");
                handler.postDelayed(this,2000);
            }
        },2000);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        artpHelper.onRequestPermissionsResult(requestCode,permissions,grantResults);
    }

}
