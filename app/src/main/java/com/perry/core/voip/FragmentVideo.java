package com.perry.core.voip;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.dds.skywebrtc.CallSession;
import com.dds.skywebrtc.EnumType.CallState;
import com.dds.skywebrtc.SkyEngineKit;
import com.dds.skywebrtc.engine.DataChannelListener;
import com.lianyun.webrtc.R;
import com.lianyun.webrtc.utils.Base64Util;
import com.llvision.glass3.core.key.client.IGlassKeyEvent;
import com.llvision.glass3.core.key.client.IKeyEventClient;
import com.llvision.glass3.core.lcd.client.IGlassDisplay;
import com.llvision.glass3.core.lcd.client.ILCDClient;
import com.llvision.glass3.platform.IGlass3Device;
import com.llvision.glass3.platform.LLVisionGlass3SDK;
import com.perry.core.socket.SocketManager;
import com.perry.core.util.BarUtils;
import com.perry.core.util.OSUtils;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;


/**
 * Created by dds on 2018/7/26.
 * android_shuai@163.com
 * 视频通话控制界面
 */
public class FragmentVideo extends SingleCallFragment implements View.OnClickListener {
    private static final String TAG = "FragmentVideo";
    private ImageView outgoingAudioOnlyImageView;
    private LinearLayout audioLayout;
    private ImageView incomingAudioOnlyImageView;
    private LinearLayout hangupLinearLayout;
    private LinearLayout acceptLinearLayout;
    private ImageView connectedAudioOnlyImageView;
    private ImageView connectedHangupImageView;
    private ImageView switchCameraImageView;
    private FrameLayout fullscreenRenderer;
    private FrameLayout pipRenderer;
    private LinearLayout inviteeInfoContainer;
    private boolean isFromFloatingView = false;
    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;
    IGlass3Device mGlass3Device;
    IGlassDisplay iGlassDisplay;//
    IKeyEventClient iKeyEventClient;
    IGlassKeyEvent iGlassKeyEvent;

    SocketManager socketManager;
    ImageView imageView;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                if(msg.obj instanceof Bitmap){
                    try {
                        imageView = new ImageView(getActivity());
                        imageView.setImageBitmap((Bitmap) msg.obj);
                        show2Glass(imageView);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else {

                }
//                messageAdapter.notifyDataSetChanged();
//                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
//                uriLocal = null;
            } else if (msg.what == -1) {
//                Toast.makeText(getActivity(), "发送失败，请检查webRTC链接情况", Toast.LENGTH_SHORT).show();
            } else if (msg.what == -2) {
//                Toast.makeText(getActivity(), "发送失败，请初始化链接情况", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    });

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (callSingleActivity != null) {
            isFromFloatingView = callSingleActivity.isFromFloatingView();
        }
    }

    @Override
    int getLayout() {
        return R.layout.fragment_video;
    }

    @Override
    public void initView(View view) {
        super.initView(view);
        fullscreenRenderer = view.findViewById(R.id.fullscreen_video_view);
        pipRenderer = view.findViewById(R.id.pip_video_view);
        inviteeInfoContainer = view.findViewById(R.id.inviteeInfoContainer);
        outgoingAudioOnlyImageView = view.findViewById(R.id.outgoingAudioOnlyImageView);
        audioLayout = view.findViewById(R.id.audioLayout);
        incomingAudioOnlyImageView = view.findViewById(R.id.incomingAudioOnlyImageView);
        hangupLinearLayout = view.findViewById(R.id.hangupLinearLayout);
        acceptLinearLayout = view.findViewById(R.id.acceptLinearLayout);
        connectedAudioOnlyImageView = view.findViewById(R.id.connectedAudioOnlyImageView);
        connectedHangupImageView = view.findViewById(R.id.connectedHangupImageView);
        switchCameraImageView = view.findViewById(R.id.switchCameraImageView);
        outgoingHangupImageView.setOnClickListener(this);
        incomingHangupImageView.setOnClickListener(this);
        minimizeImageView.setOnClickListener(this);
        connectedHangupImageView.setOnClickListener(this);
        acceptImageView.setOnClickListener(this);
        switchCameraImageView.setOnClickListener(this);
        pipRenderer.setOnClickListener(this);
        outgoingAudioOnlyImageView.setOnClickListener(this);
        incomingAudioOnlyImageView.setOnClickListener(this);
        connectedAudioOnlyImageView.setOnClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || OSUtils.isMiui() || OSUtils.isFlyme()) {
            lytParent.post(() -> {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) inviteeInfoContainer.getLayoutParams();
                params.topMargin = (int) (BarUtils.getStatusBarHeight() * 1.2);
                inviteeInfoContainer.setLayoutParams(params);
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) minimizeImageView.getLayoutParams();
                params1.topMargin = BarUtils.getStatusBarHeight();
                minimizeImageView.setLayoutParams(params1);
            });

            pipRenderer.post(() -> {
                FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) pipRenderer.getLayoutParams();
                params2.topMargin = (int) (BarUtils.getStatusBarHeight() * 1.2);
                pipRenderer.setLayoutParams(params2);
            });
        }
//        if(isOutgoing){ //测试崩溃对方是否会停止
//            lytParent.postDelayed(() -> {
//                int i = 1 / 0;
//            }, 10000);
//        }

    }


    @Override
    public void init() {
        super.init();
        CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            currentState = session.getState();
        }
        if (session == null || CallState.Idle == session.getState()) {
            if (callSingleActivity != null) {
                callSingleActivity.finish();
            }
        } else if (CallState.Connected == session.getState()) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.VISIBLE);
            inviteeInfoContainer.setVisibility(View.GONE);
            minimizeImageView.setVisibility(View.VISIBLE);
            startRefreshTime();
        } else {
            if (isOutgoing) {
                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                connectedActionContainer.setVisibility(View.GONE);
                descTextView.setText(R.string.av_waiting);
            } else {
                incomingActionContainer.setVisibility(View.VISIBLE);
                outgoingActionContainer.setVisibility(View.GONE);
                connectedActionContainer.setVisibility(View.GONE);
                descTextView.setText(R.string.av_video_invite);
                if (currentState == CallState.Incoming) {
                    View surfaceView = gEngineKit.getCurrentSession().setupLocalVideo(false);
                    Log.d(TAG, "init surfaceView != null is " + (surfaceView != null) + "; isOutgoing = " + isOutgoing + "; currentState = " + currentState);
                    if (surfaceView != null) {
                        localSurfaceView = (SurfaceViewRenderer) surfaceView;
                        localSurfaceView.setZOrderMediaOverlay(false);
                        fullscreenRenderer.addView(localSurfaceView);
                    }
                }
            }
        }
        didCreateLocalVideoTrack();
        if (isFromFloatingView) {
            if (session != null) {
                didReceiveRemoteVideoTrack(session.mTargetId);
            }
        }
        try {
            mGlass3Device = LLVisionGlass3SDK.getInstance().getGlass3DeviceList().get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        glassKeyEvent();
        socketManager = SocketManager.getInstance();
        socketManager.addDataChannelListener(dataChannelListener);
    }

    DataChannelListener dataChannelListener =  new DataChannelListener() {
        @Override
        public void onReceiveBinaryMessage(String socketId, String message, byte[] data) {
            Log.d(TAG, "onReceiveBinaryMessage socketId:" + socketId + ",message:" + message);
            Bitmap bitmap = Base64Util.base64ToBitmap(data);
            Message msg = new Message();
            msg.obj = bitmap;
            msg.what = 0;
            handler.sendMessage(msg);
        }

        @Override
        public void onReceiveMessage(String socketId, String message) {
            Log.d(TAG, "onReceiveMessage socketId:" + socketId + ",message:" + message);
            if(message.startsWith("data:image/jpeg;base64,")){
                Bitmap bitmap = Base64Util.base64ToBitmap(message.replace("data:image/jpeg;base64,",""));
                Message msg = new Message();
                msg.obj = bitmap;
                msg.what = 0;
                handler.sendMessage(msg);
            }else{
                handler.sendEmptyMessage(0);
            }
        }

        @Override
        public void onReceiveFileProgress(float progress) {
            Log.d(TAG, "onReceiveFileProgress:" + progress);
            handler.sendEmptyMessage(0);
        }

        @Override
        public void onSendFailed() {
            handler.sendEmptyMessage(-2);
        }

        @Override
        public void onSendResult(boolean isSend, byte[] message, boolean binary) {
            if (isSend) {
                //发送成功
                handler.sendEmptyMessage(0);
            } else {
                handler.sendEmptyMessage(-1);
            }
        }
    };

    @Override
    public void didChangeState(CallState state) {
        currentState = state;
        Log.d(TAG, "didChangeState, state = " + state);
        runOnUiThread(() -> {
            if (state == CallState.Connected) {

                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.GONE);
                connectedActionContainer.setVisibility(View.VISIBLE);
                inviteeInfoContainer.setVisibility(View.GONE);
                descTextView.setVisibility(View.GONE);
                minimizeImageView.setVisibility(View.VISIBLE);
                // 开启计时器
                startRefreshTime();
            } else {
                // do nothing now
            }
        });
    }

    @Override
    public void didChangeMode(Boolean isAudio) {
        runOnUiThread(() -> callSingleActivity.switchAudio());
    }


    @Override
    public void didCreateLocalVideoTrack() {
        if (localSurfaceView == null) {
            View surfaceView = gEngineKit.getCurrentSession().setupLocalVideo(true);
            if (surfaceView != null) {
                localSurfaceView = (SurfaceViewRenderer) surfaceView;
            } else {
                if (callSingleActivity != null) callSingleActivity.finish();
                return;
            }
        } else {
            if (localSurfaceView != null) {
                localSurfaceView.setZOrderMediaOverlay(true);
            }
        }
        Log.d(TAG,
                "didCreateLocalVideoTrack localSurfaceView != null is " + (localSurfaceView != null) + "; remoteSurfaceView == null = " + (remoteSurfaceView == null)
        );
        if (localSurfaceView != null) {
            if (localSurfaceView.getParent() != null) {
                ((ViewGroup) localSurfaceView.getParent()).removeView(localSurfaceView);
            }
            if (isOutgoing && remoteSurfaceView == null) {
                if (fullscreenRenderer != null && fullscreenRenderer.getChildCount() != 0)
                    fullscreenRenderer.removeAllViews();
                fullscreenRenderer.addView(localSurfaceView);
            } else {
                if (pipRenderer.getChildCount() != 0) pipRenderer.removeAllViews();
                pipRenderer.addView(localSurfaceView);
            }
        }
    }


    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        pipRenderer.setVisibility(View.VISIBLE);
        if (localSurfaceView != null) {
            localSurfaceView.setZOrderMediaOverlay(true);
            if (isOutgoing) {
                if (localSurfaceView.getParent() != null) {
                    ((ViewGroup) localSurfaceView.getParent()).removeView(localSurfaceView);
                }
                pipRenderer.addView(localSurfaceView);
            }
        }

        View surfaceView = gEngineKit.getCurrentSession().setupRemoteVideo(userId, false);
        Log.d(TAG, "didReceiveRemoteVideoTrack,surfaceView = " + surfaceView);
        if (surfaceView != null) {
            fullscreenRenderer.setVisibility(View.VISIBLE);
            remoteSurfaceView = (SurfaceViewRenderer) surfaceView;
            remoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            fullscreenRenderer.removeAllViews();
            if (remoteSurfaceView.getParent() != null) {
                ((ViewGroup) remoteSurfaceView.getParent()).removeView(remoteSurfaceView);
            }
            fullscreenRenderer.addView(remoteSurfaceView);
            remoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
//            show2Glass(null);
        }
    }

    /**
     * 眼镜的按键事件监听
     */
    private void glassKeyEvent(){
        if(iKeyEventClient == null && mGlass3Device != null) {
            try {
                iKeyEventClient = (IKeyEventClient) LLVisionGlass3SDK.getInstance().getGlass3Client(IGlass3Device.Glass3DeviceClient.KEY);
                iGlassKeyEvent = iKeyEventClient.getGlassKeyEvent(mGlass3Device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(iGlassKeyEvent != null){
            iGlassKeyEvent.setOnGlxssFnClickListener(new IGlassKeyEvent.OnGlxssClickListener() {
                @Override
                public void onClick(int i) {
//                    如果imageview为空则显示录屏界面；如果存在则显示传递过来的图片
                    show2Glass(imageView);
//                    if(glassView == null){
//                        glassView = fullscreenRenderer;
//                    }else if(glassView != null && glassView == imageView ){
//                        glassView = fullscreenRenderer;
//                    }else if(glassView != null && glassView == pipRenderer ){
//                        glassView = imageView;
//                    }else if(glassView != null && glassView == fullscreenRenderer ){
//                        glassView = pipRenderer;
//                    }
//                    show2Glass(glassView);
                }
            });
            iGlassKeyEvent.setOnGlxssFnDoubleClickListener(new IGlassKeyEvent.OnGlxssDoubleClickListener() {
                @Override
                public void onDoubleClick(int i) {
//                    显示小窗的界面 一般为本地视频界面
                    show2Glass(pipRenderer);
                }
            });
            iGlassKeyEvent.setOnGlxssFnLongClickListener(new IGlassKeyEvent.OnGlxssLongClickListener() {
                @Override
                public void onLongClick(int i) {
//                    显示全屏的界面 一般为远程视频界面
                    show2Glass(fullscreenRenderer);
                }
            });
        }
    }

    /**
     * 显示到眼镜里的界面view
     * @param showView
     */
    private void show2Glass(View showView) {
        if(iGlassDisplay == null && mGlass3Device !=null) {
            try {
                ILCDClient ilcdClient = (ILCDClient) LLVisionGlass3SDK.getInstance().getGlass3Client(IGlass3Device.Glass3DeviceClient.LCD);
                iGlassDisplay = ilcdClient.getGlassDisplay(mGlass3Device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(showView == null) {
            iGlassDisplay.createCaptureScreen(getActivity());
        }else{
            iGlassDisplay.createCaptureScreen(getActivity(),showView);
        }
    }
    @Override
    public void didUserLeave(String userId) {

    }

    @Override
    public void didError(String error) {

    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        // 接听
        CallSession session = gEngineKit.getCurrentSession();
        if (id == R.id.acceptImageView) {
            if (session != null && session.getState() == CallState.Incoming) {
                session.joinHome(session.getRoomId());
            } else if (session != null) {
                if (callSingleActivity != null) {
                    session.sendRefuse();
                    callSingleActivity.finish();
                }
            }
        }
        // 挂断电话
        if (id == R.id.incomingHangupImageView || id == R.id.outgoingHangupImageView || id == R.id.connectedHangupImageView) {
            if (session != null) {
                Log.d(TAG, "endCall");
                SkyEngineKit.Instance().endCall();
            }
            if (callSingleActivity != null) callSingleActivity.finish();
        }

        // 切换摄像头
        if (id == R.id.switchCameraImageView) {
            session.switchCamera();
        }
        if (id == R.id.pip_video_view) {
            boolean isFullScreenRemote = fullscreenRenderer.getChildAt(0) == remoteSurfaceView;
            fullscreenRenderer.removeAllViews();
            pipRenderer.removeAllViews();
            if (isFullScreenRemote) {
                pipRenderer.addView(remoteSurfaceView);
                remoteSurfaceView.setZOrderMediaOverlay(true);
                localSurfaceView.setZOrderMediaOverlay(false);
                fullscreenRenderer.addView(localSurfaceView);
            } else {
                localSurfaceView.setZOrderMediaOverlay(true);
                pipRenderer.addView(localSurfaceView);
                fullscreenRenderer.addView(remoteSurfaceView);
                remoteSurfaceView.setZOrderMediaOverlay(false);
            }
        }

        // 切换到语音拨打
        if (id == R.id.outgoingAudioOnlyImageView || id == R.id.incomingAudioOnlyImageView || id == R.id.connectedAudioOnlyImageView) {
            if (session != null) {
                if (callSingleActivity != null) callSingleActivity.isAudioOnly = true;
                session.switchToAudio();
            }
        }

        // 小窗
        if (id == R.id.minimizeImageView) {
            if (callSingleActivity != null) callSingleActivity.showFloatingView();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fullscreenRenderer.removeAllViews();
        pipRenderer.removeAllViews();
        if(iGlassDisplay != null){
            iGlassDisplay.stopCaptureScreen();
            iGlassDisplay = null;
        }
    }
}