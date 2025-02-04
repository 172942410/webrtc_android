package com.lianyun.webrtc.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Base64Util {
    //回收图片所占的内存
    public static void gcBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle(); // 回收图片所占的内存
            bitmap = null;
            System.gc(); // 提醒系统及时回收
        }
    }

    public static String bitmapToBase64Str(Bitmap bitmap) {
        byte[] byteArray = bitmapToBase64(bitmap);
        String result = Base64.encodeToString(byteArray, Base64.DEFAULT);
        return result;
    }
    /**
     *
     * @Title: bitmapToBase64
     * @Description: TODO(Bitmap 转换为字符串)
     * @param @param bitmap
     * @param @return    设定文件
     * @return String    返回类型
     * @throws
     */
    //图片编码
    @SuppressLint("NewApi")
    public static byte[] bitmapToBase64(Bitmap bitmap) {
        // 要返回的字符串
        byte[] byteArray = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                /**
                 * 压缩只对保存有效果bitmap还是原来的大小
                 */
                bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                baos.flush();
                // 转换为字节数组
                byteArray = baos.toByteArray();
                // 转换为字符串
                baos.close();
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return byteArray;
    }

    /**
     *
     * @Title: base64ToBitmap
     * @Description: TODO(base64l转换为Bitmap)
     * @param @param base64String
     * @param @return    设定文件
     * @return Bitmap    返回类型
     * @throws
     */
    //编码转为图片
    public static Bitmap base64ToBitmap(String base64String){
        byte[] decode = Base64.decode(base64String, Base64.DEFAULT);
        return base64ToBitmap(decode);
    }

    public static Bitmap base64ToBitmap(byte[] decode){
        Bitmap bitmap = BitmapFactory.decodeByteArray(decode, 0, decode.length);
        return bitmap;
    }
}
