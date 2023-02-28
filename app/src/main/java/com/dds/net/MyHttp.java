package com.dds.net;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dds.core.socket.IEvent;
import com.dds.core.util.StringUtil;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

/**
 * Created by dds on 2019/7/26.
 * android_shuai@163.com
 */
public class MyHttp {
    private final static String TAG = "MyHttp";
    private final IEvent iEvent;
    String url;
    String localPeerId = "";
    String remotePeerId = "";

    public MyHttp(String httpUrl, String localPeerId, String remotePeerId, IEvent event) {
        this.url = httpUrl;
        this.localPeerId = localPeerId;
        this.remotePeerId = remotePeerId;
        this.iEvent = event;
    }


    public void onError(Throwable ex) {
        Log.e("dds_error", "onError:" + ex.toString());
        this.iEvent.logout("onError");
    }

    public void onMessage(String message) {
        Log.d(TAG, "message：" + message);
        if (!TextUtils.isEmpty(message)) {
            handleMessage(message);
        }
//        new requestThread().start();
    }


    // ---------------------------------------处理接收消息-------------------------------------

    private void handleMessage(String message) {
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        int messageType = (Integer) map.get("MessageType");
        if (eventName == null && messageType < 0) {
            return;
        }
        // 登录成功
        if ("__login_success".equals(eventName)) {
            handleLogin(map);
            return;
        }
        // 被邀请 1.接收到offer后就作为 应答方 被邀请了
        if ("__invite".equals(eventName) || messageType == 1) {
            handleInvite(map);
            return;
        }
        // 取消拨出
        if ("__cancel".equals(eventName)) {
            handleCancel(map);
            return;
        }
        // 响铃
        if ("__ring".equals(eventName)) {
            handleRing(map);
            return;
        }
        // 进入房间 3
        if ("__peers".equals(eventName)) {
            handlePeers(map);
            return;
        }
        // 新人入房间
        if ("__new_peer".equals(eventName)) {
            handleNewPeer(map);
            return;
        }
        // 拒绝接听
        if ("__reject".equals(eventName)) {
            handleReject(map);
            return;
        }
        // offer  2.接收到offer第一步是需要先 应答 || messageType == 1
        if ("__offer".equals(eventName)) {
//            handleInvite(map);//作为 接受者 视为 需要被邀请 接受邀请
            handleOffer(map);
//            handlePeers(map);//进入房间
            return;
        }
        // answer
        if ("__answer".equals(eventName) || messageType == 2) {
            handleAnswer(map);
            return;
        }
        // ice-candidate
        if ("__ice_candidate".equals(eventName) || messageType == 3) {
            handleIceCandidate(map);
        }
        // 离开房间
        if ("__leave".equals(eventName)) {
            handleLeave(map);
        }
        // 切换到语音
        if ("__audio".equals(eventName)) {
            handleTransAudio(map);
        }
        // 意外断开
        if ("__disconnect".equals(eventName)) {
            handleDisConnect(map);
        }
    }

    private void handleDisConnect(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onDisConnect(fromId);
        }
    }

    private void handleTransAudio(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onTransAudio(fromId);
        }
    }

    private void handleLogin(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            String avatar = (String) data.get("avatar");
            this.iEvent.loginSuccess(userID, avatar);
        }


    }

    private void handleIceCandidate(Map map) {
//        TODO 这里作为发起方或应答方都需要设置
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("fromID");
            String id = (String) data.get("id");
            int label = (int) data.get("label");
            String candidate = (String) data.get("candidate");
            this.iEvent.onIceCandidate(userID, id, label, candidate);
        } else {
            String candidateStr = (String) map.get("Data");
            String iceDataSeparator = (String) map.get("IceDataSeparator");
            if (!TextUtils.isEmpty(candidateStr)) {
                if (TextUtils.isEmpty(iceDataSeparator)) {
                    iceDataSeparator = "\\|";
                } else if (iceDataSeparator.equals("|")) {
                    iceDataSeparator = "\\|";
                }
                String[] candidateInfo = candidateStr.split(iceDataSeparator);
                if (candidateInfo.length > 2) {
                    String id = candidateInfo[2];
                    int label = Integer.parseInt(candidateInfo[1]);
                    String candidate = candidateInfo[0];
                    this.iEvent.onIceCandidate("lipengjun", id, label, candidate);
                }
            }
        }
    }

    private void handleAnswer(Map map) {
//        TODO 这里作为 发起方 接收到 应答方 发来的answer时进行解析设置 然后设置证书
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.iEvent.onAnswer(userID, sdp);
        } else {
            String sdp = (String) map.get("Data");
            this.iEvent.onAnswer("lipengjun", sdp);
        }
    }

    private void handleOffer(Map map) {
        //TODO 这里作为 应答方 接收到对方发来的offer进行解析 发送成功之后设置证书
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.iEvent.onOffer(userID, sdp);
        } else {
            String sdpStr = (String) map.get("Data");
            if (!TextUtils.isEmpty(sdpStr)) {
                this.iEvent.onOffer("lipengjun", sdpStr);
            }
        }
    }

    private void handleReject(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromID = (String) data.get("fromID");
            int rejectType = Integer.parseInt(String.valueOf(data.get("refuseType")));
            this.iEvent.onReject(fromID, rejectType);
        }
    }

    private void handlePeers(Map map) {
//        Map data = (Map) map.get("data");
//        if (data != null) {
//            String you = (String) data.get("you");
//            String connections = (String) data.get("connections");
//            int roomSize = (int) data.get("roomSize");
//            this.iEvent.onPeers(you, connections, roomSize);
//        }else{
        String you = "lipengjun";
        String connections = "lipengjun";
        int roomSize = 2;
        this.iEvent.onPeers(you, connections, roomSize);
//        }
    }

    private void handleNewPeer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            this.iEvent.onNewPeer(userID);
        } else {
            this.iEvent.onNewPeer("lipengjun");
        }
    }

    private void handleRing(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onRing(fromId);
        }
    }

    private void handleCancel(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String inviteID = (String) data.get("inviteID");
            String userList = (String) data.get("userList");
            this.iEvent.onCancel(inviteID);
        }
    }

    private void handleInvite(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String room = (String) data.get("room");
            boolean audioOnly = (boolean) data.get("audioOnly");
            String inviteID = (String) data.get("inviteID");
            String userList = (String) data.get("userList");
            this.iEvent.onInvite(room, audioOnly, inviteID, userList);
        } else {
            this.iEvent.onInvite("room", false, "lipengjun", "lipengjun");
            this.iEvent.setOfferMap(map);
        }
    }

    private void handleLeave(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromID = (String) data.get("fromID");
            this.iEvent.onLeave(fromID);
        }
    }

    private void send(String jsonStr) {

    }

    /**
     * 主动发送的 http 请求
     * 目前测试就123
     *
     * @param map
     */
    private void send(Map<String, Object> map) {
//        windows hololens
        HttpRequestPresenter.getInstance().post(url + "/" + remotePeerId, map, new ICallback() {
            @Override
            public void onSuccess(String result) {
                onMessage(result);
//                new requestThread().start();
            }

            @Override
            public void onFailure(int code, Throwable t) {
                t.printStackTrace();
                onError(t);
            }
        });
    }

    /**
     * ------------------------------发送消息----------------------------------------
     */
    public void createRoom(String room, int roomSize, String myId) {
        Map<String, Object> map = new HashMap<>();
        //TODO 这里 作为发起方 要重新组织数据；用http模拟发送offer给服务器；等待 应答方 接收
        map.put("eventName", "__create");
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("roomSize", roomSize);
        childMap.put("userID", myId);
        map.put("data", childMap);
//        JSONObject object = new JSONObject(map);
//        final String jsonString = object.toString();
//        Log.d(TAG, "send-->" + jsonString);
//        send(map);
        handleNewPeer(map);
    }

    // 发送邀请
    public void sendInvite(String room, String myId, List<String> users, boolean audioOnly) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__invite");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("audioOnly", audioOnly);
        childMap.put("inviteID", myId);

        String join = StringUtil.listToString(users);
        childMap.put("userList", join);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 取消邀请
    public void sendCancel(String mRoomId, String useId, List<String> users) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__cancel");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("inviteID", useId);
        childMap.put("room", mRoomId);

        String join = StringUtil.listToString(users);
        childMap.put("userList", join);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 发送响铃通知
    public void sendRing(String myId, String toId, String room) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__ring");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("toID", toId);
        childMap.put("room", room);


        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    //加入房间
    public void sendJoin(String room, String myId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");

        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("userID", myId);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
        handlePeers(map);
    }

    // 拒接接听
    public void sendRefuse(String room, String inviteID, String myId, int refuseType) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__reject");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("toID", inviteID);
        childMap.put("fromID", myId);
        childMap.put("refuseType", String.valueOf(refuseType));

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 离开房间
    public void sendLeave(String myId, String room, String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__leave");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("fromID", myId);
        childMap.put("userID", userId);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
//        if (isOpen()) {
        send(jsonString);
//        }
    }

    // send offer
    public void sendOffer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
//        childMap.put("sdp", sdp);
//        childMap.put("userID", userId);
//        childMap.put("fromID", myId);
//        map.put("data", childMap);
//        map.put("eventName", "__offer");
//        JSONObject object = new JSONObject(map);
//        final String jsonString = object.toString();
//        Log.d(TAG, "send-->" + jsonString);
        map.put("MessageType", 1);
        map.put("Data", sdp);
        map.put("IceDataSeparator", "");
        send(map);
    }

    // send answer
    public void sendAnswer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
//        Map<String, Object> childMap = new HashMap<>();
//        childMap.put("sdp", sdp);
//        childMap.put("fromID", myId);
//        childMap.put("userID", userId);
//        map.put("data", childMap);
//        map.put("eventName", "__answer");
//        JSONObject object = new JSONObject(map);
//        final String jsonString = object.toString();
//        Log.d(TAG, "send-->" + jsonString);
        map.put("MessageType", 2);
        map.put("Data", sdp);
        map.put("IceDataSeparator", "");

        send(map);
    }

    // send ice-candidate
    public void sendIceCandidate(String myId, String userId, String id, int label, String candidate) {
        Map<String, Object> map = new HashMap<>();
//        map.put("eventName", "__ice_candidate");
//
//        Map<String, Object> childMap = new HashMap<>();
//        childMap.put("userID", userId);
//        childMap.put("fromID", myId);
//        childMap.put("id", id);
//        childMap.put("label", label);
//        childMap.put("candidate", candidate);
//
//        map.put("data", childMap);
//        JSONObject object = new JSONObject(map);
//        final String jsonString = object.toString();
//        Log.d(TAG, "send-->" + jsonString);
//        if (isOpen()) {
        map.put("MessageType", 3);
        map.put("Data", candidate + "|" + label + "|" + id);
        map.put("IceDataSeparator", "|");
        send(map);
//        }
    }

    // 切换到语音
    public void sendTransAudio(String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        map.put("data", childMap);
        map.put("eventName", "__audio");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 断开重连
    public void sendDisconnect(String room, String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        childMap.put("room", room);
        map.put("data", childMap);
        map.put("eventName", "__disconnect");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    int count;

    /**
     * 开始轮训请求
     */
    public void loop() {
        new RequestThread().start();
        //请求连接 hololens 或者 windows
    }

    boolean isRun = false;

    /**
     * 作为 应答方 要实时轮训 发起方 送来的消息
     */
    class RequestThread extends Thread {

        @Override
        public void run() {
            super.run();
            if (isRun) {
                return;
            }
            isRun = true;
            try {
                sleep(2000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
            Log.d(TAG, "开启请求线程:" + count);
//            hololens windows
            HttpRequestPresenter.getInstance().get(url + "/" + localPeerId, null, new ICallback() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "请求线程到结果:" + count + ":" + result);
                    onMessage(result);
                    isRun = false;
                    new RequestThread().start();
                }

                @Override
                public void onFailure(int code, Throwable t) {
                    Log.e(TAG, "请求线程到错误：" + count);
                    onError(t);
                }
            });
        }
    }

    // 忽略证书
    public static class TrustManagerTest implements X509TrustManager {

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
