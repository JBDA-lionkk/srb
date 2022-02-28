package com.atguigu.srb.core.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.srb.core.enums.LendStatusEnum;
import com.atguigu.srb.core.enums.TransTypeEnum;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.LendMapper;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.*;
import com.atguigu.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.service.*;
import com.atguigu.srb.core.util.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 标的准备表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
@Slf4j
public class LendServiceImpl extends ServiceImpl<LendMapper, Lend> implements LendService {

    @Resource
    private BorrowInfoService borrowInfoService;

    @Resource
    private DictService dictService;

    @Resource
    private BorrowerService borrowerService;

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private TransFlowService transFlowService;

    @Resource
    private LendItemService lendItemService;

    @Resource
    private LendReturnService lendReturnService;

    @Resource
    private LendItemReturnService lendItemReturnService;

    @Override
    public void createLend(BorrowInfoApprovalVO borrowInfoApprovalVO) {
        //通过borrowInfoId获取数据
        Long id = borrowInfoApprovalVO.getId();
        BorrowInfo borrowInfo = borrowInfoService.getById(id);

        Lend lend = new Lend();
        BeanUtils.copyProperties(borrowInfo, lend);
        lend.setBorrowInfoId(borrowInfo.getId());
        lend.setLendNo(LendNoUtils.getLendNo());
        lend.setTitle(borrowInfoApprovalVO.getTitle());
        lend.setLendYearRate(borrowInfoApprovalVO.getLendYearRate().divide(new BigDecimal(100)));
        lend.setServiceRate(borrowInfoApprovalVO.getServiceRate().divide(new BigDecimal(100)));
        lend.setLowestAmount(new BigDecimal(100));//最低投资金额
        lend.setInvestAmount(new BigDecimal(0));//已投金额
        lend.setInvestNum(0);//已投人数
        lend.setPublishDate(LocalDateTime.now()); //当前时间

        //设置时间格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        //将String日期时间转换
        LocalDate lendStartDate = LocalDate.parse(borrowInfoApprovalVO.getLendStartDate(), dateTimeFormatter);
        lend.setLendStartDate(lendStartDate);//设置起息日期

        LocalDate lendEndDate = lendStartDate.plusMonths(borrowInfo.getPeriod());//起息日期加3个月为结束日期
        lend.setLendEndDate(lendEndDate);//设置结束日期

        lend.setLendInfo(borrowInfoApprovalVO.getLendInfo());//标的描述

        //平台预期收益率 = 标的金额 * (平台服务费 / 12 * 期数)
        BigDecimal monthRate = lend.getServiceRate().divide(new BigDecimal(12), 8, BigDecimal.ROUND_DOWN);
        //(年化 / 12 * 期数)
        BigDecimal multiply = monthRate.multiply(new BigDecimal(lend.getPeriod()));
        //平台预期收益率
        BigDecimal expectAmount = lend.getAmount().multiply(multiply);
        lend.setExpectAmount(expectAmount);

        lend.setRealAmount(new BigDecimal(0));//实际收益

        lend.setStatus(LendStatusEnum.INVEST_RUN.getStatus());//状态  募资中
        lend.setCheckTime(LocalDateTime.now());//审核时间
        lend.setCheckAdminId(1L);//审核人

        //保存
        this.save(lend);
    }

    @Override
    public List<Lend> selectList() {
        //获取所有列表数据
        List<Lend> lendList = this.list();

        //封装数据
        return lendList.stream().map(lends -> {
            Lend lend = new Lend();
            BeanUtils.copyProperties(lends, lend);

            String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", lends.getReturnMethod());
            lend.getParam().put("returnMethod", returnMethod);
            lend.getParam().put("status", LendStatusEnum.getMsgByStatus(lends.getStatus()));
            return lend;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getLendDetail(Long id) {

        //获取标的对象
        Lend lend = this.getById(id);
        lend.getParam().put("returnMethod", dictService.getNameByParentDictCodeAndValue("returnMethod", lend.getReturnMethod()));
        lend.getParam().put("status", LendStatusEnum.getMsgByStatus(lend.getStatus()));

        //获取借款人对象
        Borrower borrower = borrowerService.getOne(Wrappers.<Borrower>lambdaQuery().eq(Borrower::getUserId, lend.getUserId()));
        BorrowerDetailVO borrowerDetailVO = borrowerService.getBorrowerDetailVOById(borrower.getId());

        //组装集合结果
        Map<String, Object> map = new HashMap<>();
        map.put("lend", lend);
        map.put("borrower", borrowerDetailVO);
        return map;
    }

    /**
     * @param invest       投资金额
     * @param yearRate     年化利率
     * @param totalmonth   期数
     * @param returnMethod 还款方式
     * @return BigDecimal
     */
    @Override
    public BigDecimal getInterestCount(BigDecimal invest, BigDecimal yearRate, Integer totalmonth, Integer returnMethod) {

        BigDecimal interestCount;

        //计算利息
        switch (returnMethod) {
            case 1:
                //等额本息
                interestCount = Amount1Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 2:
                //等额本金
                interestCount = Amount2Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            case 3:
                //每月还息一次还本
                interestCount = Amount3Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
            default:
                //一次还本还息
                interestCount = Amount4Helper.getInterestCount(invest, yearRate, totalmonth);
                break;
        }

        return interestCount;
    }

    /**
     * @param id 标的id
     */
    @Override
    public void makeLoan(Long id) {
        //获取标的信息
        Lend lend = this.getById(id);

        //调用汇付宝放款接口
        Map<String, Object> map = new HashMap<>();
        map.put("agentId", HfbConst.AGENT_ID);//给商户分配的唯一标识
        map.put("agentProjectCode", lend.getLendNo());//放款项目编号。只能由数字、字母组成。
        map.put("agentBillNo", LendNoUtils.getNo());//放款单号只能由数字、字母组成字符。
        //计算月年化
        BigDecimal mothRate = lend.getServiceRate().divide(new BigDecimal(12), 8, BigDecimal.ROUND_DOWN);
        //商户手续费 = 已投金额 * 月年化 * 投资期限
        BigDecimal realAmount = lend.getInvestAmount().multiply(mothRate).multiply(new BigDecimal(lend.getPeriod()));
        map.put("mchFee", realAmount);//商户手续费
        map.put("timestamp", RequestHelper.getTimestamp());//时间戳。
        map.put("sign", RequestHelper.getSign(map));//验签参数。

        //提交远程请求
        JSONObject result = RequestHelper.sendRequest(map, HfbConst.MAKE_LOAD_URL);

        //放款失败
        if (!"0000".equals(result.getString("resultCode"))) {
            throw new BusinessException(result.getString("resultMsg"));

        }

        //放款成功
        //（1）标的状态和标的平台收益:更新相关信息
        lend.setRealAmount(realAmount);//平台收益
        lend.setStatus(LendStatusEnum.PAY_RUN.getStatus());
        lend.setPaymentTime(LocalDateTime.now());
        this.updateById(lend);

        //（2）给借款账号转入金额
        //借款人id
        Long userId = lend.getUserId();
        UserInfo userInfo = userInfoService.getById(userId);
        String bindCode = userInfo.getBindCode();
        String voteAmt = result.getString("voteAmt");
        //转账
        userAccountMapper.updateAccount(bindCode, new BigDecimal(voteAmt), new BigDecimal(0));

        //（3）增加借款交易流水
        TransFlowBO transFlowBO = new TransFlowBO();
        String agentBillNo = result.getString("agentBillNo");
        transFlowBO.setAgentBillNo(agentBillNo);
        transFlowBO.setBindCode(bindCode);
        transFlowBO.setAmount(new BigDecimal(voteAmt));
        transFlowBO.setTransTypeEnum(TransTypeEnum.BORROW_BACK);
        transFlowBO.setMemo("项目放款,项目编号:" + lend.getLendNo() + "项目名称:" + lend.getTitle());

        transFlowService.saveTransFlow(transFlowBO);
        //（4）解冻并扣除投资人资金  一个标的有多个投资人
        //获取标的下的投资列表
        List<LendItem> lendItems = lendItemService.selectByLendId(id, 1);
        lendItems.forEach(item -> {
            //投资人id
            Long investUserId = item.getInvestUserId();
            UserInfo investUserInfo = userInfoService.getById(investUserId);
            String investBindCode = investUserInfo.getBindCode();

            BigDecimal investAmount = item.getInvestAmount();
            //转账
            userAccountMapper.updateAccount(investBindCode, new BigDecimal(0), item.getInvestAmount().negate());

            //（5）增加投资人交易流水
            TransFlowBO investTransFlowBO = new TransFlowBO();
            String investAgentBillNo = LendNoUtils.getTransNo();
            investTransFlowBO.setAgentBillNo(investAgentBillNo);
            investTransFlowBO.setBindCode(investBindCode);
            investTransFlowBO.setAmount(investAmount);
            investTransFlowBO.setTransTypeEnum(TransTypeEnum.INVEST_UNLOCK);
            investTransFlowBO.setMemo("项目放款冻结资金转出,项目编号:" + lend.getLendNo() + "项目名称:" + lend.getTitle());

            transFlowService.saveTransFlow(investTransFlowBO);
        });


        //（6）生成借款人还款计划和出借人回款计划
        this.repaymentPlan(lend);

    }

    /**
     * 还款计划
     *
     * @param lend
     */
    private void repaymentPlan(Lend lend) {

        //1.还款计划列表
        List<LendReturn> lendReturnList = new ArrayList<>();

        //2.按还款时间生成还款计划
        int len = lend.getPeriod(); //还款期数
        for (int i = 1; i <= len; i++) {
            //创建还款计划对象
            LendReturn lendReturn = new LendReturn();
            BeanUtils.copyProperties(lend, lendReturn);
            lendReturn.setLendId(lend.getId());
            lendReturn.setReturnNo(LendNoUtils.getReturnNo());//还款批次号
            lendReturn.setAmount(lend.getInvestAmount());
            lendReturn.setCurrentPeriod(i);//当前期数
            lendReturn.setFee(new BigDecimal("0"));
            lendReturn.setReturnDate(lend.getLendEndDate().plusMonths(i));//第二个月开始还款
            lendReturn.setOverdue(false);

            //判断是否是最后一期还款
            if (i == len) {//最后一期
                lendReturn.setLast(true);
            } else {
                lendReturn.setLast(false);
            }

            //设置还款状态
            lendReturn.setStatus(0);//未归还

            //将还款计划加入还款列表
            lendReturnList.add(lendReturn);
        }

        //3.批量保存还款计划
        lendReturnService.saveBatch(lendReturnList);

        //4.生成期数和还款记录的id 对应的k-v 集合
        Map<Integer, Long> lendReturnMap = lendReturnList.stream().collect(Collectors.toMap(LendReturn::getCurrentPeriod, LendReturn::getId));


        //5.创建所有投资的回款记录列表
        List<LendItemReturn> lendItemReturnAllList = new ArrayList<>();

        //6.获取当前标的下的 已支付的投资
        List<LendItem> lendItemList = lendItemService.selectByLendId(lend.getId(), 1);
        for (LendItem lendItem : lendItemList) {
            //根据投资记录的id调用回款计划生成的方法,得到当前这笔投资的回款计划表
            List<LendItemReturn> lendItemReturnList = this.returnInvest(lendItem.getId(), lendReturnMap, lend);

            //将当前这笔投资的回款计划表, 放入所有投资的所有回款记录列表
            lendItemReturnAllList.addAll(lendItemReturnList);
        }

        //遍历还款记录列表
        lendReturnList.forEach(lendReturn -> {
            //通过filter\map\reduce将相关期数的数据过滤出来
            //将当前期数的所在投资人的数据相加,就是当前期数的所有投资人的回款数据(本金\利息\总金额)
            //本金总和
            BigDecimal sumPrincipal = lendItemReturnAllList
                    .stream()
                    .filter(item -> item.getLendReturnId().equals(lendReturn.getId()))
                    .map(LendItemReturn::getPrincipal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            //利息总和
            BigDecimal sumInterest = lendItemReturnAllList
                    .stream()
                    .filter(item -> item.getLendReturnId().equals(lendReturn.getId()))
                    .map(LendItemReturn::getInterest)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            //总收益
            BigDecimal sumTotal = lendItemReturnAllList
                    .stream()
                    .filter(item -> item.getLendReturnId().equals(lendReturn.getId()))
                    .map(LendItemReturn::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            //设置本金 利息 总金额
            lendReturn.setPrincipal(sumPrincipal);//本金
            lendReturn.setInterest(sumInterest);//利息
            lendReturn.setTotal(sumTotal);//总金额
        });


        //批量更新
        lendReturnService.updateBatchById(lendReturnList);
    }


    /**
     * 回款计划 (针对某一笔投资的回款)
     *
     * @param lendReturnMap 还款期数与还款计划id对应map
     * @return List<LendItemReturn>
     */
    private List<LendItemReturn> returnInvest(Long lendItemId, Map<Integer, Long> lendReturnMap, Lend lend) {
        //获取当前投资记录信息
        LendItem lendItem = lendItemService.getById(lendItemId);
        BigDecimal amount = lendItem.getInvestAmount();//投资金额
        BigDecimal yearRate = lendItem.getLendYearRate();//年化利率
        Integer period = lend.getPeriod();//期数

        Map<Integer, BigDecimal> mapInterest = null;  //还款期数->利息
        Map<Integer, BigDecimal> mapPrincipal = null;  //还款期数->本金

        //计算利息   投资金额+年化利率+期数+还款方式
        //计算利息
        switch (lend.getReturnMethod()) {
            case 1:
                //利息
                mapInterest = Amount1Helper.getPerMonthInterest(amount, yearRate, period);
                //本金
                mapPrincipal = Amount1Helper.getPerMonthPrincipal(amount, yearRate, period);
                break;
            case 2:
                //等额本金
                mapInterest = Amount2Helper.getPerMonthInterest(amount, yearRate, period);
                mapPrincipal = Amount2Helper.getPerMonthPrincipal(amount, yearRate, period);
                break;
            case 3:
                //每月还息一次还本
                mapInterest = Amount3Helper.getPerMonthInterest(amount, yearRate, period);
                mapPrincipal = Amount3Helper.getPerMonthPrincipal(amount, yearRate, period);
                break;
            default:
                //一次还本还息
                mapInterest = Amount4Helper.getPerMonthInterest(amount, yearRate, period);
                mapPrincipal = Amount4Helper.getPerMonthPrincipal(amount, yearRate, period);
                break;
        }

        //创建回款计划列表
        List<LendItemReturn> lendItemReturnList = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> entry : mapInterest.entrySet()) {
            //获取当前第几期(期数)
            Integer currentPeriod = entry.getKey();
            Long lendReturnId = lendReturnMap.get(currentPeriod);

            //创建回款计划记录
            LendItemReturn lendItemReturn = new LendItemReturn();

            //将还款记录关联到回款记录
            lendItemReturn.setLendReturnId(lendReturnId);

            //设置回款的基本属性
            lendItemReturn.setLendItemId(lendItemId);
            lendItemReturn.setLendId(lend.getId());
            lendItemReturn.setInvestAmount(lendItem.getInvestAmount());
            lendItemReturn.setLendYearRate(lendItem.getLendYearRate());
            lendItemReturn.setCurrentPeriod(currentPeriod);
            lendItemReturn.setReturnMethod(lend.getReturnMethod());
            lendItemReturn.setInvestUserId(lendItem.getInvestUserId());

            //计算回款本金 利息和总额(注意最后一个月的计算)
            if (lendItemReturnList.size() > 0 && currentPeriod.equals(lend.getPeriod())) {//最后一期
                //本金:获取前几期所有本金
                BigDecimal sumPrincipal = lendItemReturnList.stream()
                        .map(LendItemReturn::getPrincipal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                //当前投资人的总金额减去 前几期所有金额,就是最后一期还款金额
                BigDecimal lastPrincipal = lendItem.getInvestAmount().subtract(sumPrincipal);//投资人总金额
                lendItemReturn.setPrincipal(lastPrincipal);

                //利息
                BigDecimal sumInterest = lendItemReturnList.stream()
                        .map(LendItemReturn::getInterest)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                //当前投资人的总收益减去 前几期所有收益,就是最后一期收益
                BigDecimal lastInterest = lendItem.getExpectAmount().subtract(sumInterest);
                lendItemReturn.setInterest(lastInterest);

            } else {//非最后一期
                lendItemReturn.setPrincipal(mapPrincipal.get(currentPeriod));//本金
                lendItemReturn.setInterest(mapInterest.get(currentPeriod));//利息
            }

            //回款总金额(总收益)
            lendItemReturn.setTotal(lendItemReturn.getPrincipal().add(lendItemReturn.getInterest()));

            //设置回款是否逾期等其他属性
            lendItemReturn.setFee(new BigDecimal("0"));//手续费
            lendItemReturn.setReturnDate(lend.getLendStartDate().plusMonths(currentPeriod));//还款时指定的还款日期
            lendItemReturn.setOverdue(false);//是否逾期
            lendItemReturn.setStatus(0);//默认未归还

            //将回款记录放入回款列表
            lendItemReturnList.add(lendItemReturn);

        }

        //批量保存
        lendItemReturnService.saveBatch(lendItemReturnList);


        return lendItemReturnList;
    }
}
