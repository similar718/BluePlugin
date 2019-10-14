package com.example.netservice;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
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
import com.example.netservice.bean.UpServiceBean;
import com.example.netservice.http.HttpUtil;
import com.example.netservice.utils.FastJsonUtil;
import com.example.netservice.utils.GPSUtils;
import com.example.netservice.utils.NetWorkUtils;
import com.example.netservice.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 *
 三、请求地址与端口
 119.23.226.237：9099
 四、APP上报数据
 数据包结构：
 JOSN格式
 数据包内容：
 {
 "Longitude": "104.43243"  //经度
 “Latitude“:“30.321432“	//纬度
 "StartTime ": 13:25, //时间
 "SerialNumber":32435325,电子轮挡序列号(区分不通的电子轮挡)
 "Temperature":30℃//芯片温度
 "ElectricQuantity":3.4V//电池电量
 "Frequency ":1323//按键次数
 }
 {
 "Longitude":"104.43243",
 "Latitude":30.321432",
 "StartTime":13:25,
 "SerialNumber":32435325,
 "Temperature":"30℃",
 "ElectricQuantity":"3.4V",
 "Frequency ":1323
 }
 */

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private Button mGetInfo;
    private TextView mResult;
    private TextView mStatus;
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private Context mContext;

    // 获取位置管理服务
    private LocationManager locationManager;
    String mProviderName = "";

    private double mLatitude = 0.0;
    private double mLongitude = 0.0;

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
                    if (NetWorkUtils.isNetConnected(mContext)) {
                        checkPermissions();
                    } else {
                        Toast.makeText(mContext, "需要网络才能正常使用哦", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mContext, "请打开GPS定位权限", Toast.LENGTH_LONG).show();
                }
//                upServiceother("null");
//                upService("null");
            }
        });
        initBlueTooth();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String serviceName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) this.getSystemService(serviceName);
        // 查找到服务信息
        Criteria criteria = new Criteria();
        // 设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // 设置是否要求速度
        criteria.setSpeedRequired(false);
        // 设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        // 设置是否需要方位信息
        criteria.setBearingRequired(false);
        // 设置是否允许运营商收费
        criteria.setCostAllowed(true);
        // 设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗

        // 为获取地理位置信息时设置查询条件
        String provider = locationManager.getBestProvider(criteria, true); // 获取GPS信息
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getIntent() == null || getIntent().getStringExtra("city") == null || "".equals(getIntent().getStringExtra("city"))) {
            if (openGPSSettings()) {
                Location lastKnownLocation = null;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "位置权限未打开", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                mProviderName = LocationManager.GPS_PROVIDER;
                if (lastKnownLocation == null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    mProviderName = LocationManager.NETWORK_PROVIDER;
                }
                if (mProviderName != null && !"".equals(mProviderName)) {
                    locationManager.requestLocationUpdates(mProviderName, 1000, 1, locationListener);
                }
            }
        }
    }

    /**
     * 这里一定要对LocationManager进行重新设置监听 mgr获取provider的过程不是一次就能成功的
     * mgr.getLastKnownLocation很可能返回null 如果只在initProvider()中注册一次监听则基本很难成功
     */
    @Override
    public void onResume() {
        super.onResume();
        if (locationManager != null && !Utils.isEmpty(mProviderName)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "位置权限未打开", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            locationManager.requestLocationUpdates(mProviderName, 1000, 1, locationListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 取消注册监听
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    /** GPS模块是否存在或者是开启 **/
    private boolean openGPSSettings() {
        LocationManager alm = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (alm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS模块正常", Toast.LENGTH_SHORT).show();
            return true;
        }

        Toast.makeText(this, "请开启GPS！", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        startActivityForResult(intent, 0); // 此为设置完成后返回到获取界面
        return false;
    }

    private Timer mTimer = null;
    private TimerTask mTimerTask = null;
    private static int count = 0;
    private boolean isPause = false;
    private boolean isStop = true;
    private static int delay = 1000;  //1s
    private static int period = 1000;  //1s

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
                            if (isStop){
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


    private void startThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BlePluginManager.getInstance().getDeviceInfo(mListener);
            }
        }).start();
    }

    private void initBlueTooth() {
        BlePluginManager.getInstance().initBlueToothPlugin(getApplication());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BlePluginManager.getInstance().destroyBlueToothPlugin();
        stopTimer();
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
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
                    mStatus.setText("当前状态：正在搜索设备");
                    isStop = false;
                    startThread();
                    startTimer();
                }
                break;
        }
    }

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

    BlueToothPluginListener mListener = new BlueToothPluginListener() {
        @Override
        public void scanDevice(int type) {
            if (type == 1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "搜索到目标设备", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "搜索到目标设备");
                        mStatus.setText("当前状态：搜索到目标设备正在连接中");
                        isStop = false;
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "未搜索到目标设备", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "未搜索到目标设备");
                        mStatus.setText("当前状态：未搜索到目标设备 请打开设备之后重试");
                        isStop = true;
                    }
                });
            }
        }

        @Override
        public void connDevice(int type, BleDevice bleDevice) {
            if (type == 0) { // 0 开始连接 1 连接成功 2 连接失败 3 断开连接 4 连接的设备不是我需要的数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e(TAG, "开始连接");
                        mStatus.setText("当前状态：开始连接");
                        isStop = false;
                    }
                });
            } else if (type == 1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "连接成功", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "连接成功");
                        mStatus.setText("当前状态：连接成功 正准备获取数据");
                        isStop = false;
                    }
                });
            } else if (type == 2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "连接失败", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "连接失败");
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
        public void getDeviceInfo(final BleDeviceInfo info) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "获取数据成功" + info.toString());
                    mResult.setText(info.toString());
                }
            });
            mInfo = info;
            // 上传到服务器
            mHandler.sendEmptyMessage(HANDLER_UPDATE_DATA_TO_SERVICE);
        }
    };

    private final int HANDLER_UPDATE_DATA_TO_SERVICE = 0x0101;
    private BleDeviceInfo mInfo = null;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case HANDLER_UPDATE_DATA_TO_SERVICE: // 将数据转成json 并且上传到服务器
                    // 将获取到的数据转化成我们需要的类
                    updateServiceInfo();
                    break;
                default:
                    break;
            }
        }
    };

    private void updateServiceInfo(){
        if (mInfo != null){
            if (mLongitude != 0.0f && mLatitude != 0.0f) {
                UpServiceBean bean = new UpServiceBean();
                bean.setElectricQuantity(mInfo.getPower());
                bean.setFrequency(mInfo.getNum());
                bean.setLatitude(String.valueOf(mLatitude));
                bean.setLongitude(String.valueOf(mLongitude));
                bean.setSerialNumber(mInfo.getMac());
                bean.setTemperature(mInfo.getTemperature());
                bean.setStartTime(System.currentTimeMillis());
                // 将数据转为json
                String data = FastJsonUtil.objToJson(bean);
                // 将数据上传到服务器
                upService(data);
            }
        }
    }

    String url = "http://119.23.226.237:9099/dataReception";

    private void upService(String data){
        //接口地址
        RequestBody requestBody = new FormBody.Builder() .add("param",data).build();
            HttpUtil.sendOkHttpPostRequest(url, requestBody, new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext,"上传服务器失败",Toast.LENGTH_SHORT).show();
                        Log.e("ooooooooooooooooooooo","e = "+e.toString());
                    }
                });
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String result = response.body().string();
                    Log.e("ooooooooooooooooooooo","data = "+result);
                    //result就是图片服务器返回的图片地址。
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext,result,Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
    }

//    String url_other = "https://gateway.xmzt.cn/user/login/out";
//    private void upServiceother(String data){
//        //接口地址
//        RequestBody requestBody = new FormBody.Builder() .add("token",data).build();
//            HttpUtil.sendOkHttpPostRequest(url_other, requestBody, new Callback() {
//                @Override
//                public void onFailure(Call call, final IOException e) {runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(mContext,"上传服务器失败",Toast.LENGTH_SHORT).show();
//                        Log.e("ooooooooooooooooooooo","e = "+e.toString());
//                    }
//                });
//                }
//                @Override
//                public void onResponse(Call call, Response response) throws IOException {
//                    final String result = response.body().string();
//                    Log.e("ooooooooooooooooooooo","data = "+result);
//                    //result就是图片服务器返回的图片地址。
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(mContext,result,Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//            });
//    }

    private LocationListener locationListener = new LocationListener() {

        /**
         * 位置信息变化时触发
         */
        public void onLocationChanged(Location location) {

            updateToNewLocation(location);
        }

        /**
         * GPS状态变化时触发
         */
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    // 当前GPS状态为可见状态
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    // 当前GPS状态为服务区外状态
                    break;
                // GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    // 当前GPS状态为暂停服务状态
                    break;
            }
        }

        /**
         * GPS开启时触发
         */
        public void onProviderEnabled(String provider) {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "位置权限未打开", Toast.LENGTH_LONG).show();
                    }
                });
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            updateToNewLocation(location);
        }

        /**
         * GPS禁用时触发
         */
        public void onProviderDisabled(String provider) {
            updateToNewLocation(null);
        }
    };

    public void updateToNewLocation(final Location location) {
        if (location == null) {
            Toast.makeText(getApplicationContext(), "GPS定位失败",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        Toast.makeText(getApplicationContext(), "经度：" + location.getLongitude() + "纬度：" + location.getLatitude(), Toast.LENGTH_SHORT).show();
        Log.i("", "经度：" + location.getLongitude());
        Log.i("", "纬度：" + location.getLatitude());
    }
}
