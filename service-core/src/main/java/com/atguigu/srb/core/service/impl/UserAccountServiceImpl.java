package com.atguigu.srb.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.base.dto.SmsDto;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.enums.LendStatusEnum;
import com.atguigu.srb.core.enums.TransTypeEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.service.TransFlowService;
import com.atguigu.srb.core.service.UserAccountService;
import com.atguigu.srb.core.service.UserBindService;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.util.LendNoUtils;
import com.atguigu.srb.rabbitutil.constant.MQConst;
import com.atguigu.srb.rabbitutil.service.MQService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户账户 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
@Slf4j
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private TransFlowService transFlowService;

    @Resource
    private UserBindService userBindService;

    @Resource
    private MQService mqService;

    @Override
    public String commitCharge(BigDecimal chargeAmt, HttpServletRequest request) {
        //获取用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        //查询用户信息
        UserInfo userInfo = userInfoService.getById(userId);

        //表单提交,组装表单字符串,用于远程提交数据
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);//给商户分配的唯一标识
        paramMap.put("agentBillNo", LendNoUtils.getChargeNo());//商户充值单号（要求唯一）
        paramMap.put("bindCode", userInfo.getBindCode());//充值人绑定协议号。
        paramMap.put("chargeAmt", chargeAmt);//充值金额，即充值到汇付宝的金额。支持小数点后2位。
        paramMap.put("feeAmt", new BigDecimal(0));//商户收取用户的手续费。支持小数点后2位。可以传0。
        paramMap.put("notifyUrl", HfbConst.RECHARGE_NOTIFY_URL);//通知商户充值成功的完整地址
        paramMap.put("returnUrl", HfbConst.RECHARGE_RETURN_URL);//充值完成后同步返回商户的完整地址。
        paramMap.put("timestamp", RequestHelper.getTimestamp());//时间戳。从1970-01-01 00:00:00算起的毫秒数。
        paramMap.put("sign", RequestHelper.getSign(paramMap));//验签参数。

        return FormHelper.buildForm(HfbConst.RECHARGE_URL, paramMap);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String notify(HttpServletRequest request) {
        //将数据转为map
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());

        log.info("用户充值异步回调:" + JSON.toJSONString(paramMap));

        //验签
        if (RequestHelper.isSignEquals(paramMap)) {

            //判断业务是否成功
            if ("0001".equals(paramMap.get("resultCode"))) {
                //同步账户数据  保证数据一致性,幂等性
                return this.idempotency(paramMap);

            } else {
                return "success";
            }

        } else {
            return "fail";
        }

    }

    @Override
    public BigDecimal getAccount(HttpServletRequest request) {
        //获取用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        //获取账户金额
        UserAccount userAccount = this.getOne(Wrappers.<UserAccount>lambdaQuery().eq(UserAccount::getUserId, userId));

        return userAccount.getAmount();
    }

    @Override
    public String commitWithdraw(BigDecimal fetchAmt, HttpServletRequest request) {
        //获取登录用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);
        String bindCode = userBindService.getBindCodeByUserId(userId);

        //校验余额是否充足
        UserAccount userAccount = this.getOne(Wrappers.<UserAccount>lambdaQuery().eq(UserAccount::getUserId, userId));
        BigDecimal amount = userAccount.getAmount();//余额

        Assert.isTrue(amount.doubleValue() >= fetchAmt.doubleValue(), ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);

        //组装自动提交表单数据
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentBillNo", LendNoUtils.getWithdrawNo());
        paramMap.put("bindCode", bindCode);
        paramMap.put("fetchAmt", fetchAmt);
        paramMap.put("feeAmt", new BigDecimal(0));
        paramMap.put("returnUrl", HfbConst.WITHDRAW_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.WITHDRAW_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());

        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);

        //自动组装并提交表单
        return FormHelper.buildForm(HfbConst.WITHDRAW_URL, paramMap);
    }

    @Override
    public String notifyWithdraw(HttpServletRequest request) {
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("提现异步回调:" + JSON.toJSONString(paramMap));

        //校验签名
        if (RequestHelper.isSignEquals(paramMap)) {

            //提现成功交易
            if ("0001".equals(paramMap.get("resultCode"))) {
                this.notifyWithdrawal(paramMap);
            } else {
                log.info("异步回调提现失败," + JSON.toJSONString(paramMap));
                return "fail";
            }
        } else {
            log.info("异步回调签名错误," + JSON.toJSONString(paramMap));
            return "fail";
        }

        return "success";
    }

    private void notifyWithdrawal(Map<String, Object> paramMap) {
        log.info("提现成功");
        //幂等判断
        String agentBillNo = (String) paramMap.get("agentBillNo");

        Boolean result = transFlowService.isSaveTransFlow(agentBillNo);
        if (result) {
            log.info("幂等性返回");
            return;
        }

        String bindCode = (String) paramMap.get("bindCode");
        String fetchAmt = (String) paramMap.get("fetchAmt");

        //账户同步:根据用户账户修改账户金额
        baseMapper.updateAccount(bindCode, new BigDecimal("-" + fetchAmt), new BigDecimal(0));

        //保存交易流水
        TransFlowBO transFlowBO = new TransFlowBO();
        transFlowBO.setBindCode(bindCode);
        transFlowBO.setAmount(new BigDecimal(fetchAmt));
        transFlowBO.setAgentBillNo(agentBillNo);
        transFlowBO.setMemo("提现");
        transFlowBO.setTransTypeEnum(TransTypeEnum.WITHDRAW);

        transFlowService.saveTransFlow(transFlowBO);

    }

    private String idempotency(Map<String, Object> paramMap) {
        //1.幂等性判断? 判断流水是否存在
        //获取流水号
        String agentBillNo = (String) paramMap.get("agentBillNo");
        Boolean isSave = transFlowService.isSaveTransFlow(agentBillNo);
        if (isSave) {
            log.info("幂等性返回");
            return "success";
        }

        //2.账户处理
        //获取bindCode
        String bindCode = (String) paramMap.get("bindCode");
        String amount = (String) paramMap.get("chargeAmt");

        //修改
        userAccountMapper.updateAccount(bindCode, new BigDecimal(amount), new BigDecimal(0));

        //3.记录账户流水

        TransFlowBO transFlowBO = new TransFlowBO();
        transFlowBO.setBindCode(bindCode);
        transFlowBO.setMemo("充值");
        transFlowBO.setAmount(new BigDecimal(amount));
        transFlowBO.setTransTypeEnum(TransTypeEnum.RECHARGE);
        transFlowBO.setAgentBillNo(agentBillNo);

        transFlowService.saveTransFlow(transFlowBO);

        //发消息
        //通过bindCode获取手机号
        String mobile = userInfoService.getMobileByBinCode(bindCode);
        SmsDto smsDto = new SmsDto();
        smsDto.setMobile(mobile);
        smsDto.setMessage("充值成功");
        mqService.sendMessage(MQConst.EXCHANGE_TOPIC_SMS, MQConst.ROUTING_SMS_ITEM, smsDto);

        return "success";
    }
}