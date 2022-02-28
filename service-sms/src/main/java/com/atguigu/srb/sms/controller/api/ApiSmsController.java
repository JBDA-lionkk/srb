package com.atguigu.srb.sms.controller.api;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.RandomUtils;
import com.atguigu.common.util.RegexValidateUtils;
import com.atguigu.srb.sms.client.CoreUserInfoClient;
import com.atguigu.srb.sms.component.SmsComponent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sms")
@Api(tags = "短信管理")
@Slf4j
public class ApiSmsController {

//    @Resource
//    private SmsService smsService;

    @Resource
    private SmsComponent smsComponent;

    @Resource
    private CoreUserInfoClient userInfoClient;

    @Resource
    private RedisTemplate redisTemplate;

    @GetMapping("/send/{mobile}")
    public R send(
            @ApiParam(value = "手机号", required = true)
            @PathVariable String mobile) {
        //校验手机号码不能为空
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);

        //校验手机号码的合法性
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);

        HashMap<String, Object> map = new HashMap<>();
        //随机生成的4位数验证码
        String code = RandomUtils.getFourBitRandom();
        map.put("code", code);

        //判断手机号是否已经注册
        boolean result = userInfoClient.checkMobile(mobile);
        Assert.isTrue(!result, ResponseEnum.MOBILE_EXIST_ERROR);

        //连接阿里云短信服务接口 (暂时不能用)
//        smsService.send(mobile, SmsProperties.TEMPLATE_CODE, map);

        //短信接口API
        smsComponent.sendSmsCode(mobile, code);

        //将验证码存入redis
        redisTemplate.opsForValue().set("srb:sms:code:" + mobile, code, 5, TimeUnit.MINUTES);

        return R.ok().message("短信发送成功");

    }
}
