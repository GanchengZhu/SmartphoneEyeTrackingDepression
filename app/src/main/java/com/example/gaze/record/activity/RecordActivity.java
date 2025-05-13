//package com.example.gaze.record.activity;
//
//
//import android.animation.ValueAnimator;
//import android.annotation.SuppressLint;
//import android.graphics.Bitmap;
//import android.graphics.Matrix;
//import android.media.MediaPlayer;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Message;
//import android.os.SystemClock;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.util.Size;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.camera.core.CameraSelector;
//import androidx.camera.core.Preview;
//import androidx.camera.lifecycle.ProcessCameraProvider;
//import androidx.camera.view.PreviewView;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.core.content.ContextCompat;
//
//import com.example.gaze.record.R;
//import com.example.gaze.record.common.helpers.CameraPermissionHelper;
//import com.example.gaze.record.utils.Constants;
//import com.example.gaze.record.utils.Defaults;
//import com.example.gaze.record.utils.FileUtil;
//import com.example.gaze.record.utils.ImgParseUtils;
//import com.example.gaze.record.utils.ReceiverUtil;
//import com.example.gaze.record.utils.SharedPreferencesUtils;
//import com.example.gaze.record.utils.SystemUtil;
//import com.example.gaze.record.widget.FixationAnimation;
//import com.example.gaze.record.widget.ScalerMoveView;
//import com.google.common.util.concurrent.ListenableFuture;
//import com.kongzue.dialogx.dialogs.TipDialog;
//import com.kongzue.dialogx.dialogs.WaitDialog;
//
//import org.opencv.android.OpenCVLoader;
//import org.opencv.android.Utils;
//import org.opencv.core.Mat;
//import org.opencv.videoio.VideoCapture;
//import org.opencv.videoio.Videoio;
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.lang.reflect.Field;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.Locale;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Executors;
//
//public class RecordActivity extends BaseActivity {
//
//    private boolean isRecording = false;
//    private ReceiverUtil receiverUtil;
//    private final String TAG = getClass().getSimpleName();
//    private long startTime = 0L;
//    private long appearTime = 0L;
//    private androidx.camera.core.Camera camera;
//    private ProcessCameraProvider cameraProvider;
//    private Preview preview;
//    private androidx.camera.core.VideoCapture videoCapture;
//    private String userId;
//    private PreviewView viewFinder;
//
//    private long openCvStart;
//    private long ffmpegStart;
//    private float duration;
//
//    public float getParentHeight() {
//        return parentHeight;
//    }
//    public float getParentWidth() {
//        return parentWidth;
//    }
//
//    private float parentHeight;
//    private float parentWidth;
//
//    private float markerHeightHalf;
//    private float markerWidthHalf;
//
//
//    // Selector showing which camera is selected (front or back)
//    private final CameraSelector lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA;
//    private final SimpleDateFormat msDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
//    private final SimpleDateFormat tipDateFormat = new SimpleDateFormat("HH_mm_ss_SSS", Locale.CHINA);
//    private final SimpleDateFormat fileNameDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm", Locale.CHINA);
//    private long trialStartTime = 0L;
//    private String saveDir;
//    private StringBuilder sbTrialInfo;
//    private Handler mHandler;
//    private ScalerMoveView circleView;
//    private Button btnVideo;
//    private TextView tipTv1;
//    private TextView tipTv2;
//    private TextView tipTv3;
//    private TextView tipTv4;
//    private ImageView imageView;
//    private TipRunnable tipThread;
//    private FixationAnimation fixationAnimation;
//    private ConstraintLayout mConstraintLayout;
//    private ImageView introIv;
//
//    @SuppressLint("HandlerLeak")
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        OpenCVLoader.initDebug();
//        setContentView(R.layout.activity_record);
//        circleView = findViewById(R.id.circle_view);
//        btnVideo = findViewById(R.id.btn_video);
//        tipTv1 = findViewById(R.id.tip_tv_1);
//        tipTv2 = findViewById(R.id.tip_tv_2);
//        tipTv3 = findViewById(R.id.tip_tv_3);
//        tipTv4 = findViewById(R.id.tip_tv_4);
//        imageView = findViewById(R.id.main_image2);
//        viewFinder = findViewById(R.id.view_finder);
//        mConstraintLayout = findViewById(R.id.constraint_layout);
//        introIv = findViewById(R.id.intro_iv);
////        maskView = findViewById(R.id.mask_preview_view);
//
//        if (!SharedPreferencesUtils.getBoolean(Constants.CA_FRAME_STATE, false)) {
//            circleView.setVisibility(View.INVISIBLE);
//        }
//
//        if (!SharedPreferencesUtils.getBoolean(Constants.PREVIEW_STATE, false)) {
////            maskView.setVisibility(View.VISIBLE);
//            viewFinder.setVisibility(View.INVISIBLE);
//        }
//
//
//        userId = SharedPreferencesUtils.getString(Constants.USER_ID, "ABC");
//        sbTrialInfo = new StringBuilder();
//        mHandler = new Handler() {
//            @Override
//            public void handleMessage(@NonNull Message msg) {
//                super.handleMessage(msg);
//                if (msg.what == 25) {
//                    sbTrialInfo.append("data frame: ").append(msg.obj);
//                    Log.i(TAG, "收到的数据帧: " + msg.obj + "\n");
//                }
//            }
//        };
//        receiverUtil = new ReceiverUtil(this);
//        receiverUtil.setHandler(mHandler);
//        trialStartTime = System.currentTimeMillis();
//        saveDir = getExternalFilesDir("").getAbsolutePath() + "/" + userId + "/" + fileNameDateFormat.format(trialStartTime);
//        FileUtil.createOrExistsDir(saveDir);
//        receiverUtil.startReceive(saveDir + "/tracker.tsv");
//
//        float scale = SharedPreferencesUtils.getFloat(Constants.SCALE, -1f);
//        float pivotX = SharedPreferencesUtils.getFloat(Constants.PIVOT_X, -1f);
//        float pivotY = SharedPreferencesUtils.getFloat(Constants.PIVOT_Y, -1f);
//        if (pivotY != -1f && pivotX != -1f) circleView.setPivot(pivotX, pivotY);
//        if (scale != -1f) circleView.setScale(scale);
//        circleView.setIsCanTouch(false);
//
//
//        btnVideo.setOnClickListener(v -> {
//            if ("退出" == btnVideo.getText()) {
//                finish();
//            } else {
//                imageView.setVisibility(View.VISIBLE);
//                btnVideo.setVisibility(View.GONE);
//                introIv.setVisibility(View.GONE);
//                recordVideo();
//                mHandler.postAtTime(() -> {
//                    fixationAnimation.start();
//                }, SystemClock.uptimeMillis() + 3000);
//            }
//        });
//
//        if (!SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true)) {
//            tipTv1.setVisibility(View.INVISIBLE);
//            tipTv2.setVisibility(View.INVISIBLE);
//            tipTv3.setVisibility(View.INVISIBLE);
//            tipTv4.setVisibility(View.INVISIBLE);
//        }
//        int frontSize = Integer.parseInt(SharedPreferencesUtils.getString(Constants.DEBUG_FRONT_SIZE, "32"));
//        tipTv1.setTextSize(frontSize);
//        tipTv2.setTextSize(frontSize);
//        tipTv3.setTextSize(frontSize);
//        tipTv4.setTextSize(frontSize);
//        imageView.setVisibility(View.INVISIBLE);
//    }
//
//    public void onTransitionStarted() {
//        appearTime = System.currentTimeMillis();
//        sbTrialInfo.append("Button press time [phone]: ").append(msDateFormat.format(startTime)).append("\n");
//        receiverUtil.setGetMassage(true);
//        sbTrialInfo.append("Target onset time [phone]: ").append(msDateFormat.format(appearTime)).append("\n");
//        tipUpdateInterval();
//    }
//
//
//    @SuppressLint("RestrictedApi")
//    public void onTransitionCompleted() {
//        imageView.setVisibility(View.INVISIBLE);
//        mHandler.postAtTime(() -> {
//            long stopTime = System.currentTimeMillis();
//            videoCapture.stopRecording();
//            sbTrialInfo.append("Video stop time [phone]: ").append(msDateFormat.format(stopTime)).append("\n");
//            tipThread.setFlag(false);
//        }, (long) (SystemClock.uptimeMillis() + Float.parseFloat(SharedPreferencesUtils.getString(Constants.FINISH_DELAY, "3")) * 1000));
//    }
//
//    class TipRunnable implements Runnable {
//        public void setFlag(boolean flag) {
//            this.flag = flag;
//        }
//
//        private boolean flag = true;
//
//        @Override
//        public void run() {
//            while (flag) {
//                tipTv1.post(() -> tipTv1.setText(tipDateFormat.format(System.currentTimeMillis())));
//                tipUpdatePosition();
//                try {
//                    Thread.sleep(8);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    private void startUpdateTime() {
//        tipThread = new TipRunnable();
//        new Thread(tipThread).start();
//    }
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus) {
////            resetAnimatorDurationScale();
//            sbTrialInfo.append("Phone Brand: ").append(SystemUtil.getDeviceBrand()).append("\n")
//                    .append("Phone Model: ").append(SystemUtil.getSystemModel()).append("\n")
//                    .append("Language: ").append(SystemUtil.getSystemLanguage()).append("\n")
//                    .append("Android Version: ").append(SystemUtil.getSystemVersion()).append("\n");
//            try {
//                initPermission();
//            } catch (
//                    Exception e) {
//                //异常的处理逻辑，将异常记录日志，异常封装后显示
//                System.out.println(e.getMessage());
//            }
////            preview
//            parentHeight = mConstraintLayout.getHeight();
//            parentWidth = mConstraintLayout.getWidth();
//            markerHeightHalf = ((float) imageView.getHeight()) / 2;
//            markerWidthHalf = ((float) imageView.getWidth()) / 2;
//            DisplayMetrics dm = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(dm);
//            float xdpi = dm.xdpi;
//            float ydpi = dm.ydpi;
////            int screenHeight = dm.heightPixels;
////            int screenWight = dm.widthPixels;
////            sbTrialInfo.append("Screen height px [phone]: ").append(screenHeight).append("\n");
////            sbTrialInfo.append("Screen width px [phone]: ").append(screenWight).append("\n");
////
////            sbTrialInfo.append("Screen height cm [phone]: ").append(screenHeight * 2.54f / ydpi).append("\n");
////            sbTrialInfo.append("Screen width cm [phone]: ").append(screenWight * 2.54f / xdpi).append("\n");
//
//            sbTrialInfo.append("Screen height px [phone]: ").append(parentHeight).append("\n");
//            sbTrialInfo.append("Screen width px [phone]: ").append(parentWidth).append("\n");
//
//            sbTrialInfo.append("Screen height cm [phone]: ").append(parentHeight * 2.54f / ydpi).append("\n");
//            sbTrialInfo.append("Screen width cm [phone]: ").append(parentWidth * 2.54f / xdpi).append("\n");
//
//            sbTrialInfo.append("Marker height px [phone]: ").append((float) imageView.getHeight()).append("\n");
//            sbTrialInfo.append("Marker width px [phone]: ").append((float) imageView.getWidth()).append("\n");
//            fixationAnimation = new FixationAnimation(this);
//            startUpdateTime();
//        }
//
//    }
//
//    public void tipUpdateDataframe(String dataFrame) {
//        tipTv3.post(() -> tipTv3.setText(dataFrame.split("\t")[1]));
//    }
//
//
//    @SuppressLint({"SetTextI18n", "DefaultLocale"})
//    public void tipUpdatePosition() {
//        tipTv4.post(() -> tipTv4.setText("X: " + String.format("%.2f", (imageView.getX() + markerWidthHalf) / parentWidth) + "    Y: " + String.format("%.2f", (imageView.getY() + markerHeightHalf) / parentHeight)));
//    }
//
//
//    private void tipUpdateInterval() {
//        tipTv2.setText(String.valueOf(appearTime - startTime));
//    }
//
//
//    private void initPermission() {
//        if (!CameraPermissionHelper.hasCameraPermission(this)) {
//            CameraPermissionHelper.requestCameraPermission(this);
//        } else {
//            startCamera();
//        }
//    }
//
//
//    private Bitmap resizeImage(Bitmap bitmap, int width, int height) {
//        int bmpWidth = bitmap.getWidth();
//        int bmpHeight = bitmap.getHeight();
//
//        float scaleWidth = ((float) width) / bmpWidth;
//        float scaleHeight = ((float) height) / bmpHeight;
//
//        Matrix matrix = new Matrix();
//        matrix.postScale(scaleWidth, scaleHeight);
//        return Bitmap.createBitmap(bitmap, 0, 0, bmpWidth, bmpHeight, matrix, true);
//    }
//
//
//    private Bitmap adjustPhotoRotation(Bitmap bm, int orientationDegree) {
//        Matrix m = new Matrix();
//        m.setRotate((float) orientationDegree, ((float) bm.getWidth()) / 2, ((float) bm.getHeight()) / 2);
//        return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
//    }
//
//
//    private void saveImg(Mat rgba, String fileName) {
//        //先把mat转成bitmap
//        Bitmap mBitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
//        //Imgproc.cvtColor(seedsImage, rgba, Imgproc.COLOR_GRAY2RGBA, 4); //转换通道
//
//        Utils.matToBitmap(rgba, mBitmap);
//        mBitmap = adjustPhotoRotation(mBitmap, 270);
//        mBitmap = resizeImage(mBitmap, 480, 640);
//
//        try {
//            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
//            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
//            fileOutputStream.flush();
//            fileOutputStream.close();
//            Log.d(TAG, "图片已保存至本地");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.e(TAG, "图片未发现");
//        } catch (IOException e1) {
//            e1.printStackTrace();
//            Log.e(TAG, "图片保存失败");
//        }
//
//    }
//
//    @Override
//    public void onPermissionsSuccess() {
//        super.onPermissionsSuccess();
//        startCamera();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (receiverUtil != null) receiverUtil.stopReceive();
//        if (fixationAnimation != null) fixationAnimation.destroy();
////        fixationAnimation.destroy();
//    }
//
//    /**
//     * Unbinds all the lifecycles from CameraX, then creates new with new parameters
//     */
//    @SuppressLint("RestrictedApi")
//    private void startCamera() {
//        // This is the Texture View where the camera will be rendere
//
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//        cameraProviderFuture.addListener(() -> {
//            try {
//                cameraProvider = cameraProviderFuture.get();
//                ProcessCameraProvider localCameraProvider = cameraProvider;
//                int rotation = viewFinder.getDisplay().getRotation();
//                preview = new Preview.Builder()
//                        .setTargetAspectRatio(Integer.parseInt(SharedPreferencesUtils.getString(Constants.ASPECT_RATIO, "0"))) // set the camera aspect ratio
//                        .setTargetRotation(rotation) // set the camera rotation
//                        .build();
//                videoCapture = androidx.camera.core.VideoCapture.Builder
//                        .fromConfig(new Defaults().getConfig())
//                        .setMaxResolution(new Size(
//                                Integer.parseInt(SharedPreferencesUtils.getString(Constants.RESOLUTION_WIDTH, "480")),
//                                Integer.parseInt(SharedPreferencesUtils.getString(Constants.RESOLUTION_HEIGHT, "640"))))
//                        .setTargetRotation(viewFinder.getDisplay().getRotation()).build();
//
//                localCameraProvider.unbindAll();
//                camera = localCameraProvider.bindToLifecycle(
//                        this, // current lifecycle owner
//                        lensFacing, // either front or back facing
//                        preview, // camera preview use case
//                        videoCapture // video capture use case
//                );
//
//                // Attach the viewfinder's surface provider to preview use case
//                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
//            } catch (ExecutionException | InterruptedException e) {
//                e.printStackTrace();
//            }
//        }, ContextCompat.getMainExecutor(this));
//
//    }
//
//    @SuppressLint({"RestrictedApi", "MissingPermission"})
//    private void recordVideo() {
//        androidx.camera.core.VideoCapture localVideoCapture = videoCapture;
//        startTime = System.currentTimeMillis();
//        // createOrExistsDir(outputDirectory)
//        // Options fot the output video file
//
//        File videoFile = new File(saveDir + "/" + fileNameDateFormat.format(startTime) + "_record.mp4");
//        androidx.camera.core.VideoCapture.OutputFileOptions outputOptions = new androidx.camera.core.VideoCapture.OutputFileOptions.Builder(videoFile).build();
//
//        if (!isRecording) {
//            localVideoCapture.startRecording(outputOptions, Executors.newSingleThreadExecutor(), new androidx.camera.core.VideoCapture.OnVideoSavedCallback() {
//                @Override
//                public void onVideoSaved(@NonNull androidx.camera.core.VideoCapture.OutputFileResults outputFileResults) {
//                    Log.i(TAG, "视频保存完成");
//                    Looper.prepare();
////                    btnVideo.post(() -> {
////                        btnVideo.setVisibility(View.VISIBLE);
////                        btnVideo.setText("正在解析视频，请稍候……");
////                        btnVideo.setClickable(false);
////                    });
//                    WaitDialog.show("正在解析视频，请稍等...");
//                    duration = getDuration(videoFile.getAbsolutePath());
//                    Log.i(TAG, "duration: " + duration);
////                    Toast.makeText(MainActivity.this, "Start to convert video to pictures!", Toast.LENGTH_SHORT).show();
//                    if("FFmpeg".equals(SharedPreferencesUtils.getString(Constants.PARSE_WAY, "FFmpeg"))){
//                        ffmpegStart = System.currentTimeMillis();
//                        videoToPicturesFFmpeg(videoFile.getAbsolutePath());
//                    }else {
//                        openCvStart = System.currentTimeMillis();
//                        videoToPictures(videoFile.getAbsolutePath());
//                    }
//                }
//
//                @Override
//                public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
//                    String msg = "视频录制或者解析失败";
//                    Looper.prepare();
//                    Toast.makeText(RecordActivity.this, msg, Toast.LENGTH_SHORT).show();
//                    Log.e(TAG, msg);
//                    if (cause != null) cause.printStackTrace();
//                }
//            });
//        } else {
//            localVideoCapture.stopRecording();
//        }
//        isRecording = !isRecording;
//    }
//
//
//    @SuppressLint("SetTextI18n")
//    private void videoToPictures(String absolutePath) {
//        VideoCapture cap = new VideoCapture();
//        cap.open(absolutePath);
//        Mat frame = new Mat();
//        Mat img = new Mat();
//        int frameCount = 0;
//        if (cap.isOpened()) {
//            double videoLength = cap.get(Videoio.CAP_PROP_FRAME_COUNT);
//            double framePerSecond = cap.get(Videoio.CAP_PROP_FPS);
////            System.out.println("Video is opened");
////            System.out.println("Number of Frames: " + videoLength);
////            System.out.println("Frames per Second " + framePerSecond);
////            System.out.println("Converting Video...");
//            FileUtil.createOrExistsDir(this.saveDir + "/image");
//            StringBuilder tmpSB = new StringBuilder();
//            while (cap.read(frame)) {
//                frameCount++;
//                tmpSB.append(this.saveDir).append("/image/discard_").append(frameCount).append(".jpg");
//                saveImg(frame, tmpSB.toString());
//                tmpSB.setLength(0);
//            }
//            cap.release();
//            receiverUtil.stopReceive();
//            ImgParseUtils.parseImg(saveDir + "/image", fixationAnimation.getFragmentList(), fixationAnimation.getPositionList(), duration);
////            Toast.makeText(this, "Finished to convert video to pictures", Toast.LENGTH_SHORT).show();
//            sbTrialInfo.append("OpenCV time used (ms): ").append(System.currentTimeMillis() - openCvStart).append("\n");
//            btnVideo.post(() -> {
//                btnVideo.setText("退出");
//                btnVideo.setClickable(true);
//            });
//            saveTrialInfo();
//        } else {
//            Log.e("VideoCapture", "onError: video parse failed");
////            Toast.makeText(this, "OpenCV视频解析失败", Toast.LENGTH_SHORT).show();
//            TipDialog.show("解析完成!", WaitDialog.TYPE.ERROR);
//        }
//        TipDialog.show("解析完成!", WaitDialog.TYPE.SUCCESS);
//        btnVideo.post(() -> {
//            btnVideo.setVisibility(View.VISIBLE);
//            btnVideo.setText("退出");
//            btnVideo.setClickable(true);
//        });
//    }
//
//    private void videoToPicturesFFmpeg(String absolutePath) {
////        Toast.makeText(MainActivity.this, "Start to convert video to pictures!", Toast.LENGTH_SHORT).show();
//        String desDir = this.saveDir + "/image";
//        FileUtil.createOrExistsDir(desDir);
//
////        MediaInformationSession mediaInformation = FFprobeKit.getMediaInformation(absolutePath);
////        float duration = Float.parseFloat(mediaInformation.getMediaInformation().getDuration());
////        FFmpegKit.executeAsync("-i " + absolutePath + " -q:v 2 " + desDir + "/discard_%d.jpg", session2 -> {
//            Looper.prepare();
////            Toast.makeText(MainActivity.this, "video parse success", Toast.LENGTH_SHORT).show();
//            ImgParseUtils.parseImg(desDir,  fixationAnimation.getFragmentList(), fixationAnimation.getPositionList(), duration);
//            sbTrialInfo.append("FFmpeg time used (ms): ").append(System.currentTimeMillis() - ffmpegStart).append("\n");
//
//            saveTrialInfo();
//            TipDialog.show("解析完成!", WaitDialog.TYPE.SUCCESS);
//            btnVideo.post(() -> {
//                btnVideo.setVisibility(View.VISIBLE);
//                btnVideo.setText("退出");
//                btnVideo.setClickable(true);
//            });
//        });
//
//
////        ImgParseUtils.parseImg(this.saveDir, frameCount, 3 * 30, fixationAnimation.getFragmentList(), fixationAnimation.getPositionList());
//    }
//
//    private float getDuration(String filePath) {
//        MediaPlayer player = new MediaPlayer();
//        try {
//            player.setDataSource(filePath);  //filePath为文件的路径
//            player.prepare();
//        } catch (Exception e) {
//            Log.d(TAG, "getDuration: " + e.toString());
//        }
//        float duration = player.getDuration();//获取媒体文件时长
//        Log.d(TAG, "getDuration: " + duration);
//        player.release();//记得释放资源
//        return duration / 1000;
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if(camera != null) camera.getCameraControl().enableTorch(false);
//    }
//
//    public ImageView getImageView() {
//        return this.imageView;
//    }
//
//    private void resetAnimatorDurationScale() {
//        try {
//            @SuppressLint("SoonBlockedPrivateApi") Field field = ValueAnimator.class.getDeclaredField("sDurationScale");
//            field.setAccessible(true);
//            if (field.getFloat(null) == 0) {
//                field.setFloat(null, 1);
//            }
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            e.printStackTrace();
//            Log.e(TAG, e.getMessage());
//        }
//    }
//
//    private void saveTrialInfo() {
//        File file = new File(saveDir + "/trial_info.txt");
//        Log.i(TAG + ": Receive saved path is", file.getAbsolutePath());
//        FileOutputStream fos = null;
//        OutputStreamWriter osw = null;
//        if (!file.exists()) {
//            try {
//                file.createNewFile();
//                fos = new FileOutputStream(file);
//                osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
//                osw.write(sbTrialInfo.toString());
//                osw.flush();
//                osw.close();
//                fos.close();
//            } catch (IOException e) {
//                Looper.prepare();
//                e.printStackTrace();
//                Log.e(TAG, "" + e.getMessage());
//                Log.e(TAG, "Fail to create file: " + file.getAbsolutePath());
//                Toast.makeText(RecordActivity.this, "Fail to create file: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//}
