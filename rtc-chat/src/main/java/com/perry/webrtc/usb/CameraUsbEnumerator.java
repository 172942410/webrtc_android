package com.perry.webrtc.usb;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.Range;

import com.llvision.glass3.core.camera.client.CameraException;
import com.llvision.glass3.core.camera.client.ICameraDevice;
import com.llvision.glass3.platform.GlassException;
import com.llvision.glass3.platform.IGlass3Device;
import com.llvision.glass3.platform.LLVisionGlass3SDK;

import org.webrtc.Camera2Capturer;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.Logging;
import org.webrtc.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TargetApi(21)
public class CameraUsbEnumerator implements CameraEnumerator {
    private final static String TAG = "CameraUsbEnumerator";
    private final static double NANO_SECONDS_PER_SECOND = 1.0e9;

    // Each entry contains the supported formats for a given camera index. The formats are enumerated
    // lazily in getSupportedFormats(), and cached for future reference.
    private static final Map<String, List<CaptureFormat>> cachedSupportedFormats =
            new HashMap<String, List<CaptureFormat>>();

    final Context context;
    ICameraDevice mICameraDevice;

    public CameraUsbEnumerator(Context context) {
        this.context = context;
    }

    @Override
    public String[] getDeviceNames() {
        List<IGlass3Device> glass3Devices = null;
        try {
            glass3Devices = LLVisionGlass3SDK.getInstance().getGlass3DeviceList();
        } catch (GlassException e) {
            e.printStackTrace();
        }
        if (glass3Devices == null) {
            return null;
        }
        ArrayList<String> namesList = new ArrayList<>();
        try {
            for (IGlass3Device iGlass3Device : glass3Devices) {
                namesList.add(iGlass3Device.getDeviceInfo().getProductID());
            }
            String[] namesArray = new String[namesList.size()];
            return namesList.toArray(namesArray);
//            return new String[]{mGlass3Device.getDeviceInfo().getProductID()};
        } catch (Exception e) {
            Logging.e(TAG, "Camera access exception: " + e);
            return new String[]{};
        }
    }

    @Override
    public boolean isFrontFacing(String deviceName) {
        //
        return false;
    }

    @Override
    public boolean isBackFacing(String deviceName) {

        return true;
    }

    @Override
    public List<CaptureFormat> getSupportedFormats(String deviceName) {
        synchronized (cachedSupportedFormats) {
            //      final long startTimeMs = SystemClock.elapsedRealtime();
            List<CaptureFormat> formatList = new ArrayList<CaptureFormat>();
            if (mICameraDevice == null) {
                final int maxFps = 120;
                formatList.add(new CaptureFormat(640, 400, 0, maxFps));
                cachedSupportedFormats.put("usb", formatList);
            } else {
                try {
                    List<com.llvision.glass3.library.camera.entity.Size> list = mICameraDevice.getSupportedPreviewSizeList();
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            com.llvision.glass3.library.camera.entity.Size size = list.get(i);
                            for (int j = 0; j < size.intervals.length; j++) {
                                formatList.add(new CaptureFormat(size.width, size.height, 0,(int) Math.floor(size.fps[j])));
                            }
                        }
                    }
                } catch (CameraException e) {
                    e.printStackTrace();
                }
            }
            //      final long endTimeMs = SystemClock.elapsedRealtime();
            return formatList;
        }
    }

    @Override
    public CameraVideoCapturer createCapturer(
            String deviceName, CameraVideoCapturer.CameraEventsHandler eventsHandler) {
        return new CameraUsbCapturer(context, deviceName, eventsHandler);
    }

    static int getFpsUnitFactor(Range<Integer>[] fpsRanges) {
        if (fpsRanges.length == 0) {
            return 1000;
        }
        return fpsRanges[0].getUpper() < 1000 ? 1000 : 1;
    }

    // Convert from android.util.Size to Size.
    private static List<Size> convertSizes(android.util.Size[] cameraSizes) {
        final List<Size> sizes = new ArrayList<Size>();
        for (android.util.Size size : cameraSizes) {
            sizes.add(new Size(size.getWidth(), size.getHeight()));
        }
        return sizes;
    }

    // Convert from android.util.Range<Integer> to CaptureFormat.FramerateRange.
    static List<CaptureFormat.FramerateRange> convertFramerates(Range<Integer>[] arrayRanges, int unitFactor) {
        final List<CaptureFormat.FramerateRange> ranges = new ArrayList<CaptureFormat.FramerateRange>();
        for (Range<Integer> range : arrayRanges) {
            ranges.add(new CaptureFormat.FramerateRange(
                    range.getLower() * unitFactor, range.getUpper() * unitFactor));
        }
        return ranges;
    }
}
