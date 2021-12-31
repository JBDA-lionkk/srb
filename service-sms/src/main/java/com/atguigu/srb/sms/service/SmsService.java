package com.atguigu.srb.sms.service;

import java.util.Map;

public interface SmsService {
    /**
     * 发送短信接口
     *
     * @param mobile       模板
     * @param templateCode 模板编号
     * @param param        模板参数
     */
    void send(String mobile, String templateCode, Map<String, Object> param);
}
