package com.example.gaze.record.motion;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.example.gaze.record.R;
import com.example.gaze.record.app.MyApp;
import com.example.gaze.record.utils.BitmapUtils;
import com.example.gaze.record.utils.UnitUtil;

import java.util.List;

public class FivePointMotion extends BaseMotion<FivePointMotion> {
    int size;
    final int markerSizeDp = 64;
    Bitmap bitmap;
    private float markerHeight;
    private float markerWidth;
    float[] p1;
    float[] p2;
    float[] p3;
    float[] p4;
    float[] p5;
    float[][] allPos;
    int curIndex;

    public float getMarkerHeight() {
        return markerHeight;
    }

    public FivePointMotion setMarkerHeight(float markerHeight) {
        this.markerHeight = markerHeight;
        return this;
    }

    public float getMarkerWidth() {
        return markerWidth;
    }

    public FivePointMotion setMarkerWidth(float markerWidth) {
        this.markerWidth = markerWidth;
        return this;
    }

    public FivePointMotion setParameter(float parentHeight, float parentWidth) {
        bitmap = BitmapFactory.decodeResource(MyApp.getInstance().getResources(),
                R.drawable.marker2);
        size = UnitUtil.dip2px(markerSizeDp);
        bitmap = BitmapUtils.scaleBitmap(bitmap, size, size);
        markerHeight = size;
        markerWidth = size;
        float factorY = (parentHeight - size) / (3 - 1);
        float factorX = (parentWidth - size) / (3 - 1);
        p1 = new float[]{factorX * 0, factorY * 0};
        p2 = new float[]{factorX * 2, factorY * 0};
        p3 = new float[]{factorX * 1, factorY * 1};
        p4 = new float[]{factorX * 0, factorY * 2};
        p5 = new float[]{factorX * 2, factorY * 2};
        allPos = new float[][]{p1, p2, p3, p4, p5};
        return this;
    }

    @Override
    public float covertPecPosX(float x, int width) {
        return 0;
    }

    @Override
    public float covertPecPosY(float y, int height) {
        return 0;
    }

    @Override
    public void drawMotion(Canvas canvas, Paint paint, long nowTime) {
        if (!isMotion) {
            return;
        }
        float deltaTime = (nowTime - getStartTime()) / 1000000000f;
        if (deltaTime < 10f) {
            curIndex = 0;
        } else if (deltaTime >= 10f && deltaTime < 20f) {
            curIndex = 1;
        } else if (deltaTime >= 20f && deltaTime < 30f) {
            curIndex = 2;
        } else if (deltaTime >= 30f && deltaTime < 40f) {
            curIndex = 3;
        } else if (deltaTime >= 40f && deltaTime < 50f) {
            curIndex = 4;
        } else {
            if (motionListener != null) {
                motionListener.onMotionEnd();
            }
            isMotion = false;
            return;
        }
        x = allPos[curIndex][0];
        y = allPos[curIndex][1];
        canvas.drawBitmap(bitmap, x, y, paint);

    }

    @Override
    public List<Float> computerXY(float deltaTime) {
        return null;
    }
}
