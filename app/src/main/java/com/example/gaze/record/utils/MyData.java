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

public class MyData {
    Mat mat;
    String string;

    public MyData(Mat mat, String string) {
        this.mat = mat;
        this.string = string;
    }
}
