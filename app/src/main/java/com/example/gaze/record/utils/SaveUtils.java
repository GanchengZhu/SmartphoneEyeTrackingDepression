/*******************************************************************************
 * Copyright (C) 2022 Gancheng Zhu
 * EasyPsycho is a Free Software project under the GNU Affero General
 * Public License v3, which means all its code is available for everyone
 * to download, examine, use, modify, and distribute, subject to the usual
 * restrictions attached to any GPL software. If you are not familiar with the AGPL,
 * see the COPYING file for for more details on license terms and other legal issues.
 ******************************************************************************/

package com.example.gaze.record.utils;


import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

public class SaveUtils implements Runnable {
    BlockingQueue<MyData> blockingQueue;

    public void setStop(boolean stop) {
        isStop = stop;
    }

    boolean isStop;

    public SaveUtils(BlockingQueue<MyData> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {
        while (!isStop) {
            try {
                MyData myData = blockingQueue.take();
                Mat mRgbaTmp = new Mat();
                Imgproc.cvtColor(myData.mat, mRgbaTmp, Imgproc.COLOR_RGBA2BGR);
                Imgcodecs.imwrite(myData.string, mRgbaTmp);
                myData.mat.release();
                mRgbaTmp.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

