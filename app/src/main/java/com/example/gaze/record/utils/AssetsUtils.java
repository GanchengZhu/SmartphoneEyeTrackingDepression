package com.example.gaze.record.utils;

import com.example.gaze.record.app.MyApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AssetsUtils {
    public static boolean copyFromAssetsToMaterialPath(String scrAssetPath, String desSdPath) {
        boolean isSuccess;
        try {
            String fileNames[] = MyApp.getInstance().getAssets().list(scrAssetPath);
            if (fileNames.length > 0) {
                File file = new File(desSdPath);
                if (!file.exists()) file.mkdirs();
                for (String fileName : fileNames) {
                    if (!scrAssetPath.equals("")) { // assets 文件夹下的目录
                        copyFromAssetsToMaterialPath(scrAssetPath + File.separator + fileName, desSdPath + File.separator + fileName);
                    } else { // assets 文件夹
                        copyFromAssetsToMaterialPath(fileName, desSdPath + File.separator + fileName);
                    }
                }
            } else {
                File outFile = new File(desSdPath);
                InputStream is = MyApp.getInstance().getAssets().open(scrAssetPath);
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
        }
        return isSuccess;
    }

}
