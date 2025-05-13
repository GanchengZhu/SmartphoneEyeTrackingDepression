package com.example.gaze.record.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.gaze.record.R;
import com.example.gaze.record.utils.BitmapUtils;
import com.example.gaze.record.utils.UnitUtil;

public class LissajousMarkerView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    //定义画布和画笔
    private Canvas mCanvas;
    private Paint mPaint;
    //定义SurfaceHolder用于得到对 Surface 操作的方法的调用
    private SurfaceHolder mHolder;
    //渲染线程绘制标识
    private boolean isDraw;
    private Bitmap mBitmap;
//    private LissajousMarkerFinishListener mListener;

    //是否开始绘制
    private boolean isDrawLissajous;
    private boolean isDrawMarker;

    //View时间相关的参数
    private float x, y;
    private float duration = 96f;

    private float startTime;
    private double interval = 0d;

    //Lissajous Curve 相关的参数
    private double ampX = 900d;
    private double ampY = 450d;

    private double freqX = 1 / 16d;
    private double freqY = 1 / 15d;

    private double phaseX = Math.PI * 3 / 2;
    private double phaseY = 0;


    public float getStartTime() {
        return startTime;
    }

    //Marker 相关的参数
    private int markerSizeDp = 64;

    //构造函数
    public LissajousMarkerView(Context context) {
        super(context);
        init();
    }

    //想要在布局中用加入控件的方式加入自己的surfaceview必须有此两个参数的构造函数
    public LissajousMarkerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //初始化
        init();
    }

//    public void setListener(LissajousMarkerFinishListener mListener) {
//        this.mListener = mListener;
//    }

    //初始化
    private void init() {
        //调用getHolder()，获得当前SurfaceView中的surface对应的SurfaceHolder
        mHolder = this.getHolder();
        //调用addCallback(),为SurfaceHolder添加一个SurfaceHolder.Callback回调接口。
        mHolder.addCallback(this);
        //surfaceView 背景透明
        this.setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSLUCENT);
        //实例化画笔
        mPaint = new Paint();
        mBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.marker2);
        int size = UnitUtil.dip2px(markerSizeDp);
        mBitmap = BitmapUtils.scaleBitmap(mBitmap, size, size);
        mPaint.setAntiAlias(true);//设置是否抗锯齿
        mPaint.setStyle(Paint.Style.STROKE);//设置画笔风格
        //其他需要的初始化....
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //surface创建时触发，渲染线程标志置true
        isDraw = true;
        //开启线程进行绘制
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //do
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Surface销毁时触发，渲染线程标志置false，以便结束线程
        isDraw = false;
        isDrawMarker = false;
        isDrawLissajous = false;
    }

    @Override
    public void run() {
        Looper.prepare();
        while (isDraw) {
            autoDraw();
        }
    }

    public void drawLissajous() {
        isDrawLissajous = true;
        startTime = System.nanoTime();
    }


    public void autoDraw() {
        try {
            //调用lockCanvas()方法得到画布
            mCanvas = mHolder.lockCanvas();
//                    mCanvas.drawColor(Color.WHITE);
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //绘制代码同Canvas用法
            if (isDrawMarker) {
                if (isDrawLissajous) {
                    interval = (System.nanoTime() - startTime) / 1000000000d;
                }
                if (interval <= duration) {
                    x = (float) calculateX(interval);
                    y = (float) calculateY(interval);
                    mCanvas.drawBitmap(mBitmap, x, y, mPaint);
                } else {
                    isDrawLissajous = false;
                    isDrawMarker = false;
                    isDraw = false;
//                    mListener.onLissajousMarkerComplete();
                }
            } //执行绘制操作
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "" + e.getMessage());
        } finally {
            try {
                mHolder.unlockCanvasAndPost(mCanvas);
            } catch (Exception ignore) {
            }
        }
    }


    public int getMarkerSizeDp() {
        return markerSizeDp;
    }

    public void setMarkerSizeDp(int markerSizeDp) {
        this.markerSizeDp = markerSizeDp;
        int size = UnitUtil.dip2px(markerSizeDp);
        mBitmap = BitmapUtils.scaleBitmap(mBitmap, size, size);
    }

    public boolean isDrawMarker() {
        return isDrawMarker;
    }

    public void setDrawMarker(boolean drawMarker) {
        isDrawMarker = drawMarker;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public double calculateX(double interval) {
        double markSize = UnitUtil.dip2px(markerSizeDp);
        return (ampX - markSize / 2) * (Math.sin(Math.PI * 2 * freqX * interval + phaseX) + 1d);
    }

    public double calculateY(double interval) {
        double markSize = UnitUtil.dip2px(markerSizeDp);
        return (ampY - markSize / 2) * (Math.sin(Math.PI * 2 * freqY * interval + phaseY) + 1d);
    }

    public float covertPecPosX(float x) {
        return (x + UnitUtil.dip2px(markerSizeDp) / ((float) 2)) / getWidth();
    }

    public float covertPecPosY(float y) {
        return (y + UnitUtil.dip2px(markerSizeDp) / ((float) 2)) / getHeight();
    }

    public void setParameters(double ampX, double ampY, double freqX, double freqY, double phaseX, double phaseY) {
        this.ampX = ampX;
        this.ampY = ampY;
        this.freqX = freqX;
        this.freqY = freqY;
        this.phaseX = phaseX;
        this.phaseY = phaseY;
        this.duration = multiple((int)Math.round(1 / freqX), (int)Math.round(1 / freqY));
    }

    //定义一个求最大公约数的额方法
    private int divisor(int num1, int num2) {
        int temp;
        while (num1 != 0) {
            temp = num1;
            num1 = num2 % num1;
            num2 = temp;
        }
        return num2;
    }

    //定义一个求最小公倍数的方法
    private int multiple(int num1, int num2) {
        return (num1 * num2) / divisor(num1, num2);
    }

    public float getMarkerPosX() {
        return covertPecPosX(x);
    }

    public float getMarkerPosY() {
        return covertPecPosY(y);
    }
}