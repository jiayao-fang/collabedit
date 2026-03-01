package com.example.collabedit.modules.system.mapper;

import com.example.collabedit.modules.system.entity.SysOperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperationLogMapper {

    @Insert("INSERT INTO sys_operation_log(user_id, operation, operate_time, ip_address, resource, user_name) " +
            "VALUES(#{userId}, #{operation}, #{operateTime}, #{ipAddress}, #{resource}, #{userName})")
    void insert(SysOperationLog log);
}