package com.blueplugin.bluetoothplugindemo;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.blueplugin.bluetoothplugindemo.utils.GPSUtils;
import com.blueplugin.bluetoothplugindemo.utils.NetWorkUtils;
import com.blueplugin.bluetoothplugindemo.utils.RssiUtils;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.libs.BleDeviceInfo;
import com.clj.fastble.libs.BlePluginManager;
import com.clj.fastble.libs.BlueToothPluginListener;
import com.clj.fastble.libs.config.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
/**
 *
 三、请求地址与端口
 119.23.226.237：9099
 四、APP上报数据
 数据包结构：
 JOSN格式
 数据包内容：
 {
 "Longitude":"104.43243", //经度
 "Latitude":30.321432", //纬度
 "StartTime":13:25, //时间
 "SerialNumber":32435325, 电子轮挡序列号(区分不通的电子轮挡)
 "Temperature":"30℃", //芯片温度
 "ElectricQuantity":"3.4V", //电池电量
 "Frequency ":1323 //按键次数
 }
 */

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private Button mGetInfo;
    private TextView mResult;
    private TextView mStatus;
    private TextView tv_Rssi;
    private TextView tv_Rssi_range;
    private EditText et_Rssi;
    private Button tv_rssi_setting;
    private TextView mLocationTxt;
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private Context mContext;

    private boolean mIsActivity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mGetInfo = findViewById(R.id.btn_get_info);
        mResult = findViewById(R.id.tv_result);
        mStatus = findViewById(R.id.tv_status);
        tv_Rssi = findViewById(R.id.tv_Rssi);
        tv_Rssi_range = findViewById(R.id.tv_rssi_result);
        tv_rssi_setting = findViewById(R.id.btn_rssi_setting);
        et_Rssi = findViewById(R.id.et_rssi);
        mIsActivity = true;
        mLocationTxt = findViewById(R.id.tv_location_txt);

        String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
        mLocationTxt.setText(mLocation);

        mGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GPSUtils.isOPen(mContext)) {
                    if (NetWorkUtils.isNetConnected(mContext)) {
                        checkPermissions();
                    } else {
                        Toast.makeText(mContext, "需要网络才能正常使用哦", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mContext, "请打开GPS定位权限", Toast.LENGTH_LONG).show();
                }
            }
        });
        initBlueTooth();

        tv_rssi_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = et_Rssi.getText().toString().trim();
                long rssi1 = Long.parseLong(data) * (-1);
                BlePluginManager.getInstance().setMinRssi(rssi1);
                tv_Rssi_range.setText("当前rssi的范围最小："+ BlePluginManager.getInstance().getMinRssi());
            }
        });
    }

    private boolean mInitSuccess = false; // 是否默认蓝牙插件初始化成功
    private boolean mClickInit = false; // 是否默认点击扫描初始化

    @Override
    public void onStart() {
        super.onStart();
    }

    /**
     * 这里一定要对LocationManager进行重新设置监听 mgr获取provider的过程不是一次就能成功的
     * mgr.getLastKnownLocation很可能返回null 如果只在initProvider()中注册一次监听则基本很难成功
     */
    @Override
    public void onResume() {
        super.onResume();
        mIsActivity = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        // 取消注册监听
        BlePluginManager.getInstance().onPauseBlueToothPlugin();
    }

    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private static int count = 0;
    private boolean isPause = false;
    private boolean isStop = true;
    private static int delay = 1000;  //1s
    private static int period = 1000;  //1s

    /**
     * 只要开始扫描就不停的进行扫描设备
     */
    private void startTimer() {
        if (mTimer != null && mTimerTask != null) {
            stopTimer();
        }
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    Log.i(TAG, "count: " + String.valueOf(count));
                    do {
                        try {
                            Log.i(TAG, "sleep(1000)...");
                            Thread.sleep(1000);
                            if (isStop) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mStatus.setText("当前状态：正在搜索设备");
                                    }
                                });
                                isStop = false;
                                startThread();
                            }
                        } catch (InterruptedException e) {
                        }
                    } while (isPause);
                    count++;
                }
            };
        }
        if (mTimer != null && mTimerTask != null) {
            mTimer.schedule(mTimerTask, delay, period);
        }
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        count = 0;
    }

    private void startThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BlePluginManager.getInstance().getDeviceInfo();
            }
        }).start();
    }

    private void initBlueTooth() {
        BlePluginManager.getInstance().initBlueToothPlugin(getApplication(),MainActivity.this,mListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsActivity = false;
        BlePluginManager.getInstance().destroyBlueToothPlugin();
        stopTimer();
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WAKE_LOCK};
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


    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    if (mInitSuccess) {
                        mStatus.setText("当前状态：正在搜索设备");
                        isStop = false;
                        startThread();
                        startTimer();
                        String mLocation = "蓝牙插件定位信息\n经度：" + Constants.mLatitude + "\n纬度：" + Constants.mLongitude;
                        mLocationTxt.setText(mLocation);
                    } else {
                        mClickInit = true;
                        initBlueTooth();
                    }
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                mStatus.setText("当前状态：正在搜索设备");
                isStop = false;
                startThread();
                startTimer();
                String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                mLocationTxt.setText(mLocation);
            }
        }
    }

    BlueToothPluginListener mListener = new BlueToothPluginListener() {
        @Override
        public void initFailed(byte data) { // TODO  初始化失败 需要配合相关操作之后再重新初始化
            if (data == (byte) 0x0001){ //没有打开GPS的情况
                Toast.makeText(mContext,"请打开GPS位置信息", Toast.LENGTH_LONG).show();
            } else if (data == (byte) 0x0010) { // 判断是否打开蓝牙设备
                Toast.makeText(mContext,"请打开蓝牙", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext,"初始化失败，其他情况", Toast.LENGTH_LONG).show();
            }
            mInitSuccess = false;
        }

        @Override
        public void initSuccess() {
            // 初始化成功 可以正常的扫描设备
            mInitSuccess = true;
            if (mClickInit){
                mClickInit = false;
                mStatus.setText("当前状态：正在搜索设备");
                isStop = false;
                startThread();
                startTimer();
                String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                mLocationTxt.setText(mLocation);
            }
        }

        @Override
        public void scanDevice() {// 扫描到目标设备
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "搜索到目标设备", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "搜索到目标设备");
                    mStatus.setText("当前状态：搜索到目标设备正在连接中");
                    isStop = false;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void scanDeviceMinRSSI() { // 扫描到目标设备 但是RSSI不在设定范围内
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "搜索到目标设备,RSSI不在设定范围内", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "搜索到目标设备,RSSI不在设定范围内");
                    mStatus.setText("当前状态：搜索到目标设备,RSSI不在设定范围内");
                    isStop = true;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void scanNotDevice() { // 未扫描到目标设备
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "未搜索到目标设备", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "未搜索到目标设备");
                    mStatus.setText("当前状态：未搜索到目标设备 请打开设备之后重试");
                    isStop = true;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void startConnDevice(BleDevice bleDevice) { // 开始连接
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "开始连接");
                    mStatus.setText("当前状态：开始连接");
                    isStop = false;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void connSuccesDevice(BleDevice bleDevice) { // 连接成功
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "连接成功", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "连接成功");
                    mStatus.setText("当前状态：连接成功 正准备获取数据");
                    isStop = false;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void connFailedDevice(BleDevice bleDevice) { // 连接失败
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "连接失败", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "连接失败");
                    mStatus.setText("当前状态：连接失败");
                    isStop = true;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void disConnDevice(BleDevice bleDevice) { // 断开连接
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "断开连接", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "断开连接");
                    mStatus.setText("当前状态：设备 断开连接");
                    isStop = true;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void connNotDesDevice(BleDevice bleDevice) { // 断开连接 连接的设备不是我需要的数据
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "断开连接 连接的设备不是我需要的数据", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "连接的设备不是我需要的数据");
                    mStatus.setText("当前状态：设备 断开连接 连接的设备不是我需要的数据");
                    isStop = true;
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void getDeviceInfo(final BleDeviceInfo info) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "获取数据成功" + info.toString());
                    mResult.setText(info.toString());
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                    tv_Rssi.setText("distance 仅供参考 = " + RssiUtils.getLeDistance(info.getRssi()) + " Rssi = " + info.getRssi());
                }
            });
            // TODO 获取到蓝牙设备上面的数据
            mInfo = info;
        }

        @Override
        public void warningTapNum() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResult.setText("累计按键次数超限");
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void warningPower() { // 电池电压低报警
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mResult.setText("电池电压低报警");
                    String mLocation = "蓝牙插件定位信息\n经度："+ Constants.mLatitude +"\n纬度："+ Constants.mLongitude;
                    mLocationTxt.setText(mLocation);
                }
            });
        }

        @Override
        public void replyDataToDeviceSuccess() { // 回复硬件蓝牙成功

        }

        @Override
        public void replyDataToDeviceFailed() { // 回复硬件蓝牙失败

        }
    };
    private BleDeviceInfo mInfo = null;
}
