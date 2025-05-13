package com.example.gaze.record.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.gaze.record.activity.BaseActivity;
import com.example.gaze.record.app.MyApp;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiverUtil {
    private final String port;
    private final String protocol;
    private BaseActivity recordActivity;
    private UDPRunnable udpRunnable;
    private TCPRunnable tcpRunnable;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
    private Handler handler;

    public ReceiverUtil(BaseActivity baseActivity) {
        this.recordActivity = baseActivity;
        this.port = SharedPreferencesUtils.getString(Constants.PORT, "50880");
        this.protocol = SharedPreferencesUtils.getString(Constants.PROTOCOL, "udp");

    }

    public void setGetMassage(boolean isGetPackage) {
        if (udpRunnable != null) udpRunnable.setGetPackage(isGetPackage);
        else tcpRunnable.setGetPackage(isGetPackage);
    }

    public void startReceive(String path) {

        File file = new File(path);
        Log.i("ReceiverUtil", file.getAbsolutePath());
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;//指定以UTF-8格式写入文件
        if (!file.exists()) {
            try {
                file.createNewFile();//如果文件不存在，就创建该文件
                fos = new FileOutputStream(file);//首次写入获取
                osw = new OutputStreamWriter(fos, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("ReceiverUtil", e.getMessage());
                Log.e("ReceiverUtil", "Fail to create file: " + file.getAbsolutePath());
                Toast.makeText(MyApp.getInstance(), "Fail to create file: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }
        } else {
            //如果文件已存在，那么就在文件末尾追加写入
            try {
                fos = new FileOutputStream(file, true);//这里构造方法多了一个参数true,表示在文件末尾追加写入
                osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(MyApp.getInstance(), "Not found file: " + path, Toast.LENGTH_SHORT).show();
            }
        }

        if (osw != null) {
            if ("tcp".equals(protocol)) {
                tcpRunnable = new TCPRunnable(fos, osw, port, recordActivity);
                tcpRunnable.setHandler(handler);
                new Thread(tcpRunnable).start();
            } else {
                udpRunnable = new UDPRunnable(fos, osw, port, recordActivity);
                udpRunnable.setHandler(handler);
                new Thread(udpRunnable).start();
            }
        }

    }

    public void stopReceive() {
        if ("tcp".equals(protocol)) tcpRunnable.release();
        else udpRunnable.release();
        recordActivity = null;
    }

    public void setHandler(@NotNull Handler handler) {
        this.handler = handler;
    }
}

class TCPRunnable implements Runnable {
    private final FileOutputStream fos;
    private final OutputStreamWriter osw;
    private final String port;
    private final BaseActivity recordActivity;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
    private ServerSocket service;
    private AtomicBoolean isGetPackage;
    private Handler handler;

    public void release() {
        isStop = true;

    }

    TCPRunnable(FileOutputStream fos, OutputStreamWriter osw, String port, BaseActivity recordActivity) {
        this.fos = fos;
        this.osw = osw;
        this.port = port;
        this.recordActivity = recordActivity;
        isGetPackage = new AtomicBoolean(false);
    }

    public void setGetPackage(boolean getPackage) {
        isGetPackage.set(getPackage);
    }

    boolean isStop;

    @Override
    public void run() {
        Looper.prepare();
        try {
            service = new ServerSocket(Integer.parseInt(port)); //建立帮助连接的socket
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while (!service.isClosed() && !isStop) {
                //接受一个连接，该方法会阻塞程序，直到一个链接到来
                try (Socket connect = service.accept()) {
                    //获得输入流
                    InputStream is = connect.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    Log.i("ReceiverUtil", "开始接受和写入TCP");
                    while ((line = br.readLine()) != null) {
//                        String dateStr = simpleDateFormat.format(new Date());
//                        stringBuilder.append(dateStr);
//                        stringBuilder.append("\t");
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                        if (isGetPackage.get()) {
                            Message msg = Message.obtain(); // 实例化消息对象
                            msg.what = 25; // 消息标识
                            msg.obj = stringBuilder.toString(); // 消息内容存放
                            handler.sendMessage(msg);
                            isGetPackage.set(false);
                        }

                        if (osw != null) {
                            osw.write(stringBuilder.toString());
                            osw.flush();
                        }
                        stringBuilder.setLength(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (service != null) {
                try {
                    service.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ReceiverUtil", e.getMessage());
            Toast.makeText(MyApp.getInstance(), "Fail to create socket server!", Toast.LENGTH_SHORT).show();
        }
        try {
            fos.close();
            if (osw != null) osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }
}

class UDPRunnable implements Runnable {
    private final FileOutputStream fos;
    private final OutputStreamWriter osw;
    private final String port;
    private final BaseActivity recordActivity;
    //    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.CHINA);
    private DatagramSocket service;
    private final AtomicBoolean isGetPackage;
    private Handler handler;
    private boolean isStop;

    UDPRunnable(FileOutputStream fos, OutputStreamWriter osw, String port, BaseActivity recordActivity) {
        this.fos = fos;
        this.osw = osw;
        this.port = port;
        this.recordActivity = recordActivity;
        isGetPackage = new AtomicBoolean(false);
    }

    public void setHandler(@NotNull Handler handler) {
        this.handler = handler;
    }

    public void release() {
//        if (service != null) service.close();
        isStop = true;
    }


    public void setGetPackage(boolean getPackage) {
        isGetPackage.set(getPackage);
    }


    @Override
    public void run() {
        Looper.prepare();
        DatagramPacket dpReceive;
        try {
            if (service == null) {
                service = new DatagramSocket(null);
                service.setReuseAddress(true);
                service.bind(new InetSocketAddress(Integer.parseInt(port)));
            }
            StringBuilder stringBuilder = new StringBuilder();
            byte[] b = new byte[1024];
            Log.i("ReceiverUtil", "开始接受和写入UDP");
            while (!service.isClosed() && !isStop) {
                dpReceive = new DatagramPacket(b, b.length);
                service.receive(dpReceive);
                byte[] data = dpReceive.getData();
                int len = data.length;
//                String dateStr = simpleDateFormat.format(new Date());
//                stringBuilder.append(dateStr);
//                stringBuilder.append("\t");
                stringBuilder.append(new String(data, 0, len).trim());
                stringBuilder.append("\n");
                if (isGetPackage.get()) {
                    Message msg = Message.obtain(); // 实例化消息对象
                    msg.what = 25; // 消息标识
                    msg.obj = stringBuilder.toString(); // 消息内容存放
                    handler.sendMessage(msg);
                    Log.d("ReceiverUtil", "发送数据帧: " + msg.obj);
                    isGetPackage.set(false);
                }
                recordActivity.tipUpdateDataframe(stringBuilder.toString());
                if (osw != null) {
                    osw.write(stringBuilder.toString());
                    osw.flush();
                }
                stringBuilder.setLength(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.e("ReceiverUtil", e.getMessage());
            Toast.makeText(MyApp.getInstance(), "Fail to create socket server!", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                service.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                fos.close();
                if (osw != null) osw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
