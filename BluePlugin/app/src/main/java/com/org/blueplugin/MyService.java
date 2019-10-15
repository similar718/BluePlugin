//package com.org.blueplugin;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.location.Criteria;
//import android.location.Location;
//import android.location.LocationListener;
//import android.location.LocationManager;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.os.PowerManager;
//import android.support.v4.app.ActivityCompat;
//
///**
// * 获取gps位置信息的service
// *
// * @author king
// *
// */
//public class MyService extends Service {
//
//    private LocationManager locationManager;
//
//    private PowerManager pm;
//    private PowerManager.WakeLock wakeLock;
//
////    private GPSUploadThread myThread;
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//
//        //创建LocationManger对象(LocationMangager，位置管理器。要想操作定位相关设备，必须先定义个LocationManager)
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        //利用Criteria选择最优的位置服务
//        Criteria criteria = new Criteria();
//        //设置定位精确度 Criteria.ACCURACY_COARSE比较粗略，Criteria.ACCURACY_FINE则比较精细
//        criteria.setAccuracy(Criteria.ACCURACY_FINE);
//        //设置是否需要海拔信息
//        criteria.setAltitudeRequired(false);
//        //设置是否需要方位信息
//        criteria.setBearingRequired(false);
//        // 设置是否允许运营商收费
//        criteria.setCostAllowed(true);
//        // 设置对电源的需求
//        criteria.setPowerRequirement(Criteria.POWER_LOW);
//        //获取最符合要求的provider
//        String provider = locationManager.getBestProvider(criteria, true);
//        //绑定监听，有4个参数
//        //参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
//        //参数2，位置信息更新周期，单位毫秒
//        //参数3，位置变化最小距离：当位置距离变化超过此值时，将更新位置信息
//        //参数4，监听
//        //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        locationManager.requestLocationUpdates(provider, 10000, 0, locationListener);// 2000,10
//    }
//
//    @SuppressLint("InvalidWakeLockTag")
//    @Override
//    public void onStart(Intent intent, int startId) {
//        // TODO Auto-generated method stub
//        super.onStart(intent, startId);
//        //创建PowerManager对象
//        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        //保持cpu一直运行，不管屏幕是否黑屏
//        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUKeepRunning");
//        wakeLock.acquire();
//    }
//
//    /**
//     * 实现一个位置变化的监听器
//     */
//    private final LocationListener locationListener = new LocationListener() {
//
//        @Override
//        public void onLocationChanged(Location location) {
//            // TODO Auto-generated method stub
//
//            /**
//             * 此处实现定位上传功能
//             */
//        }
//
//        // 当位置信息不可获取时
//        @Override
//        public void onProviderDisabled(String provider) {
//            // TODO Auto-generated method stub
//            /**
//             *
//             */
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//            // TODO Auto-generated method stub
//
//        }
//
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//            // TODO Auto-generated method stub
//
//        }
//
//    };
//
//    @Override
//    public void onDestroy() {
//        // TODO Auto-generated method stub
//        // toggleGPS(false);
//        if (locationListener != null) {
//            locationManager.removeUpdates(locationListener);
//        }
//        wakeLock.release();
//        super.onDestroy();
//    }
//
//}