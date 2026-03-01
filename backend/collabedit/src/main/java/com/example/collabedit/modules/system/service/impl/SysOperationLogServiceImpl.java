package com.example.collabedit.modules.system.service.impl;

import com.example.collabedit.modules.system.entity.SysOperationLog;
import com.example.collabedit.modules.system.mapper.SysOperationLogMapper;
import com.example.collabedit.modules.system.service.SysOperationLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SysOperationLogServiceImpl implements SysOperationLogService {

    @Resource
    private SysOperationLogMapper sysOperationLogMapper;


    public void saveLog(SysOperationLog log) {
    // 通过log对象获取resource属性，再进行判断和截断
    if (log.getResource() != null && log.getResource().length() > 1000) {
        log.setResource(log.getResource().substring(0, 1000)); // 截断后重新设置回对象
    }
    // 执行插入操作
    sysOperationLogMapper.insert(log);
    }
}