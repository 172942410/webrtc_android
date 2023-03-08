package com.perry.core.base;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.llvision.glass3.platform.base.BasePermissionActivity;
import com.perry.core.util.ActivityStackManager;

public class BaseActivity extends BasePermissionActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 添加Activity到堆栈
        ActivityStackManager.getInstance().onCreated(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPermissionAccepted(boolean isAccepted) {
        Log.d("BaseActivity", "onPermissionAccepted:" + isAccepted);
    }

    @Override
    protected void onDestroy() {
        ActivityStackManager.getInstance().onDestroyed(this);
        super.onDestroy();
    }

}
