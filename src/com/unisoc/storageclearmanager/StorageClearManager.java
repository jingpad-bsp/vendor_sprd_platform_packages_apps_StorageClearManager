package com.unisoc.storageclearmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.EnvironmentEx;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StorageClearManager extends Activity implements View.OnClickListener,
    ApplicationsState.Callbacks {
    private static final String TAG = "StorageClearManager";
    // private static final String HOME_RECENT_ENABLED = "home_recent_key_enabled";
    private static final String LRM = "\u200E";

    private static final int STAND_TIME = 1500;
    private static final int STORAGE_LOW = 200 * 1024 * 1024;
    private static final int STORAGE_EMPTY = 100 * 1024 * 1024;

    private static final int STORAGE_LEVEL_ENOUGH = 0;
    private static final int STORAGE_LEVEL_LOW = 1;
    private static final int STORAGE_LEVEL_EMPTY = 2;

    private static final int UPDATE_SCAN_SIZE = 0;
    private static final int UPDATE_CLEAN_SIZE = 1;
    private static final int SET_BUTTON_CLEAN = 2;
    private static final int UPDATE_PATH_UI = 3;
    private static final int CLEAN_COMPLETED = 4;

    private static final int TYPE_RUBBISH_LOG_EXT = 0;
    private static final int TYPE_RUBBISH_BAK_EXT = 1;
    private static final int TYPE_TMP_FILE_PREFIX = 2;
    private static final int TYPE_TMP_FILE_EXT = 3;
    private static final int TYPE_APK_CATCHE1_EXT = 4;
    private static final int TYPE_APK_CATCHE2_EXT = 5;

    private static final String RUBBISH_FILE1_EXT = ".log";
    private static final String RUBBISH_FILE2_EXT = ".bak";
    private static final String TMP_FILE_PREFIX = "~";
    private static final String TMP_FILE_EXT = ".tmp";
    private static final String APK_FILE_EXT = ".apk";

    private final int mFileDetailTypes[] = { TYPE_RUBBISH_LOG_EXT, TYPE_RUBBISH_BAK_EXT,
            TYPE_TMP_FILE_EXT, TYPE_APK_CATCHE1_EXT, TYPE_APK_CATCHE2_EXT };

    /** All the widgets to disable in the status bar */
    /*
    private final static int STATUSBAR_DISABLE = StatusBarManager.DISABLE_EXPAND
            | StatusBarManager.DISABLE_HOME
            | StatusBarManager.DISABLE_RECENT;
    */

    /** The status bar where back/home/recent buttons are shown. */
    // private StatusBarManager mStatusBar;
    // private StorageManager mStorageManager;
    private PackageManager mPackageManager;

    private DataGroup mData = DataGroup.getInstance();

    // private Context mContext;
    private Button mClearGarbageBtn;
    private View mEnterFilesManagerBtn;
    private TextView mWarningTextView;
    private TextView mSizeTextView;
    private TextView mSizeUnitView;
    private TextView mScanPathView;
    private ImageView mClearDoneView;
    private ImageView mWarningIconView;

    private boolean isStopScan = false;
    private boolean isPressBack = false;
    // private boolean isScanning = false;
    private boolean mIsScanEnd = false;

    private long mInCacheSize;
    private long mInRubbishSize;
    private long mInTmpSize;
    // private long mExCacheSize;
    // private long mExRubbishSize;
    // private long mExTmpSize;
    private long mSizeTotal;
    private boolean callBack;

    private ApplicationsState mState;
    private ApplicationsState.AppEntry mAppEntry;
    protected ApplicationsState.Session mSession;
    private void scanInCached() {
        mState = ApplicationsState.getInstance(getApplication());
        mSession = mState.newSession(this);
        mSession.resume();
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            long orgSize = 0;
            File file = (File) msg.obj;
            Log.i(TAG, "msg.what:"+msg.what);
            switch (msg.what) {
                case UPDATE_PATH_UI:
                    Log.i(TAG, "file == null:"+(file == null));
                    /*if(file != null) {
                        Log.i(TAG, "file path:"+file.getAbsolutePath());
                        mScanPathView.setText(getString(R.string.file_scanning) +
                            file.getAbsolutePath());
                    }*/
                    break;
                case UPDATE_SCAN_SIZE:
                    removeMessages(UPDATE_SCAN_SIZE);
                    Log.i(TAG, "update......" + mInCacheSize + mInRubbishSize + mInTmpSize);
                    orgSize = mInCacheSize + mInRubbishSize + mInTmpSize;
                    String[] strSize_label = convertTotalSize(orgSize);
                    if(strSize_label!= null && strSize_label.length > 1) {
                        mSizeTextView.setText(strSize_label[0]);
                        mSizeUnitView.setText(strSize_label[1]);
                    }
                    break;
                case UPDATE_CLEAN_SIZE:
                    removeMessages(UPDATE_CLEAN_SIZE);
                    String[] strSize = convertTotalSize(mSizeTotal);
                    if (strSize != null && strSize.length > 1) {
                        mSizeTextView.setText(strSize[0]);
                        mSizeUnitView.setText(strSize[1]);
                    }
                    break;
                case SET_BUTTON_CLEAN:
                    removeMessages(SET_BUTTON_CLEAN);
                    mSizeTotal = mInCacheSize + mInRubbishSize + mInTmpSize;
                    if (mSizeTotal <= 0) {
                        mScanPathView.setText(getString(R.string.scan_completed));
                        mClearGarbageBtn.setEnabled(false);
                    } else {
                        mScanPathView.setText(getString(R.string.scan_completed));
                        mClearGarbageBtn.setEnabled(true);
                    }
                    break;
                case CLEAN_COMPLETED:
                    removeMessages(CLEAN_COMPLETED);
                    if (mSizeTotal < 0) {
                        mSizeTotal = 0;
                    }
                    String[] size_label = convertTotalSize(mSizeTotal);
                    if(size_label!= null && size_label.length > 1) {
                        mSizeTextView.setText(size_label[0]);
                        mSizeUnitView.setText(size_label[1]);
                    }
                    mScanPathView.setText(getString(R.string.remove_completed));
                    mClearGarbageBtn.setVisibility(View.GONE);
                    mClearDoneView.setVisibility(View.VISIBLE);
                    setupWarningText();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_clear_manage);

        // Disable the status bar, but do NOT disable back because the user needs a way to go
        // from keyboard settings and back to the password screen.
        // mStatusBar = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);

        mPackageManager = getPackageManager();
        // mStorageManager = getSystemService(StorageManager.class);

        initViews();
        performGarbageScanning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* No need to prohibit user operations
        if (isInternalStorageEmpty()) {
            mStatusBar.disable(STATUSBAR_DISABLE);
            Settings.System.putInt(getContentResolver(), HOME_RECENT_ENABLED, 0);
        } else {
            mStatusBar.disable(StatusBarManager.DISABLE_NONE);
            Settings.System.putInt(getContentResolver(), HOME_RECENT_ENABLED, 1);
        }*/
        setupWarningText();
    }

    private void initViews() {
        // mContext = this;
        mClearGarbageBtn = (Button) findViewById(R.id.garbage_clear_btn);
        mWarningTextView = (TextView) findViewById(R.id.storage_warning);
        mSizeTextView = (TextView) findViewById(R.id.garbage_size);
        mSizeUnitView = (TextView) findViewById(R.id.garbage_size_unit);
        mScanPathView = (TextView) findViewById(R.id.scan_path);
        mClearDoneView = (ImageView) findViewById(R.id.clear_done);
        mWarningIconView = (ImageView) findViewById(R.id.storage_warning_icon);
        mEnterFilesManagerBtn = findViewById(R.id.app_clear_btn);
        mClearGarbageBtn.setOnClickListener(this);
        mClearGarbageBtn.setEnabled(false);
        mEnterFilesManagerBtn.setOnClickListener(this);
        setupWarningText();
    }

    private boolean isRTL() {
        int direction = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault());
        return direction == View.LAYOUT_DIRECTION_RTL;
    }
    private void setupWarningText() {
        int level = getStorageLevel();
        int color;
        Drawable drawable;
        switch (level) {
            case STORAGE_LEVEL_ENOUGH:
                if (isRTL()) {
                    mWarningTextView.setText(LRM + getString(R.string.storage_warning_level0) + LRM);
                } else {
                    mWarningTextView.setText(getString(R.string.storage_warning_level0));
                }
                mWarningIconView.setImageResource(R.drawable.baseline_info_24px);
                color = getResources().getColor(R.color.warning_green);
                mWarningTextView.setTextColor(color);
                break;
            case STORAGE_LEVEL_LOW:
                if (isRTL()) {
                    mWarningTextView.setText(LRM + getString(R.string.storage_warning_level1) + LRM);
                } else {
                    mWarningTextView.setText(getString(R.string.storage_warning_level1));
                }
                mWarningIconView.setImageResource(R.drawable.baseline_disc_24px);
                color = getResources().getColor(R.color.warning_orange);
                mWarningTextView.setTextColor(color);
                break;
            case STORAGE_LEVEL_EMPTY:
                if (isRTL()) {
                    mWarningTextView.setText(LRM + getString(R.string.storage_warning_level2) + LRM);
                } else {
                    mWarningTextView.setText(getString(R.string.storage_warning_level2));
                }
                mWarningIconView.setImageResource(R.drawable.baseline_disc_full_24px);
                color = getResources().getColor(R.color.warning_red);
                mWarningTextView.setTextColor(color);
                break;
            default:
                break;
        }
    }

    private void performGarbageScanning() {
        resetData();
        // isScanning = true;
        scanInCached();
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Scanning start...");
                    File file_inter = EnvironmentEx.getInternalStoragePath();
                    scanFiles(file_inter.listFiles());
                } catch (Exception e) {
                    Log.i(TAG, "InterruptedException 1");
                } finally {
                    while (true) {
                        if (isPressBack) {
                            isPressBack = false;
                            break;
                        }
                        if (isStopScan) {
                            isStopScan = false;
                            break;
                        }
                        if (mIsScanEnd) {
                            if (!isStopScan &&!isPressBack) {
                                mHandler.sendEmptyMessage(SET_BUTTON_CLEAN);
                                mIsScanEnd = false;
                                break;
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                }
            }
        }.start();
    }

    private void scanFiles(File[] files) {
        Log.i(TAG, "Enter scanFiles, files == null:" + (files == null));
        if (files != null) {
            for (File file : files) {
                if (isStopScan) {
                    Log.i(TAG, "isStopScan true");
                    break;
                }
                Message updatePathUi  = mHandler.obtainMessage(UPDATE_PATH_UI, file);
                mHandler.sendMessage(updatePathUi);
                if (file.isDirectory()) {
                    Log.i(TAG, "fileName:"+file.getName());
                    if (file.getName().equals("cache")
                            || file.getName().equals("code_cache")) {
                        long size = scanDirSize(file);
                        Log.i(TAG, "size:"+size);
                        if (size > 0) {
                            mData.mInCacheMap.put((file.getAbsolutePath()), size);
                            mInCacheSize += size;
                            Log.i(TAG, "mInCacheSize:"+mInCacheSize);
                            mData.mRubbish_cach2_ext.add(new FileDetailModel(file.getAbsolutePath(), size));
                            mHandler.sendEmptyMessage(UPDATE_SCAN_SIZE);
                        }
                        continue;
                    }
                    scanFiles(file.listFiles());
                } else {
                    Log.i(TAG, "name:"+file.getName());
                    String name = file.getName();
                    if (name.endsWith(RUBBISH_FILE1_EXT)) {
                        mInRubbishSize += file.length();
                        Log.i(TAG, "mInRubbishSize:"+mInRubbishSize);
                        mData.mRubbishCategorySize = mInRubbishSize;
                        mData.mInRubbishMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_log_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                    } else if (name.endsWith(RUBBISH_FILE2_EXT)) {
                        mInRubbishSize += file.length();
                        Log.i(TAG, "mInRubbishSize:"+mInRubbishSize);
                        mData.mRubbishCategorySize = mInRubbishSize;
                        mData.mInRubbishMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_bak_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                    } else if (name.endsWith(TMP_FILE_EXT) || name.startsWith(TMP_FILE_PREFIX)) {
                        mInTmpSize += file.length();
                        Log.i(TAG, "mInTmpSize:"+mInTmpSize);
                        mData.mTempCategorySize = mInTmpSize;
                        mData.mInTmpMap.put(file.getAbsolutePath(), file.length());
                        mData.mRubbish_tmp_ext.add(new FileDetailModel(file.getAbsolutePath(), file.length()));
                    }
                    mHandler.sendEmptyMessage(UPDATE_SCAN_SIZE);
                }
            }
        }
    }

    private long scanDirSize(File dir) {
        File[] fileList = dir.listFiles();
        int size = 0;
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    size += scanDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /* No need to prohibit user operations
    @Override
    public void onBackPressed() {
        if (!isInternalStorageEmpty()) {
            Log.d(TAG, "Available internal storage over 50MB!");
            isPressBack = true;
            mStatusBar.disable(StatusBarManager.DISABLE_NONE);
            Settings.System.putInt(getContentResolver(), HOME_RECENT_ENABLED, 1);
            super.onBackPressed();
        }
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // mStatusBar.disable(StatusBarManager.DISABLE_NONE);
        // Settings.System.putInt(getContentResolver(), HOME_RECENT_ENABLED, 1);
        mIsScanEnd = true;
        if (mState != null) {
            mState.clear();
            mState = null;
        }
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.garbage_clear_btn:
                mClearGarbageBtn.setEnabled(false);
                FileDeleteTask deleteTask = new FileDeleteTask();
                deleteTask.execute();
                break;
            case R.id.app_clear_btn:
                Intent intent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    private boolean isInternalStorageEmpty() {
        return getStorageLevel() == STORAGE_LEVEL_EMPTY;
    }

    private int getStorageLevel() {
        long usableSpace = Environment.getDataDirectory().getUsableSpace();
        if (usableSpace < STORAGE_EMPTY) {
            return STORAGE_LEVEL_EMPTY;
        } else if (usableSpace < STORAGE_LOW) {
            return STORAGE_LEVEL_LOW;
        } else {
            return STORAGE_LEVEL_ENOUGH;
        }
    }

    private  String[] convertTotalSize(long totalSize) {
        String size_lable = Formatter.formatFileSize(this, totalSize);
        String[] sizeLableArray;
        if (size_lable.contains(" ")) {
            sizeLableArray = size_lable.split(" ");
        } else {
            StringBuffer s1 = new StringBuffer(size_lable);
            for (int i = 0; i < size_lable.length(); i++) {
                if ((size_lable.charAt(i) + "").getBytes(Charset.defaultCharset()).length > 1) {
                    s1.insert(i, " ");
                    break;
                }
            }
            sizeLableArray = s1.toString().split(" ");
        }
        return sizeLableArray;
    }

    private void resetData() {
        mData.mInCacheMap.clear();
        mData.mInRubbishMap.clear();
        mData.mInTmpMap.clear();

        mData.mExCacheMap.clear();
        mData.mExRubbishMap.clear();
        mData.mExTmpMap.clear();

        mData.mRubbish_bak_ext.clear();
        mData.mRubbish_log_ext.clear();
        mData.mRubbish_tmp_prefix.clear();
        mData.mRubbish_tmp_ext.clear();
        mData.mRubbish_cach1_ext.clear();
        mData.mRubbish_cach2_ext.clear();

        mInCacheSize = 0;
        mInRubbishSize = 0;
        mInTmpSize = 0;

        // mExCacheSize = 0;
        // mExRubbishSize = 0;
        // mExTmpSize = 0;
        callBack = false;

        Log.i(TAG, "mSession=" + mSession);
        if (mSession != null) {
            if (mState != null) {
                Log.i(TAG,
                    "sessions before:" + (mState.mSessions == null ? "mState.mSessions is null"
                    : "") + mState.mSessions.size()
                    + "\n\t\tmState.mSessions="
                    + mState.mSessions);
            }
            mSession.release();
            if (mState != null) {
                Log.i(TAG,
                    "sessions before:" + (mState.mSessions == null ? "mState.mSessions is null"
                    : "") + mState.mSessions.size()
                    + "\n\t\tmState.mSessions="
                    + mState.mSessions);
            }
            mSession.release();
            if (mState != null) {
                Log.i(TAG,
                    "sessions after:" + (mState.mSessions == null ? "mState.mSessions is null"
                    : "") + mState.mSessions.size()
                    + "\n\t\tmState.mSessions="
                    + mState.mSessions);
            }
        }
        if (mState != null) {
            if (mState.mThread != null && mState.mThread.getLooper() != null) {
                mState.mThread.getLooper().quit();
            }
            mState = null;
        }
    }

    public void onAllSizesComputed() {
        Log.e(TAG, "onAllSizesComputed");
        if (callBack) {
            return;
        }
        mIsScanEnd = true;
    }

    public void onPackageSizeChanged(String packageName) {
        if (callBack) {
            Log.i(TAG, "onPackageSizeChanged");
            return;
        }

        if (mState == null) {
            mState = ApplicationsState.getInstance(getApplication());
            mSession = mState.newSession(this);
            mSession.resume();
        }

        mAppEntry = mState.getEntry(packageName, UserHandle.myUserId());
        if (mAppEntry != null) {
            boolean flag = false;
            if ((mAppEntry.info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flag = true;
            } else if ((mAppEntry.info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                flag = true;
            } else if (mAppEntry.hasLauncherEntry) {
                flag = true;
            }

            if (flag) {
                if (mAppEntry.cacheSize + mAppEntry.externalCacheSize > 0
                        && !(mAppEntry.cacheSize == 4096 * 3 && mAppEntry.externalCacheSize == 0)) {
                    Log.i(TAG, "\t\tadd:" + packageName
                            + mAppEntry.cacheSize + " ex:"
                            + mAppEntry.externalCacheSize);
                    mData.mRubbish_cach1_ext.add(new FileDetailModel((Environment.getDataDirectory()
                            .getAbsoluteFile()
                            + File.separator
                            + "data"
                            + File.separator + packageName), (mAppEntry.cacheSize
                            + mAppEntry.externalCacheSize)));
                    mInCacheSize += mAppEntry.cacheSize
                            + mAppEntry.externalCacheSize;
                    Log.i(TAG, "mInRubbishMap \t\tfile:" + packageName + ":\t" + mInCacheSize);
                    mHandler.sendEmptyMessage(UPDATE_SCAN_SIZE);

                }
            }
        }
        Log.i("TAG", "onPack:" + packageName);
    }

    public void deleteFiles(FileDetailModel f) {
        File file = new File(f.getFilePath());
        if (dirDelete(file) || !file.exists()) {
            mSizeTotal -= f.getFileSize();
        }
    }

    private boolean dirDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!dirDelete(f)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    private void notifyMediaScanDir(File dir) {
        Log.i(TAG, "send broadcast to scan dir = " + dir);
        String path = dir.getPath();
        Intent intent = new Intent("sprd.intent.action.MEDIA_SCANNER_SCAN_DIR");
        intent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        Bundle bundle = new Bundle();
        bundle.putString("scan_dir_path", path);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    public void onRunningStateChanged(boolean running) {}

    public void onPackageListChanged() {}

    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {}

    public void onPackageIconChanged() {}

    public void onLauncherInfoChanged() {}

    public void onLoadEntriesCompleted() {}

    class FileDeleteTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onStart();
        }

        private void onStart() {
            mScanPathView.setText(getString(R.string.garbage_removing));
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            for (FileDetailModel f : mData.mRubbish_bak_ext) {
                deleteFiles(f);
            }
            mData.mRubbish_bak_ext.clear();
            for (FileDetailModel f : mData.mRubbish_log_ext) {
                deleteFiles(f);
            }
            mData.mRubbish_log_ext.clear();
            for (FileDetailModel f : mData.mRubbish_tmp_ext) {
                deleteFiles(f);
            }
            mData.mRubbish_tmp_ext.clear();
            for (FileDetailModel f : mData.mRubbish_cach1_ext) {
                File file = new File(f.getFilePath());
                String packageName = file.getName();
                mPackageManager.deleteApplicationCacheFiles(packageName, null);
                mSizeTotal -= f.getFileSize();
            }
            mData.mRubbish_cach1_ext.clear();
            for (FileDetailModel f : mData.mRubbish_cach2_ext) {
                deleteFiles(f);
            }
            mData.mRubbish_cach2_ext.clear();
            try {
                Thread.sleep(STAND_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mHandler.sendEmptyMessage(CLEAN_COMPLETED);
            notifyMediaScanDir(EnvironmentEx.getInternalStoragePath());
        }
    }
}
