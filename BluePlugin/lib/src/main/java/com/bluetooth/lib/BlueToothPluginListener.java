package com.bluetooth.lib;

public interface BlueToothPluginListener {
    void scanDevice(int type);// 1 扫描到目标设备 0 未扫描到目标设备
    void connDevice(int type, BleDevices bleDevice); // 0 开始连接 1 连接成功 2 连接失败 3 断开连接
    void getDeviceInfo(BleDeviceInfo info);
}
