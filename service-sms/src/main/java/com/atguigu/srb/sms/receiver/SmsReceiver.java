package com.atguigu.srb.sms.receiver;

import com.atguigu.srb.base.dto.SmsDto;
import com.atguigu.srb.rabbitutil.constant.MQConst;
import com.atguigu.srb.sms.service.SmsService;
import com.atguigu.srb.sms.util.SmsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SmsReceiver {

    @Resource
    private SmsService smsService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MQConst.QUEUE_SMS_ITEM, declare = "true"),
            exchange = @Exchange(value = MQConst.EXCHANGE_TOPIC_SMS),
            key = {MQConst.ROUTING_SMS_ITEM}
    ))
    public void send(SmsDto smsDto) {
        log.info("SmsReceiver消息监听...........");

        Map<String, Object> map = new HashMap<>();
        map.put("code", smsDto.getMessage());
        smsService.send(smsDto.getMobile(), SmsProperties.TEMPLATE_CODE, map);
    }
}
