package com.example.bluetooth;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by  YinMengXin on 2019/4/13.
 */

public class MyApplication extends Application {
    private BluetoothAdapter mbluetoothAdapter;
    private BluetoothDevice bluetoothDevice; //我们将要连接配对的设备
    private BluetoothSocket bluetoothSocket; //蓝牙配对客户端的 socket
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static UUID getMyUuid() {
        return MY_UUID;
    }


    public BluetoothAdapter getMbluetoothAdapter() {
        return mbluetoothAdapter;
    }

    public void setMbluetoothAdapter(BluetoothAdapter mbluetoothAdapter) {
        this.mbluetoothAdapter = mbluetoothAdapter;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }
    public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }
    public void closeBluetoothSocket(){
        try {
            this.bluetoothSocket.close();
            this.bluetoothSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancelDiscovery(){
        //取消搜索
        if (mbluetoothAdapter != null)
            mbluetoothAdapter.cancelDiscovery();
    }



}
