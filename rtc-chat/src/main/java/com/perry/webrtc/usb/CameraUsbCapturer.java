package com.perry.webrtc.usb;

import android.annotation.TargetApi;
import android.content.Context;

import com.llvision.glass3.core.camera.client.ICameraClient;
import com.llvision.glass3.platform.IGlass3Device;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraCapturer;
import org.webrtc.CameraSession;
import org.webrtc.SurfaceTextureHelper;

@TargetApi(21)
public class CameraUsbCapturer extends CameraCapturer {
  private final Context context;

  public CameraUsbCapturer(Context context, String cameraName, CameraEventsHandler eventsHandler) {
    super(cameraName, eventsHandler, new CameraUsbEnumerator(context));
    this.context = context;
  }

  @Override
  protected void createCameraSession(CameraSession.CreateSessionCallback createSessionCallback,
                                     CameraSession.Events events, Context applicationContext,
                                     SurfaceTextureHelper surfaceTextureHelper, String cameraName, int width, int height,
                                     int framerate) {
    CameraUsbSession.create(createSessionCallback, events, applicationContext,
        surfaceTextureHelper, cameraName, width, height, framerate);
  }
}
