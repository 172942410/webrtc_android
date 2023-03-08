/*
 *  Copyright 2016 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.perry.webrtc.usb;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.llvision.glass3.core.camera.client.CameraException;
import com.llvision.glass3.core.camera.client.CameraStatusListener;
import com.llvision.glass3.core.camera.client.ICameraClient;
import com.llvision.glass3.core.camera.client.ICameraDevice;
import com.llvision.glass3.core.camera.client.IFrameCallback;
import com.llvision.glass3.core.camera.client.PixelFormat;
import com.llvision.glass3.library.usb.DeviceFilter;
import com.llvision.glass3.platform.ConnectionStatusListener;
import com.llvision.glass3.platform.IGlass3Device;
import com.llvision.glass3.platform.LLVisionGlass3SDK;
import com.llvision.glxss.common.exception.BaseException;
import com.llvision.glxss.common.utils.LogUtil;

import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;
import org.webrtc.CameraSession;
import org.webrtc.Histogram;
import org.webrtc.Logging;
import org.webrtc.Size;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@TargetApi(21)
class CameraUsbSession implements CameraSession {
    private static final String TAG = "Camera2Session";

    private static final Histogram camera2StartTimeMsHistogram =
            Histogram.createCounts("WebRTC.Android.Camera2.StartTimeMs", 1, 10000, 50);
    private static final Histogram camera2StopTimeMsHistogram =
            Histogram.createCounts("WebRTC.Android.Camera2.StopTimeMs", 1, 10000, 50);
    private static final Histogram camera2ResolutionHistogram = Histogram.createEnumeration(
            "WebRTC.Android.Camera2.Resolution", CameraEnumerationAndroid.COMMON_RESOLUTIONS.size());

    private static enum SessionState {RUNNING, STOPPED}

    private final Handler cameraThreadHandler;
    private final CreateSessionCallback callback;
    private final Events events;
    private final Context applicationContext;
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final String cameraId;
    private final int width;
    private final int height;
    private final int framerate;

    // Initialized at start
    private int cameraOrientation;
    private boolean isCameraFrontFacing;
    private int fpsUnitFactor;
    private CaptureFormat captureFormat;

    // Initialized when camera opens
    private Surface surface;

    // State
    private SessionState state = SessionState.RUNNING;
    private boolean firstFrameReported;

    // Used only for stats. Only used on the camera thread.
    private final long constructionTimeNs; // Construction time of this class.

    private boolean useOldGlass = true;
    ICameraClient mCameraClient;
    ICameraDevice mICameraDevice;
    IGlass3Device mGlass3Device;
    private int[] mFovArray;
    private int mFovIndex = 0;
    static int[] FOV1 = {ICameraDevice.FOV_60, ICameraDevice.FOV_40, ICameraDevice.FOV_30};
    static int[] FOV2 = {ICameraDevice.FOV_101, ICameraDevice.FOV_100, ICameraDevice.FOV_70, ICameraDevice.FOV_50};
    private boolean mHasResolution = false;

    private int mWidth = 1280;
    private int mHeight = 720;
    private int mFps = 30;
    private int mFov = 0;

    private ConnectionStatusListener mConnectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onServiceConnected(List<IGlass3Device> glass3Devices) {
            LogUtil.i(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected() {
            LogUtil.i(TAG, "onServiceDisconnected");
        }

        @Override
        public void onDeviceConnect(IGlass3Device device) {
            //open new camera
            mGlass3Device = device;
            mFovArray = mGlass3Device.getUsbDeviceProductId() >= DeviceFilter.DeviceConnectID.LLV_G40_MAIN_PID ? FOV2 : FOV1;
            mFovIndex = 0;
            openCamera();
            String projectName = null;
            try {
                projectName = mGlass3Device.getFirmwareInfo().projectName;
                checkFirmwareInfo(projectName);
                if (useOldGlass) {

                } else {

                }
            } catch (BaseException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDeviceDisconnect(IGlass3Device device) {
            checkIsOnCameraThread();
            state = SessionState.STOPPED;
            stopInternal();
//                callback.onFailure(FailureType.DISCONNECTED, "Camera disconnected / evicted.");
            events.onCameraDisconnected(CameraUsbSession.this);
            try {
                if (mICameraDevice != null) {
                    mICameraDevice.disconnect();
                    mICameraDevice.release();
                    mICameraDevice = null;
                }
            } catch (CameraException e) {
                e.printStackTrace();
            }

            mGlass3Device = null;
            mHasResolution = false;
        }

        @Override
        public void onError(int code, String msg) {
            LogUtil.i(TAG, "onError code:" + code + ",msg:" + msg);
        }
    };

    CameraStatusListener mCameraStatusListener = new CameraStatusListener() {
        @Override
        public void onCameraOpened() {
            LogUtil.i(TAG, "CameraStatusListener#onCameraOpened");
            try {
                if (!useOldGlass) {
                    mICameraDevice.setPreviewSize(mWidth, mHeight, mFps, mFov);
                } else {
                    mICameraDevice.setPreviewSize(mWidth, mHeight, mFps);
                }
//                mCameraView.setAspectRatio(mWidth / (float) mHeight);
                mICameraDevice.connect();
                if (!mHasResolution) {
//                    initCameraResolutionSpinner();
                }
            } catch (CameraException e) {
                e.printStackTrace();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCameraConnected() {
            LogUtil.i(TAG, "CameraStatusListener#onCameraConnected");
            if (mICameraDevice != null) {
                VideoSink listener = new VideoSink() {
                    @Override
                    public void onFrame(VideoFrame frame) {
                        checkIsOnCameraThread();
                        if (state != SessionState.RUNNING) {
                            Logging.d(TAG, "Texture frame captured but camera is no longer running.");
                            return;
                        }
                        if (!firstFrameReported) {
                            firstFrameReported = true;
                            final int startTimeMs =
                                    (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - constructionTimeNs);
                            camera2StartTimeMsHistogram.addSample(startTimeMs);
                        }
                        final VideoFrame modifiedFrame = new VideoFrame(CameraSession.createTextureBufferWithModifiedTransformMatrix(
                                (TextureBufferImpl) frame.getBuffer(),
                                /* mirror= */ isCameraFrontFacing,
                                /* rotation= */ -cameraOrientation),
                                /* rotation= */ getFrameOrientation(), frame.getTimestampNs());
                        events.onFrameCaptured(CameraUsbSession.this, modifiedFrame);
                        modifiedFrame.release();
                    }
                };

                surfaceTextureHelper.startListening(listener);
                Logging.d(TAG, "Camera device successfully started.");
                callback.onDone(CameraUsbSession.this);

//                checkIsOnCameraThread();
                if(captureFormat == null){
                    captureFormat = new CaptureFormat(1280, 720, 15,30);
                }
                surfaceTextureHelper.setTextureSize(captureFormat.width, captureFormat.height);
                surface = new Surface(surfaceTextureHelper.getSurfaceTexture());
                try {
//                    mICameraDevice.setFrameCallback(new IFrameCallback() {
//                        @Override
//                        public void onFrameAvailable(byte[] bytes) {
//                            LogUtil.w(TAG, "Nv21..." + bytes.length);
//                        }
//                    }, PixelFormat.PIXEL_FORMAT_NV21);
//                    TODO 以下代码是将摄像头画面投射到UI上面了
                    if (surface!= null) {
                        mICameraDevice.addSurface(surface, false);
                    }
                } catch (CameraException e) {
                    e.printStackTrace();
                }

            }
        }

        @Override
        public void onCameraDisconnected() {
            LogUtil.i(TAG, "CameraStatusListener#onCameraDisconnected");
            //close render record
            //停止合成渲染
            try {
                if (mICameraDevice != null) {
                    mICameraDevice.stopRenderCameraStream();
                }
            } catch (CameraException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCameraClosed() {
            LogUtil.i(TAG, "CameraStatusListener#onCameraClosed");
            checkIsOnCameraThread();
            events.onCameraClosed(CameraUsbSession.this);
        }

        @Override
        public void onError(int code) {
            LogUtil.i(TAG, "CameraStatusListener#onError:" + code);
        }
    };

    public static void create(CreateSessionCallback callback, Events events,
                              Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, String cameraId, int width, int height,
                              int framerate) {
        new CameraUsbSession(callback, events, applicationContext, surfaceTextureHelper,
                cameraId, width, height, framerate);
    }

    private CameraUsbSession(CreateSessionCallback callback, Events events, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, String cameraId,
                             int width, int height, int framerate) {
        Logging.d(TAG, "Create new camera2 session on camera " + cameraId);

        constructionTimeNs = System.nanoTime();

        this.cameraThreadHandler = new Handler();
        this.callback = callback;
        this.events = events;
        this.applicationContext = applicationContext;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
        this.framerate = framerate;

        start();
    }

    private void start() {
        checkIsOnCameraThread();
        Logging.d(TAG, "start");

        LLVisionGlass3SDK.getInstance().registerConnectionListener(mConnectionStatusListener);
        try {
            if (LLVisionGlass3SDK.getInstance().isServiceConnected()) {
                List<IGlass3Device> glass3DeviceList = LLVisionGlass3SDK.getInstance().getGlass3DeviceList();
                if (glass3DeviceList != null && glass3DeviceList.size() > 0) {
                    mGlass3Device = glass3DeviceList.get(0);
                    mFovArray = mGlass3Device.getUsbDeviceProductId() >= DeviceFilter.DeviceConnectID.LLV_G40_MAIN_PID ? FOV2 : FOV1;
                    mCameraClient = (ICameraClient) LLVisionGlass3SDK.getInstance().getGlass3Client(IGlass3Device.Glass3DeviceClient.CAMERA);
                    checkFirmwareInfo(mGlass3Device.getFirmwareInfo().projectName);
                    mICameraDevice = mCameraClient.openCamera(mGlass3Device, mCameraStatusListener);
                    findCaptureFormat();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        try {
//            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
//        } catch (final CameraAccessException e) {
//            reportError("getCameraCharacteristics(): " + e.getMessage());
//            return;
//        }
//        cameraOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
//        isCameraFrontFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
//                == CameraMetadata.LENS_FACING_FRONT;

//        openCamera();
//        findCaptureFormat();
    }

    private void openCamera() {
        checkIsOnCameraThread();
        Logging.d(TAG, "Opening camera " + cameraId);
        events.onCameraOpening();
        //TODO 这里是调用后置摄像头的起始位置
        try {
            mGlass3Device = LLVisionGlass3SDK.getInstance().getGlass3DeviceList().get(0);
            mCameraClient = (ICameraClient) LLVisionGlass3SDK.getInstance().getGlass3Client(IGlass3Device.Glass3DeviceClient.CAMERA);
            mICameraDevice = mCameraClient.openCamera(mGlass3Device, mCameraStatusListener);
        } catch (BaseException e) {
            e.printStackTrace();
        }
        findCaptureFormat();
    }

    private void findCaptureFormat() {
        checkIsOnCameraThread();
        if(mICameraDevice == null){
            reportError("mICameraDevice 还没来得及初始化呢.");
            return;
        }
        ArrayList<Range<Integer>> fpsRangeList = new ArrayList<>();
        List<com.llvision.glass3.library.camera.entity.Size> list = null;
        List<Size> sizes = null;
        Range<Integer>[] fpsRanges = null;
        try {
            list = mICameraDevice.getSupportedPreviewSizeList();
            if(list == null){
                reportError("外置相机获取 getSupportedPreviewSizeList 失败.");
                return;
            }
            sizes = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                com.llvision.glass3.library.camera.entity.Size size = list.get(i);
                sizes.add(new Size(size.width, size.height));
                int min = 0;
                int max = 0;
                for (int j = 0; j < size.intervals.length; j++) {
                    int cur = (int) Math.floor(size.fps[j]);
                    if (j == 0) {
                        min = cur;
                        max = cur;
                    } else {
                        if (cur < min) {
                            min = cur;
                        } else if (cur > max) {
                            max = cur;
                        }
                    }
                }
                Range<Integer> range = new Range(min, max);
                fpsRangeList.add(range);
            }
            fpsRanges = new Range[fpsRangeList.size()];
            fpsRangeList.toArray(fpsRanges);
            fpsUnitFactor = CameraUsbEnumerator.getFpsUnitFactor(fpsRanges);
        } catch (CameraException e) {
            e.printStackTrace();
        }
        Logging.d(TAG, "Available preview sizes: " + list);
//        Logging.d(TAG, "Available fps ranges: " + framerateRanges);
        if (list == null || list.isEmpty() || fpsRanges == null) {
            reportError("No supported capture formats.");
            return;
        }

        List<CaptureFormat.FramerateRange> framerateRanges = CameraUsbEnumerator.convertFramerates(fpsRanges, fpsUnitFactor);
        final CaptureFormat.FramerateRange bestFpsRange = CameraEnumerationAndroid.getClosestSupportedFramerateRange(framerateRanges, framerate);

        final Size bestSize = CameraEnumerationAndroid.getClosestSupportedSize(sizes, width, height);
        CameraEnumerationAndroid.reportCameraResolution(camera2ResolutionHistogram, bestSize);

        captureFormat = new CaptureFormat(bestSize.width, bestSize.height, bestFpsRange);
        Logging.d(TAG, "Using capture format: " + captureFormat);
    }

    private void checkFirmwareInfo(String projectName) {
        if (TextUtils.isEmpty(projectName)) {
            useOldGlass = false;
        } else {
            String productid = projectName.toLowerCase();
            if (productid.contains("g26x") || productid.contains("g30") || productid.contains("g40")) {
                useOldGlass = false;
            } else {
                useOldGlass = true;
            }
        }
    }

    @Override
    public void stop() {
        Logging.d(TAG, "Stop camera2 session on camera " + cameraId);
        checkIsOnCameraThread();
        if (state != SessionState.STOPPED) {
            final long stopStartTime = System.nanoTime();
            state = SessionState.STOPPED;
            stopInternal();
            final int stopTimeMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStartTime);
            camera2StopTimeMsHistogram.addSample(stopTimeMs);
        }
    }

    private void stopInternal() {
        Logging.d(TAG, "Stop internal");
        checkIsOnCameraThread();
        surfaceTextureHelper.stopListening();
        if (surface != null) {
            surface.release();
            surface = null;
        }
        Logging.d(TAG, "Stop done");
    }

    private void reportError(String error) {
        checkIsOnCameraThread();
        Logging.e(TAG, "Error: " + error);

        final boolean startFailure = (state != SessionState.STOPPED);
        state = SessionState.STOPPED;
        stopInternal();
        if (startFailure) {
            callback.onFailure(FailureType.ERROR, error);
        } else {
            events.onCameraError(this, error);
        }
    }

    private int getFrameOrientation() {
        int rotation = CameraSession.getDeviceOrientation(applicationContext);
        if (!isCameraFrontFacing) {
            rotation = 360 - rotation;
        }
        return (cameraOrientation + rotation) % 360;
    }

    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("错误的现场；相机线程不在主线程");
        }
    }
}
