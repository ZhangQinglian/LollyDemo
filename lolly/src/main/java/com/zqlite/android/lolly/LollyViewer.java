package com.zqlite.android.lolly;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 *  @author zhangqinglian
 */
public class LollyViewer extends Activity {

    private LinearLayout mLollyViewContainer ;
    private ListView mListView ;
    private EditText mEditText ;
    private Lolly.LogAdapter mAdapter ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(initLayout());
        final Intent intent = getIntent();
        if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(Intent.EXTRA_TITLE));
        }
        loadData(intent.getData());
    }

    private LinearLayout initLayout(){

        LinearLayout.LayoutParams LLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT);
        AbsListView.LayoutParams ALP = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT);
        ViewGroup.LayoutParams VLP = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,getDip(50));
        mLollyViewContainer = new LinearLayout(this);
        mLollyViewContainer.setLayoutParams(LLP);
        mLollyViewContainer.setBackgroundColor(Color.BLACK);
        mLollyViewContainer.setOrientation(LinearLayout.VERTICAL);

        mEditText = new EditText(this);
        mEditText.setTextColor(Color.BLACK);
        mEditText.setBackgroundColor(Color.WHITE);
        mEditText.setLayoutParams(VLP);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                mAdapter.changeTags(text);
            }
        });
        mLollyViewContainer.addView(mEditText);

        mListView = new ListView(this);
        mListView.setLayoutParams(ALP);
        mLollyViewContainer.addView(mListView);
        return  mLollyViewContainer;
    }

    private void loadData(Uri uri){

        mAdapter = new Lolly.LogAdapter(this);
        mListView.setAdapter(mAdapter);
        File file = new File(uri.getPath());
        String fileName = file.getName();
        setTitle(fileName);
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis);
            br= new BufferedReader(isr);
            String line ;
            while((line = br.readLine()) != null){
                mAdapter.addOneLog(line);
            }
            mAdapter.changeTags("");

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(br != null){
                    br.close();
                }
                if(isr != null){
                    isr.close();
                }
                if(fis != null){
                    fis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private int getDip(int dip){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources()
                .getDisplayMetrics());
    }
}
