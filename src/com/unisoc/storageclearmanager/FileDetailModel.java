package com.unisoc.storageclearmanager;

import java.text.Collator;

public class FileDetailModel implements Comparable<FileDetailModel> {
    private String filePath;
    private long fileSize;


    public FileDetailModel() {
    }

    public FileDetailModel(String path, long size) {
        this.filePath = path;
        this.fileSize = size;
    }

    public void setFilePath(String path) {
        this.filePath = path;
    }

    public void setFileSize(long size) {
        this.fileSize = size;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    @Override
    public int compareTo(FileDetailModel another) {
        long result;
        result = fileSize - another.fileSize;
        if (result == 0) {
            return filePath.compareTo(another.filePath);
        } else if (result > 0) {
            return 1;
        } else {
            return -1;
        }
    }
}
