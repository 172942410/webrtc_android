package com.lianyun.webrtc.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.dds.App;
import com.dds.core.MainActivity;
import com.dds.core.base.BaseActivity;
import com.dds.core.consts.Urls;
import com.dds.core.socket.IUserState;
import com.dds.core.socket.SocketManager;
import com.dds.core.voip.CallSingleActivity;
import com.dds.webrtc.R;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.webrtc.PeerConnectionFactory;

import java.net.URI;

public class ToWebActivity extends BaseActivity implements IUserState {
    private static final String TAG = "ToWebActivity";
    private Toolbar toolbar;
    private EditText etUser;
    private Button button8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_to_web);

        initView();

        if (SocketManager.getInstance().getUserState() == 1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar = findViewById(R.id.toolbar);
        etUser = findViewById(R.id.et_user);
        button8 = findViewById(R.id.button8);

        etUser.setText(App.getInstance().getUsername());
    }

    public void java(View view) {
        String username = etUser.getText().toString().trim();
        username = "windows";
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_LONG).show();
            return;
        }

        // 设置用户名
        App.getInstance().setUsername(username);
//        // 添加登录回调
        SocketManager.getInstance().addUserStateCallback(this);
//        // 连接socket:登录
//        SocketManager.getInstance().connect(Urls.WS, username, 0);
        SocketManager.getInstance().connectHttp(Urls.HTTP, username);
//        windows hololens
        CallSingleActivity.openActivity(ToWebActivity.this, "lipengjun", true, "NickName", false, false);
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
    public void connectSocket(){
        String host = "ws://ws.xx.com/server/xx";
        URI serverURI = URI.create(host);
        WebSocketClient mWebSocketClient = new WebSocketClient(serverURI) {
            @Override public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "socket state connect");
            }

            @Override public void onMessage(String message) {
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

            @Override public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "web socket onClose code:" + code + " reason:" + reason + " remote:" + remote);
            }

            @Override public void onError(Exception ex) {
                Log.d(TAG, "web socket onError:" + ex.getLocalizedMessage());
            }
        };
        mWebSocketClient.connect();
    }
}
