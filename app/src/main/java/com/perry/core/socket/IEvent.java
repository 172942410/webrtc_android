package com.perry.core.socket;

import java.util.Map;

/**
 * Created by dds on 2019/7/26.
 * ddssingsong@163.com
 */
public interface IEvent {


    void onOpen();

    void loginSuccess(String userId, String avatar);


    void onInvite(String room, boolean audioOnly, String inviteId, String userList);


    void onCancel(String inviteId);

    void onRing(String userId);


    void onPeers(String myId, String userList, int roomSize);

    void onNewPeer(String myId);

    void onReject(String userId, int type);

    // onOffer
    void onOffer(String userId, String sdp);

    // onAnswer
    void onAnswer(String userId, String sdp);

    // ice-candidate
    void onIceCandidate(String userId, String id, int label, String candidate);

    void onLeave(String userId);

    void logout(String str);

    void onTransAudio(String userId);

    void onDisConnect(String userId);

    void reConnect();

    // 测试的时候 加入房间后 需要 新加的接口
    void setOfferMap(Map map);
    // 测试发送消息的时候添加的
//    void sendMessage(byte[] message);
}
