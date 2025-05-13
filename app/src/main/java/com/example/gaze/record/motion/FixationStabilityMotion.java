/*******************************************************************************
 * Copyright (C) 2022 Gancheng Zhu
 * EasyPsycho is a Free Software project under the GNU Affero General
 * Public License v3, which means all its code is available for everyone
 * to download, examine, use, modify, and distribute, subject to the usual
 * restrictions attached to any GPL software. If you are not familiar with the AGPL,
 * see the COPYING file for for more details on license terms and other legal issues.
 ******************************************************************************/

package com.example.gaze.record.motion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import com.example.gaze.record.R;
import com.example.gaze.record.app.MyApp;
import com.example.gaze.record.utils.BitmapUtils;
import com.example.gaze.record.utils.CalculatorUtils;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;
import com.example.gaze.record.utils.UnitUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class FixationStabilityMotion extends BaseMotion<FixationStabilityMotion> {
    int count;
    float markerSizeDp = 64;
    List<List<Float>> positionList = new ArrayList<>();
    List<Float> showIntervalList = new ArrayList<>(); //刺激显示的时间间隔
    List<Float> dismissIntervalList = new ArrayList<>(); //刺激消失的时间间隔
    private float centerX;
    private float centerY;
    Random random = new Random(2022);
    Random randomShowInterval = new Random(2023);

    public float x1, x2, y1, y2;
    protected long transStartTime;
    protected int positionIndex;
    protected float curTime;
    protected boolean isDrawShow = true;
    protected float prepareInterval;
    private final float fragmentInterval = 3; //showInterval + dismissInterval
    private Bitmap centerBitmap;
    private final float centerDp = 48;
    private List<Long> observeDistractEndTimeList;
    private List<Long> observeDistractStartTimeList;

    @Override
    public FixationStabilityMotion setParameter(float parentHeight, float parentWidth) {
        this.parentHeight = parentHeight;
        this.parentWidth = parentWidth;
        count = (int) CalculatorUtils.conversion(SharedPreferencesUtils.getString(Constants.RANDOM_COUNT_FIXATION_STABILITY, "30"));
        bitmap = BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
                R.drawable.marker_red);
        int size = UnitUtil.dip2px(markerSizeDp);
        bitmap = BitmapUtils.scaleBitmap(bitmap, size, size);
        markerHeight = size;
        markerWidth = size;
        int centerSize = UnitUtil.dip2px(centerDp);
        centerBitmap = BitmapUtils.scaleBitmap(BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
                R.drawable.marker), centerSize, centerSize);
        centerX = (parentWidth - markerWidth) / 2;
        centerY = (parentHeight - markerHeight) / 2;
        prepareInterval = (float) CalculatorUtils.conversion(
                SharedPreferencesUtils.getString(Constants.STAY_INTERVAL_FIXATION_STABILITY, "2.0"));
        initRandom();
        Log.d(TAG, String.valueOf(positionList));
        Log.d(TAG, String.valueOf(showIntervalList));
        Log.d(TAG, String.valueOf(dismissIntervalList));

        observeDistractEndTimeList = new ArrayList<>(count);
        observeDistractStartTimeList = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            observeDistractStartTimeList.add(0L);
            observeDistractEndTimeList.add(0L);
        }

        return this;
    }

    private void initRandom() {
        float radius = (parentWidth - markerWidth) / 2f;// 圆的半径
        double radians;
        float showInterval;
        float x, y;
        for (int i = 0; i < count; i++) {
            radians = random.nextDouble() * 2d * Math.PI;
            x = (parentWidth - markerWidth) / 2 + (float) Math.cos(radians) * radius;
            y = (parentHeight - markerHeight) / 2 - (float) Math.sin(radians) * radius;
            positionList.add(Arrays.asList(x, y));
            showInterval = (float) (0.5 * randomShowInterval.nextDouble() + 2);
            showIntervalList.add(showInterval);
            dismissIntervalList.add(fragmentInterval - showInterval);
        }
        duration = count * fragmentInterval;
    }


    @Override
    public void drawMotion(Canvas canvas, Paint paint, long nowTime) {
        // prepare
        if (isDrawPreparation && !isMotion) {
            deltaTime = (nowTime - prepareStartTime) / 1000000000f;
            if (deltaTime < prepareInterval) {
                canvas.drawBitmap(centerBitmap, centerX, centerY, paint);
            } else {
                isDrawPreparation = false;
                isMotion = true;
                startTime = nowTime;
                motionListener.onMotionStart();
            }
        }
        if (!isMotion) return;
        deltaTime = (nowTime - startTime) / 1000000000f;

        if (deltaTime >= duration || positionIndex == positionList.size()) {
            isDrawPreparation = false;
            isMotion = false;
            if (motionListener != null) {
                motionListener.onMotionEnd();
            }
            return;
        }

        if (isDrawShow) {
            if (positionIndex != observeDistractStartTimeList.size() && observeDistractStartTimeList.get(positionIndex) == 0) {
                observeDistractStartTimeList.set(positionIndex, nowTime);
            }
            canvas.drawBitmap(centerBitmap, centerX, centerY, paint);
            drawShow(nowTime);
            canvas.drawBitmap(bitmap, x, y, paint);
        } else {
            canvas.drawBitmap(centerBitmap, centerX, centerY, paint);
            if (positionIndex != observeDistractEndTimeList.size() && observeDistractEndTimeList.get(positionIndex) == 0) {
                observeDistractEndTimeList.set(positionIndex, nowTime);
            }
            drawDismiss(nowTime);
        }
//            drawMove(nowTime);
    }

    protected void drawShow(long nowTime) {
        if (positionIndex == positionList.size()) return;
        if (isDrawShow && deltaTime < curTime + showIntervalList.get(positionIndex) && deltaTime < duration) {
            // stay
            x = positionList.get(positionIndex).get(0);
            y = positionList.get(positionIndex).get(1);
            isTrans = false;
        } else {
//            transTime = (long) (nowTime - (deltaTime - (int) deltaTime) * 1000000000);
            curTime += showIntervalList.get(positionIndex);
            transStartTime = (long) (startTime + curTime * 1000000000L);
            isDrawShow = false;
            drawDismiss(nowTime);
        }
    }

    float intervalTrans;

    protected void drawDismiss(long nowTime) {
        if (positionIndex == positionList.size() - 1) return;
        if (!isDrawShow && deltaTime < curTime + dismissIntervalList.get(positionIndex) && deltaTime < duration) {
            // stay
//            x1 = positionList.get(positionIndex).get(0);
//            y1 = positionList.get(positionIndex).get(1);
//            x2 = positionList.get(positionIndex + 1).get(0);
//            y2 = positionList.get(positionIndex + 1).get(1);
            intervalTrans = (nowTime - transStartTime) / 1000000000f;
//            x = x1 + intervalTrans * (x2 - x1) / moveInterval;
//            y = y1 + intervalTrans * (y2 - y1) / moveInterval;
//            x = 0;
//            y = 0;
            isTrans = true;
        } else {
            curTime += dismissIntervalList.get(positionIndex);
            isDrawShow = true;
            positionIndex += 1;
            drawShow(nowTime);
        }
    }

    @Override
    public List<Float> computerXY(float deltaTime) {
        return null;
    }

    public float covertPecPosX(float x, int width) {
        return (x + markerWidth / 2f) / width;
    }

    public float covertPecPosY(float y, int height) {
        return (y + markerHeight / 2f) / height;
    }

    public List<Float> getRes1() {
        return Arrays.asList(x1, y1);
    }

    public List<Float> getRes2() {
        return Arrays.asList(x2, y2);
    }

    public List<Long> getObserveDistractEndTimeList() {
        return observeDistractEndTimeList;
    }

    public List<Long> getObserveDistractStartTimeList() {
        return observeDistractStartTimeList;
    }
}
