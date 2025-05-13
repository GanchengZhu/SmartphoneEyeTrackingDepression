/*******************************************************************************
 * Copyright (C) 2022 Gancheng Zhu
 * EasyPsycho is a Free Software project under the GNU Affero General
 * Public License v3, which means all its code is available for everyone
 * to download, examine, use, modify, and distribute, subject to the usual
 * restrictions attached to any GPL software. If you are not familiar with the AGPL,
 * see the COPYING file for for more details on license terms and other legal issues.
 ******************************************************************************/

package com.example.gaze.record.activity;


import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.gaze.record.R;
import com.example.gaze.record.utils.Constants;
import com.example.gaze.record.utils.SharedPreferencesUtils;

import lib.gaze.tracker.core.GazeTracker;

public class ShowCalibrationActivity extends BaseActivity {
    ImageView imageView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageView = new ImageView(this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.drawable.calibration_grid);
        GazeTracker.getInstance().gazeSetting();
        setContentView(imageView);
        imageView.setOnClickListener(v -> {
            Intent intent = new Intent();
            switch (SharedPreferencesUtils.getString(Constants.MOTION_TYPE, "FreeViewing")) {
                case "FreeViewing":
                    intent = new Intent(this, PictureFreeViewingActivity.class);
                    break;
                case "SmoothPursuit":
                case "HorizontalSinusoid":
                case "FixationStability":
                case "VerticalSinusoid":
                    intent = new Intent(this, MarkMotionActivity.class);
                    break;
            }
            startActivity(intent);
            finish();
        });
    }

}
