package com.example.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class ChooseConnectDevice extends AppCompatActivity {

    private static final String TAG = "yin_chooseDevice";

    MyApplication myApplication ;
    // 返回时数据标签
    public static String EXTRA_DEVICE_ADDRESS = "设备地址";
    public static String EXTRA_DEVICE_NAME = "设备";

    // 成员域
    private BluetoothAdapter mbluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    //private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private Set<BluetoothDevice> pairedBluetoothSevice ;

    private ListView lv_pairedDeviceListView;//lv_newDeviceListView
    //标题栏
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_connect_device);

        initeView();

    }

    private void initeView() {
        mToolbar = (Toolbar) findViewById(R.id.chooseDevice_toolBar);
        mToolbar.setTitle(" ");
        setSupportActionBar(mToolbar);

        myApplication = (MyApplication) getApplication();
        //获取默认的蓝牙适配器
        mbluetoothAdapter = myApplication.getMbluetoothAdapter();
        //蓝牙搜索需要注册
        //IntentFilter filter = new IntentFilter();
       // filter.addAction(BluetoothDevice.ACTION_FOUND);  //蓝牙搜索
       // filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); //蓝牙搜索结束
       // filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //蓝牙设备状态改变
        //registerReceiver(mReceiver, filter);


        lv_pairedDeviceListView = (ListView) findViewById(R.id.lv_paired_deviceListView);
        //lv_newDeviceListView = (ListView) findViewById(R.id.lv_new_deviceListView);

        // 设定默认返回值为取消
        setResult(Activity.RESULT_CANCELED);

        if (mbluetoothAdapter.isEnabled()) {
            searchDevice();
            //getSystemPairedDevice();
        } else {
            dialogOpenBluetooth();
        }
    }
    //弹出打开蓝牙提示框
    private void dialogOpenBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChooseConnectDevice.this);
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
            Toast.makeText(ChooseConnectDevice.this, "设备不支持蓝牙！", Toast.LENGTH_SHORT);
            return;
        }
        if (!mbluetoothAdapter.isEnabled()) { //如果蓝牙没有打开
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
            //bluetoothAdapter.enable();//隐式打开蓝牙
        } else {
            Toast.makeText(ChooseConnectDevice.this, "蓝牙已经打开", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "蓝牙已经打开");
        }
    }

    //列表点击事件响应
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // 准备连接设备，关闭服务查找
            mbluetoothAdapter.cancelDiscovery();

            // 得到mac地址
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // 设置返回数据
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            intent.putExtra(EXTRA_DEVICE_NAME,info);

            // 设置返回值并结束程序
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    //搜索蓝牙设备
    private void searchDevice() {
        //Toast.makeText(ChooseConnectDevice.this, "开始搜索请稍候", Toast.LENGTH_SHORT).show();
        //Log.i(TAG, "开始进行搜索");
        // 初使化设备存储数组
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        //mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        lv_pairedDeviceListView.setAdapter(mPairedDevicesArrayAdapter);


        //lv_newDeviceListView.setAdapter(mNewDevicesArrayAdapter);
       // lv_newDeviceListView.setOnItemClickListener(mDeviceClickListener);
        lv_pairedDeviceListView.setOnItemClickListener(mDeviceClickListener);

        //mbluetoothAdapter.startDiscovery();//搜索设备

        //获取系统已配对的蓝牙设备
        getSystemPairedDevice();
    }

    private void getSystemPairedDevice(){
        pairedBluetoothSevice = mbluetoothAdapter.getBondedDevices();
        if(pairedBluetoothSevice.size() > 0 ){
            for(BluetoothDevice bluetoothDevice : pairedBluetoothSevice){
                mPairedDevicesArrayAdapter.add(bluetoothDevice.getName() + "\n"
                        + bluetoothDevice.getAddress());
            }
            //解决scrollView只显示一行listView的问题
            fixListViewHeight(lv_pairedDeviceListView);
        }
    }

    /**
    //搜索蓝牙 需要进行广播接收 搜索到一个设备 接收到一个广播
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //搜索蓝牙设备
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    //  mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    Log.i(TAG, "搜索到 已经配对的设备; device name: " + device.getName() +
                            "  device address: " + device.getAddress());
                } else {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    Log.i(TAG, "搜索到 没有配对的设备; device name: " + device.getName() +
                            "  device address: " + device.getAddress());
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                // 更新蓝牙设备的绑定状态
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.i(TAG, "正在配对 device name: " + device.getName() + "  device address: " + device.getAddress() + " devices uuid: " + device.getUuids());
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.i(TAG, "完成配对 device name: " + device.getName() + "  device address: " + device.getAddress() + " devices uuid: " + device.getUuids());
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.i(TAG, "取消配对 device name: " + device.getName() + "  device address: " + device.getAddress() + " devices uuid: " + device.getUuids());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {  //搜索结束
                if(mNewDevicesArrayAdapter.isEmpty()){
                    mNewDevicesArrayAdapter.add("没搜索到新的设备");
                }
                //解决scrollView只显示一行listView的问题
                //fixListViewHeight(lv_newDeviceListView);
                Toast.makeText(ChooseConnectDevice.this, "搜索结束", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "设备搜索结束");
            }
        }
    };
**/

    public void fixListViewHeight(ListView listView) {
        // 如果没有设置数据适配器，则ListView没有子项，返回。
        ListAdapter listAdapter = listView.getAdapter();
        int totalHeight = 0;
        if (listAdapter == null) {
            return;
        }
        for (int index = 0, len = listAdapter.getCount(); index < len; index++) {
            View listViewItem = listAdapter.getView(index , null, listView);
            // 计算子项View 的宽高
            listViewItem.measure(0, 0);
            // 计算所有子项的高度和
            totalHeight += listViewItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        // listView.getDividerHeight()获取子项间分隔符的高度
        // params.height设置ListView完全显示需要的高度
        params.height = totalHeight+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    public void onDestroy() {
        super.onDestroy();
        //解除注册
//        unregisterReceiver(mReceiver);
 //       Log.e(TAG, "解除注册");
        //取消扫描
 //       myApplication.cancelDiscovery();
    }

    //接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    searchDevice();
                } else {
                    Toast.makeText(ChooseConnectDevice.this, "蓝牙开启失败", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "蓝牙开启失败");
                }
                break;
        }
    }
}
