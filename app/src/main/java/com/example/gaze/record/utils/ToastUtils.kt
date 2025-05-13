package com.example.imagesave.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.gaze.record.app.MyApp
import java.lang.reflect.Field

/**
 * toast工具类
 */
object ToastUtils {
    private const val TAG = "ToastUtil"
    private var mToast: Toast? = null
    private var sField_TN: Field? = null
    private var sField_TN_Handler: Field? = null
    private var sIsHookFieldInit = false
    private const val FIELD_NAME_TN = "mTN"
    private const val FIELD_NAME_HANDLER = "mHandler"
    private fun showToast(
        context: Context, text: CharSequence,
        duration: Int, isShowCenterFlag: Boolean
    ) {
        val toastRunnable = ToastRunnable(context, text, duration, isShowCenterFlag)
        if (context is Activity) {
            if (!context.isFinishing) {
                context.runOnUiThread(toastRunnable)
            }
        } else {
            val handler = Handler(context.mainLooper)
            handler.post(toastRunnable)
        }
    }

    fun shortToast(context: Context, text: CharSequence) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    fun longToast(context: Context, text: CharSequence) {
        showToast(context, text, Toast.LENGTH_LONG, false)
    }

    fun shortToast(msg: String) {
        Toast.makeText(MyApp.getInstance(), msg, Toast.LENGTH_SHORT).show();
    }

    fun shortToast(@StringRes resId: Int) {
        showToast(
            MyApp.getInstance(), MyApp.getInstance().getText(resId),
            Toast.LENGTH_SHORT, false
        )
    }

    fun centerShortToast(msg: String) {
        showToast(MyApp.getInstance(), msg, Toast.LENGTH_SHORT, true)
    }

    fun centerShortToast(@StringRes resId: Int) {
        showToast(
            MyApp.getInstance(), MyApp.getInstance().getText(resId),
            Toast.LENGTH_SHORT, true
        )
    }

    fun cancelToast() {
        val looper = Looper.getMainLooper()
        if (looper.thread === Thread.currentThread()) {
            mToast!!.cancel()
        } else {
            Handler(looper).post { mToast!!.cancel() }
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private fun hookToast(toast: Toast?) {
        try {
            if (!sIsHookFieldInit) {
                sField_TN = Toast::class.java.getDeclaredField(FIELD_NAME_TN)
                sField_TN?.run {
                    isAccessible = true
                    sField_TN_Handler = type.getDeclaredField(FIELD_NAME_HANDLER)
                }
                sField_TN_Handler?.isAccessible = true
                sIsHookFieldInit = true
            }
            val tn = sField_TN!![toast]
            val originHandler = sField_TN_Handler!![tn] as Handler
            sField_TN_Handler!![tn] = SafelyHandlerWrapper(originHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Hook toast exception=$e")
        }
    }

    private class ToastRunnable(
        private val context: Context,
        private val text: CharSequence,
        private val duration: Int,
        private val isShowCenter: Boolean
    ) : Runnable {
        @SuppressLint("ShowToast")
        override fun run() {
            if (mToast == null) {
                mToast = Toast.makeText(context, text, duration)
            } else {
                mToast!!.setText(text)
                if (isShowCenter) {
                    mToast!!.setGravity(Gravity.CENTER, 0, 0)
                }
                mToast!!.duration = duration
            }
            hookToast(mToast)
            mToast!!.show()
        }
    }

    private class SafelyHandlerWrapper(private val originHandler: Handler?) : Handler() {
        override fun dispatchMessage(msg: Message) {
            try {
                super.dispatchMessage(msg)
            } catch (e: Exception) {
                Log.e(TAG, "Catch system toast exception:$e")
            }
        }

        override fun handleMessage(msg: Message) {
            originHandler?.handleMessage(msg)
        }
    }
}