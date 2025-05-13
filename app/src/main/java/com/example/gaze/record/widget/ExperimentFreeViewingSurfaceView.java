package com.example.gaze.record.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.alibaba.fastjson.JSONObject;
import com.example.gaze.record.R;
import com.example.gaze.record.listener.ExperimentListener;
import com.example.gaze.record.utils.CalculatorUtils;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.DataUtils;
import com.example.gaze.record.utils.SharedPreferencesUtils;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.w3c.dom.ls.LSException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lib.gaze.tracker.core.GazeTracker;

enum ExperimentPhase{
    PRACTICE, NORMAL
}

enum ExpIntro{
    STAPLE, PRACTICE, NORMAL
}

public class ExperimentFreeViewingSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable, View.OnClickListener {
    private float x;
    private float y;
    private final String TAG = getClass().getSimpleName();
    private volatile boolean debugAble;
    private SurfaceHolder holder;
    private volatile boolean isDraw = false;// 控制绘制的开关
    private Paint gazePaint;
    private Canvas mCanvas;
    private Paint cameraImgPaint;
    private Paint textPaint;
    private Paint fixationPaint;
    private final float fixationSize = 64;
    private Paint fpsTextPaint;
    private float textSize = 64;
    private long lastTimeStamp;
//    private Bitmap mCacheBitmap;
//    private float mScale;
    private volatile boolean hasGaze = false;
    private Queue<Long> timeStampRecord;
    private volatile float fps;
    private volatile long startTime;
    private volatile float interval;
    private float prepareInterval = 1.0f;
    private float duration;
    // 每张图片的停留的时间

    private List<Bitmap> normalBitmaps;
    private List<Bitmap> practiceBitmaps;
    private int selfWidth;
    private int selfHeight;

    private float fixationDuration;
    private float pictureDuration;

    private int normalTrialId = 0;
    private int practiceTrialId = 0;
//    private long lastTrialStartTimeStamp;

    // 实验相关的变量
    private long expStartTimeStamp;
    private long expEndTimeStamp;
    private long stapleIntroStartTimeStamp;
    private long stapleIntroEndTimeStamp;
    private long practiceIntroStartTimeStamp;
    private long practiceIntroEndTimeStamp;
    private long practiceTrialStartTimeStamp;
    private long practiceTrialEndTimeStamp;
    private long normalIntroStartTimeStamp;
    private long normalIntroEndTimeStamp;
    private long normalTrialStartTimeStamp;
    private long normalTrialEndTimeStamp;

    // intro bitmap
    private Bitmap stapleIntroBitmap;
    private Bitmap normalIntroBitmap;
    private Bitmap practiceIntroBitmap;
    // save dir
    private String saveDir;

    private ExperimentListener experimentListener;

    private volatile boolean expOver;

    private List<Long> practiceShowTimeStampList;
    private List<Long> normalShowTimeStampList;

    private List<Long> practiceFixationShowTimeStampList;
    private List<Long> normalFixationShowTimeStampList;
    List<String> pathList = new ArrayList<>();
    MediaPlayer mediaPlayer;

    public ExperimentFreeViewingSurfaceView(Context context) throws IOException {
        super(context);
        init();
        initPictures();
    }

    public ExperimentFreeViewingSurfaceView(Context context, AttributeSet attrs) throws IOException {
        super(context, attrs);
        init();
        initPictures();
    }


    private void init() {
        Log.d(TAG, "init");
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
        fixationPaint.setTextSize(fixationSize);
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

        stapleIntroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.intro_fv);
        practiceIntroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.intro_fv_practice);
        normalIntroBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.intro_fv_normal);
        expStartTimeStamp = System.nanoTime();

        mediaPlayer = MediaPlayer.create(getContext(), R.raw.qbeep);
    }

    private void initPictures() throws IOException {
        practiceBitmaps = new ArrayList<>();
        normalBitmaps = new ArrayList<>();

        List<Bitmap> bitmaps = new ArrayList<>();
        pathList.addAll(getPathList("generated_images"));
//        pathList.addAll(getPathList("Animal"));
//        pathList.addAll(getPathList("Food"));
//        pathList.addAll(getPathList("Human"));
//        pathList.addAll(getPathList("Manmade"));
//        pathList.addAll(getPathList("Natural"));
//        pathList.addAll(getPathList("Object"));
        Random random = new Random(2023);
        Collections.shuffle(pathList, random);
        Log.d(TAG, String.valueOf(pathList));
        for (String path : pathList) {
            bitmaps.add(BitmapFactory.decodeStream(getContext().getAssets().open(path)));
        }
        // 提取前五个元素到 practiceBitmaps
        for (int i = 0; i < 5; i++) {
            int randInt = random.nextInt(pathList.size());
            practiceBitmaps.add(bitmaps.get(randInt));
        }

        // 提取第一个元素到最后一个到 normalBitmaps
        normalBitmaps.addAll(bitmaps);


        practiceShowTimeStampList = new ArrayList<>(practiceBitmaps.size());
        practiceFixationShowTimeStampList = new ArrayList<>(practiceBitmaps.size());
        normalShowTimeStampList = new ArrayList<>(normalBitmaps.size());
        normalFixationShowTimeStampList = new ArrayList<>(practiceBitmaps.size());
        for (int i = 0; i < practiceBitmaps.size(); i++) {
            practiceShowTimeStampList.add(0L);
            practiceFixationShowTimeStampList.add(0L);
        }
        for (int i = 0; i < normalBitmaps.size(); i++) {
            normalShowTimeStampList.add(0L);
            normalFixationShowTimeStampList.add(0L);
        }

        // Set experiment duration and every picture duration
        pictureDuration = (float) CalculatorUtils
                .conversion(SharedPreferencesUtils.
                        getString(Constants.PICTURE_DURATION_FREE_VIEWING, "3.0"));
        fixationDuration = (float)  CalculatorUtils
                .conversion(SharedPreferencesUtils.
                        getString(Constants.FIXATION_DURATION_FREE_VIEWING, "0.5"));
        Log.d(TAG, "picture duration: "+ pictureDuration);
        Log.d(TAG, "fixation duration: " + fixationDuration);

        duration = bitmaps.size() * (pictureDuration + fixationDuration);
        setOnClickListener(this);
    }

    private List<String> getPathList(String category) throws IOException {
        List<String> imagePathList = new ArrayList<>();
        String[] animals = getContext().getAssets().list(category);
        for (String animal : animals) {
            Log.d(TAG, animal);
            imagePathList.add(category + "/" + animal);
        }
        return imagePathList;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isDraw = true;
        selfHeight = getHeight();
        selfWidth = getWidth();
        new Thread(this).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isDraw = false;
        mediaPlayer.release();
    }

    @Override
    public void run() {
        Looper.prepare();
        while (isDraw) {
            drawUI();
        }
    }

    public void drawUI() {
        try {
            mCanvas = holder.lockCanvas();
            if (mCanvas != null) {
                // Draw Background
                mCanvas.drawRGB(255 / 2 , 255 / 2, 255 / 2);
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

    private void drawDebug(){
        if (debugAble) {
            if (expEndTimeStamp == 0L) {
                mCanvas.drawText(
                        "Delta Time: " + String.format(Locale.CHINA, "%.4f", (System.nanoTime() - expStartTimeStamp) / 1E9F),
                        selfWidth * 1f / 4f, selfHeight * 3 / 4f, textPaint);
            }
            if (timeStampRecord!= null && fps != 0f) {
                mCanvas.drawText(String.format(Locale.CHINA, "FPS: %.2f", fps),
                        50, 50, fpsTextPaint);
            }
            if(hasGaze) mCanvas.drawCircle(x, y, 10, gazePaint);
        }
    }

    private void drawIntroduction(ExpIntro expIntro){
        switch (expIntro){
            case STAPLE:
                drawIntroWithBitmap(stapleIntroBitmap);
                break;
            case PRACTICE:
                drawIntroWithBitmap(practiceIntroBitmap);
                break;
            case NORMAL:
                drawIntroWithBitmap(normalIntroBitmap);
                break;
            default:
                break;
        }
    }

    private void drawIntroWithBitmap(Bitmap bitmap) {
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



    public synchronized void drawTrial(ExperimentPhase experimentPhase){
        int trialId = getTrialId(false, experimentPhase);
        List<Long> showTimeStamp = getShowTimeStampList(experimentPhase);
        List<Bitmap> bitmaps = getTrialBitmaps(experimentPhase);
        List<Long> fixationShowTimeStamp = getFixationShowTimeStampList(experimentPhase);
//        Log.d(TAG, "bitmap size: " + bitmaps.size());
//        Log.d(TAG, "trialId: " + trialId);
        if (trialId == bitmaps.size() && experimentPhase == ExperimentPhase.NORMAL){
            normalTrialEndTimeStamp = System.nanoTime();
            return;
        }else if (trialId == bitmaps.size() && experimentPhase == ExperimentPhase.PRACTICE){
            practiceTrialEndTimeStamp = System.nanoTime();
            return;
        }

        long nowTime = System.nanoTime();
        long  lastTrialStartTimeStamp = experimentPhase==ExperimentPhase.NORMAL ?
                normalTrialStartTimeStamp : practiceTrialStartTimeStamp;
        float elapsedTime = (nowTime - lastTrialStartTimeStamp) / 1E9F;
//        Log.d(TAG, "elapsed time: " + elapsedTime);
//        Log.d(TAG, "condition: " + (elapsedTime - (trialId + 1) * (pictureDuration + fixationDuration)));
        if (elapsedTime - (trialId + 1) * (pictureDuration + fixationDuration) > 0){
//            Log.d(TAG, "condition: YES");
            trialId = getTrialId(true, experimentPhase);
        }
        float trialElapsedTime = elapsedTime - trialId * (pictureDuration + fixationDuration);
//        Log.d(TAG, "trial elapsed time: " + trialElapsedTime);
        if (trialElapsedTime <= fixationDuration){
            // draw fixation
            if (trialId != fixationShowTimeStamp.size() && fixationShowTimeStamp.get(trialId) == 0){
                fixationShowTimeStamp.set(trialId, nowTime);
                mediaPlayer.start();
            }

            String text = "+";
            float textWidth = fixationPaint.measureText(text);
            float textHeight = fixationPaint.getTextSize();
            mCanvas.drawText(text, selfWidth / 2f - textWidth / 2, selfHeight / 2f - textHeight / 2, fixationPaint);
        }

        if (trialElapsedTime <= (pictureDuration + fixationDuration) && trialElapsedTime > fixationDuration){
            if (trialId != showTimeStamp.size() && showTimeStamp.get(trialId) == 0){
                showTimeStamp.set(trialId, nowTime);
            }
            // draw picture
            Bitmap bitmap = bitmaps.get(trialId);
            mCanvas.drawBitmap(bitmap,0,0, null);
//            float scale = Math.min();
//            mCanvas.drawBitmap(bitmap,   new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()),
//                    new Rect(0, 0, selfWidth, selfHeight), null);
        }
    }

    private List<Long> getShowTimeStampList(ExperimentPhase experimentPhase) {
        if (experimentPhase == ExperimentPhase.NORMAL){
            return normalShowTimeStampList;
        }else {
            return practiceShowTimeStampList;
        }
    }

    private List<Long> getFixationShowTimeStampList(ExperimentPhase experimentPhase) {
        if (experimentPhase == ExperimentPhase.NORMAL){
            return  normalFixationShowTimeStampList;
        }else {
            return practiceFixationShowTimeStampList;
        }
    }

    private int getTrialId(boolean increment, ExperimentPhase experimentPhase){
        int trialId;
        if (experimentPhase == ExperimentPhase.PRACTICE){
             trialId = !increment? practiceTrialId : ++practiceTrialId;
        }else{
             trialId = !increment?normalTrialId: ++normalTrialId;
        }
        return trialId; 
    }

    private List<Bitmap> getTrialBitmaps(ExperimentPhase experimentPhase){
        if (experimentPhase == ExperimentPhase.NORMAL){
            return normalBitmaps;
        }else {
            return practiceBitmaps;
        }
    }
    
    private void drawExp(){
        if (expStartTimeStamp == 0L){
            return;
        }
        
        if (expStartTimeStamp != 0L && stapleIntroStartTimeStamp == 0L){
            stapleIntroStartTimeStamp = System.nanoTime();
        }
        
        // staple intro show
        if(stapleIntroStartTimeStamp != 0L && stapleIntroEndTimeStamp == 0L){
            drawIntroduction(ExpIntro.STAPLE);
            return;
        }
        
        // init practice intro time stamp
        if (stapleIntroStartTimeStamp != 0L && stapleIntroEndTimeStamp != 0L && practiceIntroStartTimeStamp == 0L){
            Log.d(TAG,"init practice intro");
            practiceIntroStartTimeStamp = System.nanoTime();
        }

        // practice intro show
        if (practiceIntroStartTimeStamp != 0L && practiceIntroEndTimeStamp == 0L){
            drawIntroduction(ExpIntro.PRACTICE);
            return;
        }

        //init practice trial
        if (practiceIntroStartTimeStamp != 0L && practiceIntroEndTimeStamp != 0L && practiceTrialStartTimeStamp == 0L){
            Log.d(TAG,"init practice trial");
            practiceTrialStartTimeStamp = System.nanoTime();
        }

        // practice trial show
        if (practiceTrialStartTimeStamp != 0L && practiceTrialEndTimeStamp == 0L){
            drawTrial(ExperimentPhase.PRACTICE);
            return;
        }

        // init normal intro
        if (practiceTrialStartTimeStamp != 0L && practiceTrialEndTimeStamp != 0L && normalIntroStartTimeStamp == 0L){
            Log.d(TAG,"init normal intro");
            normalIntroStartTimeStamp = System.nanoTime();
        }

        // normal intro show
        if (normalIntroStartTimeStamp != 0L && normalIntroEndTimeStamp == 0L){
            drawIntroduction(ExpIntro.NORMAL);
            return;
        }

        // init normal trial
        if (normalIntroStartTimeStamp != 0L && normalIntroEndTimeStamp != 0L && normalTrialStartTimeStamp == 0L){
            Log.d(TAG,"init normal trial");
            normalTrialStartTimeStamp = System.nanoTime();
        }

        // normal trial show
        if (normalTrialStartTimeStamp != 0L && normalTrialEndTimeStamp == 0L){
            drawTrial(ExperimentPhase.NORMAL);
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
            float textWidth = fixationPaint.measureText("保持头部不动，");
            float textHeight = fixationPaint.getTextSize();
            mCanvas.drawText("保持头部不动，", mCanvas.getWidth() / 2f - textWidth / 2, mCanvas.getHeight() / 2f - textHeight / 2, fixationPaint);
            mCanvas.drawText("按空格键进行下一步。", mCanvas.getWidth() / 2f - textWidth / 2 - 100, mCanvas.getHeight() / 2f + textHeight / 2, fixationPaint);
        }
    }
    
    private void saveExpData(){
        String saveData = "{" +
                "\"expStartTimeStamp\": " + expStartTimeStamp + ", " +
                "\"expEndTimeStamp\": " + expEndTimeStamp + ", " +
                "\"stapleIntroStartTimeStamp\": " + stapleIntroStartTimeStamp + ", " +
                "\"stapleIntroEndTimeStamp\": " + stapleIntroEndTimeStamp + ", " +
                "\"practiceIntroStartTimeStamp\": " + practiceIntroStartTimeStamp + ", " +
                "\"practiceIntroEndTimeStamp\": " + practiceIntroEndTimeStamp + ", " +
                "\"practiceTrialStartTimeStamp\": " + practiceTrialStartTimeStamp + ", " +
                "\"practiceTrialEndTimeStamp\": " + practiceTrialEndTimeStamp + ", " +
                "\"normalIntroStartTimeStamp\": " + normalIntroStartTimeStamp + ", " +
                "\"normalIntroEndTimeStamp\": " + normalIntroEndTimeStamp + ", " +
                "\"normalTrialStartTimeStamp\": " + normalTrialStartTimeStamp + ", " +
                "\"normalTrialEndTimeStamp\": " + normalTrialEndTimeStamp + ", " +
                "\"normalShowTimeStampList\":" + normalShowTimeStampList.toString() + "," +
                "\"practiceShowTimeStampList\":" + practiceShowTimeStampList.toString() + "," +
                "\"normalFixationShowTimeStampList\":" + normalFixationShowTimeStampList.toString() + "," +
                "\"practiceFixationShowTimeStampList\":" + practiceFixationShowTimeStampList.toString() + "," +
                "\"imgPathList\":" + JSONObject.toJSONString(pathList) +
                "}";
        DataUtils.saveTextToPath(saveDir + "/fv_timestamps.json", saveData);
    }

    public SurfaceHolder getSurfaceHolder() {
        return holder;
    }

    public void setExperimentListener(ExperimentListener experimentListener){
        this.experimentListener = experimentListener;
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

    public void setTimeStamp(long timeStamp) {
        interval = timeStamp - lastTimeStamp;
        lastTimeStamp = timeStamp;
    }


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

//    public Bitmap getCacheBitmap() {
//        return mCacheBitmap;
//    }
//
//    public void setCacheBitmap(Bitmap mCacheBitmap) {
//        this.mCacheBitmap = mCacheBitmap;
//    }

//    public void setCacheBitmap(Mat mat) {
//        if (mat != null && mat.cols() != 0 && mat.rows() != 0) {
//            Utils.matToBitmap(mat, this.mCacheBitmap);
//            mat.release();
//        }
//    }

//    public float getScale() {
//        return mScale;
//    }
//
//    public void setScale(float mScale) {
//        this.mScale = mScale;
//    }

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
            long interval = timeStampRecord.peek() - ((LinkedList<Long>) timeStampRecord).getLast();
            fps = -timeStampRecord.size() / (interval / 1e9f);
        } else {
            timeStampRecord.offer(timeStamp);
        }
    }

    public void release(){
        for (Bitmap bitmap: practiceBitmaps) {
            bitmap.recycle();
        }
        for (Bitmap bitmap: normalBitmaps) {
            bitmap.recycle();
        }
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    public boolean isDebugAble() {
        return debugAble;
    }

    public void setDebugAble(boolean debugAble) {
        this.debugAble = debugAble;
    }

    public Paint getGazePaint() {
        return gazePaint;
    }

    public void setGazePaint(Paint gazePaint) {
        this.gazePaint = gazePaint;
    }

    public Canvas getCanvas() {
        return mCanvas;
    }

    public void setCanvas(Canvas mCanvas) {
        this.mCanvas = mCanvas;
    }

    public Paint getCameraImgPaint() {
        return cameraImgPaint;
    }

    public void setCameraImgPaint(Paint cameraImgPaint) {
        this.cameraImgPaint = cameraImgPaint;
    }

    public Paint getTextPaint() {
        return textPaint;
    }

    public void setTextPaint(Paint textPaint) {
        this.textPaint = textPaint;
    }

    public Paint getFpsTextPaint() {
        return fpsTextPaint;
    }

    public void setFpsTextPaint(Paint fpsTextPaint) {
        this.fpsTextPaint = fpsTextPaint;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public long getLastTimeStamp() {
        return lastTimeStamp;
    }

    public void setLastTimeStamp(long lastTimeStamp) {
        this.lastTimeStamp = lastTimeStamp;
    }

//    public Bitmap getmCacheBitmap() {
//        return mCacheBitmap;
//    }
//
//    public void setmCacheBitmap(Bitmap mCacheBitmap) {
//        this.mCacheBitmap = mCacheBitmap;
//    }

//    public float getmScale() {
//        return mScale;
//    }
//
//    public void setmScale(float mScale) {
//        this.mScale = mScale;
//    }

    public boolean isHasGaze() {
        return hasGaze;
    }

    public void setHasGaze(boolean hasGaze) {
        this.hasGaze = hasGaze;
    }

    public Queue<Long> getTimeStampRecord() {
        return timeStampRecord;
    }

    public void setTimeStampRecord(Queue<Long> timeStampRecord) {
        this.timeStampRecord = timeStampRecord;
    }

    public float getFps() {
        return fps;
    }

    public void setFps(float fps) {
        this.fps = fps;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }



    public float getPrepareInterval() {
        return prepareInterval;
    }

    public void setPrepareInterval(float prepareInterval) {
        this.prepareInterval = prepareInterval;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public float getPictureDuration() {
        return pictureDuration;
    }

    public void setPictureDuration(float pictureDuration) {
        this.pictureDuration = pictureDuration;
    }


    @Override
    public void onClick(View view) {
        Log.d(TAG, "view was clicked!");
        synchronized (this) {
            if (stapleIntroStartTimeStamp != 0L && stapleIntroEndTimeStamp == 0L) {
                stapleIntroEndTimeStamp = System.nanoTime();
                Log.d(TAG, "set stapleIntroEndTimeStamp!");
            }

            if (practiceIntroStartTimeStamp != 0L && practiceIntroEndTimeStamp == 0L) {
                practiceIntroEndTimeStamp = System.nanoTime();
                Log.d(TAG, "set practiceIntroEndTimeStamp!");
            }

            if (normalIntroStartTimeStamp != 0L && normalIntroEndTimeStamp == 0L) {
                normalIntroEndTimeStamp = System.nanoTime();
                Log.d(TAG, "set normalIntroEndTimeStamp!");
            }

            if (expEndTimeStamp != 0L){
                // 实验做完了，点击事件
                GazeTracker.getInstance().gazeSetting();
            }
        }
    }

    public void setSaveDir(String saveDir) {
        this.saveDir = saveDir;
    }
}