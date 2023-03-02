package com.lianyun.webrtc.bean;

import android.net.Uri;
import android.text.TextUtils;

public class MessageBean {
    public String time;
    public long timeLong;
    public String type;
    public String tag;
    public String content;
    public Uri uri;

    private final String _TYPE = " : type:";
    private final String _TAG = " tag:";
    private final String _LOG = " log:";

    public MessageBean() {

    }

    public MessageBean(String logStr) {
        toLogBean(logStr);
    }

    public MessageBean toLogBean(String logStr){
        if (!TextUtils.isEmpty(logStr)) {
            String[] strType = logStr.split(_TYPE);
            if (strType.length == 2) {
                time = strType[0];
                String[] strTag = strType[1].split(_TAG);
                if (strTag.length == 2) {
                    type = strTag[0];
                    String[] strsLog = strTag[1].split(_LOG);
                    if (strsLog.length == 2) {
                        tag = strsLog[0];
                        content = strsLog[1];
                    } else {
                        content = strTag[1];
                    }
                } else {
                    content = strType[1];
                }
            } else {
                content = logStr;
            }
        }
        return this;
    }
    public String toLog() {

        return time + _TYPE + type + _TAG + tag + _LOG + content + "\r\n";
    }

    @Override
    public String toString() {
        return "LogBean{" +
                "time='" + time + '\'' +
                ", timelong=" + timeLong +
                ", tag='" + tag + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
