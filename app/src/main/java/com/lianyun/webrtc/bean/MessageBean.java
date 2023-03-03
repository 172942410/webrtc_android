package com.lianyun.webrtc.bean;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

public class MessageBean {
    public long sendTime;
    public long receiveTime;
    public int type;//谁发的-1是我发的在左边；1是对方发的在右边
    public boolean binary;//是否是二进制消息；即文本消息还是文件类型的消息
    public String content;
    public Uri uri;//我发的图片有地址
    public Bitmap bitmap;//接收到的是这个图片数据

    public MessageBean() {
    }

    public MessageBean(String logStr) {
        toLogBean(logStr);
    }

    public MessageBean toLogBean(String logStr) {
        if (!TextUtils.isEmpty(logStr)) {
            content = logStr;
        }
        return this;
    }

    @Override
    public String toString() {
        return "MessageBean{" +
                "sendTime=" + sendTime +
                ", receiveTime=" + receiveTime +
                ", type=" + type +
                ", binary='" + binary + '\'' +
                ", content='" + content + '\'' +
                ", uri=" + uri +
                ", bitmap=" + bitmap +
                '}';
    }
}
