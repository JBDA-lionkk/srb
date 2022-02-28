package com.atguigu.srb.core.pojo.bo;

import com.atguigu.srb.core.enums.TransTypeEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransFlowBO {

    @ApiModelProperty(value = "订单号")
    private String agentBillNo;

    @ApiModelProperty(value = "充值人绑定协议号")
    private String bindCode;

    @ApiModelProperty(value = "金额")
    private BigDecimal amount;

    @ApiModelProperty(value = "类型")
    private TransTypeEnum transTypeEnum;

    @ApiModelProperty(value = "描述")
    private String memo;
}
