///*******************************************************************************
// * Copyright (C) 2022 Gancheng Zhu
// * EasyPsycho is a Free Software project under the GNU Affero General
// * Public License v3, which means all its code is available for everyone
// * to download, examine, use, modify, and distribute, subject to the usual
// * restrictions attached to any GPL software. If you are not familiar with the AGPL,
// * see the COPYING file for for more details on license terms and other legal issues.
// ******************************************************************************/
//
//package com.example.gaze.record.activity;
//
//import android.annotation.SuppressLint;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Message;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.util.TypedValue;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.constraintlayout.widget.ConstraintLayout;
//
//import com.example.gaze.record.R;
//import com.example.gaze.record.app.MyApp;
//import com.example.gaze.record.motion.BaseMotion;
//import com.example.gaze.record.motion.MotionFactory;
//import com.example.gaze.record.motion.MotionListener;
//import com.example.gaze.record.utils.BitmapUtils;
//import com.example.gaze.record.utils.Constants;
//import com.example.gaze.record.utils.DataUtils;
//import com.example.gaze.record.utils.FileUtil;
//import com.example.gaze.record.utils.ReceiverUtil;
//import com.example.gaze.record.utils.SharedPreferencesUtils;
//import com.example.gaze.record.utils.SystemUtil;
//import com.example.gaze.record.utils.UnitUtil;
//import com.example.gaze.record.widget.ScalerMoveView;
//
//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Mat;
//import org.opencv.imgcodecs.Imgcodecs;
//import org.opencv.imgproc.Imgproc;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//import java.util.Queue;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//
//public class MarkMotionActivity extends BaseActivity implements MotionListener{
//    private float parentHeight;
//    private float parentWidth;
//    private ConstraintLayout mConstraintLayout;
//    private float xdpi;
//    private float ydpi;
//    private BaseMotion baseMotion;
//    private ReceiverUtil receiverUtil;
//    private final String TAG = getClass().getSimpleName();
//    //    private long startTime = 0L;
//    private long appearTime = 0L;
//    private String userId;
//    //    private float cameraWidth;
////    private float cameraHeight;
////    private String imageSaveDir;
////    private int imageDirLen;
////    private boolean isPreview = false;
////    ExecutorService cachedThreadPool;
//    private final SimpleDateFormat msDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
//    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.CHINA);
//    //    private long trialStartTime = 0L;
//    private String saveDir;
//    private StringBuffer sbTrialInfo;
//    //    private StringBuilder sbBtnData;
//    private Handler mHandler;
////    private ScalerMoveView circleView;
////    private Button btnVideo;
////    private ImageView introIv;
////    private StringBuilder stringBuilderImgPath;
////    private Bitmap bitmap;
////    private Paint positionPaint;
////    ExecutorService fixedThreadPool;
//
//
//    @SuppressLint({"HandlerLeak", "ClickableViewAccessibility"})
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_marker_motion);
//        OpenCVLoader.initDebug(true);
//
//        mConstraintLayout = findViewById(R.id.constraint_layout);
////        javaCamera2View = findViewById(R.id.java_camera_2_view);
////        javaCamera2View.setCameraPermissionGranted();
////        javaCamera2View.setMaxFrameSize(640, 480);
////        javaCamera2View.setCvCameraViewListener(this);
////        javaCamera2View.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
////        javaCamera2View.setVisibility(View.VISIBLE);
//////        javaCamera2View.enableFpsMeter();
////        javaCamera2View.enableView();
//        mConstraintLayout = findViewById(R.id.constraint_layout);
//        baseMotion = MotionFactory.getInstance(SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth"));
//
////        circleView = findViewById(R.id.circle_view);
////        btnVideo = findViewById(R.id.btn_video);
////        introIv = findViewById(R.id.intro_iv);
////
////        fixedThreadPool = Executors.newFixedThreadPool(4);
////
////        int id;
////        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth")) {
////            case "SmoothPursuit":
////                id = R.drawable.intro_sp;
////                break;
////            case "HorizontalSinusoid":
////            case "VerticalSinusoid":
////                id = R.drawable.intro_hs_vs;
////                break;
////            case "FixationStability":
////                id = R.drawable.intro_fs;
////                break;
////            default:
////                throw new IllegalStateException("Unexpected value: " + SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth"));
////        }
////        introIv.setImageResource(id);
//
////        cachedThreadPool = Executors.newCachedThreadPool();
//
//        if (!SharedPreferencesUtils.getBoolean(Constants.CA_FRAME_STATE, false)) {
////            circleView.setVisibility(View.INVISIBLE);
//        }
//
//        userId = SharedPreferencesUtils.getString(Constants.USER_ID, "ABC");
//        sbTrialInfo = new StringBuffer();
////        sbBtnData = new StringBuilder();
////        sbBtnData.append("trial_id").append(",")
////                .append("marker_digit").append(",")
////                .append("response_digit").append(",")
////                .append("response_time").append("\n");
//        mHandler = new Handler() {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                super.handleMessage(msg);
//                if (msg.what == 25) {
////                    sbTrialInfo.append("data frame: ").append(msg.obj);
//                    sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ")
//                            .append(msg.obj.toString().split("\t")[1])
//                            .append("\n");
//                    sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
//                    Log.i(TAG, "收到的数据帧: " + msg.obj + "\n");
//                }
//            }
//        };
//
//        receiverUtil = new ReceiverUtil(this);
//        receiverUtil.setHandler(mHandler);
//
//        long trialStartTime = System.currentTimeMillis();
//
//        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
//            case "FreeViewing":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fv_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "SmoothPursuit":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/sp_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "HorizontalSinusoid":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/hs_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "VerticalSinusoid":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/vs_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "FixationStability":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fs_" + fileNameDateFormat.format(trialStartTime);
//                break;
//        }
//
//        FileUtil.createOrExistsDir(saveDir);
////        imageSaveDir = saveDir + "/image";
////        FileUtil.createOrExistsDir(imageSaveDir);
////        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
////            receiverUtil.startReceive(saveDir + "/tracker.tsv");
////        }
////        stringBuilderImgPath = new StringBuilder(imageSaveDir);
////        imageDirLen = imageSaveDir.length();
//
////        float scale = SharedPreferencesUtils.getFloat(Constants.SCALE, -1f);
////        float pivotX = SharedPreferencesUtils.getFloat(Constants.PIVOT_X, -1f);
////        float pivotY = SharedPreferencesUtils.getFloat(Constants.PIVOT_Y, -1f);
////        if (pivotY != -1f && pivotX != -1f) circleView.setPivot(pivotX, pivotY);
////        if (scale != -1f) circleView.setScale(scale);
////        circleView.setIsCanTouch(false);
////
////        btnVideo.setOnClickListener(v -> {
////            if ("退出" == btnVideo.getText()) {
////                finish();
////            } else {
////                btnVideo.setVisibility(View.GONE);
////                introIv.setVisibility(View.GONE);
////                baseMotion.setDrawPreparation(true);
////                isPreview = SharedPreferencesUtils.getBoolean(Constants.PREVIEW_STATE, false);
////            }
////        });
//
////        int size = UnitUtil.dip2px(64);
////        bitmap = BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
////                R.drawable.marker);
////        bitmap = BitmapUtils.scaleBitmap(bitmap, size, size);
//
//
////        positionPaint = new Paint();
////        positionPaint.setAntiAlias(true);
////        positionPaint.setDither(true);
////        DisplayMetrics metrics = getResources().getDisplayMetrics();
////        int fontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
////                Integer.parseInt(SharedPreferencesUtils.getString(Constants.DEBUG_FRONT_SIZE, "32")),
////                metrics);
////        positionPaint.setTextSize(fontSize);
////        positionPaint.setColor(Color.WHITE);
////        positionPaint.setStyle(Paint.Style.FILL);
//
//
//    }
//
//
//    Paint markerPaint = new Paint();
//    long nowTime;
//
////    @Override
////    public void drawCustom(Canvas canvas) {
////    }
//
//    boolean isDebug = SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true);
//
////    @Override
////    public void drawCustomBeforeFrame(Canvas canvas) {
////        nowTime = System.nanoTime();
////        baseMotion.drawMotion(canvas, markerPaint, nowTime);
////        if (isDebug && (baseMotion.isDrawPreparation() || baseMotion.isMotion())) {
////            canvas.drawText("X: " + String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosX(baseMotion.getX(), (int) parentWidth)),
////                    parentWidth * 1 / 4, parentHeight * 3 / 4, positionPaint);
////            canvas.drawText(
////                    "Y: " + String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosY(baseMotion.getY(), (int) parentHeight)),
////                    parentWidth * 1 / 4, parentHeight * 3 / 4 + positionPaint.getTextSize() * 1, positionPaint);
////            canvas.drawText(
////                    "Delta Time: " + String.format(Locale.CHINA, "%.4f", baseMotion.getDeltaTime()),
////                    parentWidth * 1 / 4, parentHeight * 3 / 4 + positionPaint.getTextSize() * 2, positionPaint);
////
//////            if (isStartReceivedFrame) {
//////                canvas.drawText("Data Frame: " + dataFrame,
//////                        parentWidth * 1 / 4, parentHeight * 3 / 4 + positionPaint.getTextSize() * 3, positionPaint);
//////            }
////        }
////    }
//
////    @Override
////    public void onCameraViewStarted(int width, int height) {
////        cameraWidth = width;
////        cameraHeight = height;
////        stringPathQueue = new LinkedList<>();
////        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
////            case "FreeViewing":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_FREE_VIEWING, "4"));
////                break;
////            case "SmoothPursuit":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_SMOOTH_PURSUIT, "4"));
////                break;
////            case "HorizontalSinusoid":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_HORIZONTAL_SINUSOID, "4"));
////                break;
////            case "VerticalSinusoid":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_VERTICAL_SINUSOID, "4"));
////                break;
////            case "FixationStability":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_FIXATION_STABILITY, "4"));
////                break;
////        }
////        delayFrameCopy = delayFrame;
////    }
//
////    @Override
////    public void onCameraViewStopped() {
////        javaCamera2View.disableView();
////    }
//
//    int count;
//    boolean isMotionOver;
//    private int delayFrame;
//    private Queue<String> stringPathQueue;
//    private String dataFrame;
//    private boolean isStartReceivedFrame;
//    private boolean needSupplement;
//    private int delayFrameCopy;
//
////    @Override
////    public Mat onCameraFrame(@NonNull CustomCamera2BridgeViewBase.CvCameraViewFrame inputFrame) {
////        Mat mRgba = inputFrame.rgba();
////        if (baseMotion.isMotion()) {
////            generatorSavePath(count++);
////            stringPathQueue.offer(stringBuilderImgPath.toString());
////            if (delayFrame > 0) {
////                generatorSaveNullPath(delayFrameCopy - delayFrame);
////                delayFrame--;
////                saveImgFile(stringBuilderImgPath.toString(), mRgba);
////            }
////        }
////
////        if (stringPathQueue.size() > 0 && delayFrame == 0) {
////            saveImgFile(stringPathQueue.poll(), mRgba);
////            if (stringPathQueue.size() == 0){
////                onMotionEnd();
////            }
////        }
////
////        if (!SharedPreferencesUtils.getBoolean(Constants.PREVIEW_STATE, false)) {
////            return null;
////        } else {
////            Core.flip(mRgba, mRgba, 1);
////            return mRgba;
////        }
////    }
//
//    List<Future<?>> futures = new ArrayList<Future<?>>();
//
//    private void saveImgFile(final String savePath, final Mat mRgba) {
//        final Mat mRgbaTmp = new Mat();
//        Future<?> f = fixedThreadPool.submit(() -> {
//            Imgproc.cvtColor(mRgba, mRgbaTmp, Imgproc.COLOR_RGBA2BGR);
//            Imgcodecs.imwrite(savePath, mRgbaTmp);
//        });
//        futures.add(f);
//    }
//
//    private void generatorSavePath(int count) {
//        initImgDir();
//        stringBuilderImgPath.append("s_")
//                .append(String.format(Locale.CHINA, "%.4f", baseMotion.getDeltaTime()))
//                .append("_n_")
//                .append(String.format(Locale.CHINA, "%05d", count))
//                .append("_X_")
//                .append(String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosX(baseMotion.getX(), (int) parentWidth)))
//                .append("_Y_")
//                .append(String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosY(baseMotion.getY(), (int) parentHeight)));
//        stringBuilderImgPath.append(".jpg");
//    }
//
//
//    private void generatorSaveNullPath(int count) {
//        initImgDir();
//        stringBuilderImgPath.append("buffer_")
//                .append(String.format(Locale.CHINA, "%05d", count));
//        stringBuilderImgPath.append(".jpg");
//    }
//
//    private void initImgDir() {
//        stringBuilderImgPath.setLength(imageDirLen);
//        String tmp = "";
//        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
//            case "FreeViewing":
//                tmp = "/fv";
//                break;
//            case "SmoothPursuit":
//                tmp = "/sp";
//                break;
//            case "HorizontalSinusoid":
//                tmp = "/hs";
//                break;
//            case "VerticalSinusoid":
//                tmp = "/vs";
//                break;
//            case "FixationStability":
//                tmp = "/fs";
//                break;
//        }
//        stringBuilderImgPath.append(tmp).append("_");
//    }
//
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            sbTrialInfo.append("Phone Brand: ").append(SystemUtil.getDeviceBrand()).append("\n")
//                    .append("Phone Model: ").append(SystemUtil.getSystemModel()).append("\n")
//                    .append("Language: ").append(SystemUtil.getSystemLanguage()).append("\n")
//                    .append("Android Version: ").append(SystemUtil.getSystemVersion()).append("\n");
////            preview
//            parentHeight = mConstraintLayout.getHeight();
//            parentWidth = mConstraintLayout.getWidth();
//            DisplayMetrics dm = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(dm);
//            xdpi = dm.xdpi;
//            ydpi = dm.ydpi;
//
//            sbTrialInfo.append("Screen height px [phone]: ").append(parentHeight).append("\n");
//            sbTrialInfo.append("Screen width px [phone]: ").append(parentWidth).append("\n");
//
//            sbTrialInfo.append("Screen height cm [phone]: ").append(parentHeight * 2.54f / ydpi).append("\n");
//            sbTrialInfo.append("Screen width cm [phone]: ").append(parentWidth * 2.54f / xdpi).append("\n");
//
//            baseMotion.setParameter(parentHeight, parentWidth).setListener(this);
//            sbTrialInfo.append("Marker height px [phone]: ").append(baseMotion.getMarkerHeight()).append("\n");
//            sbTrialInfo.append("Marker width px [phone]: ").append(baseMotion.getMarkerWidth()).append("\n");
//        }
//
//    }
//
//    public void tipUpdateDataframe(String dataFrame) {
//        this.dataFrame = dataFrame.split("\t")[1];
////        sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ").append(dataFrame).append("\n");
////        sbTrialInfo.append("Delta Time When Receiving First Frame[phone]: ").append(baseMotion.getDeltaTime()).append("\n");
//        isStartReceivedFrame = true;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
//            if (receiverUtil != null) receiverUtil.stopReceive();
//        }
////        if (javaCamera2View != null) javaCamera2View.disableView();
//    }
//
//    @Override
//    public void onMotionStart() {
//        appearTime = System.currentTimeMillis();
//        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
//            receiverUtil.setGetMassage(true);
//        }
//        sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
//    }
//
//    @Override
//    public void onMotionEnd() {
//        long stopTime = System.currentTimeMillis();
//        sbTrialInfo.append("Motion offset [phone]: ").append(msDateFormat.format(stopTime)).append("\n");
//        sbTrialInfo.append("Image saved count [phone]: ").append(count).append("\n");
//        DataUtils.saveTextToPath(saveDir + "/trial_info.txt", sbTrialInfo.toString());
//        DataUtils.saveTextToPath(saveDir + "/exp_btn_data.txt", sbBtnData.toString());
//        isMotionOver = true;
//        checkingSaveFileState();
////        btnVideo.post(() -> {
////            btnVideo.setVisibility(View.VISIBLE);
////            btnVideo.setText("退出");
////            btnVideo.setClickable(true);
////        });
//    }
//
//    private void checkingSaveFileState() {
//        mHandler.post(() -> {
//            while (true) {
//                boolean allDone = true;
//                for (Future<?> future : futures) {
//                    allDone &= future.isDone(); // check if future is done
//                }
//                if (allDone) break;
//            }
//            btnVideo.post(() -> {
//                btnVideo.setVisibility(View.VISIBLE);
//                btnVideo.setText("退出");
//                btnVideo.setClickable(true);
//            });
//        });
//    }
//
//
////    @Override
////    public float changeScale() {
////        if (!isPreview) return 0;
////        else return Math.min(parentHeight / cameraWidth, parentWidth / cameraHeight);
////    }
////
////    public float getParentHeight() {
////        return parentHeight;
////    }
////
////    public float getParentWidth() {
////        return parentWidth;
////    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
////        if (javaCamera2View != null) {
////            javaCamera2View.disableView();
////        }
//    }
//}
//
///*******************************************************************************
// * Copyright (C) 2022 Gancheng Zhu
// * EasyPsycho is a Free Software project under the GNU Affero General
// * Public License v3, which means all its code is available for everyone
// * to download, examine, use, modify, and distribute, subject to the usual
// * restrictions attached to any GPL software. If you are not familiar with the AGPL,
// * see the COPYING file for for more details on license terms and other legal issues.
// ******************************************************************************/
//
//package com.example.gaze.record.activity;
//
//        import android.annotation.SuppressLint;
//        import android.graphics.Bitmap;
//        import android.graphics.BitmapFactory;
//        import android.graphics.Color;
//        import android.graphics.Paint;
//        import android.os.Bundle;
//        import android.os.Handler;
//        import android.os.Looper;
//        import android.os.Message;
//        import android.util.DisplayMetrics;
//        import android.util.Log;
//        import android.util.TypedValue;
//        import android.view.View;
//        import android.widget.Button;
//        import android.widget.ImageView;
//        import android.widget.Toast;
//
//        import androidx.annotation.NonNull;
//        import androidx.annotation.Nullable;
//        import androidx.constraintlayout.widget.ConstraintLayout;
//
//        import com.example.gaze.record.R;
//        import com.example.gaze.record.app.MyApp;
//        import com.example.gaze.record.motion.BaseMotion;
//        import com.example.gaze.record.motion.MotionFactory;
//        import com.example.gaze.record.motion.MotionListener;
//        import com.example.gaze.record.utils.BitmapUtils;
//        import com.example.gaze.record.utils.Constants;
//        import com.example.gaze.record.utils.DataUtils;
//        import com.example.gaze.record.utils.FileUtil;
//        import com.example.gaze.record.utils.ReceiverUtil;
//        import com.example.gaze.record.utils.SharedPreferencesUtils;
//        import com.example.gaze.record.utils.SystemUtil;
//        import com.example.gaze.record.utils.UnitUtil;
//        import com.example.gaze.record.widget.ScalerMoveView;
//
//        import org.opencv.android.OpenCVLoader;
//        import org.opencv.core.Mat;
//        import org.opencv.imgcodecs.Imgcodecs;
//        import org.opencv.imgproc.Imgproc;
//
//        import java.io.File;
//        import java.io.FileOutputStream;
//        import java.io.IOException;
//        import java.io.OutputStreamWriter;
//        import java.nio.charset.StandardCharsets;
//        import java.text.SimpleDateFormat;
//        import java.util.ArrayList;
//        import java.util.List;
//        import java.util.Locale;
//        import java.util.Queue;
//        import java.util.concurrent.ExecutorService;
//        import java.util.concurrent.Executors;
//        import java.util.concurrent.Future;
//
//public class MarkMotionActivity extends BaseActivity implements MotionListener{
//    private float parentHeight;
//    private float parentWidth;
//    private ConstraintLayout mConstraintLayout;
//    private float xdpi;
//    private float ydpi;
//    private BaseMotion baseMotion;
//    private ReceiverUtil receiverUtil;
//    private final String TAG = getClass().getSimpleName();
//    //    private long startTime = 0L;
//    private long appearTime = 0L;
//    private String userId;
//    //    private float cameraWidth;
////    private float cameraHeight;
////    private String imageSaveDir;
////    private int imageDirLen;
////    private boolean isPreview = false;
////    ExecutorService cachedThreadPool;
//    private final SimpleDateFormat msDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
//    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.CHINA);
//    //    private long trialStartTime = 0L;
//    private String saveDir;
//    private StringBuffer sbTrialInfo;
//    //    private StringBuilder sbBtnData;
//    private Handler mHandler;
////    private ScalerMoveView circleView;
////    private Button btnVideo;
////    private ImageView introIv;
////    private StringBuilder stringBuilderImgPath;
////    private Bitmap bitmap;
////    private Paint positionPaint;
////    ExecutorService fixedThreadPool;
//
//
//    @SuppressLint({"HandlerLeak", "ClickableViewAccessibility"})
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_marker_motion);
//        OpenCVLoader.initDebug(true);
//
//        mConstraintLayout = findViewById(R.id.constraint_layout);
////        javaCamera2View = findViewById(R.id.java_camera_2_view);
////        javaCamera2View.setCameraPermissionGranted();
////        javaCamera2View.setMaxFrameSize(640, 480);
////        javaCamera2View.setCvCameraViewListener(this);
////        javaCamera2View.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
////        javaCamera2View.setVisibility(View.VISIBLE);
//////        javaCamera2View.enableFpsMeter();
////        javaCamera2View.enableView();
//        mConstraintLayout = findViewById(R.id.constraint_layout);
//        baseMotion = MotionFactory.getInstance(SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth"));
//
////        circleView = findViewById(R.id.circle_view);
////        btnVideo = findViewById(R.id.btn_video);
////        introIv = findViewById(R.id.intro_iv);
////
////        fixedThreadPool = Executors.newFixedThreadPool(4);
////
////        int id;
////        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth")) {
////            case "SmoothPursuit":
////                id = R.drawable.intro_sp;
////                break;
////            case "HorizontalSinusoid":
////            case "VerticalSinusoid":
////                id = R.drawable.intro_hs_vs;
////                break;
////            case "FixationStability":
////                id = R.drawable.intro_fs;
////                break;
////            default:
////                throw new IllegalStateException("Unexpected value: " + SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "Smooth"));
////        }
////        introIv.setImageResource(id);
//
////        cachedThreadPool = Executors.newCachedThreadPool();
//
//        if (!SharedPreferencesUtils.getBoolean(Constants.CA_FRAME_STATE, false)) {
////            circleView.setVisibility(View.INVISIBLE);
//        }
//
//        userId = SharedPreferencesUtils.getString(Constants.USER_ID, "ABC");
//        sbTrialInfo = new StringBuffer();
////        sbBtnData = new StringBuilder();
////        sbBtnData.append("trial_id").append(",")
////                .append("marker_digit").append(",")
////                .append("response_digit").append(",")
////                .append("response_time").append("\n");
//        mHandler = new Handler() {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                super.handleMessage(msg);
//                if (msg.what == 25) {
////                    sbTrialInfo.append("data frame: ").append(msg.obj);
//                    sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ")
//                            .append(msg.obj.toString().split("\t")[1])
//                            .append("\n");
//                    sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
//                    Log.i(TAG, "收到的数据帧: " + msg.obj + "\n");
//                }
//            }
//        };
//
//        receiverUtil = new ReceiverUtil(this);
//        receiverUtil.setHandler(mHandler);
//
//        long trialStartTime = System.currentTimeMillis();
//
//        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
//            case "FreeViewing":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fv_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "SmoothPursuit":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/sp_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "HorizontalSinusoid":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/hs_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "VerticalSinusoid":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/vs_" + fileNameDateFormat.format(trialStartTime);
//                break;
//            case "FixationStability":
//                saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/fs_" + fileNameDateFormat.format(trialStartTime);
//                break;
//        }
//
//        FileUtil.createOrExistsDir(saveDir);
////        imageSaveDir = saveDir + "/image";
////        FileUtil.createOrExistsDir(imageSaveDir);
////        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
////            receiverUtil.startReceive(saveDir + "/tracker.tsv");
////        }
////        stringBuilderImgPath = new StringBuilder(imageSaveDir);
////        imageDirLen = imageSaveDir.length();
//
////        float scale = SharedPreferencesUtils.getFloat(Constants.SCALE, -1f);
////        float pivotX = SharedPreferencesUtils.getFloat(Constants.PIVOT_X, -1f);
////        float pivotY = SharedPreferencesUtils.getFloat(Constants.PIVOT_Y, -1f);
////        if (pivotY != -1f && pivotX != -1f) circleView.setPivot(pivotX, pivotY);
////        if (scale != -1f) circleView.setScale(scale);
////        circleView.setIsCanTouch(false);
////
////        btnVideo.setOnClickListener(v -> {
////            if ("退出" == btnVideo.getText()) {
////                finish();
////            } else {
////                btnVideo.setVisibility(View.GONE);
////                introIv.setVisibility(View.GONE);
////                baseMotion.setDrawPreparation(true);
////                isPreview = SharedPreferencesUtils.getBoolean(Constants.PREVIEW_STATE, false);
////            }
////        });
//
////        int size = UnitUtil.dip2px(64);
////        bitmap = BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
////                R.drawable.marker);
////        bitmap = BitmapUtils.scaleBitmap(bitmap, size, size);
//
//
////        positionPaint = new Paint();
////        positionPaint.setAntiAlias(true);
////        positionPaint.setDither(true);
////        DisplayMetrics metrics = getResources().getDisplayMetrics();
////        int fontSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
////                Integer.parseInt(SharedPreferencesUtils.getString(Constants.DEBUG_FRONT_SIZE, "32")),
////                metrics);
////        positionPaint.setTextSize(fontSize);
////        positionPaint.setColor(Color.WHITE);
////        positionPaint.setStyle(Paint.Style.FILL);
//
//
//    }
//
//
//    Paint markerPaint = new Paint();
//    long nowTime;
//
////    @Override
////    public void drawCustom(Canvas canvas) {
////    }
//
//    boolean isDebug = SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true);
//
////    @Override
////    public void drawCustomBeforeFrame(Canvas canvas) {
////        nowTime = System.nanoTime();
////        baseMotion.drawMotion(canvas, markerPaint, nowTime);
////        if (isDebug && (baseMotion.isDrawPreparation() || baseMotion.isMotion())) {
////            canvas.drawText("X: " + String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosX(baseMotion.getX(), (int) parentWidth)),
////                    parentWidth * 1 / 4, parentHeight * 3 / 4, positionPaint);
////            canvas.drawText(
////                    "Y: " + String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosY(baseMotion.getY(), (int) parentHeight)),
////                    parentWidth * 1 / 4, parentHeight * 3 / 4 + positionPaint.getTextSize() * 1, positionPaint);
////            canvas.drawText(
////                    "Delta Time: " + String.format(Locale.CHINA, "%.4f", baseMotion.getDeltaTime()),
////                    parentWidth * 1 / 4, parentHeight * 3 / 4 + positionPaint.getTextSize() * 2, positionPaint);
////
//////            if (isStartReceivedFrame) {
//////                canvas.drawText("Data Frame: " + dataFrame,
//////                        parentWidth * 1 / 4, parentHeight * 3 / 4 + positionPaint.getTextSize() * 3, positionPaint);
//////            }
////        }
////    }
//
////    @Override
////    public void onCameraViewStarted(int width, int height) {
////        cameraWidth = width;
////        cameraHeight = height;
////        stringPathQueue = new LinkedList<>();
////        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
////            case "FreeViewing":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_FREE_VIEWING, "4"));
////                break;
////            case "SmoothPursuit":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_SMOOTH_PURSUIT, "4"));
////                break;
////            case "HorizontalSinusoid":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_HORIZONTAL_SINUSOID, "4"));
////                break;
////            case "VerticalSinusoid":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_VERTICAL_SINUSOID, "4"));
////                break;
////            case "FixationStability":
////                delayFrame = Integer.parseInt(SharedPreferencesUtils.getString(Constants.FRAME_DROP_FIXATION_STABILITY, "4"));
////                break;
////        }
////        delayFrameCopy = delayFrame;
////    }
//
////    @Override
////    public void onCameraViewStopped() {
////        javaCamera2View.disableView();
////    }
//
//    int count;
//    boolean isMotionOver;
//    private int delayFrame;
//    private Queue<String> stringPathQueue;
//    private String dataFrame;
//    private boolean isStartReceivedFrame;
//    private boolean needSupplement;
//    private int delayFrameCopy;
//
////    @Override
////    public Mat onCameraFrame(@NonNull CustomCamera2BridgeViewBase.CvCameraViewFrame inputFrame) {
////        Mat mRgba = inputFrame.rgba();
////        if (baseMotion.isMotion()) {
////            generatorSavePath(count++);
////            stringPathQueue.offer(stringBuilderImgPath.toString());
////            if (delayFrame > 0) {
////                generatorSaveNullPath(delayFrameCopy - delayFrame);
////                delayFrame--;
////                saveImgFile(stringBuilderImgPath.toString(), mRgba);
////            }
////        }
////
////        if (stringPathQueue.size() > 0 && delayFrame == 0) {
////            saveImgFile(stringPathQueue.poll(), mRgba);
////            if (stringPathQueue.size() == 0){
////                onMotionEnd();
////            }
////        }
////
////        if (!SharedPreferencesUtils.getBoolean(Constants.PREVIEW_STATE, false)) {
////            return null;
////        } else {
////            Core.flip(mRgba, mRgba, 1);
////            return mRgba;
////        }
////    }
//
//    List<Future<?>> futures = new ArrayList<Future<?>>();
//
//    private void saveImgFile(final String savePath, final Mat mRgba) {
//        final Mat mRgbaTmp = new Mat();
//        Future<?> f = fixedThreadPool.submit(() -> {
//            Imgproc.cvtColor(mRgba, mRgbaTmp, Imgproc.COLOR_RGBA2BGR);
//            Imgcodecs.imwrite(savePath, mRgbaTmp);
//        });
//        futures.add(f);
//    }
//
//    private void generatorSavePath(int count) {
//        initImgDir();
//        stringBuilderImgPath.append("s_")
//                .append(String.format(Locale.CHINA, "%.4f", baseMotion.getDeltaTime()))
//                .append("_n_")
//                .append(String.format(Locale.CHINA, "%05d", count))
//                .append("_X_")
//                .append(String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosX(baseMotion.getX(), (int) parentWidth)))
//                .append("_Y_")
//                .append(String.format(Locale.CHINA, "%.4f", baseMotion.covertPecPosY(baseMotion.getY(), (int) parentHeight)));
//        stringBuilderImgPath.append(".jpg");
//    }
//
//
//    private void generatorSaveNullPath(int count) {
//        initImgDir();
//        stringBuilderImgPath.append("buffer_")
//                .append(String.format(Locale.CHINA, "%05d", count));
//        stringBuilderImgPath.append(".jpg");
//    }
//
//    private void initImgDir() {
//        stringBuilderImgPath.setLength(imageDirLen);
//        String tmp = "";
//        switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
//            case "FreeViewing":
//                tmp = "/fv";
//                break;
//            case "SmoothPursuit":
//                tmp = "/sp";
//                break;
//            case "HorizontalSinusoid":
//                tmp = "/hs";
//                break;
//            case "VerticalSinusoid":
//                tmp = "/vs";
//                break;
//            case "FixationStability":
//                tmp = "/fs";
//                break;
//        }
//        stringBuilderImgPath.append(tmp).append("_");
//    }
//
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
//            sbTrialInfo.append("Phone Brand: ").append(SystemUtil.getDeviceBrand()).append("\n")
//                    .append("Phone Model: ").append(SystemUtil.getSystemModel()).append("\n")
//                    .append("Language: ").append(SystemUtil.getSystemLanguage()).append("\n")
//                    .append("Android Version: ").append(SystemUtil.getSystemVersion()).append("\n");
////            preview
//            parentHeight = mConstraintLayout.getHeight();
//            parentWidth = mConstraintLayout.getWidth();
//            DisplayMetrics dm = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(dm);
//            xdpi = dm.xdpi;
//            ydpi = dm.ydpi;
//
//            sbTrialInfo.append("Screen height px [phone]: ").append(parentHeight).append("\n");
//            sbTrialInfo.append("Screen width px [phone]: ").append(parentWidth).append("\n");
//
//            sbTrialInfo.append("Screen height cm [phone]: ").append(parentHeight * 2.54f / ydpi).append("\n");
//            sbTrialInfo.append("Screen width cm [phone]: ").append(parentWidth * 2.54f / xdpi).append("\n");
//
//            baseMotion.setParameter(parentHeight, parentWidth).setListener(this);
//            sbTrialInfo.append("Marker height px [phone]: ").append(baseMotion.getMarkerHeight()).append("\n");
//            sbTrialInfo.append("Marker width px [phone]: ").append(baseMotion.getMarkerWidth()).append("\n");
//        }
//
//    }
//
//    public void tipUpdateDataframe(String dataFrame) {
//        this.dataFrame = dataFrame.split("\t")[1];
////        sbTrialInfo.append("First Eye Tracker Frame Received [phone]: ").append(dataFrame).append("\n");
////        sbTrialInfo.append("Delta Time When Receiving First Frame[phone]: ").append(baseMotion.getDeltaTime()).append("\n");
//        isStartReceivedFrame = true;
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
//            if (receiverUtil != null) receiverUtil.stopReceive();
//        }
////        if (javaCamera2View != null) javaCamera2View.disableView();
//    }
//
//    @Override
//    public void onMotionStart() {
//        appearTime = System.currentTimeMillis();
//        if (SharedPreferencesUtils.getBoolean(Constants.COLLECT_UDP_DATA, false)) {
//            receiverUtil.setGetMassage(true);
//        }
//        sbTrialInfo.append("Motion onset [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
//    }
//
//    @Override
//    public void onMotionEnd() {
//        long stopTime = System.currentTimeMillis();
//        sbTrialInfo.append("Motion offset [phone]: ").append(msDateFormat.format(stopTime)).append("\n");
//        sbTrialInfo.append("Image saved count [phone]: ").append(count).append("\n");
//        DataUtils.saveTextToPath(saveDir + "/trial_info.txt", sbTrialInfo.toString());
//        DataUtils.saveTextToPath(saveDir + "/exp_btn_data.txt", sbBtnData.toString());
//        isMotionOver = true;
//        checkingSaveFileState();
////        btnVideo.post(() -> {
////            btnVideo.setVisibility(View.VISIBLE);
////            btnVideo.setText("退出");
////            btnVideo.setClickable(true);
////        });
//    }
//
//    private void checkingSaveFileState() {
//        mHandler.post(() -> {
//            while (true) {
//                boolean allDone = true;
//                for (Future<?> future : futures) {
//                    allDone &= future.isDone(); // check if future is done
//                }
//                if (allDone) break;
//            }
//            btnVideo.post(() -> {
//                btnVideo.setVisibility(View.VISIBLE);
//                btnVideo.setText("退出");
//                btnVideo.setClickable(true);
//            });
//        });
//    }
//
//
////    @Override
////    public float changeScale() {
////        if (!isPreview) return 0;
////        else return Math.min(parentHeight / cameraWidth, parentWidth / cameraHeight);
////    }
////
////    public float getParentHeight() {
////        return parentHeight;
////    }
////
////    public float getParentWidth() {
////        return parentWidth;
////    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
////        if (javaCamera2View != null) {
////            javaCamera2View.disableView();
////        }
//    }
//}
//
