package com.unisoc.storageclearmanager;

import android.util.ArrayMap;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataGroup {
    private final static String TAG = "DataGroup";
    public ArrayMap<String, Long> mInCacheMap;
    public ArrayMap<String, Long> mExCacheMap;
    public ArrayMap<String, Long> mInRubbishMap;
    public ArrayMap<String, Long> mExRubbishMap;
    public ArrayMap<String, Long> mInTmpMap;
    public ArrayMap<String, Long> mExTmpMap;

    public ArrayList<FileDetailModel> mRubbish_log_ext;
    public ArrayList<FileDetailModel> mRubbish_bak_ext;
    public ArrayList<FileDetailModel> mRubbish_tmp_prefix;
    public ArrayList<FileDetailModel> mRubbish_tmp_ext;

    public ArrayList<FileDetailModel> mRubbish_cach1_ext;
    public ArrayList<FileDetailModel> mRubbish_cach2_ext;

    // public ArrayList<String> mTempKey = new ArrayList<String>();
    // public ArrayList<String> mTempValues = new ArrayList<String>();
    private static DataGroup instance;

    private static final int RUBBISH_FILE_BIT = 1;
    private static final int TMP_FILE_BIT = 2;

    private static final int CACHE_ITEM = 0;
    private static final int RUBBISH_ITEM = 1;
    private static final int TMP_ITEM = 2;

    private static final int TYPE_RUBBISH_LOG_EXT = 0;
    private static final int TYPE_RUBBISH_BAK_EXT = 1;
    private static final int TYPE_TMP_FILE_PREFIX = 2;
    private static final int TYPE_TMP_FILE_EXT = 3;
    private static final int TYPE_APK_CATCHE1_EXT = 4;
    private static final int TYPE_APK_CATCHE2_EXT = 5;

    // public int mFileUpdateBits;

    public long mTempCategorySize;
    // public long mExTempCategorySize;
    public long mRubbishCategorySize;
    // public long mExRubbishCategorySize;
    // public long mExCacheFileSize;

    public static DataGroup getInstance() {
        if (instance == null) {
            instance = new DataGroup();
        }
        return instance;
    }

    private DataGroup() {
        mInCacheMap = new ArrayMap<String, Long>();
        mExCacheMap = new ArrayMap<String, Long>();
        mInRubbishMap = new ArrayMap<String, Long>();
        mExRubbishMap = new ArrayMap<String, Long>();
        mInTmpMap = new ArrayMap<String, Long>();
        mExTmpMap = new ArrayMap<String, Long>();

        mRubbish_log_ext = new ArrayList<FileDetailModel>();
        mRubbish_bak_ext = new ArrayList<FileDetailModel>();
        mRubbish_tmp_prefix = new ArrayList<FileDetailModel>();
        mRubbish_tmp_ext = new ArrayList<FileDetailModel>();
        mRubbish_cach1_ext = new ArrayList<FileDetailModel>();
        mRubbish_cach2_ext = new ArrayList<FileDetailModel>();
    }


    public ArrayMap<String, Long> getNeedMap(int type) {
        if (type == CACHE_ITEM) {
            return mInCacheMap;
        } else if (type == RUBBISH_ITEM) {
            return mInRubbishMap;
        } else if (type == TMP_ITEM) {
            return  mInTmpMap;
        }

        return null;
    }

    public void destroy() {
        mInCacheMap.clear();
        mExCacheMap.clear();
        mInRubbishMap.clear();
        mExRubbishMap.clear();
        mInTmpMap.clear();
        mExTmpMap.clear();

        mRubbish_log_ext.clear();
        mRubbish_bak_ext.clear();
        mRubbish_tmp_prefix.clear();
        mRubbish_tmp_ext.clear();
        mRubbish_cach1_ext.clear();
        mRubbish_cach2_ext.clear();
        instance = null;
    }

    public long getTotalSize(int type) {
        if (type == CACHE_ITEM) {
            return getCategorySize(mInCacheMap);
        } else if (type == RUBBISH_ITEM) {
            return getCategorySize(mInRubbishMap);
        } else if (type == TMP_ITEM) {
            return  getCategorySize(mInTmpMap);
        }
        return 0;
    }

    private long getCategorySize(ArrayMap<String,Long> map) {
        long size = 0;

        for (Iterator<Map.Entry<String, Long>> it =map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> entry = it.next();
            String key = entry.getKey();
            File f = new File(key);
            if (f.exists()) {
                size += f.length();
            } else {
                it.remove();
            }
        }
        return size;
    }

    public long getDetailTotalSize(int type) {
        long size = 0;
        switch (type) {
            case TYPE_RUBBISH_BAK_EXT:
                size = getTotalSizeByType(mRubbish_bak_ext);
                break;
            case TYPE_RUBBISH_LOG_EXT:
                size = getTotalSizeByType(mRubbish_log_ext);
                break;
            case TYPE_TMP_FILE_PREFIX:
                size = getTotalSizeByType(mRubbish_tmp_prefix);
                break;
            case TYPE_TMP_FILE_EXT:
                size = getTotalSizeByType(mRubbish_tmp_ext);
                break;
            case TYPE_APK_CATCHE1_EXT:
                size = getTotalSizeByType(mRubbish_cach1_ext);
                break;
            case TYPE_APK_CATCHE2_EXT:
                size = getTotalSizeByType(mRubbish_cach2_ext);
                break;
        }
        return size;
    }

    public long getTotalSizeByType(ArrayList<FileDetailModel> list){
        long size = 0;
        for(FileDetailModel m : list){
            size += m.getFileSize();
        }
        return size;
    }

    public long getCategorySizeByType(int type) {
        long size = 0;
        if (type == CACHE_ITEM) {
            size = getTotalSizeByType(mRubbish_cach1_ext)+getTotalSizeByType(mRubbish_cach2_ext);
        } else if (type == RUBBISH_ITEM) {
            size = getTotalSizeByType(mRubbish_bak_ext)+getTotalSizeByType(mRubbish_log_ext);
        } else if (type == TMP_ITEM) {
            size = getTotalSizeByType(mRubbish_tmp_ext)+getTotalSizeByType(mRubbish_tmp_prefix);
        }
        return size;
    }

    /*
    public void updateSize(int updateType) {
        if ((updateType & RUBBISH_FILE_BIT) > 0) {
            mRubbishCategorySize = getTotalSize(RUBBISH_ITEM);
        }
        if ((updateType & TMP_FILE_BIT) > 0) {
            mTempCategorySize = getTotalSize(TMP_ITEM);
        }
    }*/

    public HashMap<Integer, ArrayList<FileDetailModel>> getDetailAssortmentType(int type) {
        HashMap<Integer, ArrayList<FileDetailModel>> typeList = new HashMap<Integer, ArrayList<FileDetailModel>>();
        switch (type) {
            case CACHE_ITEM:
                typeList.clear();
                typeList.put(TYPE_APK_CATCHE1_EXT, mRubbish_cach1_ext);
                typeList.put(TYPE_APK_CATCHE2_EXT, mRubbish_cach2_ext);
                break;
            case RUBBISH_ITEM:
                typeList.clear();
                typeList.put(TYPE_RUBBISH_LOG_EXT, mRubbish_log_ext);
                typeList.put(TYPE_RUBBISH_BAK_EXT, mRubbish_bak_ext);
                break;
            case TMP_ITEM:
                typeList.clear();
                typeList.put(TYPE_TMP_FILE_EXT, mRubbish_tmp_ext);
                typeList.put(TYPE_TMP_FILE_PREFIX, mRubbish_tmp_prefix);
                break;
        }

        return typeList;
    }

    public Long getFileListTotalSize(ArrayList<FileDetailModel> fileList){
        long size = 0;
        for(FileDetailModel f:fileList){
            size += f.getFileSize();
        }
        return size;
    }

    public boolean isDisplayIcon(int type) {
        boolean flag = false;
        switch (type) {
            case CACHE_ITEM:
                flag = this.mRubbish_cach1_ext.size() > 0 || this.mRubbish_cach2_ext.size() > 0;
                break;
            case RUBBISH_ITEM:
                flag = this.mRubbish_log_ext.size() > 0 || this.mRubbish_bak_ext.size() > 0;
                break;
            case TMP_ITEM:
                flag = this.mRubbish_tmp_prefix.size() > 0 || this.mRubbish_tmp_ext.size() > 0;
                break;
        }
        return flag;
    }
}
