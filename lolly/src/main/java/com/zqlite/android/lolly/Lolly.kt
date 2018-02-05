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

package com.zqlite.android.lolly

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.AsyncTask
import android.os.Environment
import android.os.IBinder
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import com.zqlite.android.logly.Logly

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.concurrent.Executors

/**
 * @author zhangqinglian
 */
class Lolly : Service() {

    private var mWM: WindowManager? = null
    private var mDisplayMetrics: DisplayMetrics? = null
    private var mWMLayoutPatams: WindowManager.LayoutParams? = null
    private var mLolly: LinearLayout? = null
    private var mContainer: RecyclerView? = null
    private var mBack2EndCheckBox: CheckBox? = null
    private var mTerminalBar: TouchAbleView? = null
    private var mLogLevelSpinner: Spinner? = null
    private var mTagsSpinner: Spinner? = null
    private var mOrientationBtn: Button? = null
    private var mShowContainerBtn: Button? = null

    private var mClearBtn: Button? = null


    private var mScroll2End = true
    private val mCollect = true
    private var mLogAdapter: RVLogAdapter? = null
    private var mTags: MutableList<String>? = null


    private var saveF = false

    init {
        Logly.setGlobalTag(Logly.Tag(Logly.FLAG_THREAD_NAME, Lolly::class.java!!.getSimpleName(), Logly.INFO))
    }

    override fun onCreate() {
        Logly.i("Lolly create ...")
        initLollyWindow()
    }

    private fun initLollyWindow() {

        //init the window layout params
        mWM = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplayMetrics = DisplayMetrics()
        mWM!!.defaultDisplay.getMetrics(mDisplayMetrics)
        mWMLayoutPatams = WindowManager.LayoutParams()
        mWMLayoutPatams!!.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mWMLayoutPatams!!.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        mWMLayoutPatams!!.format = PixelFormat.TRANSLUCENT
        mWMLayoutPatams!!.gravity = Gravity.START or Gravity.TOP
        mWMLayoutPatams!!.x = 0
        mWMLayoutPatams!!.y = 0
        mWMLayoutPatams!!.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mWMLayoutPatams!!.width = WindowManager.LayoutParams.MATCH_PARENT
        mWMLayoutPatams!!.height = WindowManager.LayoutParams.WRAP_CONTENT

        //init the log container
        initLolly()

        mBack2EndCheckBox!!.setOnCheckedChangeListener { buttonView, isChecked ->
            mScroll2End = isChecked
            if (mScroll2End) {
                mBack2EndCheckBox!!.text = "O"
                mContainer?.scrollToPosition(mLogAdapter!!.itemCount)
            } else {
                mBack2EndCheckBox!!.text = "X"
            }
        }

        mTerminalBar!!.setOnTouchListener(object : View.OnTouchListener {

            private val originY = mWMLayoutPatams!!.y
            private var deltaY: Int = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                    }
                    MotionEvent.ACTION_MOVE -> {
                        deltaY = (event.rawY - originY).toInt()
                        if (Math.abs(deltaY) > 5.0f) {
                            mWMLayoutPatams!!.y = originY + deltaY - 90
                            mWM!!.updateViewLayout(mLolly, mWMLayoutPatams)
                        }
                    }
                }
                return true
            }
        })

        mOrientationBtn!!.setOnClickListener {
            if (mWMLayoutPatams!!.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mWMLayoutPatams!!.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                mWM!!.updateViewLayout(mLolly, mWMLayoutPatams)
                mOrientationBtn!!.text = "V"
            } else {
                mWMLayoutPatams!!.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                mWM!!.updateViewLayout(mLolly, mWMLayoutPatams)
                mOrientationBtn!!.text = "H"
            }
        }

        mShowContainerBtn!!.setOnClickListener {
            if (mContainer!!.visibility == View.VISIBLE) {
                mContainer!!.visibility = View.GONE
                mShowContainerBtn!!.text = "+"
            } else {
                mContainer!!.visibility = View.VISIBLE
                mShowContainerBtn!!.text = "-"
            }
        }

        mClearBtn!!.setOnClickListener { mLogAdapter!!.cleanUp() }
        //show logs
        showLog()
        Toast.makeText(this, "init Lolly", Toast.LENGTH_LONG).show()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.getIntExtra("action", -1)) {
            FLAG_INIT_ADD_WINDOW -> {

                //get window orientation
                val ori = intent.getIntExtra(LOLLY_ORIENTATION, -233)
                if (ori != -233) {
                    mWMLayoutPatams!!.screenOrientation = ori
                }

                //get tags from activity
                val tags = intent.getStringArrayExtra(LOLLY_TAGS)
                if (tags != null) {
                    var change = false
                    for (tag in tags) {
                        if (!mTags!!.contains(tag)) {
                            mTags!!.add(tag)
                            change = true
                        }
                    }
                    if (change) {
                        val tagAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mTags!!)
                        mTagsSpinner!!.adapter = tagAdapter
                    }
                }


                if (mLolly!!.visibility != View.VISIBLE) {
                    mLolly!!.visibility = View.VISIBLE
                    mWM!!.addView(mLolly, mWMLayoutPatams)
                } else {
                    mWM!!.updateViewLayout(mLolly, mWMLayoutPatams)
                }
            }

            FLAG_REMOVE_WINDOW -> if (mLolly!!.visibility == View.VISIBLE) {
                mLolly!!.visibility = View.INVISIBLE
                mWM!!.removeView(mLolly)
            }

            FLAG_COLLECT_LOG -> {
                Toast.makeText(this, "save log", Toast.LENGTH_LONG).show()
                saveLog()
            }
        }

        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Logly.i("Lolly onDestroy ...")
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    private fun showLog() {
        object : AsyncTask<Void, String, Void>() {

            override fun doInBackground(vararg params: Void): Void? {
                try {
                    mLogAdapter!!.cleanUp()
                    Runtime.getRuntime().exec("logcat -c")
                    val process = Runtime.getRuntime().exec("logcat -v time")
                    val `is` = process.inputStream
                    val reader = InputStreamReader(`is`!!)
                    val bufferedReader = BufferedReader(
                            reader)

                    bufferedReader.forEachLine {
                        if(mCollect){
                            publishProgress(it)
                        }
                    }
                    bufferedReader?.close()
                    reader?.close()
                    `is`?.close()


                } catch (e: IOException) {
                }

                return null
            }

            override fun onProgressUpdate(vararg values: String) {
                val line = values[0]
                if (mLogAdapter!!.addOneLog(line)) {
                    mContainer!!.post {
                        if (mScroll2End) {
                            mContainer!!.scrollToPosition(mLogAdapter!!.itemCount)
                        }
                    }
                }
            }
        }.execute()

    }

    private fun saveLog() {

        Logly.i("saveLog")
        if (saveF) return
        Executors.newSingleThreadExecutor().execute {
            saveF = true
            val logs = mLogAdapter!!.allLog
            val sdcard = Environment.getExternalStorageDirectory()
            val lolly = File(sdcard, "/lolly")
            if (!lolly.exists()) {
                lolly.mkdir()
            }
            val sdf = SimpleDateFormat("yy-MM-dd-HH:mm:ss:SSS")
            val time = sdf.format(Calendar.getInstance().time)
            val logTxt = File(lolly, "/lolly-log-$time.txt")
            Logly.i("log Path = " + logTxt.absolutePath)
            try {
                val fos = FileOutputStream(logTxt)
                val osw = OutputStreamWriter(fos)
                val bw = BufferedWriter(osw)
                for (i in logs!!.indices) {
                    bw.write(logs[i] + "\n")
                }
                bw.close()
                osw.close()
                fos.close()

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                saveF = false
            }
        }
    }

    private fun initLolly() {

        //LollyContainer
        mLolly = LinearLayout(this)
        var lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getDip(300))
        mLolly!!.layoutParams = lp
        mLolly!!.orientation = LinearLayout.VERTICAL
        mLolly!!.visibility = View.INVISIBLE
        mLolly!!.setBackgroundColor(Color.BLACK)

        //mTerminalBar
        mTerminalBar = LinearLayout(this)
        lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        mTerminalBar!!.layoutParams = lp
        mTerminalBar!!.orientation = LinearLayout.HORIZONTAL
        mTerminalBar!!.setBackgroundColor(Color.parseColor("#303F9F"))

        //mBack2EndCheckBox
        var lpc = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mBack2EndCheckBox = CheckBox(this)
        mBack2EndCheckBox!!.layoutParams = lpc
        mBack2EndCheckBox!!.text = "O"
        mBack2EndCheckBox!!.setTextColor(Color.BLACK)
        mBack2EndCheckBox!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        mBack2EndCheckBox!!.isChecked = true

        //mLogLevelSpinner
        mLogLevelSpinner = Spinner(this)
        lpc = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mLogLevelSpinner!!.layoutParams = lpc
        val logLevel = ArrayList<String>()
        logLevel.add("V")
        logLevel.add("D")
        logLevel.add("I")
        logLevel.add("W")
        logLevel.add("E")
        logLevel.add("A")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, logLevel)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mLogLevelSpinner!!.adapter = spinnerAdapter
        mLogLevelSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val level = spinnerAdapter.getItem(position)
                mLogAdapter!!.changeLogLevel(level)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                mLogAdapter!!.changeLogLevel(spinnerAdapter.getItem(0))
            }
        }


        //mTagsSpinner
        mTagsSpinner = Spinner(this)
        lpc = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mTagsSpinner!!.layoutParams = lpc
        mTags = ArrayList()
        mTags!!.add("All")
        val tagAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, mTags!!)
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mTagsSpinner!!.adapter = tagAdapter
        mTagsSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                mLogAdapter!!.changeTags(tagAdapter.getItem(position))
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                mLogAdapter!!.changeTags(tagAdapter.getItem(0))
            }
        }


        //mOrientationBtn
        mOrientationBtn = Button(this)
        lpc = ViewGroup.LayoutParams(getDip(40), ViewGroup.LayoutParams.WRAP_CONTENT)
        mOrientationBtn!!.layoutParams = lpc
        mOrientationBtn!!.text = "H"

        //mShowContainerBtn
        mShowContainerBtn = Button(this)
        lpc = ViewGroup.LayoutParams(getDip(40), ViewGroup.LayoutParams.WRAP_CONTENT)
        mShowContainerBtn!!.layoutParams = lpc
        mShowContainerBtn!!.text = "-"

        //mClearBtn
        mClearBtn = Button(this)
        lpc = ViewGroup.LayoutParams(getDip(40), ViewGroup.LayoutParams.WRAP_CONTENT)
        mClearBtn!!.layoutParams = lpc
        mClearBtn!!.text = "c"

        //mContainer
        mContainer = RecyclerView(this)
        mContainer?.layoutManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        mLogAdapter = RVLogAdapter()
        mContainer?.adapter = mLogAdapter

        val ALP = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, getDip(300))
        mContainer!!.layoutParams = ALP

        mTerminalBar!!.addView(mBack2EndCheckBox)
        mTerminalBar!!.addView(mLogLevelSpinner)
        mTerminalBar!!.addView(mTagsSpinner)
        mTerminalBar!!.addView(mOrientationBtn)
        mTerminalBar!!.addView(mShowContainerBtn)
        mTerminalBar!!.addView(mClearBtn)
        mLolly!!.addView(mTerminalBar)
        mLolly!!.addView(mContainer)
    }

    private fun getDip(dip: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), resources
                .displayMetrics).toInt()
    }

    inner class RVLogAdapter : RecyclerView.Adapter<RVLogItemHolder>(){


        private var mCurrentLogs: MutableList<String> = mutableListOf()
        private val mAllLogs: MutableList<String> = mutableListOf()
        private val mDebugLogs: MutableList<String> = mutableListOf()
        private val mInfoLogs: MutableList<String> = mutableListOf()
        private val mWarnLogs: MutableList<String> = mutableListOf()
        private val mErrorLogs: MutableList<String> = mutableListOf()
        private val mAssertLogs: MutableList<String> = mutableListOf()

        private val mSystemErrorCache: MutableList<String> = mutableListOf()
        private val mTagsLogs: MutableList<String> = mutableListOf()

        private val space = "            "
        private val lock = Any()
        private var mCurrentLevel: String = "V"
        private var mCurrentTag: String = "All"

        private var redSpan = ForegroundColorSpan(Color.RED)
        private var whiteSpan = ForegroundColorSpan(Color.WHITE)
        private var blueSpan = ForegroundColorSpan(Color.BLUE)
        private var greenSpan = ForegroundColorSpan(Color.GREEN)
        private var yellowSpan = ForegroundColorSpan(Color.YELLOW)

        private val fullTag: String
            get() {
                val fullTag: String
                if ("V" == mCurrentLevel) {
                    fullTag = "/" + mCurrentTag!!
                } else {
                    fullTag = mCurrentLevel + "/" + mCurrentTag
                }
                return fullTag
            }

        val allLog: List<String>?
            get() = mAllLogs

        init {
            mCurrentLogs = mDebugLogs
        }

        private fun spanLine(line: String): SpannableStringBuilder {
            val builder = SpannableStringBuilder(line)

            if (line.contains("D/")) {
                builder.setSpan(whiteSpan, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return builder
            }
            if (line.contains("System.err")) {
                builder.setSpan(redSpan, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return builder
            }
            if (line.contains("E/")) {
                builder.setSpan(redSpan, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return builder
            }
            if (line.contains("W/")) {
                builder.setSpan(yellowSpan, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return builder
            }
            if (line.contains("A/")) {
                builder.setSpan(greenSpan, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return builder
            }
            if (line.contains("I/")) {
                builder.setSpan(blueSpan, 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return builder
            }
            return builder
        }

        /**
         * @param line
         * @return true:更新UI，将listView滑动到最底层 false:不做处理
         */
        fun addOneLog(line: String): Boolean {

            //对异常日志做特殊处理
            //如果是日常日志则加入到mSystemError，不进行屏幕输出
            if (line.contains("System.err")) {
                if (mSystemErrorCache.size == 0) {
                    mSystemErrorCache.add(line)
                } else {
                    if (line.contains("at")) {
                        val indexAT = line.indexOf("at")
                        mSystemErrorCache.add(line.substring(indexAT))
                    } else {
                        mSystemErrorCache.add(line)
                    }
                }
                return false
            }

            //对正常日志做处理
            if (mAllLogs != null) {
                //若mSystemError内容不为空，则将其内容打印至屏幕
                if (mSystemErrorCache.size != 0) {
                    val sb = StringBuilder()
                    for (i in mSystemErrorCache.indices) {
                        if (i != 0) {
                            sb.append(space).append(mSystemErrorCache[i]).append("\n")
                        } else {
                            sb.append(mSystemErrorCache[i]).append("\n")
                        }
                    }
                    assignLog(sb.toString())
                    //清空mSystemError
                    mSystemErrorCache.clear()
                }
                assignLog(line)
                return true
            }

            return false
        }

        private fun assignLog(line: String) {
            mAllLogs!!.add(line)
            if (line.contains("D/")) {
                mDebugLogs.add(line)
            }
            if (line.contains("I/")) {
                mInfoLogs.add(line)
            }
            if (line.contains("W/")) {
                mWarnLogs.add(line)
            }
            if (line.contains("E/")) {
                mErrorLogs.add(line)
            }
            if (line.contains("A/")) {
                mAssertLogs.add(line)
            }

            if (line.contains("System.err")) {
                mErrorLogs.add(line)
            }

            if (line.contains(fullTag) && mCurrentTag != "All") {
                mTagsLogs.add(line)
            }
            notifyDataSetChanged()
        }

        fun changeLogLevel(level: String) {

            synchronized(lock) {
                mCurrentLevel = level
                if ("V" == level) {
                    mCurrentLogs = mAllLogs
                }

                if ("D" == level) {
                    mCurrentLogs = mDebugLogs
                }

                if ("I" == level) {
                    mCurrentLogs = mInfoLogs
                }
                if ("W" == level) {
                    mCurrentLogs = mWarnLogs
                }
                if ("E" == level) {
                    mCurrentLogs = mErrorLogs
                }
                if ("A" == level) {
                    mCurrentLogs = mAssertLogs
                }
                notifyDataSetChanged()
            }
            if (mCurrentTag != "All") {
                changeTags(mCurrentTag)
            }

        }

        fun changeTags(tag: String) {
            mCurrentTag = tag
            if ("All" == mCurrentTag) {
                changeLogLevel(mCurrentLevel)
                mTagsLogs.clear()
                return
            }
            synchronized(lock) {
                val fullTag = fullTag
                mCurrentLogs = getLogsByLevel(mCurrentLevel)
                mTagsLogs.clear()
                for (line in mCurrentLogs) {
                    if (line.contains(fullTag)) {
                        mTagsLogs.add(line)
                    }
                }
                mCurrentLogs = mTagsLogs
                notifyDataSetChanged()
            }
        }

        private fun getLogsByLevel(level: String?): MutableList<String> {
            return when (level) {
                "V" -> mAllLogs
                "D" -> mDebugLogs
                "I" -> mInfoLogs
                "W" -> mWarnLogs
                "E" -> mErrorLogs
                "A" -> mAssertLogs
                else -> mAllLogs
            }
        }


        fun cleanUp() {
            mCurrentLogs.clear()
            mAllLogs.clear()
            mDebugLogs.clear()
            mInfoLogs.clear()
            mWarnLogs.clear()
            mErrorLogs.clear()
            mAssertLogs.clear()
            mSystemErrorCache.clear()
            mTagsLogs.clear()
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: RVLogItemHolder?, position: Int) {
            holder?.updateText(spanLine(mCurrentLogs[position]))
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RVLogItemHolder {
            val textView = TextView(this@Lolly.application)
            val lp = AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.WRAP_CONTENT)
            textView.layoutParams = lp
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            textView.setTextColor(Color.WHITE)
            return RVLogItemHolder(textView)
        }

        override fun getItemCount(): Int {
            synchronized(lock) {
                return mCurrentLogs.size
            }
        }
    }

    class RVLogItemHolder(view:View) : RecyclerView.ViewHolder(view){
        fun updateText(logText:CharSequence){
            (itemView as TextView).text = logText
        }
    }

    class TouchAbleView(context: Context?) : LinearLayout(context) {
        override fun performClick(): Boolean {
            return super.performClick()
        }
    }
    companion object {

        /**
         * 将Lolly窗口显示在设备屏幕上
         */
        val FLAG_INIT_ADD_WINDOW = 1

        /**
         * 将Lolly窗口从屏幕上移除
         */
        val FLAG_REMOVE_WINDOW = 2

        /**
         * 停止收集log，并停止service
         */
        val FLAG_COLLECT_LOG = 4

        val LOLLY_TAGS = "tags"
        val LOLLY_ORIENTATION = "orientation"

        /**
         * 显示Lolly
         *
         * @param activity
         * @param tags     自定义tag
         */
        fun showLolly(activity: Activity, tags: Array<String>) {
            try {
                val info = activity.packageManager.getActivityInfo(activity.componentName, PackageManager.GET_META_DATA)
                val start = Intent(activity, Lolly::class.java)
                start.putExtra("action", FLAG_INIT_ADD_WINDOW)
                start.putExtra(LOLLY_TAGS, tags)
                start.putExtra(LOLLY_ORIENTATION, info.screenOrientation)
                activity.startService(start)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

        }

        fun showLolly(service: Service, tags: Array<String>) {
            val start = Intent(service, Lolly::class.java)
            start.putExtra("action", FLAG_INIT_ADD_WINDOW)
            start.putExtra(LOLLY_TAGS, tags)
            service.startService(start)
        }

        /**
         * 移除Lolly窗口
         *
         * @param context
         */
        fun hideLolly(context: Context) {
            val stop = Intent(context, Lolly::class.java)
            stop.putExtra("action", Lolly.FLAG_REMOVE_WINDOW)
            stop.addFlags(Lolly.FLAG_REMOVE_WINDOW)
            context.startService(stop)
        }

        /**
         * 保存当前log
         *
         * @param context
         */
        fun saveLog(context: Context) {
            val saveLog = Intent(context, Lolly::class.java)
            saveLog.putExtra("action", FLAG_COLLECT_LOG)
            context.startService(saveLog)

        }
    }
}
