package com.atguigu.srb.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.enums.LendStatusEnum;
import com.atguigu.srb.core.enums.TransTypeEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.*;
import com.atguigu.srb.core.mapper.LendReturnMapper;
import com.atguigu.srb.core.service.*;
import com.atguigu.srb.core.util.LendNoUtils;
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
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 还款记录表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
@Slf4j
public class LendReturnServiceImpl extends ServiceImpl<LendReturnMapper, LendReturn> implements LendReturnService {

    @Resource
    private LendService lendService;

    @Resource
    private UserBindService userBindService;

    @Resource
    private LendItemReturnService lendItemReturnService;

    @Resource
    private UserAccountService userAccountService;

    @Resource
    private TransFlowService transFlowService;

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private LendItemService lendItemService;

    @Override
    public List<LendReturn> selectByLendId(Long lendId) {

        return this.list(Wrappers.<LendReturn>lambdaQuery().eq(LendReturn::getLendId, lendId));
    }

    @Override
    public String commitReturn(Long lendReturnId, HttpServletRequest request) {
        //获取登录用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        //获取还款记录表数据
        LendReturn lendReturn = this.getById(lendReturnId);

        //获取用户余额
        UserAccount userAccount = userAccountService.getOne(Wrappers.<UserAccount>lambdaQuery().eq(UserAccount::getUserId, userId));
        BigDecimal amount = userAccount.getAmount();//余额

        Assert.isTrue(amount.doubleValue() >= lendReturn.getTotal().doubleValue(),
                ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);

        //获取标的数据
        Lend lend = lendService.getById(lendReturn.getLendId());
        //获取还款人bindCode
        String bindCode = userBindService.getBindCodeByUserId(userId);

        //组装参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentGoodsName", lend.getTitle());
        paramMap.put("agentBatchNo", lendReturn.getReturnNo());
        paramMap.put("fromBindCode", bindCode);
        paramMap.put("totalAmt", lendReturn.getTotal());
        paramMap.put("note", "");

        //还款明细
        List<Map<String, Object>> lendItemReturnDetailList = lendItemReturnService.addReturnDetail(lendReturnId);
        paramMap.put("data", JSONObject.toJSONString(lendItemReturnDetailList));

        paramMap.put("voteFeeAmt", new BigDecimal(0));
        paramMap.put("returnUrl", HfbConst.BORROW_RETURN_RETURN_URL);
        paramMap.put("notifyUrl", HfbConst.BORROW_RETURN_NOTIFY_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);

        //构建自动提交表单
        return FormHelper.buildForm(HfbConst.BORROW_RETURN_URL, paramMap);

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String notify(HttpServletRequest request) {
        //将接收到的数据转为map
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());

        log.info("还款异步回调 :" + JSON.toJSONString(paramMap));

        //验签
        if (RequestHelper.isSignEquals(paramMap)) {
            if ("0001".equals(paramMap.get("resultCode"))) {
                this.idempotency(paramMap);
            } else {
                log.info("还款异步回调失败:" + JSON.toJSONString(paramMap));
                return "fail";
            }
        } else {
            log.info("还款异步回调验签失败:" + JSON.toJSONString(paramMap));
            return "fail";
        }

        return "success";
    }

    private void idempotency(Map<String, Object> paramMap) {
        log.info("还款成功");

        //1.幂等性判断
        //获取还款编号
        String agentBatchNo = (String) paramMap.get("agentBatchNo");
        Boolean result = transFlowService.isSaveTransFlow(agentBatchNo);
        if (result) {
            log.warn("幂等性返回");
            return;
        }

        //2.更新还款状态
        //获取还款对象
        LendReturn lendReturn = this.getOne(Wrappers.<LendReturn>lambdaQuery().eq(LendReturn::getReturnNo, agentBatchNo));

        //更新还款状态
        String voteFeeAmt = (String) paramMap.get("voteFeeAmt");//商户手续费
        lendReturn.setStatus(1);//已还款
        lendReturn.setFee(new BigDecimal(voteFeeAmt));
        lendReturn.setRealReturnTime(LocalDateTime.now());//还款时间
        this.updateById(lendReturn);

        //3.更新标的信息
        //获取标的信息
        Lend lend = lendService.getById(lendReturn.getLendId());

        //如果是最后一期还款
        if (lendReturn.getLast()) {
            //更新标的状态
            lend.setStatus(LendStatusEnum.PAY_OK.getStatus());
            lendService.updateById(lend);
        }

        //4.还款账号转出金额

        //获取还款人bindCode
        String bindCode = userBindService.getBindCodeByUserId(lendReturn.getUserId());
        //获取扣款金额
        String totalAmt = (String) paramMap.get("totalAmt");
        userAccountMapper.updateAccount(bindCode, new BigDecimal("-" + totalAmt), new BigDecimal(0));

        //5.保存还款流水
        TransFlowBO transFlowBO = new TransFlowBO();
        transFlowBO.setAgentBillNo(agentBatchNo);
        transFlowBO.setBindCode(bindCode);
        transFlowBO.setAmount(new BigDecimal(totalAmt));
        transFlowBO.setMemo("借款人还款扣减,项目编号:" + lend.getLendNo() + ",项目名称:" + lend.getTitle());
        transFlowBO.setTransTypeEnum(TransTypeEnum.RETURN_DOWN);

        transFlowService.saveTransFlow(transFlowBO);

        //6.回款明细的获取 (更新回款状态 - 投资账号转入金额 -保存投资人流水)
        //获取回款明细
        List<LendItemReturn> lendItemReturnList = lendItemReturnService.selectLendItemReturnList(lendReturn.getId());
        lendItemReturnList.forEach(item -> {
            //更新回款状态
            item.setStatus(1);
            item.setRealReturnTime(LocalDateTime.now());
            lendItemReturnService.updateById(item);

            //更新出借信息
            LendItem lendItem = lendItemService.getById(item.getLendItemId());
            lendItem.setStatus(2);
            lendItem.setRealAmount(lendItem.getRealAmount().add(item.getInterest()));//动态的实际收益  将之前几期金额都取出来 加上最新收益
            lendItemService.updateById(lendItem);

            //投资账号转入金额
            //获取投资人bindCode
            String investBindCode = userBindService.getBindCodeByUserId(item.getInvestUserId());
            userAccountMapper.updateAccount(investBindCode, item.getTotal(), new BigDecimal(0));

            //保存投资人回款流水
            TransFlowBO investTransFlowBO = new TransFlowBO();
            investTransFlowBO.setAgentBillNo(LendNoUtils.getReturnItemNo());//新生成
            investTransFlowBO.setBindCode(investBindCode);
            investTransFlowBO.setAmount(item.getTotal());
            investTransFlowBO.setMemo("还款到账,项目编号:" + lend.getLendNo() + ",项目名称:" + lend.getTitle());
            investTransFlowBO.setTransTypeEnum(TransTypeEnum.INVEST_BACK);

            transFlowService.saveTransFlow(investTransFlowBO);
        });

    }
}
