package com.atguigu.srb.sms.component;

import com.aliyuncs.exceptions.ServerException;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.RandomUtils;
import com.atguigu.srb.sms.util.HttpUtils;
import com.google.gson.Gson;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Data
@ConfigurationProperties(prefix = "spring.alicloud.sms")
public class SmsComponent {

    private String host;

    private String path;

    private String appCode;

    private String smsSignId;

    private String templateId;

    public void sendSmsCode(String mobile, String code) {
        String method = "POST";

        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appCode);
        Map<String, String> querys = new HashMap<>();
        querys.put("mobile", mobile);

        querys.put("param", "**code**:" + code + ",**minute**:5");
        querys.put("smsSignId", smsSignId);

        //模板
        querys.put("templateId", templateId);
        Map<String, String> bodys = new HashMap<String, String>();


        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println("response:" + response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            log.error("阿里云短信发送sdk调用失败:{}", e.getMessage());
            throw new BusinessException(ResponseEnum.ALIYUN_SMS_ERROR, e);

//            e.printStackTrace();
        }
    }

}
