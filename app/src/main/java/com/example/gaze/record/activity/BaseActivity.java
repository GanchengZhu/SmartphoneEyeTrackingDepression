package com.example.gaze.record.activity;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gaze.record.app.MyApp;
import com.example.gaze.record.common.helpers.CameraPermissionHelper;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;

public class BaseActivity extends AppCompatActivity {
    protected final String TAG = getClass().getSimpleName();

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) {
            View v = getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
        if (SharedPreferencesUtils.getBoolean(Constants.IS_PORTRAIT, true)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (getLayoutId() != 0) {
            setContentView(getLayoutId());
        }
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }else{
            onPermissionsSuccess();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if (!CameraPermissionHelper.hasCameraPermission(this)) {
                Toast.makeText(MyApp.getInstance(), "请您打开必要权限", Toast.LENGTH_SHORT).show();
                onPermissionsDeny();
                finish();
            } else {
                onPermissionsSuccess();
            }
        }
    }

    public void onPermissionsSuccess() {

    }

    public void onPermissionsDeny() {

    }

    public void tipUpdateDataframe(String toString) {
    }

    public int getLayoutId(){
        return 0;
    }
}
