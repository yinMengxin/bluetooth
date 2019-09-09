package com.example.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;


import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "yin_main_activity";
    private MyApplication myApplication;
    private BluetoothAdapter mbluetoothAdapter;
    //标题栏
    private Toolbar mToolbar;
    private OutputStream outputStream; //输出流
    private InputStream inputStream; //输入流
    private ClientThread clientThread; //客户端蓝牙连接线程

    private TextView main_toolBar_tv;

    private static final int REQUEST_ENABLE = 0x1; //请求能够打开蓝牙
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 0x2; //权限请求
    private final static int REQUEST_CONNECT_DEVICE = 0x3;    //宏定义查询设备句柄

    private com.xw.repo.BubbleSeekBar main_seekBar1, main_seekBar2, main_seekBar3, main_seekBar4, main_seekBar5;
    private Button bt_main_send, bt_main_receive, bt_main_save, bt_main_startOrEnd;

    private String str_nowDeviceName;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://蓝牙连接成功
                    str_nowDeviceName = (String) msg.obj;
                    Toast.makeText(MainActivity.this, "连接设备成功！设备名称 : " +
                            str_nowDeviceName, Toast.LENGTH_SHORT).show();
                    main_toolBar_tv.setText("当前设备：" + str_nowDeviceName);
                    bt_main_receive.setVisibility(View.VISIBLE);//显示接收按钮
                    bt_main_send.setVisibility(View.VISIBLE);//显示发送按钮
                    clientReadRev();//蓝牙连接成功，开启接收数据线程
                    break;
                case 2://蓝牙连接失败
                    str_nowDeviceName = " ";
                    Toast.makeText(MainActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    main_toolBar_tv.setText("接收 发送数据");
                    break;
                case 3://蓝牙发送数据错误
                    main_toolBar_tv.setText((String) msg.obj);
                    break;
                case 4://接收蓝牙数据
                    Log.i(TAG, "main : receive handler4=== " + msg.obj);
                    byte[] obj = (byte[]) msg.obj;
                    main_toolBar_tv.setText("当前设备：" + str_nowDeviceName);
                    Log.i(TAG, "main : receive handler4=== " + Arrays.toString(obj));
                    if (obj != null) {
                        receiveDataAndShow(obj);
                    }
                    break;
                case 5://接收数据错误
                    main_toolBar_tv.setText((String) msg.obj);
                    break;
                default:
                    break;

            }

        }
    };

    private void receiveDataAndShow(byte[] obj) {
        if (READ == obj[0]) {
            switch (obj[2]) {
                case BTYERX://读取参数的返回值
                    byte sum = 0;
                    for (int j = 0; j < 8; j++) {
                        sum += obj[j];
                    }
                    if ((lenMax-2)==obj[1] && sum == obj[8]) {//校验
                        main_seekBar1.setProgress(obj[3]);
                        main_seekBar2.setProgress(obj[4]);
                        main_seekBar3.setProgress(obj[5]);
                        main_seekBar4.setProgress(obj[6]);
                        main_seekBar5.setProgress(obj[7]);
                        Toast.makeText(MainActivity.this, "更新完毕", Toast.LENGTH_SHORT).show();

                    } else {
                        Toast.makeText(MainActivity.this, "校验错误！请重新设置", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case BTYERX_SAVE://保存参数返回值
                    if ((READ + BTYEP_2 + BTYERX_SAVE) == obj[3]) {
                        Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "校验错误！请重新设置", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case BTYERX_START://启停返回值
                    if ((READ + BTYEP_3 + BTYERX_SAVE + obj[3]) == obj[4]) {
                        if(obj[3] == 0x00){//当前状态为转动
                            bt_main_startOrEnd.setText("停止");
                        }else if(obj[3] == 0x01){//当前状态为停止
                            bt_main_startOrEnd.setText("启动");
                        }
                        Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "校验错误！请重新设置", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }

        }else if(WRITE == obj[0]){
            if (BTYETX == obj[2] && (WRITE + BTYEP_2 + BTYETX) == obj[3]) {
                Toast.makeText(MainActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "校验错误！请重新设置", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, "数据格式接收错误", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();
        checkBLEFeature();
        initView();

    }

    private void initView() {
        // 标题栏初始化
        mToolbar = (Toolbar) findViewById(R.id.main_toolBar);
        mToolbar.setTitle(" ");
        setSupportActionBar(mToolbar);
        //获取全局变量
        //获取全局变量
        myApplication = (MyApplication) getApplication();
        //获取默认的蓝牙适配器
        mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //设置全局蓝牙适配器
        myApplication.setMbluetoothAdapter(mbluetoothAdapter);

        main_toolBar_tv = (TextView) findViewById(R.id.main_toolBar_tv);
        actionView_set();//悬浮按钮的设置
        main_seekBar1 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar1);
        main_seekBar2 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar2);
        main_seekBar3 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar3);
        main_seekBar4 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar4);
        main_seekBar5 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar5);
        bt_main_send = (Button) findViewById(R.id.bt_main_send);
        bt_main_receive = (Button) findViewById(R.id.bt_main_receive);
        bt_main_save = (Button) findViewById(R.id.bt_main_save);
        bt_main_startOrEnd = (Button) findViewById(R.id.bt_main_startOrEnd);

        bt_main_send.setOnClickListener(this);
        bt_main_receive.setOnClickListener(this);
        bt_main_save.setOnClickListener(this);
        bt_main_startOrEnd.setOnClickListener(this);

        initData();
        if (mbluetoothAdapter.isEnabled()) {
            dialogDoSearch();
        } else {
            dialogOpenBluetooth();
        }
    }

    private void actionView_set() {
        final FloatingActionButton actionA = (FloatingActionButton) findViewById(R.id.action_a);
        final View actionB = findViewById(R.id.action_b);
        final FloatingActionButton actionD = (FloatingActionButton) findViewById(R.id.action_d);
        FloatingActionButton actionC = new FloatingActionButton(getBaseContext());
        actionC.setTitle("隐藏/显示");
        actionC.setIcon(R.drawable.show);


        actionC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bt_main_send.setVisibility(bt_main_send.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                bt_main_receive.setVisibility(bt_main_receive.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                //actionB.setVisibility(actionB.getVisibility() == View.GONE ? View.VISIBLE: View.GONE);
                //actionD.setVisibility(actionD.getVisibility() == View.GONE ? View.VISIBLE: View.GONE);
            }
        });

        final FloatingActionsMenu menuMultipleActions = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        menuMultipleActions.addButton(actionC);


        actionA.setOnClickListener(this);
        actionB.setOnClickListener(this);
        actionD.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.bt_main_send://设置参数
                //Toast.makeText(MainActivity.this, "设置参数", Toast.LENGTH_SHORT).show();
                if (myApplication.getBluetoothSocket() != null) {
                    receiveFlag = BTYETX;//设置接收‘设置参数’标志位
                    bytesW[3] = (byte) main_seekBar1.getProgress();
                    bytesW[4] = (byte) main_seekBar2.getProgress();
                    bytesW[5] = (byte) main_seekBar3.getProgress();
                    bytesW[6] = (byte) main_seekBar4.getProgress();
                    bytesW[7] = (byte) main_seekBar5.getProgress();
                    for (int i = 0; i < 8; i++) {
                        bytesW[8] += bytesW[i];
                    }
                    // Log.i(TAG, "onClick: send===" + bytesW[8]);
                    sendData(bytesW, myApplication.getBluetoothSocket());
                } else {
                    Toast.makeText(MainActivity.this, "设备未连接！", Toast.LENGTH_SHORT).show();
                }
                for (int k = 3; k < 9; k++) {
                    bytesW[k] = 0;//原数据清零
                }
                break;
            case R.id.bt_main_receive://读取参数
                if (myApplication.getBluetoothSocket() != null) {
                    receiveFlag = BTYERX;//设置接收‘读取参数’标志位
                    sendData(bytesR, myApplication.getBluetoothSocket());
                } else {
                    Toast.makeText(MainActivity.this, "设备未连接！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bt_main_save:
                if (myApplication.getBluetoothSocket() != null) {
                    receiveFlag = BTYERX_SAVE;//设置接收‘保存参数’标志位
                    sendData(bytesSave, myApplication.getBluetoothSocket());
                } else {
                    Toast.makeText(MainActivity.this, "设备未连接！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bt_main_startOrEnd:
                if (myApplication.getBluetoothSocket() != null) {
                    receiveFlag = BTYERX_START;//设置接收‘保存参数’标志位
                    sendData(bytesStart, myApplication.getBluetoothSocket());
                } else {
                    Toast.makeText(MainActivity.this, "设备未连接！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_a:
                Intent backChooseDevice = new Intent(MainActivity.this, ChooseConnectDevice.class);
                startActivityForResult(backChooseDevice, REQUEST_CONNECT_DEVICE);
                break;
            case R.id.action_b:
                Intent flushMain = new Intent(MainActivity.this, MainActivity.class);
                startActivity(flushMain);
                finish();
                break;
            case R.id.action_d:
                Toast.makeText(MainActivity.this, "uploading", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    /**
     * 定义一帧数据格式
     */
    private final byte READ = (byte) 0x50, WRITE = (byte) 0x51,
            BTYEP_2 = (byte) 0x02, BTYEP_7 = (byte) 0x07, BTYEP_3 = (byte) 0x03,
            BTYERX = (byte) 0x0A, BTYETX = (byte) 0x0B,
            BTYERX_SAVE = (byte) 0x0C, BTYERX_START = (byte) 0x0D;
    private int lenMin = 4, len_3 = 5;
    private int lenMax = 9;
    //发送
    private byte[] bytesR = new byte[lenMin];
    private byte[] bytesW = new byte[lenMax];
    private byte[] bytesSave = new byte[lenMin];
    private byte[] bytesStart = new byte[lenMin];


    //接收
    private byte[] receiveByteR = new byte[lenMax];
    private byte[] receiveByteW = new byte[lenMin];
    private byte[] receiveByteSave = new byte[lenMin];
    private byte[] receiveByteSart = new byte[len_3];
    private byte receiveFlag;//设置发送数据的标志位
    private int receiveIndex;//接收数据的下标

    public void initData() {//初始化发送的字节数组
        bytesR[0] = READ;
        bytesR[1] = BTYEP_2;//校验位
        bytesR[2] = BTYERX;
        bytesR[3] = (byte) (bytesR[0] + bytesR[1] + bytesR[2]);

        bytesW[0] = WRITE;
        bytesW[1] = BTYEP_7;
        bytesW[2] = BTYETX;

        bytesSave[0] = READ;
        bytesSave[1] = BTYEP_2;
        bytesSave[2] = BTYERX_SAVE;
        bytesSave[3] = (byte) (bytesSave[0] + bytesSave[1] + bytesSave[2]);

        bytesStart[0] = READ;
        bytesStart[1] = BTYEP_2;
        bytesStart[2] = BTYERX_START;
        bytesStart[3] = (byte) (bytesStart[0] + bytesStart[1] + bytesStart[2]);
        initReceiveData();
    }

    public void initReceiveData(byte byteRx) {
        receiveIndex = 0;//接收下标清零
        receiveFlag = 0;//标志位清零
        switch (byteRx) {
            case BTYERX:
                for (int q2 = 0; q2 < lenMax; q2++) {
                    receiveByteR[q2] = 0;//接收缓冲区清零
                }
                break;
            case BTYETX:
                for (int q1 = 0; q1 < lenMin; q1++) {
                    receiveByteW[q1] = 0;//接收缓冲区清零
                }
                break;
            case BTYERX_SAVE:
                for (int q3 = 0; q3 < lenMin; q3++) {
                    receiveByteSave[q3] = 0;
                }
                break;
            case BTYERX_START:
                for (int q4 = 0; q4 < len_3; q4++) {
                    receiveByteSart[q4] = 0;
                }
                break;
            default:
                break;

        }
    }

    public void initReceiveData() {//是否是W
        receiveIndex = 0;//接收下标清零
        receiveFlag = 0;//标志位清零
        for (int q1 = 0; q1 < lenMin; q1++) {
            receiveByteW[q1] = 0;//接收缓冲区清零
        }
        for (int q2 = 0; q2 < lenMax; q2++) {
            receiveByteR[q2] = 0;//接收缓冲区清零
        }
        for (int q3 = 0; q3 < lenMin; q3++) {
            receiveByteSave[q3] = 0;
        }
        for (int q4 = 0; q4 < len_3; q4++) {
            receiveByteSart[q4] = 0;
        }

    }


    public void sendData(byte[] byteData, BluetoothSocket socket) {
        try {
            if (outputStream == null) {
                outputStream = socket.getOutputStream();
            }
            Log.i(TAG, "sendData: " + outputStream);
            outputStream.write(byteData);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(MainActivity.this, "数据传送管道已关闭！请重新连接", Toast.LENGTH_SHORT).show();

            Message msg = handler.obtainMessage();
            msg.obj = "发送数据错误！";
            msg.what = 3;
            handler.sendMessage(msg);


            Log.e(TAG, "发送数据错误");
            // myApplication.closeBluetoothSocket();
        }
    }


    //客户端接收数据
    private void clientReadRev() {
        ReadReceiveThread clientReadThread = new ReadReceiveThread(myApplication.getBluetoothSocket());
        clientReadThread.start();
    }


    // 取数据线程
    private class ReadReceiveThread extends Thread {

        private BluetoothSocket socket;

        public ReadReceiveThread(BluetoothSocket bluetoothSocket) {
            socket = bluetoothSocket;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            try {
                Log.i(TAG, "is socket connect: " + socket.isConnected() + " socket: " + socket);
                if (inputStream == null) {
                    inputStream = socket.getInputStream();
                }

                while (true) {
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        // Log.i(TAG, "read thread run: len ==="+ bytes);
                        for (int i = 0; i < bytes; i++) {
                            //Log.i(TAG, "===一帧数据==="+i+"===="+buffer[i]);
                            switch (receiveFlag) {
                                case BTYETX://接收到的是‘设置参数’的回复
                                    receiveByteW[receiveIndex++] = buffer[i];
                                    if (receiveIndex == lenMin) {//接收完毕
                                        byte[] rw = new byte[4];
                                        rw = receiveByteW.clone();

                                        Message msg = handler.obtainMessage();
                                        msg.obj = rw;
                                        Log.i(TAG, "read thread run: " + Arrays.toString((byte[]) msg.obj));
                                        msg.what = 4;
                                        handler.sendMessage(msg);//发送主线程处理
                                        initReceiveData(BTYETX);//清空
                                    }
                                    break;
                                case BTYERX:
                                    receiveByteR[receiveIndex++] = buffer[i];
                                    if (receiveIndex == lenMax) {
                                        byte[] rr = new byte[9];
                                        rr = receiveByteR.clone();

                                        Message msg = handler.obtainMessage();
                                        msg.obj = rr;
                                        Log.i(TAG, "read thread run: " + Arrays.toString((byte[]) msg.obj));
                                        msg.what = 4;
                                        handler.sendMessage(msg);//发送主线程处理
                                        initReceiveData(BTYERX);//清空
                                    }
                                    break;
                                case BTYERX_SAVE:
                                    receiveByteSave[receiveIndex++] = buffer[i];
                                    if (receiveIndex == lenMin) {
                                        byte[] rs = new byte[4];
                                        rs = receiveByteSave.clone();

                                        Message msg = handler.obtainMessage();
                                        msg.obj = rs;
                                        Log.i(TAG, "read thread run: " + Arrays.toString((byte[]) msg.obj));
                                        msg.what = 4;
                                        handler.sendMessage(msg);//发送主线程处理
                                        initReceiveData(BTYERX_SAVE);//清空
                                    }
                                    break;
                                case BTYERX_START:
                                    receiveByteSart[receiveIndex++] = buffer[i];
                                    if (receiveIndex == len_3) {
                                        byte[] rsta = new byte[5];
                                        rsta = receiveByteSart.clone();

                                        Message msg = handler.obtainMessage();
                                        msg.obj = rsta;
                                        Log.i(TAG, "read thread run: " + Arrays.toString((byte[]) msg.obj));
                                        msg.what = 4;
                                        handler.sendMessage(msg);//发送主线程处理
                                        initReceiveData(BTYERX_START);//清空
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                Message msg = handler.obtainMessage();
                msg.obj = "接收数据错误";
                msg.what = 5;
                handler.sendMessage(msg);

                Log.e(TAG, "接收数据错误");
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

    //弹出打开蓝牙提示框
    private void dialogOpenBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("请打开蓝牙");
        builder.setPositiveButton("好的", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                enableBluetooh();
            }
        });
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    //启动蓝牙
    private void enableBluetooh() {
        if (mbluetoothAdapter == null) {
            Log.i(TAG, "设备不支持蓝牙功能");
            Toast.makeText(MainActivity.this, "设备不支持蓝牙！", Toast.LENGTH_SHORT);
            return;
        }
        if (!mbluetoothAdapter.isEnabled()) { //如果蓝牙没有打开
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE);
            //bluetoothAdapter.enable();//隐式打开蓝牙
        } else {
            Toast.makeText(MainActivity.this, "蓝牙已经打开", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "蓝牙已经打开");
        }
    }

    //弹出是否搜索蓝牙提示框
    void dialogDoSearch() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("请选择您需要调试的蓝牙设备");
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //搜索跳转
                Log.i(TAG, "开始进行搜索");
                Intent intent = new Intent(MainActivity.this, ChooseConnectDevice.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
            }
        });
        builder.setNegativeButton("否", null);
        builder.show();
    }

    //接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE:
                if (resultCode == RESULT_OK) {
                    dialogDoSearch();
                } else {
                    Toast.makeText(MainActivity.this, "蓝牙开启失败", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "蓝牙开启失败");
                }
                break;
            //连接结果，由DeviceListActivity设置返回
            case REQUEST_CONNECT_DEVICE:
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras()
                            .getString(ChooseConnectDevice.EXTRA_DEVICE_ADDRESS);
                    String device = data.getExtras()
                            .getString(ChooseConnectDevice.EXTRA_DEVICE_NAME);
                    // 得到蓝牙设备句柄
                    myApplication.setBluetoothDevice(mbluetoothAdapter.getRemoteDevice(address));
                    //弹窗
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("提示：确认前请确保设备可用");
                    builder.setMessage("您已选择设备：" + "\n" +
                            "设备名：" + device + "\n" + "是否连接？");
                    builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            clientConnectDevices();
                        }
                    });
                    builder.setNegativeButton("否", null);
                    builder.show();
                }
                break;
            default:
                break;
        }
    }

    //蓝牙连接
    private void clientConnectDevices() {
        clientThread = new ClientThread();
        clientThread.start();
    }

    //客户端连接
    private class ClientThread extends Thread {
        public ClientThread() {
            BluetoothSocket temp = null;
            //配对之前把扫描关闭
            if (mbluetoothAdapter.isDiscovering()) {
                myApplication.cancelDiscovery();
            }
            try {
                temp = myApplication.getBluetoothDevice().
                        createInsecureRfcommSocketToServiceRecord(MyApplication.getMyUuid());
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "连接失败");
            }
            myApplication.setBluetoothSocket(temp); //把配对时 反射获取的 获取的socket 赋值
            Log.i(TAG, "ClientThread: 设备 " + myApplication.getBluetoothDevice());
            Log.i(TAG, "客户端 配对 socket 初始化：" + temp);

        }

        public void run() {
            try {
                myApplication.getBluetoothSocket().connect();

                Log.i(TAG, "连接设备");
                Message msg = handler.obtainMessage();
                msg.obj = myApplication.getBluetoothDevice().getName();
                msg.what = 1;
                handler.sendMessage(msg);

            } catch (IOException e) {
                e.printStackTrace();
                try {
                    cancel();
                    Message msg = handler.obtainMessage();
                    msg.obj = "设备连接失败";
                    msg.what = 2;
                    handler.sendMessage(msg);
                    Log.e(TAG, "设备连接失败");
                } finally {

                }
            }
        }

        public void cancel() {
            if (myApplication.getBluetoothSocket() != null && myApplication.getBluetoothSocket().isConnected()) {
                myApplication.closeBluetoothSocket();
                //myApplication.setBluetoothSocket(null);
                Log.i(TAG, "取消设备连接");
            }
        }
    }


    /**
     * 检查BLE是否起作用
     */
    private void checkBLEFeature() {
        //判断是否支持蓝牙4.0
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.i(TAG, "设备支持BLE");
        } else {
            Log.i(TAG, "设备不支持BLE");
        }
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECORD_AUDIO};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    /**
     * 权限回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public final void onRequestPermissionsResult(int requestCode,
                                                 @NonNull String[] permissions,
                                                 @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
        }
    }


    /**
     * 开启GPS
     *
     * @param permission
     */
    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("当前手机扫描蓝牙需要打开定位功能。")
                            .setNegativeButton("取消",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton("前往设置",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivity(intent);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    //GPS已经开启了
                }
                break;
        }
    }

    /**
     * 检查GPS是否打开
     *
     * @return
     */
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }


//    public void onDestroy() {
//        super.onDestroy();
//        myApplication.cancelDiscovery();
//    }
}
