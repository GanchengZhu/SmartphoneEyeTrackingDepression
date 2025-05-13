package com.example.gaze.record.app;

import android.app.Application;

import com.kongzue.dialogx.DialogX;
//import com.tencent.bugly.crashreport.CrashReport;


public class MyApp extends Application {
    private static MyApp app = null;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        DialogX.init(this);
//        CrashReport.initCrashReport(getApplicationContext(), "da5697849b", true);
    }

    public static MyApp getInstance(){
        return app;
    }
}
