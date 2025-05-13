package com.example.gaze.record.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.gaze.record.R;
import com.example.gaze.record.fragment.SettingFragment;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lib.gaze.tracker.core.GazeTracker;
import lib.gaze.tracker.core.GazeTrackerConfig;
import lib.gaze.tracker.enumeration.CalibrationType;
import lib.gaze.tracker.enumeration.FilterType;
import lib.gaze.tracker.enumeration.ValidationType;
import lib.gaze.tracker.util.CommonUtils;

public class SettingActivity extends BaseActivity {

    private ConstraintLayout rootLayout;
    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.CHINA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.setting_fragment, new SettingFragment())
                .commit();



        findViewById(R.id.finish_btn).setOnClickListener(v -> {
            if (v.getId() == R.id.finish_btn) {
                GazeTracker.getInstance().getConfig().userDir = getExternalFilesDir("").getAbsolutePath() + "/" + SharedPreferencesUtils.getString("user_id", "ABC");
                CommonUtils.createOrExistsDir(GazeTracker.getInstance().getConfig().userDir);
                GazeTracker.getInstance().validationSavePath = GazeTracker.getInstance().getConfig().userDir + "/validation_result_" +
                        SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing") + "_" + fileNameDateFormat.format(new Date());

                CommonUtils.createOrExistsDir(GazeTracker.getInstance().validationSavePath);
                Intent intent;
//                if (SharedPreferencesUtils.getBoolean(Constants.DEVICE_DISTANCE, false)) {
//                    intent = new Intent(SettingActivity.this, FaceDistanceActivity.class);
//                } else {
//                    Log.d(TAG, SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing"));
//                    switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
//                        case "FreeViewing":
//                            intent = new Intent(SettingActivity.this, PictureFreeViewingActivity.class);
//                            break;
//                        case "SmoothPursuit":
//                        case "HorizontalSinusoid":
//                        case "VerticalSinusoid":
//                        case "FixationStability":
//                            intent = new Intent(SettingActivity.this, MarkerMotionActivity.class);
//                            break;
//                    }
//
//                }
//                intent = new Intent(this, FaceDistanceActivity.class);
//                startActivity(intent);
                intent = new Intent(this, ShowCalibrationActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onPermissionsDeny() {
        super.onPermissionsDeny();
        System.exit(0);
    }

    @Override
    public void onPermissionsSuccess() {
        super.onPermissionsSuccess();
        initGazeTracker();
    }

    void initGazeTracker() {
        rootLayout = findViewById(R.id.layout_container);
        rootLayout.post(() -> {
            GazeTrackerConfig gazeTrackerConfig = GazeTrackerConfig.createFromSharedPreference(SettingActivity.this, rootLayout.getWidth(), rootLayout.getHeight());
            gazeTrackerConfig.verbose = false;
            gazeTrackerConfig.drawFPS = false;
            gazeTrackerConfig.hiddenValidationErrorBar = true;
            gazeTrackerConfig.enableFilter = true;
            gazeTrackerConfig.filterType = FilterType.HEURISTIC_FILTER;

            //随机点验证，24点
            gazeTrackerConfig.calibrationType = CalibrationType.LISSAJOUS;
            gazeTrackerConfig.validationType = ValidationType.RANDOM_POINT;
            // Marker size
            gazeTrackerConfig.markRadius = 0.5f;

            // gazeTrackerConfig.
            Log.d(TAG,gazeTrackerConfig.toString());
            try {
                GazeTracker.create(gazeTrackerConfig);
            } catch (Exception e) {
                Toast.makeText(this, "GazeTracker初始化失败，请检查设置！", Toast.LENGTH_SHORT).show();
            // jumpToActivity(SettingActivity.class);
            }
            if (!GazeTracker.getInstance().isLoadSuccess()) {
            // jumpToActivity(SettingActivity.class);
                Toast.makeText(this,"GazeTracker初始化成功！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getLayoutId(){
          return R.layout.activity_setting;
    }

}