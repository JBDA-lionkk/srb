package com.atguigu.srb.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.enums.LendStatusEnum;
import com.atguigu.srb.core.enums.TransTypeEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.LendItemMapper;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.atguigu.srb.core.pojo.vo.InvestVO;
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
 * 标的出借记录表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
@Slf4j
public class LendItemServiceImpl extends ServiceImpl<LendItemMapper, LendItem> implements LendItemService {

    @Resource
    private LendService lendService;

    @Resource
    private UserAccountService userAccountService;

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private UserBindService userBindService;

    @Resource
    private TransFlowService transFlowService;

    @Override
    public String commitInvest(InvestVO investVO, HttpServletRequest request) {
        //获取userid 和userName
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);
        String userName = JwtUtils.getUserName(token);

        investVO.setInvestUserId(userId);
        investVO.setInvestName(userName);

        //构建充值自动提交表单
        return this.autoCommitInvest(investVO);

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
                log.info("用户投资异步回调失败:" + JSON.toJSONString(paramMap));
                return "fail";
            }

        } else {
            return "fail";
        }
    }

    @Override
    public List<LendItem> selectByLendId(Long lendId, Integer status) {
        //获取所有投资人,并且状态为已支付
        return this.list(Wrappers.<LendItem>lambdaQuery().eq(LendItem::getLendId, lendId).eq(LendItem::getStatus, status));
    }

    @Override
    public List<LendItem> selectByLendId(Long lendId) {
        return this.list(Wrappers.<LendItem>lambdaQuery().eq(LendItem::getLendId, lendId));
    }

    private String idempotency(Map<String, Object> paramMap) {
        //1.判断幂等性返回
        String agentBillNo = (String) paramMap.get("agentBillNo");
        Boolean result = transFlowService.isSaveTransFlow(agentBillNo);
        if (result) {
            log.info("幂等性返回");
            return "success";
        }

        //2.修改账户金额:从余额中减去投资金额,在冻结金额中增加投资金额
        String bindCode = (String) paramMap.get("voteBindCode");
        String voteAmt = (String) paramMap.get("voteAmt");//投资金额
        userAccountMapper.updateAccount(bindCode, new BigDecimal("-" + voteAmt), new BigDecimal(voteAmt));

        //3.修改投资记录状态
        this.update(Wrappers.<LendItem>lambdaUpdate()
                .eq(LendItem::getLendItemNo, agentBillNo)
                .set(LendItem::getStatus, 1));

        //4.标的修改:投资人数  已投金额
        LendItem lendItem = this.getOne(Wrappers.<LendItem>lambdaQuery().eq(LendItem::getLendItemNo, agentBillNo));
        Long lendId = lendItem.getLendId();
        Lend lend = lendService.getById(lendId);
        lend.setInvestNum(lend.getInvestNum() + 1);
        lend.setInvestAmount(lend.getInvestAmount().add(lendItem.getInvestAmount()));
        lendService.updateById(lend);

        //5.新增交易流水
        TransFlowBO transFlowBO = new TransFlowBO();
        transFlowBO.setBindCode(bindCode);
        transFlowBO.setMemo("项目编号" + lend.getLendNo() + ",项目名称:" + lend.getTitle());
        transFlowBO.setAmount(new BigDecimal(voteAmt));
        transFlowBO.setTransTypeEnum(TransTypeEnum.INVEST_LOCK);
        transFlowBO.setAgentBillNo(agentBillNo);

        transFlowService.saveTransFlow(transFlowBO);

        return "success";
    }


    private String autoCommitInvest(InvestVO investVO) {
        //1.校验
        Long lendId = investVO.getLendId();
        Lend lend = lendService.getById(lendId);

        //标的状态为募资中
        Assert.isTrue(lend.getStatus().equals(LendStatusEnum.INVEST_RUN.getStatus()), ResponseEnum.LEND_INVEST_ERROR);

        //超卖: 已投金额 + 当前投资金额 > 标的金额(超卖)
        BigDecimal sum = lend.getInvestAmount().add(new BigDecimal(investVO.getInvestAmount()));
        Assert.isTrue(sum.doubleValue() <= lend.getAmount().doubleValue(), ResponseEnum.LEND_FULL_SCALE_ERROR);

        //判断用户余额: 当前用户余额 > 当前投资金额
        Long userId = investVO.getInvestUserId();
        UserAccount userAccount = userAccountService.getOne(Wrappers.<UserAccount>lambdaQuery().eq(UserAccount::getUserId, userId));
        BigDecimal amount = userAccount.getAmount();
        Assert.isTrue(amount.doubleValue() >= Double.parseDouble(investVO.getInvestAmount()), ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);

        //2.获取paramMap中需要的参数
        //生成标的下的投资记录
        LendItem lendItem = new LendItem();
        String lendItemNo = LendNoUtils.getLendItemNo();
        lendItem.setLendItemNo(lendItemNo);//投资编号
        lendItem.setLendId(lendId);//标的id
        lendItem.setInvestUserId(userId);//投资用户id
        lendItem.setInvestName(investVO.getInvestName());//投资人名称
        lendItem.setInvestAmount(new BigDecimal(investVO.getInvestAmount()));//投资金额
        lendItem.setLendYearRate(lend.getLendYearRate());//年化利率
        lendItem.setInvestTime(LocalDateTime.now());//投资时间
        lendItem.setLendStartDate(lend.getLendStartDate());//开始日期
        lendItem.setLendEndDate(lend.getLendEndDate());//结束日期

        //预期收益
        BigDecimal interestCount = lendService.getInterestCount(
                new BigDecimal(investVO.getInvestAmount()),
                lend.getLendYearRate(),
                lend.getPeriod(),
                lend.getReturnMethod());

        lendItem.setExpectAmount(interestCount);//投资人预期收益
        lendItem.setRealAmount(new BigDecimal(0));//实际收益
        lendItem.setStatus(0);//状态（0：默认 1：已支付 2：已还款）

        //保存投资记录
        this.save(lendItem);


        //获取投资人的bindCode
        String bindCode = userBindService.getBindCodeByUserId(userId);

        //获取借款人的bindCode
        String benefitBindCode = userBindService.getBindCodeByUserId(lend.getUserId());

        //3.封装提交至汇付宝的参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);//商户号
        paramMap.put("voteBindCode", bindCode);//投资人绑定号
        paramMap.put("benefitBindCode", benefitBindCode);//借款人绑定号
        paramMap.put("agentProjectCode", lend.getLendNo());//项目标号
        paramMap.put("agentProjectName", lend.getTitle());
        //在资金托管平台上的投资订单的唯一编号，要和lendItemNo保持一致。
        paramMap.put("agentBillNo", lendItemNo);//订单编号
        paramMap.put("voteAmt", investVO.getInvestAmount());
        paramMap.put("votePrizeAmt", "0");
        paramMap.put("voteFeeAmt", "0");
        paramMap.put("projectAmt", lend.getAmount()); //标的总金额
        paramMap.put("note", "");
        paramMap.put("notifyUrl", HfbConst.INVEST_NOTIFY_URL); //检查常量是否正确
        paramMap.put("returnUrl", HfbConst.INVEST_RETURN_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);
        //构建充值自动提交表单
        String formStr = FormHelper.buildForm(HfbConst.INVEST_URL, paramMap);
        return formStr;

    }
}
