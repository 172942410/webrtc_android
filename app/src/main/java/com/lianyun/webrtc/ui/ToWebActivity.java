package com.lianyun.webrtc.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dds.skywebrtc.engine.DataChannelListener;
import com.lianyun.webrtc.ui.adapter.AutoAdapter;
import com.lianyun.webrtc.ui.adapter.MessageAdapter;
import com.perry.App;
import com.perry.core.MainActivity;
import com.perry.core.base.BaseActivity;
import com.perry.core.consts.Urls;
import com.perry.core.socket.IUserState;
import com.perry.core.socket.SocketManager;
import com.perry.core.voip.CallSingleActivity;
import com.lianyun.webrtc.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ToWebActivity extends BaseActivity implements IUserState {
    private static final String TAG = "ToWebActivity";
    private Toolbar toolbar;
    private AppCompatAutoCompleteTextView etAdd;
    private Button button8;
    SharedPreferences sharedPreferences;
    TextView tvInfo;
    EditText etMessage;
    AppCompatButton buttonSend,buttonSendImage,buttonSendCamera;
    SocketManager socketManager;
    RecyclerView recyclerView;
    MessageAdapter messageAdapter;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if(msg.what == 0){
                messageAdapter.notifyDataSetChanged();
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_web);

        initView();

        if (socketManager.getUserState() == 1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initView() {
        sharedPreferences = getSharedPreferences("URL_HOST", Context.MODE_PRIVATE);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar = findViewById(R.id.toolbar);
        etAdd = findViewById(R.id.et_add);
        tvInfo = findViewById(R.id.tv_info);
        button8 = findViewById(R.id.button8);
        etMessage = findViewById(R.id.et_message);
        buttonSend = findViewById(R.id.button_send);
        buttonSendImage = findViewById(R.id.button_send_image);
        buttonSendCamera = findViewById(R.id.button_send_camera);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(ToWebActivity.this));
        messageAdapter = new MessageAdapter(ToWebActivity.this);
        recyclerView.setAdapter(messageAdapter);

        socketManager = SocketManager.getInstance();
        socketManager.setDataChannelListener(new DataChannelListener() {
            @Override
            public void onReceiveBinaryMessage(String socketId, String message) {
                Log.d(TAG,"onReceiveBinaryMessage socketId:"+socketId+",message:"+message);
                messageAdapter.addItemDataPath(message);
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onReceiveMessage(String socketId, String message) {
                Log.d(TAG,"onReceiveMessage socketId:"+socketId+",message:"+message);
                messageAdapter.addItemData(message);
                handler.sendEmptyMessage(0);
            }

            @Override
            public void onReceiveFileProgress(float progress) {
                Log.d(TAG,"onReceiveFileProgress:" + progress);
                messageAdapter.showProgress(progress);
                handler.sendEmptyMessage(0);
            }
        });
        Urls.URL_HOST = sharedPreferences.getString("host",Urls.URL_HOST);
        etAdd.setText(Urls.URL_HOST);
        tvInfo.setText("当前服务器地址:(" + Urls.URL_HOST + ")");
        Observable.<ArrayList>create(new ObservableOnSubscribe<ArrayList>() {
            @Override
            public void subscribe(@io.reactivex.rxjava3.annotations.NonNull ObservableEmitter<ArrayList> emitter) throws Throwable {
                Map map = sharedPreferences.getAll();
                Set set = map.keySet();
                Collection collection = map.values();
                ArrayList<String> arrayList = new ArrayList<>();
                Iterator iterator = collection.iterator();
                while (iterator.hasNext()) {
                    String str = (String) iterator.next();
                    if (!arrayList.contains(str) && str.contains(".")) {
                        arrayList.add(str);
                    }
                }
                Iterator iteratorKey = set.iterator();
                while (iteratorKey.hasNext()) {
                    String str = (String) iteratorKey.next();
//                     && !str.equals("host")
                    if (!arrayList.contains(str) && str.contains(".")) {
                        arrayList.add(str);
                    }
                }
//                AutoAdapter<String> adapter = new AutoAdapter<String>(ToWebActivity.this, R.layout.simple_dropdown_item, arrayList);
//                etAdd.setAdapter(adapter);
                emitter.onNext(arrayList);
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(
                next ->{
                    Log.d(TAG,"next"+next);
                    AutoAdapter<String> adapter = new AutoAdapter<String>(ToWebActivity.this, R.layout.simple_dropdown_item, next);
                    etAdd.setAdapter(adapter);
        },
                error -> {
                    Log.e(TAG,"error:"+error);
        },
                ()->{
                    Log.d(TAG,"complete()");
        }
        );

        etAdd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    try {
                        etAdd.showDropDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "[调试页面][异常]debug页面 inputIpView 异常：" + e.toString());
                    }
                }
            }
        });
        etAdd.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_UP == event.getAction())) {
                    // TODO 此处来点freestyle~ 保存服务器地址了
                    String inputStr = etAdd.getText().toString().trim();
                    if(TextUtils.isEmpty(inputStr)){
                        Toast.makeText(ToWebActivity.this, "服务器地址不能为空", Toast.LENGTH_SHORT).show();
                    }else{
                        sharedPreferences.edit().putString("host", inputStr).putString(System.currentTimeMillis() + "", Urls.URL_HOST).apply();
                        Urls.URL_HOST = inputStr;
                        tvInfo.setText("当前服务器地址:(" + Urls.URL_HOST + ")");
                    }
                }
                return false;
            }
        });

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString().trim();
                if(TextUtils.isEmpty(message)){
                    Toast.makeText(ToWebActivity.this, "发生内容不能为空", Toast.LENGTH_SHORT).show();
                }else{
                    // 发送文本消息
                    socketManager.sendMessage(message);
                }
            }
        });
        buttonSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 发送图片；选择或者照相
                openGallery();
            }
        });
        buttonSendCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO 选择照相
                openCamera();
            }
        });
    }

    public static final int TAKE_CAMERA = 101;
    public static final int PICK_PHOTO = 102;
    private Uri imageUri;

    /**
     * 打开相机
     */
    private Uri openCamera(){
//        // 需要说明一下，以下操作使用照相机拍照，
//        // 拍照后的图片会存放在相册中的,这里使用的这种方式有一个好处就是获取的图片是拍照后的原图，
//        // 如果不实用ContentValues存放照片路径的话，拍照后获取的图片为缩略图不清晰
//        ContentValues values = new ContentValues();
////        java.lang.SecurityException: Permission Denial: writing com.android.providers.media.MediaProvider uri content://media/external/images/media from pid=13680, uid=10137 requires android.permission.WRITE_EXTERNAL_STORAGE, or grantUriPermission()
//        Uri photoUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // 创建File对象，用于存储拍照后的图片
        //存放在手机SD卡的应用关联缓存目录下
        File outputImage = new File(getExternalCacheDir(), System.currentTimeMillis()+".jpg");
               /* 从Android 6.0系统开始，读写SD卡被列为了危险权限，如果将图片存放在SD卡的任何其他目录，
                  都要进行运行时权限处理才行，而使用应用关联 目录则可以跳过这一步
                */
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }else{
                outputImage.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        curImgPath = outputImage.getAbsolutePath();
//        Log.d(TAG,"curImgPath:"+curImgPath);
                /*
                   7.0系统开始，直接使用本地真实路径的Uri被认为是不安全的，会抛 出一个FileUriExposedException异常。
                   而FileProvider则是一种特殊的内容提供器，它使用了和内 容提供器类似的机制来对数据进行保护，
                   可以选择性地将封装过的Uri共享给外部，从而提高了 应用的安全性
                 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //大于等于版本24（7.0）的场合
            String packageStr = getApplicationContext().getPackageName();
            imageUri = FileProvider.getUriForFile(ToWebActivity.this, packageStr + ".provider", outputImage);
        } else {
            //小于android 版本7.0（24）的场合
            imageUri = Uri.fromFile(outputImage);
        }

        //启动相机程序
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //MediaStore.ACTION_IMAGE_CAPTURE = android.media.action.IMAGE_CAPTURE
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_CAMERA);
        return imageUri;
    }
    /**
     * 打开相册
     *
     */
    private void openGallery() {
//        Intent intent = new Intent(Intent.ACTION_PICK);
//        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(intent, PICK_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode : " + requestCode + " , resultCode : " + resultCode + " , data : " + data);
        // 这里没有判断是否匹配，data为空
        if (requestCode == PICK_PHOTO) {
            if(data == null){
                Log.e(TAG,"onActivityResult  接收到的intent为空了 requestCode："+requestCode+"，resultCode："+resultCode);
                return;
            }
            Uri uri = data.getData();
//        Glide.with(this).load(data.getData()).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).into(helpBinding.ivHelpImageFirst);
            messageAdapter.addItemDataUri(uri);
            handler.sendEmptyMessage(0);
        }else if(requestCode == TAKE_CAMERA){
//            curImgPath
//            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(curImgPath));
            messageAdapter.addItemDataUri(imageUri);
            handler.sendEmptyMessage(0);
        }
    }


    public void java(View view) {
        String username = etAdd.getText().toString().trim();
        username = "windows";
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_LONG).show();
            return;
        }

        // 设置用户名
        App.getInstance().setUsername(username);
//        // 添加登录回调
        socketManager.addUserStateCallback(this);
//        // 连接socket:登录
//        SocketManager.getInstance().connect(Urls.WS, username, 0);
        String localPeerId = "windows";
        String remotePeerId = "hololens";
        socketManager.connectHttp(Urls.URL_HOST, localPeerId, remotePeerId);
//        windows hololens
//        CallSingleActivity.openActivity(ToWebActivity.this, "lipengjun", true, "NickName", false, false);
    }

    @Override
    public void userLogin() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void userLogout() {
        Looper.prepare();
        Toast.makeText(this, "userLogout", Toast.LENGTH_SHORT).show();
        Looper.loop();
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            super.onBackPressed();
        }

    }

    /**
     * 1，创建socket链接；获取信息
     */
    public void connectSocket() {
        String host = "ws://ws.xx.com/server/xx";
        URI serverURI = URI.create(host);
        WebSocketClient mWebSocketClient = new WebSocketClient(serverURI) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "socket state connect");
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "web socket onMessage:" + message);
//                try {
//                    TestBean bean =  JSON.parseObject(message,TestBean.class);
//                    if (bean != null && bean.message != null) {
//                        if ("init".equals(bean.message.type)) {
//                            Log.d(TAG, "初始化成功");
//                            if (!mIsReady) {
//                                mIsTerminalEnd = false;
//                                mIsReady = true;
//                                messageHandler.onMessage("ready", id, payload);
//                            }
//                        } else if ("answer".equals(bean.message.type)) {
//                            Log.d(TAG, "收到answer");
//                            if (bean.message.data != null) {
//                                JSONObject payload = new JSONObject();
//                                payload.put("type", bean.message.data.type);
//                                payload.put("description", bean.message.data.description);
//                                messageHandler.onMessage("answer", id, payload);
//                            }
//                        } else if ("candidate".equals(bean.message.type)) {
//                            Log.d(TAG, "收到candidate");
//                            if (bean.message.data != null) {
//                                JSONObject payload = new JSONObject();
//                                payload.put("id", bean.message.data.id);
//                                payload.put("label", bean.message.data.label);
//                                payload.put("candidate", bean.message.data.candidate);
//                                messageHandler.onMessage("candidate", id, payload);
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e(TAG, "WebSocket连接异常：" + e.getLocalizedMessage());
//                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "web socket onClose code:" + code + " reason:" + reason + " remote:" + remote);
            }

            @Override
            public void onError(Exception ex) {
                Log.d(TAG, "web socket onError:" + ex.getLocalizedMessage());
            }
        };
        mWebSocketClient.connect();
    }

    public void callVideo(View view) {
        String username = etAdd.getText().toString().trim();
        username = "windows";
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_LONG).show();
            return;
        }

        // 设置用户名
        App.getInstance().setUsername(username);
//        // 添加登录回调
        socketManager.addUserStateCallback(this);
//        // 连接socket:登录
//        SocketManager.getInstance().connect(Urls.WS, username, 0);
        String localPeerId = "hololens";
        String remotePeerId = "windows";
        socketManager.connectHttp(Urls.URL_HOST, localPeerId,remotePeerId);
//        windows hololens
        CallSingleActivity.openActivity(ToWebActivity.this, "lipengjun", true, "NickName", false, false);
    }
}
