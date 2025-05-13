package com.example.gaze.record.motion;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.gaze.record.R;
import com.example.gaze.record.app.MyApp;
import com.example.gaze.record.utils.BitmapUtils;
import com.example.gaze.record.utils.CalculatorUtils;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;
import com.example.gaze.record.utils.UnitUtil;

import java.util.Arrays;
import java.util.List;

public class HorizontalSinusoidMotion extends BaseMotion<HorizontalSinusoidMotion> {
    //Lissajous Curve 相关的参数
    private double ampX = 900d;
    private double ampY = 450d;
    private double freqX = 1 / 30d;
    private double freqY = 1 / 25d;
    private double phaseX = Math.PI * 3 / 2;
    private double phaseY = 0;
    final float markerSizeDp = 64;
    private int size;
    private double stayInterval;
    private double moveInterval;


    @Override
    public HorizontalSinusoidMotion setParameter(float parentHeight, float parentWidth) {
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        size = UnitUtil.dip2px(markerSizeDp);
        this.ampX = (parentWidth - size) / 2d;
        this.ampY = (parentHeight - size) / 2d;
        this.freqX = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.FREQ_X_HORIZONTAL_SINUSOID, "1/10"));
        this.freqY = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.FREQ_Y_HORIZONTAL_SINUSOID, "1/12"));
        this.phaseX = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.PHASE_X_HORIZONTAL_SINUSOID, "0.0"));
        stayInterval = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.STAY_INTERVAL_HORIZONTAL_SINUSOID, "2.0"));
        this.duration = multiple((int) Math.round(1 / freqX), (int) Math.round(1 / freqY));
        bitmap = BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
                R.drawable.marker);
        markerWidth = size;
        markerHeight = size;
        bitmap = BitmapUtils.scaleBitmap(bitmap, size, size);
        return this;

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

    @Override
    public void drawMotion(Canvas canvas, Paint paint, long nowTime) {
        // prepare
        if (isDrawPreparation && !isMotion) {
            deltaTime = (nowTime - prepareStartTime) / 1000000000f;
            if (deltaTime < stayInterval + moveInterval) {
                x = (float) calculateX(0);
                y = (float) calculateY(0);
                canvas.drawBitmap(bitmap, x, y, paint);
            } else {
                isDrawPreparation = false;
                isMotion = true;
                startTime = nowTime;
                deltaTime = 0f;
                motionListener.onMotionStart();
            }
        }
        if (!isMotion) return;
        deltaTime = (nowTime - startTime) / 1000000000f;
        if (deltaTime < duration) {
            x = (float) calculateX(deltaTime);
            y = (float) calculateY(deltaTime);
            canvas.drawBitmap(bitmap, x, y, paint);
        } else {
            isMotion = false;
            isDrawPreparation = false;
//            if (motionListener != null) motionListener.onMotionEnd();
        }
    }

    public double calculateX(double interval) {
        return ampX * (Math.sin(Math.PI * 2d * freqX * interval + phaseX) + 1d);
    }

    public double calculateY(double interval) {
        return (parentHeight - markerHeight) / 2f;
//        return (ampY - size / 2d) * (Math.sin(Math.PI * 2d * freqY * interval + phaseY) + 1d);
    }

    public float covertPecPosX(float x, int width) {
        return (x + markerWidth / 2f) / width;
    }

    public float covertPecPosY(float y, int height) {
        return (y + markerHeight / 2f) / height;
    }


    @Override
    public List<Float> computerXY(float deltaTime) {
        float resX = (float) ((float) ampY * (Math.sin(Math.PI * 2d * freqY * deltaTime + phaseY) + 1d));
        float resY = (parentHeight - markerHeight) / 2f;
        return Arrays.asList(resX, resY);
    }

}
