package com.example.gaze.record.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.gaze.record.app.MyApp;

import java.io.File;

/**

 * 文件存储目录管理工具类
 */
public class FileManager {
    // 媒体模块根目录
    private static final String SAVE_MEDIA_ROOT_DIR = Environment.DIRECTORY_DCIM;
    // 媒体模块存储路径
    private static final String SAVE_MEDIA_DIR = SAVE_MEDIA_ROOT_DIR + "/imagesave";
    private static final String AVATAR_DIR = "/avatar";
    private static final String SAVE_MEDIA_VIDEO_DIR = SAVE_MEDIA_DIR + "/video";
    private static final String SAVE_MEDIA_PHOTO_DIR = SAVE_MEDIA_DIR + "/photo";
    // JPG后缀
    public static final String JPG_SUFFIX = ".jpg";
    // PNG后缀
    public static final String PNG_SUFFIX = ".png";
    // MP4后缀
    public static final String MP4_SUFFIX = ".mp4";
    // YUV后缀
    public static final String YUV_SUFFIX = ".yuv";
    // h264后缀
    public static final String H264_SUFFIX = ".h264";


    /**
     * 保存图片到系统相册
     *
     * @param context
     * @param file
     */
    public static String saveImage(Context context, File file) {
        ContentResolver localContentResolver = context.getContentResolver();
        ContentValues localContentValues = getImageContentValues(context, file, System.currentTimeMillis());
        localContentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, localContentValues);

        Intent localIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
        final Uri localUri = Uri.fromFile(file);
        localIntent.setData(localUri);
        context.sendBroadcast(localIntent);
        return file.getAbsolutePath();
    }

    public static ContentValues getImageContentValues(Context paramContext, File paramFile, long paramLong) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("title", paramFile.getName());
        localContentValues.put("_display_name", paramFile.getName());
        localContentValues.put("mime_type", "image/jpeg");
        localContentValues.put("datetaken", Long.valueOf(paramLong));
        localContentValues.put("date_modified", Long.valueOf(paramLong));
        localContentValues.put("date_added", Long.valueOf(paramLong));
        localContentValues.put("orientation", Integer.valueOf(0));
        localContentValues.put("_data", paramFile.getAbsolutePath());
        localContentValues.put("_size", Long.valueOf(paramFile.length()));
        return localContentValues;
    }

    /**
     * 获取App存储根目录
     */
    public static String getAppRootDir() {
        String path = getStorageRootDir();
        FileUtil.createOrExistsDir(path);
        return path;
    }

    /**
     * 获取文件存储根目录
     */
    public static String getStorageRootDir() {
        File filePath = MyApp.getInstance().getExternalFilesDir("");
        String path;
        if (filePath != null) {
            path = filePath.getAbsolutePath();
        } else {
            path = MyApp.getInstance().getFilesDir().getAbsolutePath();
        }
        return path;
    }

    /**
     * 图片地址
     */
    public static String getCameraPhotoPath() {
        return getFolderDirPath(SAVE_MEDIA_PHOTO_DIR);
    }

    /**
     * 获取拍照普通图片文件
     */
    public static File getSavedPictureFile(long timeStamp) {
        String fileName = "image"+ "_"+ + timeStamp + JPG_SUFFIX;
        return new File(getCameraPhotoPath(), fileName);
    }

    /**
     * 头像地址
     */
    public static String getAvatarPath(String fileName) {
        String path;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            path = getFolderDirPath(SAVE_MEDIA_DIR + AVATAR_DIR);
        } else {
            path = getSaveDir(AVATAR_DIR);
        }
        return path + File.separator + fileName;
    }

    /**
     * 视频地址
     */
    public static String getCameraVideoPath() {
        return getFolderDirPath(SAVE_MEDIA_VIDEO_DIR);
    }

    public static String getFolderDirPath(String dstDirPathToCreate) {
        File dstFileDir = new File(Environment.getExternalStorageDirectory(), dstDirPathToCreate);
        if (!dstFileDir.exists() && !dstFileDir.mkdirs()) {
            Log.e("Failed to create file", dstDirPathToCreate);
            return null;
        }
        return dstFileDir.getAbsolutePath();
    }

    /**
     * 获取具体模块存储目录
     */
    public static String getSaveDir(@NonNull String directory) {
        String path = "";
        if (TextUtils.isEmpty(directory) || "/".equals(directory)) {
            path = "";
        } else if (directory.startsWith("/")) {
            path = directory;
        } else {
            path = "/" + directory;
        }
        path = getAppRootDir() + path;
        FileUtil.createOrExistsDir(path);
        return path;
    }

    /**
     * 通过媒体文件Uri获取文件-Android 11兼容
     *
     * @param fileUri 文件Uri
     */
    public static File getMediaUri2File(Uri fileUri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = MyApp.getInstance().getContentResolver().query(fileUri, projection,
                null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String path = cursor.getString(columnIndex);
                cursor.close();
                return new File(path);
            }
        }
        return null;
    }

    /**
     * 根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换
     *
     * @param context  上下文
     * @param imageUri 图片地址
     */
    public static String getImageAbsolutePath(Activity context, Uri imageUri) {
        if (context == null || imageUri == null)
            return null;
        if (DocumentsContract.isDocumentUri(context, imageUri)) {
            if (isExternalStorageDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(imageUri)) {
                String id = DocumentsContract.getDocumentId(imageUri);
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(imageUri)) {
                String docId = DocumentsContract.getDocumentId(imageUri);
                String[] split = docId.split(":");
                String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selection = MediaStore.Images.Media._ID + "=?";
                String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } // MediaStore (and general)
        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(imageUri))
                return imageUri.getLastPathSegment();
            return getDataColumn(context, imageUri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
            return imageUri.getPath();
        }
        return null;
    }

    /**
     * @param uri The Uri to checkRemote.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to checkRemote.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to checkRemote.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to checkRemote.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String column = MediaStore.Images.Media.DATA;
        String[] projection = {column};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        return null;
    }
}
