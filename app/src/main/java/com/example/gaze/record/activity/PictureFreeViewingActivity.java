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
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.alibaba.fastjson.JSONObject;
import com.example.gaze.record.R;
import com.example.gaze.record.listener.ExperimentListener;
import com.example.gaze.record.motion.MotionListener;
import com.example.gaze.record.utils.CalculatorUtils;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.DataRecordUtils;
import com.example.gaze.record.utils.DataUtils;
import com.example.gaze.record.utils.FileUtil;
import com.example.gaze.record.utils.Landmark;
import com.example.gaze.record.utils.ReceiverUtil;
import com.example.gaze.record.utils.SharedPreferencesUtils;
import com.example.gaze.record.utils.SystemUtil;
import com.example.gaze.record.widget.ExperimentFreeViewingSurfaceView;
import com.google.mediapipe.formats.proto.LandmarkProto;

import org.json.JSONArray;
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

public class PictureFreeViewingActivity extends BaseActivity implements MotionListener, GazeCallback, ExperimentListener, FaceMeshCallback, GazeWithoutFilterCallback {
    private float parentHeight;
    private float parentWidth;
    private ConstraintLayout mConstraintLayout;
    private float xdpi;
    private float ydpi;
    private ReceiverUtil receiverUtil;
    private final String TAG = getClass().getSimpleName();
    private long appearTime = 0L;
    private String userId;

    private final SimpleDateFormat msDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.CHINA);
    private String saveDir;
    private StringBuilder sbTrialInfo;
    private StringBuilder sbBtnData;
    private Handler mHandler;

//    private Button btnVideo;
    private ImageView introIv;
//    boolean isDrawPreparation;
    float prepareInterval;
    ExperimentFreeViewingSurfaceView surfaceView;

    private StringBuffer gazeDataBuffer;
//    private StringBuffer faceMeshBuffer;
//    private boolean isTrialBegin = false;
//    private boolean isTrialOver = false;
    boolean debugAble = SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true);

    private DataRecordUtils faceMeshRecord;
    List<Landmark> landmarks = new ArrayList<>(468);

    @SuppressLint({"HandlerLeak", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_free_viewing);
        OpenCVLoader.initDebug(true);
        mConstraintLayout = findViewById(R.id.constraint_layout);

//        circleView = findViewById(R.id.circle_view);
//        btnVideo = findViewById(R.id.btn_video);
        mConstraintLayout = findViewById(R.id.constraint_layout);
        introIv = findViewById(R.id.intro_iv);
        introIv.setImageResource(R.drawable.intro_fv);

        userId = SharedPreferencesUtils.getString(Constants.USER_ID, "ABC");
        prepareInterval = (float) CalculatorUtils.conversion(
                SharedPreferencesUtils.getString(Constants.STAY_INTERVAL_FREE_VIEWING, "2.0"));
        sbTrialInfo = new StringBuilder();
        sbBtnData = new StringBuilder();
        sbBtnData.append("trial_id").append(",")
                .append("marker_digit").append(",")
                .append("response_digit").append(",")
                .append("response_time").append("\n");

        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 25) {
//                    sbTrialInfo.append("data frame: ").append(msg.obj);
                    sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ")
                            .append(msg.obj.toString().split("\t")[1])
                            .append("\n");
                    sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
                    Log.d(TAG, "收到的数据帧: " + msg.obj + "\n");
                }
            }
        };

        receiverUtil = new ReceiverUtil(PictureFreeViewingActivity.this);
        receiverUtil.setHandler(mHandler);
        long trialSessionTime = System.currentTimeMillis();
        saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fv_" + fileNameDateFormat.format(trialSessionTime);
        FileUtil.createOrExistsDir(saveDir);
        // init surfaceView
        surfaceView = findViewById(R.id.free_viewing_surface_view);
        surfaceView.setSaveDir(saveDir);
        surfaceView.setExperimentListener(this);


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

//        faceMeshBuffer = new StringBuffer();
//        faceMeshBuffer.append("timestamp,face_mesh_json\n");

        faceMeshRecord = new DataRecordUtils(saveDir + "/FaceMeshRecord.csv");
        faceMeshRecord.writeLine("timestamp,face_mesh_json");
        for (int i = 0; i < 468; i++) {
            landmarks.add(new Landmark(0,0,0));

        }
    }

    @Override
    public void onGaze(float[] gazeResult, long timeStamp, boolean isValid) {
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
    public void onFrameImage(Mat mat) {
        mat.release();
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

            GazeTracker.getInstance().addCallBacks(this);
            GazeTracker.getInstance().setFaceMeshCallback(this);
            GazeTracker.getInstance().setGazeWithoutFilterCallback(this);
            GazeTracker.getInstance().startTracker();
        }

    }

    public void tipUpdateDataframe(String dataFrame) {
        // TODO
//        String frame = dataFrame.split("\t")[1];
//        sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ").append(frame).append("\n");
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
    }

    @Override
    public void onMotionEnd() {
//        long stopTime = System.currentTimeMillis();
//        sbTrialInfo.append("Motion offset [phone]: ").append(msDateFormat.format(stopTime)).append("\n");
//        DataUtils.saveTextToPath(saveDir + "/trial_info.txt", sbTrialInfo.toString());
//        DataUtils.saveTextToPath(saveDir + "/exp_btn_data.txt", sbBtnData.toString());
//        btnVideo.post(() -> {
//            btnVideo.setVisibility(View.VISIBLE);
//            btnVideo.setText("退出");
//            btnVideo.setClickable(true);
//        });
//        GazeTracker.getInstance().endTracker();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
}

