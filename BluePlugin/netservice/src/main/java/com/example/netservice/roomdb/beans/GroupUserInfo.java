package com.example.netservice.roomdb.beans;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;

@Entity(primaryKeys = {"mac"}) // 组合主键和主键两个只能存在一个
public class GroupUserInfo {
    @NonNull
    @ColumnInfo(name = "mac")
    public String mac; // 设备mac地址
    @ColumnInfo(name = "time")
    public long time;// 当前时间戳 long time = TimeUtil.currentTimeMillis()
    @ColumnInfo(name = "type")
    public int up_num;// 上传次数
}