package com.example.netservice.roomdb.dao;


import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.example.netservice.roomdb.beans.GroupUserInfo;

import java.util.List;


@Dao
public interface GroupUserInfoDao {
    /**
     * 查询所有
     *
     * @return
     */
    @Query("SELECT * FROM GroupUserInfo")
    List<GroupUserInfo> getDataAll();


    /**
     * 查询当前指定群内所有缓存
     * @return
     */
    @Query("SELECT * FROM GroupUserInfo where groupId = :groupid")
    List<GroupUserInfo> getDataAll(String groupid);


    /**
     * 删除当前指定群类所有消息
     * @param groupid
     */
    @Query("DELETE FROM GroupUserInfo WHERE groupId = :groupid")
    int deleteFunTypes(String groupid);

    /**
     * 项数据库添加数据
     *
     * @param keyWordsList
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<GroupUserInfo> keyWordsList);

    /**
     * 项数据库添加数据
     * @param keyWords
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GroupUserInfo keyWords);

    /**
     * 修改数据
     * @param keyWords
     */
    @Update()
    int update(GroupUserInfo keyWords);

    /**
     * 删除数据
     * @param keyWords
     */
    @Delete()
    void delete(GroupUserInfo keyWords);

    /**
     * 删除群中的这个用户
     */
    @Query("DELETE FROM GroupUserInfo WHERE groupId = :groupId and userId = :userId")
    void delete(String groupId, String userId);
}