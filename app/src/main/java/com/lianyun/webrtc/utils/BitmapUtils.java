package com.lianyun.webrtc.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Created by 李鹏军 on 2022/04/15.
 */
public class BitmapUtils {
    private Bitmap mBitmap;
    private final String TAG = "BitmapUtils";

    private static BitmapUtils instance;
    private Context context;
    String URL_HOST = "";
    private BitmapUtils() {
    }

    public static BitmapUtils getInstance() {
        if (instance == null) {
            synchronized (BitmapUtils.class) {
                if (instance == null) {
                    instance = new BitmapUtils();
                }
            }
        }
        return instance;
    }

    public BitmapUtils setContext(Context context) {
        if(context != null) {
            this.context = context;
        }
        return this;
    }

    public Bitmap getBitmap(String fileName) {
        if(context == null){
            Log.d(TAG,"读取图片时发现 context 上下文为空异常");
            return null;
        }
        if(TextUtils.isEmpty(fileName)){
            Log.d(TAG,"读取图片时发现传入文件名称为空");
            return null;
        }
        File dirFile = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String[] urls = fileName.split("/");
        String name = urls[urls.length-1];
        File bitmapFile = new File(dirFile,name);
        if(bitmapFile.exists()){
            return BitmapFactory.decodeFile(dirFile+"/"+name);
        }else{
            try {
                //TODO 这里是这个图片恶意删除后需要再次去服务器请求该图片
                downloadSync(URL_HOST + "/" + fileName);
            }catch (Exception e){
                e.printStackTrace();
            }
            return mBitmap;
        }
    }
    /**
     * 异步下载；不用再rx中使用
     * @param url
     */
    public void download(String url) {
        if(context == null){
            Log.d(TAG, "请求图片路径时 context 环境为空了");
            return;
        }
        if (TextUtils.isEmpty(url)) {
            Log.d(TAG, "请求图片路径地址不能为空");
            return;
        }
        if (!url.startsWith("http")) {
            Log.d(TAG, "请求图片路径地址非法：" + url);
            return;
        }
        Runnable saveFileRunnable = new SaveFileRunnable(url);
        ThreadPoolUtil.executeAlone(saveFileRunnable);
    }

    /**
     * 同步下载；可以直接在rx中使用
     * @param imageUrl
     */
    public void downloadSync(String imageUrl) throws IOException {
        if (!TextUtils.isEmpty(imageUrl)) {
            Log.d(TAG, "准备下载图片中..." + imageUrl);
            String[] urls = imageUrl.split("/");
            String fileName = urls[urls.length - 1];
            // 对资源链接
            URL url = new URL(imageUrl);
            //打开输入流
            InputStream inputStream = url.openStream();
            //对网上资源进行下载转换位图图片
            mBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            saveFile(mBitmap, fileName);
        }
    }

    /**
     * 异步加载图片到ImageView
     * @param imageView
     * @param fileId
     */
    public void bind(ImageView imageView, String fileId) {
        bind(imageView,fileId,null);
    }

    public void bind(ImageView imageView, String fileId,ImageView.ScaleType scaleType) {
        Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Bitmap> emitter) throws Throwable {
                Bitmap bitmap = getBitmap(fileId);
                if(bitmap == null){
                    emitter.onError(new Throwable("获取图片为空："+fileId+"，需要再次同步"));
                }else{
                    emitter.onNext(bitmap);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())//调用它之后代码的所在线程;
                .subscribe(
                        onNext-> {
                            if (scaleType != null) {
                                imageView.setScaleType(scaleType);
                            }
                            imageView.setImageBitmap(onNext);
                        },
                        onError -> {
                            Log.d(TAG,"onError:"+onError);
                            Toast.makeText(context, onError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                );

    }

    /**
     * 检查图片是否存在
     * @param fileId
     * @return
     */
    public boolean check(String fileId){
        File dirFile = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String[] urls = fileId.split("/");
        String name  = urls[urls.length - 1];
        File bitmapFile = new File(dirFile,name);
        if(bitmapFile.exists()) {
            return true;
        }
        return false;
    }
    /**
     * 删除图片
     * @param fileId 删除图片所需要的名称
     */
    public boolean delete(String fileId) {
        File dirFile = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String[] urls = fileId.split("/");
        String name  = urls[urls.length - 1];
        File bitmapFile = new File(dirFile,name);
        if(bitmapFile.exists()) {
            return bitmapFile.delete();
        }else{
            Log.d(TAG,"需要删除的bitmap本身就不存在 fileId："+fileId);
        }
        return false;
    }

    private class SaveFileRunnable implements Runnable{

        String imageUrl;
        SaveFileRunnable(String url){
            imageUrl = url;
        }
        @Override
        public void run() {
            try {
                downloadSync(imageUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 保存图片
     *
     * @param bm
     * @throws IOException
     */
    public void saveFile(Bitmap bm,String fileName) throws IOException {
        File dirFile = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//        File bitmapPath = new File(dirFile,"/bitmap");
        if (!dirFile.exists()) {
            dirFile.mkdir();
        }
        File bitmapFile = new File(dirFile,fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(bitmapFile));
        bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
        bos.flush();
        bos.close();
    }

}
