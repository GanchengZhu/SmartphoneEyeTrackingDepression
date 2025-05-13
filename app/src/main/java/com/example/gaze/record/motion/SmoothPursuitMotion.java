/*******************************************************************************
 * Copyright (C) 2022 Gancheng Zhu
 * EasyPsycho is a Free Software project under the GNU Affero General
 * Public License v3, which means all its code is available for everyone
 * to download, examine, use, modify, and distribute, subject to the usual
 * restrictions attached to any GPL software. If you are not familiar with the AGPL,
 * see the COPYING file for for more details on license terms and other legal issues.
 ******************************************************************************/

package com.example.gaze.record.motion;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;

import com.example.gaze.record.R;
import com.example.gaze.record.app.MyApp;
import com.example.gaze.record.utils.BitmapUtils;
import com.example.gaze.record.utils.CalculatorUtils;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;
import com.example.gaze.record.utils.UnitUtil;

import java.util.Arrays;
import java.util.List;

public class SmoothPursuitMotion extends BaseMotion<SmoothPursuitMotion> {
    final float markerSizeDp = 64;
    //Lissajous Curve 相关的参数
    private double ampX = 900d;
    private double ampY = 450d;
    private double freqX = 1 / 36d;
    private double freqY = 1 / 24d;
    private double phaseX = 0;
    private double phaseY = 0;
    private int size;
    private double stayInterval;
    private double moveInterval;


    @Override
    public SmoothPursuitMotion setParameter(float parentHeight, float parentWidth) {
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        this.ampX = parentWidth / 2d - 75;
        this.ampY = parentHeight / 2d - 75;
        this.freqX = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.FREQ_X_SMOOTH_PURSUIT, "1/32"));
        this.freqY = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.FREQ_Y_SMOOTH_PURSUIT, "1/24"));
        this.phaseX = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.PHASE_X_SMOOTH_PURSUIT, "0"));
        this.phaseY = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.PHASE_Y_SMOOTH_PURSUIT, "0"));
        stayInterval = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.STAY_INTERVAL_SMOOTH_PURSUIT, "1.0"));
//        moveInterval = CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.MOVE_INTERVAL_SMOOTH_PURSUIT, "2.0"));
        this.duration = multiple((int) Math.round(1 / freqX), (int) Math.round(1 / freqY));
        bitmap = BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
                R.drawable.marker);
        size = UnitUtil.dip2px(markerSizeDp);
        Log.d(TAG, "marker size: " + size);
        markerWidth = size;
        markerHeight = size;
        bitmap = BitmapUtils.scaleBitmap(bitmap, size, size);

//        for (int i = 0; i < duration * 60; i++) {
//            // i / 60
//            System.out.println((i / 60) + "," + calculateX(i / 60f) + "," + calculateY(i));
//        }


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
        if (prepareStartTime == 0) {
            prepareStartTime = nowTime;
        }

        // prepare
        if (isDrawPreparation && !isMotion) {
            deltaTime = (nowTime - prepareStartTime) / 1000000000f;
            if (deltaTime < stayInterval + moveInterval) {
                x = (float) calculateX(0F);
                y = (float) calculateY(0F);
                x += 75;
                y += 75;
                canvas.drawBitmap(bitmap, x, y, paint);
            } else {
                isDrawPreparation = false;
                isMotion = true;
                startTime = nowTime;
                if (motionListener != null) motionListener.onMotionStart();
                deltaTime = 0f;
            }
        }

        if (!isMotion) return;

        deltaTime = (nowTime - startTime) / 1000000000f;
        if (deltaTime <= duration) {
            x = (float) calculateX(deltaTime);
            y = (float) calculateY(deltaTime);
            x += 75;
            y += 75;
            System.out.println(deltaTime);
            System.out.println("[" + x + "," + y + "]");
            canvas.drawBitmap(bitmap, x, y, paint);
        } else {
            isMotion = false;
            isDrawPreparation = false;
            if (motionListener != null) motionListener.onMotionEnd();
        }
    }

    public double calculateX(double interval) {
        // +1是需要将笛卡尔坐标系转成Android坐标系
//        System.out.println("ampX = " + ampX);
//        System.out.println("freqX = " + freqX);
//        System.out.println("phaseX = " + phaseX);
        return (ampX - size / 2d) * (Math.sin(Math.PI * 2d * freqX * interval + phaseX) + 1d);
    }

    public double calculateY(double interval) {
//        System.out.println("ampY = " + ampY);。0
//        System.out.println("freqY = " + freqY);
//        System.out.println("phaseY = " + phaseY);
//        System.out.println("size = " + size);
        return (ampY - size / 2d) * (Math.sin(Math.PI * 2d * freqY * interval + phaseY) + 1d);
    }

    public float covertPecPosX(float x, int width) {
        return (x + markerWidth / 2f) / width;
    }

    public float covertPecPosY(float y, int height) {
        return (y + markerHeight / 2f) / height;
    }


    @Override
    public List<Float> computerXY(float deltaTime) {
        float resX = (float) ((ampY - size / 2d) * (Math.sin(Math.PI * 2d * freqY * deltaTime + phaseY) + 1d));
        float resY = (float) ((ampX - size / 2d) * (Math.sin(Math.PI * 2d * freqX * deltaTime + phaseX) + 1d));
        return Arrays.asList(resX, resY);
    }

}
