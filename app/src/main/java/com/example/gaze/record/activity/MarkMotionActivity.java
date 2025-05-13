/*******************************************************************************
 * Copyright (C) 2022 Gancheng Zhu
 * EasyPsycho is a Free Software project under the GNU Affero General
 * Public License v3, which means all its code is available for everyone
 * to download, examine, use, modify, and distribute, subject to the usual
 * restrictions attached to any GPL software. If you are not familiar with the AGPL,
 * see the COPYING file for for more details on license terms and other legal issues.
 ******************************************************************************/

package com.example.gaze.record.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.internal.SafeIterableMap;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alibaba.fastjson.JSONObject;
import com.example.gaze.record.R;
import com.example.gaze.record.listener.ExperimentListener;
import com.example.gaze.record.motion.BaseMotion;
import com.example.gaze.record.motion.MotionFactory;
import com.example.gaze.record.motion.MotionListener;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.DataRecordUtils;
import com.example.gaze.record.utils.DataUtils;
import com.example.gaze.record.utils.FileUtil;
import com.example.gaze.record.utils.Landmark;
import com.example.gaze.record.utils.ReceiverUtil;
import com.example.gaze.record.utils.SharedPreferencesUtils;
import com.example.gaze.record.utils.SystemUtil;
import com.example.gaze.record.widget.ExperimentPointMotionSurfaceView;
import com.google.mediapipe.formats.proto.LandmarkProto;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lib.gaze.tracker.callback.FaceMeshCallback;
import lib.gaze.tracker.callback.GazeCallback;
import lib.gaze.tracker.callback.GazeWithoutFilterCallback;
import lib.gaze.tracker.core.GazeTracker;

public class MarkMotionActivity extends BaseActivity implements MotionListener, ExperimentListener, GazeCallback, FaceMeshCallback, GazeWithoutFilterCallback {
    private float parentHeight;
    private float parentWidth;
    private ConstraintLayout mConstraintLayout;
    private float xdpi;
    private float ydpi;
    private BaseMotion baseMotion;
    private ReceiverUtil receiverUtil;
    private final String TAG = getClass().getSimpleName();
//    private long startTime = 0L;
    private long appearTime = 0L;
    private String userId;
    private final SimpleDateFormat msDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.CHINA);
//    private long trialStartTime = 0L;
    private String saveDir;
    private StringBuffer sbTrialInfo;
    private Handler mHandler;
    boolean isDebug = SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true);

    private ExperimentPointMotionSurfaceView surfaceView;
    private StringBuffer gazeDataBuffer;
    private StringBuffer faceMeshBuffer;
    private boolean debugAble = SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true);

    private DataRecordUtils faceMeshRecord;

    List<Landmark> landmarks = new ArrayList<>(468);

    @SuppressLint({"HandlerLeak", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_motion);
        OpenCVLoader.initDebug();

        mConstraintLayout = findViewById(R.id.constraint_layout);
        baseMotion = MotionFactory.getInstance(SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth"));

        userId = SharedPreferencesUtils.getString(Constants.USER_ID, "ABC");
        sbTrialInfo = new StringBuffer();

        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 25) {
                    sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ")
                            .append(msg.obj.toString().split("\t")[1])
                            .append("\n");
                    sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
                    Log.i(TAG, "收到的数据帧: " + msg.obj + "\n");
                }
            }
        };

        receiverUtil = new ReceiverUtil(this);
        receiverUtil.setHandler(mHandler);

        long trialStartTime = System.currentTimeMillis();

        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
            case "FreeViewing":
                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fv_" + fileNameDateFormat.format(trialStartTime);
                break;
            case "SmoothPursuit":
                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/sp_" + fileNameDateFormat.format(trialStartTime);
                break;
            case "HorizontalSinusoid":
                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/hs_" + fileNameDateFormat.format(trialStartTime);
                break;
            case "VerticalSinusoid":
                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/vs_" + fileNameDateFormat.format(trialStartTime);
                break;
            case "FixationStability":
                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fs_" + fileNameDateFormat.format(trialStartTime);
                break;
        }

        FileUtil.createOrExistsDir(saveDir);

        surfaceView = findViewById(R.id.experiment_point_motion_surfaceView);

        // collect eyelink data
        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
            receiverUtil.startReceive(saveDir + "/tracker.tsv");
        }


//        btnVideo.setOnClickListener(v -> {
//            if ("退出".contentEquals(btnVideo.getText())) {
//                finish();
//            } else {
//                btnVideo.setVisibility(View.GONE);
//                introIv.setVisibility(View.GONE);
//                isTrialBegin = false;
//                isDrawPreparation = true;
//                long startTime = System.nanoTime();
//                surfaceView.setStartTime(startTime);
//                onMotionStart();
//            }
//        });

        gazeDataBuffer = new StringBuffer();
        gazeDataBuffer.append("x_raw,y_raw,x_filtered,y_filtered,gaze_timestamp,record_timestamp\n");

        faceMeshRecord = new DataRecordUtils(saveDir + "/FaceMeshRecord.csv");
        faceMeshRecord.writeLine("timestamp,face_mesh_json");
        for (int i = 0; i < 468; i++) {
            landmarks.add(new Landmark(0,0,0));

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            sbTrialInfo.append("Phone Brand: ").append(SystemUtil.getDeviceBrand()).append("\n")
                    .append("Phone Model: ").append(SystemUtil.getSystemModel()).append("\n")
                    .append("Language: ").append(SystemUtil.getSystemLanguage()).append("\n")
                    .append("Android Version: ").append(SystemUtil.getSystemVersion()).append("\n");
//            preview
            parentHeight = mConstraintLayout.getHeight();
            parentWidth = mConstraintLayout.getWidth();
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            xdpi = dm.xdpi;
            ydpi = dm.ydpi;

            sbTrialInfo.append("Screen height px [phone]: ").append(parentHeight).append("\n");
            sbTrialInfo.append("Screen width px [phone]: ").append(parentWidth).append("\n");

            sbTrialInfo.append("Screen height cm [phone]: ").append(parentHeight * 2.54f / ydpi).append("\n");
            sbTrialInfo.append("Screen width cm [phone]: ").append(parentWidth * 2.54f / xdpi).append("\n");

            baseMotion.setParameter(parentHeight, parentWidth).setListener(surfaceView);
            sbTrialInfo.append("Marker height px [phone]: ").append(baseMotion.getMarkerHeight()).append("\n");
            sbTrialInfo.append("Marker width px [phone]: ").append(baseMotion.getMarkerWidth()).append("\n");

            surfaceView.setSaveDir(saveDir);
            surfaceView.setExperimentListener(this);
            surfaceView.setMotionType(SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing"));
            surfaceView.setBaseMotion(baseMotion);

            GazeTracker.getInstance().addCallBacks(this);
            GazeTracker.getInstance().setFaceMeshCallback(this);
            GazeTracker.getInstance().setGazeWithoutFilterCallback(this);
            GazeTracker.getInstance().startTracker();
        }
    }

    public void tipUpdateDataframe(String dataFrame) {
//        this.dataFrame = dataFrame.split("\t")[1];
//        isStartReceivedFrame = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
            if (receiverUtil != null) receiverUtil.stopReceive();
        }
    }

    @Override
    public void onMotionStart() {
        appearTime = System.currentTimeMillis();
        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
            receiverUtil.setGetMassage(true);
        }
        sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
    }

    @Override
    public void onMotionEnd() {
        long stopTime = System.currentTimeMillis();
        sbTrialInfo.append("Motion offset [phone]: ").append(msDateFormat.format(stopTime)).append("\n");
        DataUtils.saveTextToPath(saveDir + "/trial_info.txt", sbTrialInfo.toString());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onGaze(@Nullable float[] gazeResult, long timeStamp, boolean b) {
        if (debugAble) {
            surfaceView.setGaze(gazeResult);
            surfaceView.addTimeStamp(timeStamp);
        }

        if (gazeResult != null) {
            gazeDataBuffer.append(gazeResult[0])
                    .append(",").append(gazeResult[1])
                    .append(",").append(timeStamp).append(",")
                    .append(System.nanoTime()).append("\n");
//            surfaceView.setXY(gazeResult[0], gazeResult[1]);
//            surfaceView.addTimeStamp(timeStamp);
//            Log.d(TAG, "x: " + gazeResult[0] + ", " + "y: " + gazeResult[1]);
        }
        else {
            gazeDataBuffer.append("Nan")
                    .append(",").append("Nan")
                    .append(",").append(timeStamp).append(",")
                    .append(System.nanoTime()).append("\n");
//            surfaceView.setXY(-1, -1);
//            surfaceView.addTimeStamp(timeStamp);
//            Log.d(TAG, "x: Nan" + ", " + "y: Nan");
        }
    }

    @Override
    public void onGaze(@Nullable float[] gazeResult, long timeStamp) {
        if (gazeResult != null) {
            gazeDataBuffer.append(gazeResult[0])
                    .append(",").append(gazeResult[1])
                    .append(",");
//            surfaceView.setXY(gazeResult[0], gazeResult[1]);
//            surfaceView.addTimeStamp(timeStamp);
//            Log.d(TAG, "x: " + gazeResult[0] + ", " + "y: " + gazeResult[1]);
        }
        else {
            gazeDataBuffer.append("Nan")
                    .append(",").append("Nan")
                    .append(",");
//            surfaceView.setXY(-1, -1);
//            surfaceView.addTimeStamp(timeStamp);
//            Log.d(TAG, "x: Nan" + ", " + "y: Nan");
        }
    }

    @Override
    public void onFrameImage(@NonNull Mat mat) {

    }

    @Override
    public void onExpEnd() {
        GazeTracker.getInstance().endTracker();
        surfaceView.setHasGaze(false);
        DataUtils.saveTextToPath(saveDir + "/MobileEyeTrackingRecord.csv", gazeDataBuffer.toString());
        faceMeshRecord.close();
        GazeTracker.getInstance().removeCallBacks(this);
        GazeTracker.getInstance().removeFaceMeshCallback();
        GazeTracker.getInstance().removeGazeWithoutFilterCallback();
//        GazeTracker.getInstance().endTracker();
    }


    @Override
    public void onFaceMeshAvailable(List<LandmarkProto.NormalizedLandmark> list, long timeStamp) {
        if (!list.isEmpty()){
            for (int i = 0; i < list.size(); i++) {
                LandmarkProto.NormalizedLandmark normalizedLandmark = list.get(i);
                Landmark landmark = landmarks.get(i);
                landmark.x = normalizedLandmark.getX();
                landmark.y = normalizedLandmark.getY();
                landmark.z = normalizedLandmark.getZ();
            }
            faceMeshRecord.writeLine(timeStamp + "," + JSONObject.toJSONString(landmarks));
        }else {
            faceMeshRecord.writeLine(timeStamp + ",{}");
        }
    }


}

