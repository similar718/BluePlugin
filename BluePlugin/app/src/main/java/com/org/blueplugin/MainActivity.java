package com.org.blueplugin;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.data.BleDevice;
import com.clj.fastble.lib.BleDeviceInfo;
import com.clj.fastble.lib.BlePluginManager;
import com.clj.fastble.lib.BlueToothPluginListener;
import com.org.blueplugin.utils.GPSUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private Button mGetInfo;
    private TextView mResult;
    private TextView mStatus;
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mGetInfo = findViewById(R.id.btn_get_info);
        mResult = findViewById(R.id.tv_result);
        mStatus = findViewById(R.id.tv_status);

        mGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GPSUtils.isOPen(mContext)) {
                    checkPermissions();
                } else {
                    Toast.makeText(mContext,"请打开GPS定位权限",Toast.LENGTH_LONG).show();
                }
            }
        });
        initBlueTooth();
    }

    private void startThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                BlePluginManager.getInstance().getDeviceInfo(mListener);
            }
        }).start();
    }

    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private static int count = 0;
    private boolean isPause = false;
    private boolean isStop = true;
    private static int delay = 1000;  //1s
    private static int period = 1000;  //1s

    /**
     * 为了保证一直在执行
     */
    private void startTimer(){
        if (mTimer != null && mTimerTask != null){
            stopTimer();
        }
        if (mTimer == null) {
            mTimer = new Timer();
        }
        if (mTimerTask == null) {
            mTimerTask = new TimerTask() {
                @Override
                public void run() {
                    Log.i(TAG, "count: "+String.valueOf(count));
                    do {
                        try {
                            Log.i(TAG, "sleep(1000)...");
                            Thread.sleep(1000);
                            if (isStop){ // 如果当前扫描已经停止就开始下一次的扫描连接
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
                    count ++;
                }
            };
        }
        if(mTimer != null && mTimerTask != null ) {
            mTimer.schedule(mTimerTask, delay, period);
        }
    }

    /**
     * 关闭定时器
     */
    private void stopTimer(){
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

    /**
     * 初始化蓝牙控件
     */
    private void initBlueTooth(){
        BlePluginManager.getInstance().initBlueToothPlugin(getApplication());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BlePluginManager.getInstance().destroyBlueToothPlugin();
        stopTimer();
    }

    /**
     * 权限事件的监听 需要打开蓝牙 需要获取应用程序位置信息
     */
    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
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
     * 权限监听 如果权限全部可以就可以开始扫描
     * @param permission
     */
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
                    mStatus.setText("当前状态：正在搜索设备");
                    isStop = false;
                    startThread();
                    startTimer();
                }
                break;
        }
    }

    /**
     * 判断GPS是否打开
     * @return
     */
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
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
            }
        }
    }

    /**
     * 扫描时候的监听返回情况
     */
    BlueToothPluginListener mListener = new BlueToothPluginListener() {
        @Override
        public void scanDevice(int type) { // 设备扫描情况
            if (type == 1){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"搜索到目标设备",Toast.LENGTH_LONG).show();
                        Log.e(TAG,"搜索到目标设备");
                        mStatus.setText("当前状态：搜索到目标设备正在连接中");
                        isStop = false;
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"未搜索到目标设备",Toast.LENGTH_LONG).show();
                        Log.e(TAG,"未搜索到目标设备");
                        mStatus.setText("当前状态：未搜索到目标设备 请打开设备之后重试");
                        isStop = true;
                    }
                });
            }
        }

        @Override
        public void connDevice(int type, BleDevice bleDevice) { // 设备连接情况
            if (type == 0){ // 0 开始连接 1 连接成功 2 连接失败 3 断开连接 4 连接的设备不是我需要的数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG,"开始连接");
                        mStatus.setText("当前状态：开始连接");
                        isStop = false;
                    }
                });
            } else if (type == 1){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"连接成功",Toast.LENGTH_LONG).show();
                        Log.e(TAG,"连接成功");
                        mStatus.setText("当前状态：连接成功 正准备获取数据");
                        isStop = false;
                    }
                });
            } else if (type == 2){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"连接失败",Toast.LENGTH_LONG).show();
                        Log.e(TAG,"连接失败");
                        mStatus.setText("当前状态：连接失败");
                        isStop = true;
                    }
                });
            } else if (type == 3){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"断开连接",Toast.LENGTH_LONG).show();
                        Log.e(TAG,"断开连接");
                        mStatus.setText("当前状态：设备 断开连接");
                        isStop = true;
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"断开连接 连接的设备不是我需要的数据",Toast.LENGTH_LONG).show();
                        Log.e(TAG,"连接的设备不是我需要的数据");
                        mStatus.setText("当前状态：设备 断开连接 连接的设备不是我需要的数据");
                        isStop = true;
                    }
                });
            }
        }

        @Override
        public void getDeviceInfo(final BleDeviceInfo info) { // 连接成功并获取到需要的设备信息
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG,"获取数据成功"+info.toString());
                    mResult.setText(info.toString());
                }
            });
        }
    };
}
