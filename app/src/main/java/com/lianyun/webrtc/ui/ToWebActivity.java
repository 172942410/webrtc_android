package com.lianyun.webrtc.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
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
    AppCompatButton buttonSend;
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
                    //TODO 发送文本消息
                    socketManager.sendMessage(message);
                }
            }
        });
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
