package com.orhanobut.logger;

/**
 * @author Orhan Obut
 */
public final class Settings {

    private int methodCount = 2;
    private boolean showThreadInfo = true;
    private int methodOffset = 0;
    private boolean justshowLog = true;

    /**
     * Determines how logs will printed
     */
    private LogLevel logLevel = LogLevel.FULL;

    public Settings hideThreadInfo() {
        showThreadInfo = false;
        return this;
    }

    public Settings setMethodCount(int methodCount) {
        this.methodCount = methodCount;
        return this;
    }

    public Settings setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public Settings setMethodOffset(int offset) {
        this.methodOffset = offset;
        return this;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public boolean isShowThreadInfo() {
        return showThreadInfo;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public int getMethodOffset() {
        return methodOffset;
    }

    /**
     * 是否只显示log信息
     *
     * @return
     */
    public boolean isJustshowLog() {
        return justshowLog;
    }

    public void setJustshowLog(boolean justshowLog) {
        this.justshowLog = justshowLog;
    }

    public void setShowThreadInfo(boolean showThreadInfo) {
        this.showThreadInfo = showThreadInfo;
    }
}