package com.example.gaze.record.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.example.gaze.record.R;
import com.example.gaze.record.listener.ExperimentListener;
import com.example.gaze.record.motion.BaseMotion;
import com.example.gaze.record.motion.FixationStabilityMotion;
import com.example.gaze.record.motion.MotionListener;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.DataUtils;
import com.example.gaze.record.utils.SharedPreferencesUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lib.gaze.tracker.core.GazeTracker;

public class ExperimentPointMotionSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable, ExperimentListener, View.OnClickListener, MotionListener {

    private final String TAG = getClass().getSimpleName();
    private float x;
    private float y;
    private SurfaceHolder holder;
    private volatile boolean isDraw = false;// 控制绘制的开关
    private Paint gazePaint;
    private Canvas mCanvas;
    private Paint cameraImgPaint;
    private Paint textPaint;
    private Paint fpsTextPaint;
    private float textSize = 64;
    private float mScale;
    private volatile boolean hasGaze = false;
    private Queue<Long> timeStampRecord;
    private volatile float fps;

    private boolean debugAble = true;

    private BaseMotion baseMotion;

    // 实验开始或者结束
    private long expStartTimeStamp;
    private long expEndTimeStamp;

    private long stapleIntroStartTimeStamp;
    private long stapleIntroEndTimeStamp;
    private long normalTrialStartTimeStamp;
    private long markerMotionStartTimeStamp;
    private long normalTrialEndTimeStamp;

//    private long normalTrialPreparationStartTimeStamp;
//    private long normalTrialPreparationEndTimeStamp;
    private ExperimentListener experimentListener;
    private volatile boolean expOver;
    private Paint fixationPaint;
    private String motionType;
    private Bitmap smoothPursuitIntroBitmap;
    private Bitmap fixationStabilityIntroBitmap;
    private String saveDir;
    MediaPlayer mediaPlayer;

    public ExperimentPointMotionSurfaceView(Context context) {
        super(context);
        init();
    }

    public ExperimentPointMotionSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        Log.d("surface view", "init");
        holder = this.getHolder();
        holder.addCallback(this);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int stroke = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
        gazePaint = new Paint();
        gazePaint.setAntiAlias(true);
        gazePaint.setDither(true);
        gazePaint.setColor(Color.GREEN);
        gazePaint.setStrokeWidth(stroke);
        gazePaint.setStyle(Paint.Style.STROKE);

        cameraImgPaint = new Paint();
        cameraImgPaint.setAlpha(255 * 2 / 10);

        textPaint = new Paint();
        textPaint.setColor(Color.RED);
        textPaint.setAntiAlias(true);
        textPaint.setStrokeWidth(50);
        textPaint.setTextSize(textSize);

        fixationPaint = new Paint();
        fixationPaint.setColor(Color.WHITE);
        fixationPaint.setAntiAlias(true);
        fixationPaint.setStrokeWidth(50);
        fixationPaint.setTextSize(64);
//        fixationPaint.setTextAlign(Paint.Align.CENTER);

        fpsTextPaint = new Paint();
        fpsTextPaint.setColor(Color.WHITE);
        fpsTextPaint.setAntiAlias(true);
        fpsTextPaint.setTextSize(36);

        setZOrderOnTop(true);
        setZOrderMediaOverlay(true);
        holder.setFormat(PixelFormat.TRANSPARENT);
        timeStampRecord = new LinkedList<>();

        debugAble = SharedPreferencesUtils.getBoolean(Constants.DEBUG_STATE, true);
        smoothPursuitIntroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.intro_sp);
        fixationStabilityIntroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.intro_fs);

        expStartTimeStamp = System.nanoTime();
        setOnClickListener(this);

        mediaPlayer = MediaPlayer.create(getContext(), R.raw.qbeep);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDraw = true;
        new Thread(this).start();
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDraw = false;
    }

    @Override
    public void run() {
        Looper.prepare();
        while (isDraw) {
            drawUI();
        }
    }

    /**
     * 界面绘制
     */
    public void drawUI() {
        try {
            mCanvas = holder.lockCanvas();
            if (mCanvas != null) {
                mCanvas.drawRGB(255 / 2, 255 / 2, 255 / 2);
                drawExp();
                drawDebug();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (holder != null) {
                    holder.unlockCanvasAndPost(mCanvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void drawIntroduction(){
        if (motionType.equals("SmoothPursuit")){
            drawIntroWithBitmap(smoothPursuitIntroBitmap);
        }else{
            drawIntroWithBitmap(fixationStabilityIntroBitmap);
        }
    }

    void drawIntroWithBitmap(Bitmap bitmap){
        int canvasWidth = mCanvas.getWidth();
        int canvasHeight = mCanvas.getHeight();

        // 计算缩放比例，选择宽度和高度的较小值作为基准
        float scale = Math.min((float) canvasWidth / bitmap.getWidth(), (float) canvasHeight / bitmap.getHeight());

        // 创建源矩形，用于指定要绘制的位图的区域（整个位图）
        Rect srcRect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        // 创建目标矩形，用于指定位图在画布上的位置和大小
        Rect dstRect = new Rect(
                (canvasWidth - (int) (scale * bitmap.getWidth())) / 2,
                (canvasHeight - (int) (scale * bitmap.getHeight())) / 2,
                (canvasWidth + (int) (scale * bitmap.getWidth())) / 2,
                (canvasHeight + (int) (scale * bitmap.getHeight())) / 2
        );

        // 使用画布对象绘制缩放后的位图
        mCanvas.drawBitmap(bitmap, srcRect, dstRect, null);
    }

    void drawExp(){
        if (expStartTimeStamp == 0L){
            return;
        }

        if (expStartTimeStamp != 0L && stapleIntroStartTimeStamp == 0L){
            stapleIntroStartTimeStamp = System.nanoTime();
        }

        // staple intro show
        if(stapleIntroStartTimeStamp != 0L && stapleIntroEndTimeStamp == 0L){
            drawIntroduction();
            return;
        }

        // init practice intro time stamp
        if (stapleIntroStartTimeStamp != 0L && stapleIntroEndTimeStamp != 0L && normalTrialStartTimeStamp == 0L){
            Log.d(TAG,"init practice intro");
            normalTrialStartTimeStamp = System.nanoTime();
            mediaPlayer.start();
        }

        // practice intro show
//        if (normalTrialPreparationStartTimeStamp != 0L && normalTrialPreparationEndTimeStamp == 0L){
////            drawPreparation();
//            return;
//        }
//
//        //init practice trial
//        if (normalTrialPreparationStartTimeStamp != 0L && normalTrialPreparationEndTimeStamp != 0L && normalTrialStartTimeStamp == 0L){
//            Log.d(TAG,"init practice trial");
//            normalTrialStartTimeStamp = System.nanoTime();
//        }

        // practice trial show
        if (normalTrialStartTimeStamp != 0L && normalTrialEndTimeStamp == 0L){
            drawNormalTrial();
            return;
        }

        // end the exp
        if (normalTrialStartTimeStamp != 0L && normalTrialEndTimeStamp != 0L && expEndTimeStamp == 0L){
            expEndTimeStamp = System.nanoTime();
            saveExpData();

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.schedule(() -> {
                if (experimentListener != null) experimentListener.onExpEnd();
                expOver = true;
                Log.d(TAG, "expOver is true");
            }, 2, TimeUnit.SECONDS);

        }

        if (expOver){
//            Log.d(TAG, "expOver is drawing");
            float textWidth = fixationPaint.measureText("保持头部不动，");
            float textHeight = fixationPaint.getTextSize();
            mCanvas.drawText("保持头部不动，", mCanvas.getWidth() / 2f - textWidth / 2, mCanvas.getHeight() / 2f - textHeight / 2, fixationPaint);
            mCanvas.drawText("按空格键进行下一步。", mCanvas.getWidth() / 2f - textWidth / 2 - 100, mCanvas.getHeight() / 2f + textHeight / 2, fixationPaint);
        }
    }

    private void saveExpData(){
        String saveData = "";

        if (!motionType.equals("SmoothPursuit")){
            List<Long> observeDistractEndTimeList = ((FixationStabilityMotion)baseMotion).getObserveDistractEndTimeList();
            List<Long> observeDistractStartTimeList = ((FixationStabilityMotion)baseMotion).getObserveDistractStartTimeList();
            saveData = "{" +
                    "\"expStartTimeStamp\": " + expStartTimeStamp + ", " +
                    "\"expEndTimeStamp\": " + expEndTimeStamp + ", " +
                    "\"stapleIntroStartTimeStamp\": " + stapleIntroStartTimeStamp + ", " +
                    "\"stapleIntroEndTimeStamp\": " + stapleIntroEndTimeStamp + ", " +
                    "\"distractorEndTimeList\": " + observeDistractEndTimeList + ", " +
                    "\"distractorStartTimeList\": " + observeDistractStartTimeList + ", " +
                    "\"normalTrialStartTimeStamp\": " + normalTrialStartTimeStamp + ", " +
                    "\"markerMotionStartTimeStamp\": " + markerMotionStartTimeStamp + ", " +
                    "\"normalTrialEndTimeStamp\": " + normalTrialEndTimeStamp +
                    "}";
            DataUtils.saveTextToPath(saveDir + "/fs_timestamps.json", saveData);

        }else{
            saveData = "{" +
                    "\"expStartTimeStamp\": " + expStartTimeStamp + ", " +
                    "\"expEndTimeStamp\": " + expEndTimeStamp + ", " +
                    "\"stapleIntroStartTimeStamp\": " + stapleIntroStartTimeStamp + ", " +
                    "\"stapleIntroEndTimeStamp\": " + stapleIntroEndTimeStamp + ", " +
//                "\"normalTrialPreparationStartTimeStamp\": " + normalTrialPreparationStartTimeStamp + ", " +
//                "\"normalTrialPreparationEndTimeStamp\": " + normalTrialPreparationEndTimeStamp + ", " +
                    "\"normalTrialStartTimeStamp\": " + normalTrialStartTimeStamp + ", " +
                    "\"markerMotionStartTimeStamp\": " + markerMotionStartTimeStamp + ", " +
                    "\"normalTrialEndTimeStamp\": " + normalTrialEndTimeStamp +
                    "}";
            DataUtils.saveTextToPath(saveDir + "/sp_timestamps.json", saveData);
        }

    }

    private void drawNormalTrial() {
        baseMotion.drawMotion(mCanvas, null, System.nanoTime());

    }

//    private void drawFixation() {
//        baseMotion.drawMotion(mCanvas, null, System.nanoTime());
//    }

    void drawDebug(){
        if (debugAble) {
            if (expEndTimeStamp == 0L) {
                mCanvas.drawText(
                        "Delta Time: " + String.format(Locale.CHINA, "%.4f", (System.nanoTime() - expStartTimeStamp) / 1E9F),
                        mCanvas.getWidth() * 1f / 4f, mCanvas.getHeight() * 3 / 4f, textPaint);
            }
            if (timeStampRecord!= null && fps != 0f) {
                mCanvas.drawText(String.format(Locale.CHINA, "FPS: %.2f", fps),
                        50, 50, fpsTextPaint);
            }
            if (hasGaze) {
                mCanvas.drawCircle(x, y, 10, gazePaint);
            }
        }
    }

    public SurfaceHolder getSurfaceHolder() {
        return holder;
    }

    @Override
    public void setX(float x) {
        this.x = x;
    }

    @Override
    public void setY(float y) {
        this.y = y;
    }

    public void setXY(float x, float y) {
        this.x = x;
        this.y = y;
    }

//    public void setTimeStamp(long timeStamp) {
//        interval = timeStamp - lastTimeStamp;
//        lastTimeStamp = timeStamp;
//    }


    public boolean isDraw() {
        return isDraw;
    }

    public void setDraw(boolean draw) {
        isDraw = draw;
    }

//    public void setMat(Mat imgMat) {
//        new Thread(() -> {
//            if (mCacheBitmap != null) {
//                Utils.matToBitmap(imgMat, mCacheBitmap);
//                imgMat.release();
//            }
//        }).start();
//    }
//
//    public Bitmap getCacheBitmap() {
//        return mCacheBitmap;
//    }
//
//    public void setCacheBitmap(Bitmap mCacheBitmap) {
//        this.mCacheBitmap = mCacheBitmap;
//    }
//
//    public void setCacheBitmap(Mat mat) {
//        if (mat != null && mat.cols() != 0 && mat.rows() != 0) {
//            Utils.matToBitmap(mat, this.mCacheBitmap);
//            mat.release();
//        }
//    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float mScale) {
        this.mScale = mScale;
    }

    public void setGaze(float[] gazeResult) {
        if (gazeResult == null) {
            hasGaze = false;
            return;
        }
        hasGaze = true;
        x = gazeResult[0];
        y = gazeResult[1];
    }

    public void addTimeStamp(long timeStamp) {
        if (timeStampRecord.size() == 60) {
            timeStampRecord.poll();
            timeStampRecord.offer(timeStamp);
            long interval = (((LinkedList<Long>) timeStampRecord).peek() - ((LinkedList<Long>) timeStampRecord).getLast());
            fps = -timeStampRecord.size() / (interval / 1e9f);
        } else {
            timeStampRecord.offer(timeStamp);
        }
    }

    @Override
    public void onExpEnd() {

    }

    public BaseMotion getBaseMotion() {
        return baseMotion;
    }

    public void setBaseMotion(BaseMotion baseMotion) {
        this.baseMotion = baseMotion;
    }

    public void setExperimentListener(ExperimentListener experimentListener) {
        this.experimentListener = experimentListener;
    }

    public void setMotionType(String motionType) {
        this.motionType = motionType;
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "view was clicked!");
        synchronized (this) {
            if (stapleIntroStartTimeStamp != 0L && stapleIntroEndTimeStamp == 0L) {
                stapleIntroEndTimeStamp = System.nanoTime();
                Log.d(TAG, "set stapleIntroEndTimeStamp!");
            }

            if (expEndTimeStamp != 0L){
                // 实验做完了，点击事件
                GazeTracker.getInstance().gazeSetting();
            }
        }
    }

    @Override
    public void onMotionStart() {
        markerMotionStartTimeStamp = System.nanoTime();
    }

    @Override
    public void onMotionEnd() {
        normalTrialEndTimeStamp = System.nanoTime();
    }

    public void setHasGaze(boolean hasGaze) {
        this.hasGaze = hasGaze;
    }
}