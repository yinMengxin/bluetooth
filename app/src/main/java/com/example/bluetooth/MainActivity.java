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
    //private FloatActionView actionView;

    private static final int REQUEST_ENABLE = 0x1; //请求能够打开蓝牙
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 0x2; //权限请求
    private final static int REQUEST_CONNECT_DEVICE = 0x3;    //宏定义查询设备句柄

    private com.xw.repo.BubbleSeekBar main_seekBar1,main_seekBar2,main_seekBar3,main_seekBar4,main_seekBar5;
    private Button bt_main_send,bt_main_receive;

    private String str_nowDeviceName;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1://蓝牙连接成功
                    str_nowDeviceName = (String)msg.obj;
                    Toast.makeText(MainActivity.this, "连接设备成功！设备名称 : " +
                            str_nowDeviceName, Toast.LENGTH_SHORT).show();
                    main_toolBar_tv.setText("当前设备："+str_nowDeviceName);
                    bt_main_receive.setVisibility(View.VISIBLE);//显示接收按钮
                    bt_main_send.setVisibility(View.VISIBLE);//显示发送按钮
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
                    main_toolBar_tv.setText("当前设备："+str_nowDeviceName);
                    receiveDataAndShow((byte [] )msg.obj);
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
        if(WRITE == obj[0] && BTYE1 == obj[1] && BTYE2 == obj[2]){//校验前三位  接收数据
            // tx1 = obj[3];
            main_seekBar1.setProgress(obj[3]);
            main_seekBar2.setProgress(obj[4]);
            main_seekBar3.setProgress(obj[5]);
            main_seekBar4.setProgress(obj[6]);
            main_seekBar5.setProgress(obj[7]);
        }else {
            Toast.makeText(MainActivity.this,"数据格式发送错误", Toast.LENGTH_SHORT).show();
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

        main_toolBar_tv = (TextView)findViewById(R.id.main_toolBar_tv);
        actionView_set();//悬浮按钮的设置
        main_seekBar1 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar1);
        main_seekBar2 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar2);
        main_seekBar3 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar3);
        main_seekBar4 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar4);
        main_seekBar5 = (com.xw.repo.BubbleSeekBar) findViewById(R.id.main_seekBar5);
        bt_main_send = (Button)findViewById(R.id.bt_main_send);
        bt_main_receive = (Button) findViewById(R.id.bt_main_receive);


        bt_main_send.setOnClickListener(this);
        bt_main_receive.setOnClickListener(this);

        if (mbluetoothAdapter.isEnabled()) {
            dialogDoSearch();
        } else {
            dialogOpenBluetooth();
        }
    }

    private void actionView_set() {
        final View actionB = findViewById(R.id.action_b);

        FloatingActionButton actionC = new FloatingActionButton(getBaseContext());
        actionC.setTitle("隐藏/显示 按钮");
        actionC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               bt_main_send.setVisibility(bt_main_send.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
               bt_main_receive.setVisibility(bt_main_receive.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
           }
        });

        final FloatingActionsMenu menuMultipleActions = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        menuMultipleActions.addButton(actionC);

        final FloatingActionButton actionA = (FloatingActionButton) findViewById(R.id.action_a);
        actionA.setOnClickListener(this);
        actionB.setOnClickListener(this);
   }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.bt_main_send:
                Toast.makeText(MainActivity.this, "send " , Toast.LENGTH_SHORT).show();
                bytes[0] = READ;
                bytes[1] = BTYE1;
                bytes[2] = BTYE2;
                bytes[3] = (byte) (bytes[0] + bytes[1] + bytes[2]);
                sendData(bytes,myApplication.getBluetoothSocket());
                break;
            case R.id.bt_main_receive:
                Toast.makeText(MainActivity.this, "receive", Toast.LENGTH_SHORT).show();
                clientReadRev();
                break;

            case R.id.action_a:
                Intent backChooseDevice = new Intent(MainActivity.this,ChooseConnectDevice.class);
                startActivity(backChooseDevice);
                break;
            case R.id.action_b:
                Intent flushMain = new Intent(MainActivity.this,MainActivity.class);
                startActivity(flushMain);
                finish();

                //Toast.makeText(MainActivity.this, "刷新！", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }
    /**
     * 定义一帧数据格式
     */
    private final byte READ = (byte)0x50, WRITE = (byte)0x51, BTYE1 = (byte)0x02, BTYE2 = (byte) 0x0A;
    private byte [] bytes = new byte[4];

    public void sendData(byte byteData5, BluetoothSocket socket) {
        //this.sendData((byte) 0x00, byteData5, socket);
        //sendData(bytData2, myApplication.getBluetoothSocket());
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
            myApplication.closeBluetoothSocket();
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
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            buf_data[i] = buffer[i];
                        }
                        String str = new String(buf_data);

                        Log.i(TAG, "接收的数据是： " + str);

                        Message msg = handler.obtainMessage();
                        msg.obj = buf_data;
                        msg.what = 4 ;
                        handler.sendMessage(msg);

                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
                Message msg = handler.obtainMessage();
                msg.obj = "接收数据错误" ;
                msg.what = 5 ;
                handler.sendMessage(msg);

                Log.e(TAG, "接收数据错误");
            } finally {
                try {
                    if(inputStream != null){
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
        builder.setMessage("是否进行设备扫描？");
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
                myApplication.setBluetoothSocket(null);
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


    public void onDestroy() {
        super.onDestroy();
        myApplication.cancelDiscovery();
    }
}
