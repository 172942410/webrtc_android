package com.perry.core.voip;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.llvision.glass3.core.camera.client.CameraException;
import com.llvision.glass3.core.camera.client.CameraStatusListener;
import com.llvision.glass3.core.camera.client.ICameraClient;
import com.llvision.glass3.core.camera.client.ICameraDevice;
import com.llvision.glass3.platform.IGlass3Device;
import com.llvision.glass3.platform.LLVisionGlass3SDK;
import com.llvision.glxss.common.exception.BaseException;
import com.llvision.glxss.common.ui.SurfaceCallback;
import com.llvision.glxss.common.utils.LogUtil;
import com.perry.core.util.BarUtils;
import com.perry.core.util.OSUtils;
import com.dds.skywebrtc.CallSession;
import com.dds.skywebrtc.EnumType.CallState;
import com.dds.skywebrtc.SkyEngineKit;
import com.lianyun.webrtc.R;
import com.perry.webrtc.usb.TextureViewRenderer;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;

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
    private TextureViewRenderer usbTextureView;
    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;
//    private CameraTextureView cameraTextureView;

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
//        cameraTextureView = view.findViewById(R.id.camera_view);
//        cameraTextureView.setSurfaceCallback(mSurfaceCallback);

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
                        if(surfaceView instanceof TextureViewRenderer){
                            usbTextureView = (TextureViewRenderer) surfaceView;
                            fullscreenRenderer.addView(usbTextureView);
                        }else{
                            localSurfaceView = (SurfaceViewRenderer) surfaceView;
                            localSurfaceView.setZOrderMediaOverlay(false);
                            fullscreenRenderer.addView(localSurfaceView);
                        }
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
    }

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
        if (localSurfaceView == null || usbTextureView == null) {
            View surfaceView = gEngineKit.getCurrentSession().setupLocalVideo(true);
            if (surfaceView != null) {
                if(surfaceView instanceof TextureViewRenderer){
                    usbTextureView = (TextureViewRenderer) surfaceView;
                }else{
                    localSurfaceView = (SurfaceViewRenderer) surfaceView;
                }
            } else {
                if (callSingleActivity != null) callSingleActivity.finish();
                return;
            }
        } else {
            if(localSurfaceView != null) {
                localSurfaceView.setZOrderMediaOverlay(true);
            }
        }
        Log.d(TAG,
                "didCreateLocalVideoTrack localSurfaceView != null is " + (localSurfaceView != null) + "; remoteSurfaceView == null = " + (remoteSurfaceView == null)
        );
        if(usbTextureView != null){
            if (usbTextureView.getParent() != null) {
                ((ViewGroup) usbTextureView.getParent()).removeView(usbTextureView);
            }
            if (isOutgoing && remoteSurfaceView == null) {
                if (fullscreenRenderer != null && fullscreenRenderer.getChildCount() != 0)
                    fullscreenRenderer.removeAllViews();
                fullscreenRenderer.addView(usbTextureView);
            } else {
                if (pipRenderer.getChildCount() != 0) pipRenderer.removeAllViews();
                pipRenderer.addView(usbTextureView);
            }
        }else if(localSurfaceView != null){
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
                remoteSurfaceView.setZOrderMediaOverlay(true);
                pipRenderer.addView(remoteSurfaceView);
                if(usbTextureView != null){
                    fullscreenRenderer.addView(usbTextureView);
                }else{
                    localSurfaceView.setZOrderMediaOverlay(false);
                    fullscreenRenderer.addView(localSurfaceView);
                }
            } else {
                if(usbTextureView != null){
                    pipRenderer.addView(usbTextureView);
                }else{
                    localSurfaceView.setZOrderMediaOverlay(true);
                    pipRenderer.addView(localSurfaceView);
                }
                remoteSurfaceView.setZOrderMediaOverlay(false);
                fullscreenRenderer.addView(remoteSurfaceView);
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

    private IGlass3Device mGlass3Device;
    private ICameraDevice mICameraDevice;
    private ICameraClient mCameraClient;
    /**
     * 打开camera
     */
    @SuppressLint("StringFormatMatches")
    private void openCamera() {
        try {
            if (LLVisionGlass3SDK.getInstance().isServiceConnected()) {
                List<IGlass3Device> glass3DeviceList = LLVisionGlass3SDK.getInstance().getGlass3DeviceList();
                if (glass3DeviceList != null && glass3DeviceList.size() > 0) {
                    mGlass3Device = glass3DeviceList.get(0);
//                    mFovArray = mGlass3Device.getUsbDeviceProductId() >= DeviceFilter.DeviceConnectID.LLV_G40_MAIN_PID ? FOV2 : FOV1;
                    mCameraClient = (ICameraClient) LLVisionGlass3SDK.getInstance().getGlass3Client(IGlass3Device.Glass3DeviceClient.CAMERA);
//                    checkFirmwareInfo(mGlass3Device.getFirmwareInfo().projectName);
                }
            }
            mICameraDevice = mCameraClient.openCamera(mGlass3Device, new CameraStatusListener() {
                @Override
                public void onCameraOpened() {

                }

                @Override
                public void onCameraConnected() {

                }

                @Override
                public void onCameraDisconnected() {

                }

                @Override
                public void onCameraClosed() {

                }

                @Override
                public void onError(int i) {

                }
            });
        } catch (BaseException e) {
            e.printStackTrace();

        }
    }

    private SurfaceCallback mSurfaceCallback = new SurfaceCallback() {
        @Override
        public void onSurfaceCreated(Surface surface) {
            LogUtil.i(TAG, "onSurfaceCreated");
            if (mICameraDevice != null && mICameraDevice.isCameraConnected()) {
                try {
                    mICameraDevice.addSurface(surface, false);
                } catch (CameraException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }

        @Override
        public void onSurfaceChanged(Surface surface, int width, int height) {
            LogUtil.i(TAG, "onSurfaceChanged");

        }

        @Override
        public void onSurfaceDestroy(Surface surface) {
            LogUtil.i(TAG, "onSurfaceDestroy");
            if (mICameraDevice != null) {
                try {
                    mICameraDevice.removeSurface(surface);
                } catch (CameraException e) {
                    LogUtil.e(TAG, e);
                }
            }
        }

        @Override
        public void onSurfaceUpdate(Surface surface) {
//            LogUtil.i(TAG, "onSurfaceUpdate");
        }
    };


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fullscreenRenderer.removeAllViews();
        pipRenderer.removeAllViews();
    }
}