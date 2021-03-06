package com.bluetooth.lib;

import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleReadCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.util.List;

public class BlePluginManager {
    public static final String TAG = "BlePluginManager";
    String mData = "0201060502c0ffe0ff12ff0d00";
    // 单例模式
    private BlePluginManager() {

    }

    public static synchronized BlePluginManager getInstance() {
        return SingletonHolder.instance;
    }

    private static final class SingletonHolder {
        private static BlePluginManager instance = new BlePluginManager();
    }

    public void initBlueToothPlugin(Application context){
        BleManager.getInstance().init(context);
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);
    }

    private BlueToothPluginListener mBlueToothListener;

    public void getDeviceInfo(BlueToothPluginListener listener){
        mBlueToothListener = listener;
        setScanRule();
        startScan();
    }

    private void setScanRule() {
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setAutoConnect(false)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private boolean mIsScanDes = false;

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                if ("BLE_KEY".equals(bleDevice.getName())){
                    if (!BleManager.getInstance().isConnected(bleDevice)) {
                        mIsScanDes = true;
                        mBlueToothListener.scanDevice(1);
                        BleManager.getInstance().cancelScan();
                        connect(bleDevice);
                    }
                }
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                if (!mIsScanDes){
                    mBlueToothListener.scanDevice(0);
                } else {
                    mIsScanDes = false;
                }
            }
        });
    }

    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                mBlueToothListener.connDevice(0, (BleDevices) bleDevice);
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                mBlueToothListener.connDevice(2, (BleDevices) bleDevice);
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                mBlueToothListener.connDevice(1, (BleDevices) bleDevice);
                String scancord =  HexUtil.formatHexString(bleDevice.getScanRecord());
                // 成功获取到数据进行判断当前数据的是否是我要的格式
                // 广播指示 02 01 06  数据固定 2 bytes
                // UUID 05 02 c0 ff e0 ff 数据固定 6 bytes
                /**
                 *  自定义数据
                 *      长度 12 数据固定 1 bytes
                 *      类型 FF 数据固定 1 bytes
                 *      DevID 0D 00 数据固定 2 bytes
                 *      电量 2 bytes
                 *      芯片温度 2 bytes
                 *      按键次数 4 bytes
                 *      mac地址 6bytes
                 *      发射功率 c5 数据固定 1bytes
                 */
                // 02 01 06 05 02 c0 ff e0 ff 12 ff 0d 00 61 03 36 00 02 00 00 00 15 12 29 f8 e6 a0 c5 08 09 42 4c 45 5f 4b 45 59 05 12 0a 00 14 00 02 0a 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

                String datas = scancord.substring(0, mData.length());
                if (mData.equals(datas)){
                    setOptionsData(scancord);
                    // 获取成功之后再进行Indicate的执行和读取和发送消息  将在这里开始运行
                    setWriteData(bleDevice);
                }
            }
            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                mBlueToothListener.connDevice(3, (BleDevices) bleDevice);
            }
        });
    }

    private void setReadData(final BleDevice bleDevice){
        BleManager.getInstance().read(
                bleDevice,
                uuid_service,
                characteristic_uuid,
                new BleReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {
                        // 读特征值数据成功
                        Log.e("ooooooooo","data = " + HexUtil.encodeHexStr(data));
                        String scancord =  HexUtil.formatHexString(bleDevice.getScanRecord());
                        // 成功获取到数据进行判断当前数据的是否是我要的格式
                        // 广播指示 02 01 06  数据固定 2 bytes
                        // UUID 05 02 c0 ff e0 ff 数据固定 6 bytes
                        /**
                         *  自定义数据
                         *      长度 12 数据固定 1 bytes
                         *      类型 FF 数据固定 1 bytes
                         *      DevID 0D 00 数据固定 2 bytes
                         *      电量 2 bytes
                         *      芯片温度 2 bytes
                         *      按键次数 4 bytes
                         *      mac地址 6bytes
                         *      发射功率 c5 数据固定 1bytes
                         */
                        // 02 01 06 05 02 c0 ff e0 ff 12 ff 0d 00 61 03 36 00 02 00 00 00 15 12 29 f8 e6 a0 c5 08 09 42 4c 45 5f 4b 45 59 05 12 0a 00 14 00 02 0a 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00

                        String datas = scancord.substring(0, mData.length());
                        if (mData.equals(datas)){
                            setOptionsData(scancord);
                            // 获取成功之后再进行Indicate的执行和读取和发送消息  将在这里开始运行
                            setWriteData(bleDevice);
                        }
                    }

                    @Override
                    public void onReadFailure(BleException exception) {
                        // 读特征值数据失败
                    }
                });
    }

    public void setCloseConn(BleDevice bleDevice){
        if (BleManager.getInstance().isConnected(bleDevice)) {
            BleManager.getInstance().disconnect(bleDevice);
        }
    }

    private void setOptionsData(String data){
        BleDeviceInfo info = new BleDeviceInfo();
        info.setBroadcast(data.substring(0,6));
        info.setUuid(data.substring(6,18));
        info.setLength(data.substring(18,20));
        info.setType(data.substring(20,22));
        info.setDevId(data.substring(22,26));
        // 电量
        String powerstr = data.substring(26,30);
        if (!"".equals(powerstr) && null != powerstr){
            String start = powerstr.substring(0,2);
            String end = powerstr.substring(2,4);
            String datainfo = Integer.parseInt(end)+"."+start+"V";
            info.setPower(datainfo);
        }
        // 芯片温度
        String Tempstr = data.substring(30,34);
        if (!"".equals(Tempstr) && null != Tempstr){
            String start = Tempstr.substring(0,2);
            String end = Tempstr.substring(2,4);
            String datainfo = start+"."+end+"℃";
            info.setTemperature(datainfo);
        }
        // 按键次数
        String numstr = data.substring(34,42);
        if (!"".equals(numstr) && null != numstr){
            int da = Integer.parseInt(numstr);
            if (da == 0){
                info.setNum(0+"");
            } else {
                for (int i = 0;i<numstr.length();i++){
                    int das = Integer.parseInt(numstr.substring(i,i+1));
                    if (das == 0 && i != 0){
                        int num = 0;
                        int pos = 0;
                        for (int j = (i-1); j>0;j--){
                            int d = Integer.parseInt(numstr.substring(j,j+1));
                            num = num + d * 16^pos;
                        }
                        info.setNum(num+"");
                    }
                }
            }
        }
        // mac 地址
        info.setMac(data.substring(42,54));
        info.setSend(data.substring(54,56));
        info.setOther(data.substring(56,data.length()));
        mBlueToothListener.getDeviceInfo(info);
    }

    String uuid_service = "0000FFE0-0000-1000-8000-00805F9B34FB";
    String characteristic_uuid = "0000FFF1-0000-1000-8000-00805F9B34FB";
    private void setWriteData(BleDevice bleDevice){
        byte[] data = {0x01};
        BleManager.getInstance().write(
                bleDevice,
                uuid_service,
                characteristic_uuid,
                data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        // 发送数据到设备成功（分包发送的情况下，可以通过方法中返回的参数可以查看发送进度）
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                    }
                });
    }

    public void destroyBlueToothPlugin(){
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }
}
