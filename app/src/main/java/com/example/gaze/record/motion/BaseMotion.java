package com.example.gaze.record.motion;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Arrays;
import java.util.List;

public abstract class BaseMotion<T extends BaseMotion> {
    protected long startTime;
    protected float x;
    protected float y;
    protected double duration;
    protected boolean isMotion = false;
    protected boolean isDrawPreparation = true;
    protected boolean isTargetMoving = false;
    protected float parentWidth;
    protected float parentHeight;
    protected float markerHeight;
    protected float markerWidth;
    protected long prepareStartTime;
    protected double deltaTime;
    protected boolean isTrans;
    protected Bitmap bitmap;


    protected MotionListener motionListener;
    public final String TAG = getClass().getSimpleName();

    public T setListener(MotionListener motionListener) {
        this.motionListener = motionListener;
        return (T) this;
    }

    public T motionStart() {
        this.startTime = System.nanoTime();
        isDrawPreparation = true;
        isMotion = true;
        return (T) this;
    }

    public boolean isMotion() {
        return isMotion;
    }

    public T setMotion(boolean motion) {
        isMotion = motion;
        return (T) this;
    }

    public boolean isDrawTarget() {
        return isDrawPreparation;
    }

    public T setDrawPreparation(boolean draw) {
        prepareStartTime = System.nanoTime();
        isDrawPreparation = draw;
        return (T) this;
    }


    public double getDuration() {
        return duration;
    }

    public T setDuration(double duration) {
        this.duration = duration;
        return (T) this;
    }


    public float getStartTime() {
        return startTime;
    }

    public T setStartTime(long startTime) {
        this.startTime = startTime;
        return (T) this;
    }

    public float getX() {
        return x;
    }

    public T setX(float x) {
        this.x = x;
        return (T) this;
    }

    public float getY() {
        return y;
    }

    public T setY(float y) {
        this.y = y;
        return (T) this;
    }

    public T setXY(List<Float> xy) {
        assert xy.size() == 2;
        this.x = xy.get(0);
        this.y = xy.get(1);
        return (T) this;
    }

    public List<Float> getXY() {
        return Arrays.asList(x, y);
    }

    public abstract void drawMotion(Canvas canvas, Paint paint, long nowTime);

    public abstract List<Float> computerXY(float deltaTime);

    public boolean isTargetMoving() {
        return isTargetMoving;
    }

    public T setParameter(float parentHeight, float parentWidth) {
        this.parentHeight = parentHeight;
        this.parentWidth = parentWidth;
        return (T) this;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public void setDeltaTime(float deltaTime) {
        this.deltaTime = deltaTime;
    }

    public abstract float covertPecPosX(float x, int width);

    public abstract float covertPecPosY(float y, int height);


    public float getMarkerHeight() {
        return markerHeight;
    }

    public T setMarkerHeight(float markerHeight) {
        this.markerHeight = markerHeight;
        return (T) this;
    }

    public float getMarkerWidth() {
        return markerWidth;
    }

    public T setMarkerWidth(float markerWidth) {
        this.markerWidth = markerWidth;
        return (T) this;
    }

    public boolean isTrans() {
        return isTrans;
    }

    public void setTrans(boolean trans) {
        isTrans = trans;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isDrawPreparation() {
        return isDrawPreparation;
    }
}
