package com.example.netservice.roomdb.beans;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.support.annotation.NonNull;

@Entity(primaryKeys = {"groupId","userId"}) // 组合主键和主键两个只能存在一个 TODO 共享导航界面缓存信息
public class GroupUserInfo {
    @NonNull
    @ColumnInfo(name = "groupId")
    public String groupId; // 群id
    @NonNull
    @ColumnInfo(name = "userId")
    public String userId;// 用户id
    @ColumnInfo(name = "time")
    public long time;// 当前时间戳 long time = TimeUtil.currentTimeMillis()
    @ColumnInfo(name = "nickName")
    public String nickName;// 昵称
    @ColumnInfo(name = "type")
    public int type;// 1: 队长  2: 成员
    @ColumnInfo(name = "avatar")
    public String avatar;// 用户头像
    @ColumnInfo(name = "numberPlate")
    public String numberPlate;// 车牌号
    @ColumnInfo(name = "latitude")
    public double latitude;// 当前纬度
    @ColumnInfo(name = "longitude")
    public double longitude;// 当前经度
}