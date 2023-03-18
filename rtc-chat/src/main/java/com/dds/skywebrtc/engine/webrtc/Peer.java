package com.dds.skywebrtc.engine.webrtc;

import android.content.Context;
import android.util.Log;

import com.dds.skywebrtc.engine.DataChannelListener;
import com.dds.skywebrtc.render.ProxyVideoSink;

import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dds on 2020/3/11.
 * android_shuai@163.com
 */
public class Peer implements SdpObserver, PeerConnection.Observer {
    private final static String TAG = "dds_Peer";
    private final PeerConnection pc;
    private final String mUserId;
    private List<IceCandidate> queuedRemoteCandidates;
    private SessionDescription localSdp;
    private final PeerConnectionFactory mFactory;
    private final List<PeerConnection.IceServer> mIceLis;
    private final IPeerEvent mEvent;
    private boolean isOffer;

    public MediaStream _remoteStream;
    public SurfaceViewRenderer renderer;
    public ProxyVideoSink sink;

    DataChannel dataChannel;//发生普通消息需要的
    DataChannel.Init dataChannelInit;
    ArrayList<DataChannelListener> dataChannelListener;

    public Peer(PeerConnectionFactory factory, List<PeerConnection.IceServer> list, String userId, IPeerEvent event) {
        mFactory = factory;
        mIceLis = list;
        mEvent = event;
        mUserId = userId;
        queuedRemoteCandidates = new ArrayList<>();
        this.pc = createPeerConnection();
        Log.d("dds_test", "create Peer:" + mUserId);
        createDataChannel("createDataChannel", pc);
    }

    public PeerConnection createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(mIceLis);
        if (mFactory != null) {
            return mFactory.createPeerConnection(rtcConfig, this);
        } else {
            return null;
        }
    }

    public void setOffer(boolean isOffer) {
        this.isOffer = isOffer;
    }

    // 创建offer
    public void createOffer() {
        if (pc == null) return;
        Log.d("dds_test", "createOffer");
        pc.createOffer(this, offerOrAnswerConstraint());
    }

    // 创建answer
    public void createAnswer() {
        if (pc == null) return;
        Log.d("dds_test", "createAnswer");
        pc.createAnswer(this, offerOrAnswerConstraint());

    }

    // 设置LocalDescription
    public void setLocalDescription(SessionDescription sdp) {
        Log.d("dds_test", "setLocalDescription:"+sdp.description);
        if (pc == null) return;
//        sdp.description = sdp.description + "";
        pc.setLocalDescription(this, sdp);
    }

    // 设置RemoteDescription
    public void setRemoteDescription(SessionDescription sdp) {
        if (pc == null) return;
        Log.d("dds_test", "setRemoteDescription：" + sdp.description);
//        String sdpString = sdp.description;
        String sdpString = sdp.description.replace("webrtc-datachannel 1024","webrtc-datachannel 1024000");
        SessionDescription remoteSdp = new SessionDescription(sdp.type, sdpString);
        pc.setRemoteDescription(this, remoteSdp);
//        pc.setRemoteDescription(this, sdp);
    }

    //添加本地流
    public void addLocalStream(MediaStream stream) {
        if (pc == null) return;
        Log.d("dds_test", "addLocalStream");
        pc.addStream(stream);
    }

    // 添加RemoteIceCandidate
    public synchronized void addRemoteIceCandidate(final IceCandidate candidate) {
        Log.d("dds_test", "addRemoteIceCandidate");
        if (pc != null) {
            if (queuedRemoteCandidates != null) {
                Log.d("dds_test", "addRemoteIceCandidate  2222");
                synchronized (Peer.class) {
                    if (queuedRemoteCandidates != null) {
                        queuedRemoteCandidates.add(candidate);
                    }
                }
            } else {
                Log.d("dds_test", "addRemoteIceCandidate1111");
                pc.addIceCandidate(candidate);
            }
        }
    }

    // 移除RemoteIceCandidates
    public void removeRemoteIceCandidates(final IceCandidate[] candidates) {
        if (pc == null) {
            return;
        }
        drainCandidates();
        pc.removeIceCandidates(candidates);
    }

    public void createRender(EglBase mRootEglBase, Context context, boolean isOverlay) {
        renderer = new SurfaceViewRenderer(context);
        renderer.init(mRootEglBase.getEglBaseContext(), new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {
                Log.d(TAG, "createRender onFirstFrameRendered");

            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
                Log.d(TAG, "createRender onFrameResolutionChanged videoWidth:" + videoWidth + ",videoHeight:" + videoHeight + ",rotation:" + rotation);
            }
        });
//        画幅被裁剪了；显示的太大了
//        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//        renderer.setEnableHardwareScaler(true);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        renderer.setMirror(false);
        renderer.setZOrderMediaOverlay(isOverlay);
        sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        if (_remoteStream != null && _remoteStream.videoTracks.size() > 0) {
            _remoteStream.videoTracks.get(0).addSink(sink);
        }
    }

    // 关闭Peer
    public void close() {
        if (renderer != null) {
            renderer.release();
        }
        if (sink != null) {
            sink.setTarget(null);
        }
        if (pc != null) {
            try {
                pc.close();
            } catch (Exception e) {
                Log.e(TAG, "close: " + e);

            }
        }
    }

    //------------------------------Observer-------------------------------------
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.i(TAG, "onIceConnectionChange: " + newState);
        if (newState == PeerConnection.IceConnectionState.DISCONNECTED || newState == PeerConnection.IceConnectionState.FAILED) {
            mEvent.onDisconnected(mUserId);
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        Log.i(TAG, "onIceConnectionReceivingChange:" + receiving);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        // 检测本地ice状态
        Log.i(TAG, "onIceGatheringChange:" + newState.toString());
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        Log.d(TAG, "获取到 onIceCandidate :" + candidate);
        // 发送IceCandidate
        mEvent.onSendIceCandidate(mUserId, candidate);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.i(TAG, "onIceCandidatesRemoved:");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        Log.i(TAG, "onAddStream:");
        stream.audioTracks.get(0).setEnabled(true);
        _remoteStream = stream;
        if (mEvent != null) {
            mEvent.onRemoteStream(mUserId, stream);
        }
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.i(TAG, "onRemoveStream:");
        if (mEvent != null) {
            mEvent.onRemoveStream(mUserId, stream);
        }
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.i(TAG, "onDataChannel:" + dataChannel);
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded:");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        Log.i(TAG, "onAddTrack:" + mediaStreams.length);
    }


    //-------------SdpObserver--------------------
    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        Log.d(TAG, "sdp创建成功       " + origSdp.type);
//        String sdpString = origSdp.description + "a=max-message-size:262144";
        String sdpString = origSdp.description.replace("webrtc-datachannel 1024","webrtc-datachannel 1024000");
//        String sdpString = origSdp.description;
        final SessionDescription sdp = new SessionDescription(origSdp.type, sdpString);
        localSdp = sdp;
        setLocalDescription(sdp);
    }

    @Override
    public void onSetSuccess() {
        Log.d(TAG, "sdp连接成功   " + pc.signalingState().toString());
        if (pc == null) return;
        // 发送者
        if (isOffer) {
            if (pc.getRemoteDescription() == null) {
                Log.d(TAG, "Local SDP set succesfully");
                if (!isOffer) {
                    //接收者，发送Answer
                    mEvent.onSendAnswer(mUserId, localSdp);
                } else {
                    //发送者,发送自己的offer
                    mEvent.onSendOffer(mUserId, localSdp);
                }
            } else {
                Log.d(TAG, "Remote SDP set succesfully");

                drainCandidates();
            }

        } else {
            if (pc.getLocalDescription() != null) {
                Log.d(TAG, "Local SDP set succesfully");
                if (!isOffer) {
                    //接收者，发送Answer
                    mEvent.onSendAnswer(mUserId, localSdp);
                } else {
                    //发送者,发送自己的offer
                    mEvent.onSendOffer(mUserId, localSdp);
                }

                drainCandidates();
            } else {
                Log.d(TAG, "Remote SDP set succesfully");
            }
        }

    }

    @Override
    public void onCreateFailure(String error) {
        Log.i(TAG, " SdpObserver onCreateFailure:" + error);
    }

    @Override
    public void onSetFailure(String error) {
        Log.i(TAG, "SdpObserver onSetFailure:" + error);
    }


    private void drainCandidates() {
        Log.i("dds_test", "drainCandidates");
        synchronized (Peer.class) {
            if (queuedRemoteCandidates != null) {
                Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
                for (IceCandidate candidate : queuedRemoteCandidates) {
                    pc.addIceCandidate(candidate);
                }
                queuedRemoteCandidates = null;
            }
        }
//        createDataChannel("",pc);
    }

    /**
     * 创建DataChannel
     *
     * @param socketId 用户id
     * @return 数据通道
     */
    public DataChannel createDataChannel(String socketId, PeerConnection peerConnection) {
        if (dataChannelInit == null) {
            /**
             DataChannel.Init 可配参数说明：
             ordered：是否保证顺序传输；
             maxRetransmitTimeMs：重传允许的最长时间；
             maxRetransmits：重传允许的最大次数；
             */
            dataChannelInit = new DataChannel.Init();
            dataChannelInit.ordered = true; // 消息的传递是否有序 true代表有序
            dataChannelInit.negotiated = true; // 协商方式
            dataChannelInit.id = 0; // 通道ID
        }
        if (dataChannel == null) {
            dataChannel = peerConnection.createDataChannel("dataChannel", dataChannelInit);
            //注册DataChannel的回调函数
            dataChannel.registerObserver(new DataChannel.Observer() {
                boolean isHeader = true;
                String suffix = null;
                int fileLength = 0;
                long currentLength = 0;
                boolean isFinish = false;
                List<byte[]> queue = new ArrayList<>();

                //
                @Override
                public void onBufferedAmountChange(long l) {
                    Log.d(TAG, "onBufferedAmountChange:" + l);
                }

                //状态发生改变
                @Override
                public void onStateChange() {
                    Log.d(TAG, "onStateChange:");
                }

                /**
                 * 接收二进制消息时需要定义一个简单的header用于存放文件信息
                 * 1.filename 文件名
                 * 2.suffix  后缀名
                 * 3.totalLength 文件总大小
                 * @param buffer
                 */
                //接收消息
                @Override
                public void onMessage(DataChannel.Buffer buffer) {
                    try {
                        ByteBuffer data = buffer.data;
                        byte[] bytes = new byte[data.capacity()];
                        Log.e(TAG, "initDataChannel----->onMessage--->" + bytes.length);
                        data.get(bytes);
                        if (dataChannelListener != null) {
                            if (buffer.binary) { //是二进制数据
                                for(DataChannelListener listener:dataChannelListener) {
                                    listener.onReceiveBinaryMessage(socketId, "", bytes);
                                }
//                                if (isHeader) {
//                                    isHeader = false;//为false时就不是第一次，只有第一次需要检测文件后缀
//                                    //检测文件后缀
//                                    byte[] headerPayload = new byte[200];
//                                    System.arraycopy(bytes, 0, headerPayload, 0, 200);
////                                    String filePath = ByteUtil.getInstance().getStringFromByteArray(headerPayload);
//                                    byte[] lengthPayload = new byte[200];
//                                    System.arraycopy(bytes, 200, lengthPayload, 0, 20);
////                                    String length = ByteUtil.getInstance().getStringFromByteArray(lengthPayload);
////                                    Log.e(TAG, "initDataChannel----->onMessage--->filePath---->" + filePath);
////                                    Log.e(TAG, "initDataChannel----->onMessage--->length---->" + length);
////                                    suffix = FileUtils.getInstance().getFileSuffix(filePath);
////                                    fileLength = Integer.parseInt(length) - 220;
////                                    Log.e(TAG, "initDataChannel----->onMessage--->suffix---->" + suffix);
//                                }
//                                if (!isHeader) {
//                                    currentLength += bytes.length;
//                                    if ((currentLength - 220) >= fileLength) {
//                                        isFinish = true;
//                                    }
//                                    queue.add(bytes);
//                                    float progress = (currentLength / (float) fileLength) * 100;
//                                    dataChannelListener.onReceiveFileProgress(progress);
//                                }
//                                if (isFinish) {
//                                    String realPath = null;
//                                    queue.remove(0);
////                                    realPath = FileUtils.getInstance().writeBytesToFile(context, suffix, queue);
//                                    if (realPath != null) {
//                                        Log.e(TAG, "initDataChannel----->onMessage--->realPath----> 执行了多少次");
////                                        dataChannelListener.onReceiveBinaryMessage(socketId, realPath);
//                                    }
//                                }
                            } else { //不是二进制数据
                                //此处接收的是非二进制数据
                                String msg = new String(bytes);
                                if(msg.startsWith("imageSection:")){
                                    String section = msg.replace("imageSection:","");
                                    if(section.contains("data:")){
                                        section = section.substring(0,section.indexOf("data:"));
                                    }
                                    if(section.contains(".")){
                                        section = section.substring(0,section.indexOf("."));
                                    }
                                    imageSection = Integer.parseInt(section);
                                }
                                if(imageSection > 0){
                                    if(imageSectionBuilder == null){
                                        imageSectionBuilder = new StringBuilder();
                                    }else {
                                        imageSectionBuilder.append(msg);
                                        imageSection--;
                                        if(imageSection == 0){
                                            for (DataChannelListener listener : dataChannelListener) {
                                                listener.onReceiveMessage(socketId, imageSectionBuilder.toString());
                                            }
                                            imageSectionBuilder = null;
                                        }
                                    }
                                }else {
                                        for (DataChannelListener listener : dataChannelListener) {
                                            listener.onReceiveMessage(socketId, msg);
                                        }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        return dataChannel;
    }

    int imageSection = 0;
    StringBuilder imageSectionBuilder;
    /**
     * 使用DataChannel发送普通消息
     *
     * @param message
     */
    public boolean sendMsg(String message) {
        if (null != message) {
            byte[] msg = message.getBytes();
            return sendMsg(msg, false);
        }
        return false;
    }

    public boolean sendMsg(byte[] message, boolean binary) {
        if (dataChannel != null) {
            if (message != null) {
                DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(message), binary);
                boolean isSend = dataChannel.send(buffer);
                Log.d(TAG, "发送完成：" + isSend);
                for(DataChannelListener listener : dataChannelListener) {
                    listener.onSendResult(isSend, message, binary);
                }
                return isSend;
            }
        } else {
            Log.e(TAG, "发送消息异常");
        }
        return false;
    }

    void setDataChannelListener(ArrayList<DataChannelListener> Listeners) {
        dataChannelListener = Listeners;
    }

    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    // ----------------------------回调-----------------------------------

    public interface IPeerEvent {

        void onSendIceCandidate(String userId, IceCandidate candidate);

        void onSendOffer(String userId, SessionDescription description);

        void onSendAnswer(String userId, SessionDescription description);

        void onRemoteStream(String userId, MediaStream stream);

        void onRemoveStream(String userId, MediaStream stream);

        void onDisconnected(String userId);
    }

}
