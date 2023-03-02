package com.dds.skywebrtc.engine;

/**
 * webRTC 接受到消息的监听
 */
public interface DataChannelListener {

    void onReceiveBinaryMessage(String socketId,String message);

    void onReceiveMessage(String socketId,String message);

    void onReceiveFileProgress(float progress);

}
