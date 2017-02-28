/*
 * Copyright [2016] [qinglian.zhang]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.zqlite.android.lolly;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zqlite.android.logly.Logly;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author zhangqinglian
 */
public class Lolly extends Service {

    /**
     * 将Lolly窗口显示在设备屏幕上
     */
    public static final int FLAG_INIT_ADD_WINDOW = 1;

    /**
     * 将Lolly窗口从屏幕上移除
     */
    public static final int FLAG_REMOVE_WINDOW = 2;

    /**
     * 停止收集log，并停止service
     */
    public static final int FLAG_COLLECT_LOG = 4;

    public static final String LOLLY_TAGS = "tags";
    public static final String LOLLY_ORIENTATION = "orientation";

    private WindowManager mWM;
    private DisplayMetrics mDisplayMetrics;
    private WindowManager.LayoutParams mWMLayoutPatams;
    private LinearLayout mLolly;
    private ListView mContainer;
    private CheckBox mBack2EndCheckBox;
    private LinearLayout mTerminalBar;
    private Spinner mLogLevelSpinner;
    private Spinner mTagsSpinner;
    private Button mOrientationBtn;
    private Button mShowContainerBtn;

    private Button mClearBtn;


    private boolean mScroll2End = true;
    private boolean mCollect = true;
    private LogAdapter mLogAdapter;
    private List<String> mTags;

    public Lolly() {
        Logly.setGlobalTag(new Logly.Tag(Logly.FLAG_THREAD_NAME, Lolly.class.getSimpleName(), Logly.INFO));
    }

    @Override
    public void onCreate() {
        Logly.i("Lolly create ...");
        initLuffyWindow();
    }

    private void initLuffyWindow() {

        //init the window layout params
        mWM = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mDisplayMetrics = new DisplayMetrics();
        mWM.getDefaultDisplay().getMetrics(mDisplayMetrics);
        mWMLayoutPatams = new WindowManager.LayoutParams();
        mWMLayoutPatams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        mWMLayoutPatams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mWMLayoutPatams.format = PixelFormat.TRANSLUCENT;
        mWMLayoutPatams.gravity = Gravity.START | Gravity.TOP;
        mWMLayoutPatams.x = 0;
        mWMLayoutPatams.y = 0;
        mWMLayoutPatams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWMLayoutPatams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWMLayoutPatams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        //init the log container
        initLolly();

        mBack2EndCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScroll2End = isChecked;
                if (mScroll2End) {
                    mBack2EndCheckBox.setText("O");
                    mContainer.setSelection(mLogAdapter.getCount());
                } else {
                    mBack2EndCheckBox.setText("X");
                }
            }
        });

        mTerminalBar.setOnTouchListener(new View.OnTouchListener() {

            private int originY = mWMLayoutPatams.y;
            private int deltaY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        deltaY = (int) (event.getRawY() - originY);
                        if (Math.abs(deltaY) > 5.0f) {
                            mWMLayoutPatams.y = originY + deltaY - 90;
                            mWM.updateViewLayout(mLolly, mWMLayoutPatams);
                        }
                        break;
                }
                return true;
            }
        });

        mOrientationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWMLayoutPatams.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    mWMLayoutPatams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    mWM.updateViewLayout(mLolly, mWMLayoutPatams);
                    mOrientationBtn.setText("V");
                } else {
                    mWMLayoutPatams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    mWM.updateViewLayout(mLolly, mWMLayoutPatams);
                    mOrientationBtn.setText("H");
                }

            }
        });

        mShowContainerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mContainer.getVisibility() == View.VISIBLE) {
                    mContainer.setVisibility(View.GONE);
                    mShowContainerBtn.setText("+");
                } else {
                    mContainer.setVisibility(View.VISIBLE);
                    mShowContainerBtn.setText("-");
                }
            }
        });

        mClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogAdapter.cleanUp();
            }
        });
        //show logs
        showLog();
        Toast.makeText(this, "init Lolly", Toast.LENGTH_LONG).show();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        switch (intent.getIntExtra("action", -1)) {
            case FLAG_INIT_ADD_WINDOW:

                //get window orientation
                int ori = intent.getIntExtra(LOLLY_ORIENTATION, -233);
                if (ori != -233) {
                    mWMLayoutPatams.screenOrientation = ori;
                }

                //get tags from activity
                String[] tags = intent.getStringArrayExtra(LOLLY_TAGS);
                if (tags != null) {
                    boolean change = false;
                    for (String tag : tags) {
                        if (!mTags.contains(tag)) {
                            mTags.add(tag);
                            change = true;
                        }
                    }
                    if (change) {
                        ArrayAdapter<String> tagAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mTags);
                        mTagsSpinner.setAdapter(tagAdapter);
                    }
                }


                if (mLolly.getVisibility() != View.VISIBLE) {
                    mLolly.setVisibility(View.VISIBLE);
                    mWM.addView(mLolly, mWMLayoutPatams);
                } else {
                    mWM.updateViewLayout(mLolly, mWMLayoutPatams);
                }
                break;

            case FLAG_REMOVE_WINDOW:
                if (mLolly.getVisibility() == View.VISIBLE) {
                    mLolly.setVisibility(View.INVISIBLE);
                    mWM.removeView(mLolly);
                }
                break;

            case FLAG_COLLECT_LOG:
                Toast.makeText(this, "save log", Toast.LENGTH_LONG).show();
                saveLog();
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logly.i("Lolly onDestroy ...");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void showLog() {
        new AsyncTask<Void, String, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mLogAdapter.cleanUp();
                    Runtime.getRuntime().exec("logcat -c");
                    Process process = Runtime.getRuntime().exec("logcat -v time");
                    InputStream is = process.getInputStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader bufferedReader = new BufferedReader(
                            reader);

                    String line = "";
                    while ((line = bufferedReader.readLine()) != null && mCollect) {
                        publishProgress(line);
                    }
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                    if (reader != null) {
                        reader.close();
                    }
                    if (is != null) {
                        is.close();
                    }


                } catch (IOException e) {
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                String line = values[0];
                if (mLogAdapter.addOneLog(line)) {
                    mContainer.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mScroll2End) {
                                mContainer.setSelection(mLogAdapter.getCount());
                            }
                        }
                    });
                }
            }
        }.execute();

    }


    private boolean saveF = false;
    private void saveLog() {

        Logly.i("saveLog");
        if(saveF) return ;
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                saveF = true;
                List<String> logs = mLogAdapter.getAllLog();
                File sdcard = Environment.getExternalStorageDirectory();
                File lolly = new File(sdcard, "/lolly");
                if (!lolly.exists()) {
                    lolly.mkdir();
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd-HH:mm:ss:SSS");
                String time = sdf.format(Calendar.getInstance().getTime());
                File logTxt = new File(lolly, "/lolly-log-" + time + ".txt");
                Logly.i("log Path = " + logTxt.getAbsolutePath());
                try {
                    FileOutputStream fos = new FileOutputStream(logTxt);
                    OutputStreamWriter osw = new OutputStreamWriter(fos);
                    BufferedWriter bw = new BufferedWriter(osw);
                    for (int i = 0;i<logs.size();i++) {
                        bw.write(logs.get(i) + "\n");
                    }
                    bw.close();
                    osw.close();
                    fos.close();

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    saveF = false;
                }
            }
        });
    }

    private void initLolly() {

        //LollyContainer
        mLolly = new LinearLayout(this);
        LinearLayout.LayoutParams LP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getDip(300));
        mLolly.setLayoutParams(LP);
        mLolly.setOrientation(LinearLayout.VERTICAL);
        mLolly.setVisibility(View.INVISIBLE);
        mLolly.setBackgroundColor(Color.BLACK);

        //mTerminalBar
        mTerminalBar = new LinearLayout(this);
        LP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mTerminalBar.setLayoutParams(LP);
        mTerminalBar.setOrientation(LinearLayout.HORIZONTAL);
        mTerminalBar.setBackgroundColor(Color.parseColor("#303F9F"));

        //mBack2EndCheckBox
        ViewGroup.LayoutParams LPC = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mBack2EndCheckBox = new CheckBox(this);
        mBack2EndCheckBox.setLayoutParams(LPC);
        mBack2EndCheckBox.setText("O");
        mBack2EndCheckBox.setTextColor(Color.BLACK);
        mBack2EndCheckBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mBack2EndCheckBox.setChecked(true);

        //mLogLevelSpinner
        mLogLevelSpinner = new Spinner(this);
        LPC = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mLogLevelSpinner.setLayoutParams(LPC);
        List<String> logLevel = new ArrayList<String>();
        logLevel.add("V");
        logLevel.add("D");
        logLevel.add("I");
        logLevel.add("W");
        logLevel.add("E");
        logLevel.add("A");
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, logLevel);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mLogLevelSpinner.setAdapter(spinnerAdapter);
        mLogLevelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String level = spinnerAdapter.getItem(position);
                mLogAdapter.changeLogLevel(level);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mLogAdapter.changeLogLevel(spinnerAdapter.getItem(0));
            }
        });


        //mTagsSpinner
        mTagsSpinner = new Spinner(this);
        LPC = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mTagsSpinner.setLayoutParams(LPC);
        mTags = new ArrayList<String>();
        mTags.add("All");
        final ArrayAdapter<String> tagAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mTags);
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTagsSpinner.setAdapter(tagAdapter);
        mTagsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mLogAdapter.changeTags(tagAdapter.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mLogAdapter.changeTags(tagAdapter.getItem(0));
            }
        });


        //mOrientationBtn
        mOrientationBtn = new Button(this);
        LPC = new ViewGroup.LayoutParams(getDip(40), ViewGroup.LayoutParams.WRAP_CONTENT);
        mOrientationBtn.setLayoutParams(LPC);
        mOrientationBtn.setText("H");

        //mShowContainerBtn
        mShowContainerBtn = new Button(this);
        LPC = new ViewGroup.LayoutParams(getDip(40), ViewGroup.LayoutParams.WRAP_CONTENT);
        mShowContainerBtn.setLayoutParams(LPC);
        mShowContainerBtn.setText("-");

        //mClearBtn
        mClearBtn = new Button(this);
        LPC = new ViewGroup.LayoutParams(getDip(40), ViewGroup.LayoutParams.WRAP_CONTENT);
        mClearBtn.setLayoutParams(LPC);
        mClearBtn.setText("c");

        //mContainer
        mContainer = new ListView(this);
        AbsListView.LayoutParams ALP = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, getDip(300));
        mContainer.setLayoutParams(ALP);
        mLogAdapter = new LogAdapter(this);
        mContainer.setDivider(null);

        mContainer.setAdapter(mLogAdapter);


        mTerminalBar.addView(mBack2EndCheckBox);
        mTerminalBar.addView(mLogLevelSpinner);
        mTerminalBar.addView(mTagsSpinner);
        mTerminalBar.addView(mOrientationBtn);
        mTerminalBar.addView(mShowContainerBtn);
        mTerminalBar.addView(mClearBtn);
        mLolly.addView(mTerminalBar);
        mLolly.addView(mContainer);
    }

    private int getDip(int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources()
                .getDisplayMetrics());
    }


    public static class LogAdapter extends BaseAdapter {

        private Context mContext;

        private List<String> mCurrentLogs;
        private List<String> mAllLogs;
        private List<String> mDebugLogs;
        private List<String> mInfoLogs;
        private List<String> mWarnLogs;
        private List<String> mErrorLogs;
        private List<String> mAssertLogs;

        private List<String> mSystemErrorCache;
        private List<String> mTagsLogs;

        private final String space = "            ";
        private Object lock = new Object();
        private String mCurrentLevel = "V";
        private String mCurrentTag = "All";

        ForegroundColorSpan redSpan = new ForegroundColorSpan(Color.RED);
        ForegroundColorSpan whiteSpan = new ForegroundColorSpan(Color.WHITE);
        ForegroundColorSpan blueSpan = new ForegroundColorSpan(Color.BLUE);
        ForegroundColorSpan greenSpan = new ForegroundColorSpan(Color.GREEN);
        ForegroundColorSpan yellowSpan = new ForegroundColorSpan(Color.YELLOW);

        private SpannableStringBuilder spanLine(String line) {
            SpannableStringBuilder builder = new SpannableStringBuilder(line);

            if (line.contains("D/")) {
                builder.setSpan(whiteSpan, 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
            if (line.contains("System.err")) {
                builder.setSpan(redSpan, 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
            if (line.contains("E/")) {
                builder.setSpan(redSpan, 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
            if (line.contains("W/")) {
                builder.setSpan(yellowSpan, 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
            if (line.contains("A/")) {
                builder.setSpan(greenSpan, 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
            if (line.contains("I/")) {
                builder.setSpan(blueSpan, 0, line.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
            return builder;
        }

        public LogAdapter(Context context) {

            mContext = context;
            mAllLogs = new ArrayList<String>(1000);
            mDebugLogs = new ArrayList<String>();
            mInfoLogs = new ArrayList<String>();
            mWarnLogs = new ArrayList<String>();
            mAssertLogs = new ArrayList<String>();
            mErrorLogs = new ArrayList<String>();

            mSystemErrorCache = new ArrayList<String>();
            mTagsLogs = new ArrayList<String>();
            mCurrentLogs = mDebugLogs;
        }

        /**
         * @param line
         * @return true:更新UI，将listView滑动到最底层 false:不做处理
         */
        public boolean addOneLog(String line) {

            //对异常日志做特殊处理
            //如果是日常日志则加入到mSystemError，不进行屏幕输出
            if (line.contains("System.err")) {
                if (mSystemErrorCache.size() == 0) {
                    mSystemErrorCache.add(line);
                } else {
                    if (line.contains("at")) {
                        int indexAT = line.indexOf("at");
                        mSystemErrorCache.add(line.substring(indexAT));
                    } else {
                        mSystemErrorCache.add(line);
                    }
                }
                return false;
            }

            //对正常日志做处理
            if (mAllLogs != null) {
                //若mSystemError内容不为空，则将其内容打印至屏幕
                if (mSystemErrorCache.size() != 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mSystemErrorCache.size(); i++) {
                        if (i != 0) {
                            sb.append(space).append(mSystemErrorCache.get(i)).append("\n");
                        } else {
                            sb.append(mSystemErrorCache.get(i)).append("\n");
                        }
                    }
                    assignLog(sb.toString());
                    //清空mSystemError
                    mSystemErrorCache.clear();
                }
                assignLog(line);
                return true;
            }

            return false;
        }

        private void assignLog(String line) {
            mAllLogs.add(line);
            if (line.contains("D/")) {
                mDebugLogs.add(line);
            }
            if (line.contains("I/")) {
                mInfoLogs.add(line);
            }
            if (line.contains("W/")) {
                mWarnLogs.add(line);
            }
            if (line.contains("E/")) {
                mErrorLogs.add(line);
            }
            if (line.contains("A/")) {
                mAssertLogs.add(line);
            }

            if (line.contains("System.err")) {
                mErrorLogs.add(line);
            }

            if (line.contains(getFullTag()) && !mCurrentTag.equals("All")) {
                mTagsLogs.add(line);
            }
            notifyDataSetChanged();
        }

        public void changeLogLevel(String level) {

            synchronized (lock) {
                mCurrentLevel = level;
                if ("V".equals(level)) {
                    mCurrentLogs = mAllLogs;
                }

                if ("D".equals(level)) {
                    mCurrentLogs = mDebugLogs;
                }

                if ("I".equals(level)) {
                    mCurrentLogs = mInfoLogs;
                }
                if ("W".equals(level)) {
                    mCurrentLogs = mWarnLogs;
                }
                if ("E".equals(level)) {
                    mCurrentLogs = mErrorLogs;
                }
                if ("A".equals(level)) {
                    mCurrentLogs = mAssertLogs;
                }
                notifyDataSetChanged();
            }
            if (!mCurrentTag.equals("All")) {
                changeTags(mCurrentTag);
            }

        }

        public void changeTags(String tag) {
            mCurrentTag = tag;
            if ("All".equals(mCurrentTag)) {
                changeLogLevel(mCurrentLevel);
                mTagsLogs.clear();
                return;
            }
            synchronized (lock) {
                String fullTag = getFullTag();
                mCurrentLogs = getLogsByLevel(mCurrentLevel);
                mTagsLogs.clear();
                for (String line : mCurrentLogs) {
                    if (line.contains(fullTag)) {
                        mTagsLogs.add(line);
                    }
                }
                mCurrentLogs = mTagsLogs;
                notifyDataSetChanged();
            }
        }

        private String getFullTag() {
            String fullTag;
            if ("V".equals(mCurrentLevel)) {
                fullTag = "/" + mCurrentTag;
            } else {
                fullTag = mCurrentLevel + "/" + mCurrentTag;
            }
            return fullTag;
        }

        private List<String> getLogsByLevel(String level) {
            switch (level) {
                case "V":
                    return mAllLogs;
                case "D":
                    return mDebugLogs;
                case "I":
                    return mInfoLogs;
                case "W":
                    return mWarnLogs;
                case "E":
                    return mErrorLogs;
                case "A":
                    return mAssertLogs;
                default:
                    return mAllLogs;
            }
        }

        public List<String> getAllLog() {
            return mAllLogs;
        }

        @Override
        public int getCount() {
            synchronized (lock) {
                if (mCurrentLogs != null) {
                    return mCurrentLogs.size();
                }
            }
            return 0;
        }

        public void cleanUp() {
            mCurrentLogs.clear();
            mAllLogs.clear();
            ;
            mDebugLogs.clear();
            mInfoLogs.clear();
            mWarnLogs.clear();
            mErrorLogs.clear();
            mAssertLogs.clear();
            mSystemErrorCache.clear();
            ;
            mTagsLogs.clear();
            notifyDataSetChanged();
        }

        @Override
        public Object getItem(int position) {
            return mCurrentLogs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LogHolder holder;
            if (convertView == null) {
                holder = new LogHolder();
                TextView textView = new TextView(mContext);
                AbsListView.LayoutParams LP = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT);
                textView.setLayoutParams(LP);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                textView.setTextColor(Color.WHITE);
                convertView = textView;
                holder.logText = textView;
                convertView.setTag(holder);
            }

            holder = (LogHolder) convertView.getTag();
            holder.logText.setText(spanLine(mCurrentLogs.get(position)));
            return convertView;
        }

        class LogHolder {
            TextView logText;
        }
    }

    /**
     * 显示Lolly
     *
     * @param activity
     * @param tags     自定义tag
     */
    public static void showLolly(Activity activity, String[] tags) {
        try {
            ActivityInfo info = activity.getPackageManager().getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA);
            Intent start = new Intent(activity, Lolly.class);
            start.putExtra("action", FLAG_INIT_ADD_WINDOW);
            start.putExtra(LOLLY_TAGS, tags);
            start.putExtra(LOLLY_ORIENTATION, info.screenOrientation);
            activity.startService(start);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static void showLolly(Service service, String[] tags) {
        Intent start = new Intent(service, Lolly.class);
        start.putExtra("action", FLAG_INIT_ADD_WINDOW);
        start.putExtra(LOLLY_TAGS, tags);
        service.startService(start);
    }

    /**
     * 移除Lolly窗口
     *
     * @param context
     */
    public static void hideLolly(Context context) {
        Intent stop = new Intent(context, Lolly.class);
        stop.putExtra("action", Lolly.FLAG_REMOVE_WINDOW);
        stop.addFlags(Lolly.FLAG_REMOVE_WINDOW);
        context.startService(stop);
    }

    /**
     * 保存当前log
     *
     * @param context
     */
    public static void saveLog(Context context) {
        Intent saveLog = new Intent(context, Lolly.class);
        saveLog.putExtra("action", FLAG_COLLECT_LOG);
        context.startService(saveLog);

    }
}
