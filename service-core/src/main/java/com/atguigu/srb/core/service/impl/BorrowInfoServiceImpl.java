package com.atguigu.srb.core.service.impl;

import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.enums.BorrowInfoStatusEnum;
import com.atguigu.srb.core.enums.BorrowerStatusEnum;
import com.atguigu.srb.core.enums.UserBindEnum;
import com.atguigu.srb.core.mapper.BorrowInfoMapper;
import com.atguigu.srb.core.pojo.entity.BorrowInfo;
import com.atguigu.srb.core.pojo.entity.Borrower;
import com.atguigu.srb.core.pojo.entity.IntegralGrade;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowInfoVO;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.service.*;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 借款信息表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class BorrowInfoServiceImpl extends ServiceImpl<BorrowInfoMapper, BorrowInfo> implements BorrowInfoService {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private IntegralGradeService integralGradeService;

    @Resource
    private DictService dictService;

    @Resource
    private BorrowerService borrowerService;

    @Resource
    private LendService lendService;

    @Override
    public BigDecimal getBorrowerAmount(Long userId) {
        //获取用户积分
        UserInfo userInfo = userInfoService.getById(userId);
        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR.getMessage());
        Integer integral = userInfo.getIntegral();

        //根据积分查询借款额度
        IntegralGrade integralGrade = integralGradeService.getOne(Wrappers.<IntegralGrade>lambdaQuery()
                .ge(IntegralGrade::getIntegralEnd, integral)
                .le(IntegralGrade::getIntegralStart, integral));

        if (integralGrade == null) {
            return new BigDecimal("0");
        }
        return integralGrade.getBorrowAmount();
    }

    @Override
    public void saveBorrowInfo(BorrowInfo borrowInfo, HttpServletRequest request) {
        //1.获取用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        //2.判断用户绑定状态
        UserInfo userInfo = userInfoService.getById(userId);
        //绑定状态
        Integer bindStatus = userInfo.getBindStatus();
        Assert.isTrue(bindStatus.equals(UserBindEnum.BIND_OK.getStatus()), ResponseEnum.USER_NO_BIND_ERROR.getMessage());

        //3.判断借款人额度申请状态
        //借款人认证状态
        Integer borrowAuthStatus = userInfo.getBorrowAuthStatus();
        Assert.isTrue(borrowAuthStatus.equals(BorrowerStatusEnum.AUTH_OK.getStatus()), ResponseEnum.USER_NO_AMOUNT_ERROR.getMessage());

        //4.判断借款人额度是否充足
        BigDecimal amount = this.getBorrowerAmount(userId);
        Assert.isTrue(borrowInfo.getAmount().doubleValue() <= amount.doubleValue(), ResponseEnum.USER_AMOUNT_LESS_ERROR.getMessage());

        //5.保存借款人申请信息
        borrowInfo.setUserId(userId);
        //将年利率除以100  变为0.12
        BigDecimal divide = borrowInfo.getBorrowYearRate().divide(new BigDecimal(100));
        borrowInfo.setBorrowYearRate(divide);
        borrowInfo.setStatus(BorrowInfoStatusEnum.CHECK_RUN.getStatus());
        this.save(borrowInfo);

    }

    @Override
    public Integer getStatusByUserId(HttpServletRequest request) {
        //获取用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);
        BorrowInfo borrowInfo = this.getOne(Wrappers.<BorrowInfo>lambdaQuery().eq(BorrowInfo::getUserId, userId));
        if (borrowInfo == null) {
            return BorrowInfoStatusEnum.NO_AUTH.getStatus();
        }

        return borrowInfo.getStatus();
    }

    @Override
    public List<BorrowInfoVO> selectList() {
        List<BorrowInfoVO> borrowInfoList = baseMapper.selectBorrowInfoList();

        //封装VO数据
        List<BorrowInfoVO> list = borrowInfoList.stream().map(borrowInfo -> {
            BorrowInfoVO borrowInfoVO = new BorrowInfoVO();
            BeanUtils.copyProperties(borrowInfo, borrowInfoVO);

            String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", borrowInfo.getReturnMethod());
            String moneyUse = dictService.getNameByParentDictCodeAndValue("moneyUse", borrowInfo.getMoneyUse());
            borrowInfoVO.getParam().put("returnMethod", returnMethod);
            borrowInfoVO.getParam().put("moneyUse", moneyUse);

            //获取审批状态
            String status = BorrowInfoStatusEnum.getMsgByStatus(borrowInfo.getStatus());
            borrowInfoVO.getParam().put("status", status);
            return borrowInfoVO;
        }).collect(Collectors.toList());

        return list;
    }

    @Override
    public Map<String, Object> getBorrowInfoDetail(Long id) {

        //查询借款对象 BorrowInfo
        BorrowInfo borrowInfo = this.getById(id);
        BorrowInfoVO borrowInfoVo = new BorrowInfoVO();
        BeanUtils.copyProperties(borrowInfo, borrowInfoVo);
        String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", borrowInfo.getReturnMethod());
        String moneyUse = dictService.getNameByParentDictCodeAndValue("moneyUse", borrowInfo.getMoneyUse());
        String status = BorrowInfoStatusEnum.getMsgByStatus(borrowInfo.getStatus());
        borrowInfoVo.getParam().put("returnMethod", returnMethod);
        borrowInfoVo.getParam().put("moneyUse", moneyUse);
        borrowInfoVo.getParam().put("status", status);

        //借款人对象 Borrower(BorrowerDetailVo)
        Borrower borrower = borrowerService.getOne(Wrappers.<Borrower>lambdaQuery().eq(Borrower::getUserId, borrowInfo.getUserId()));
        BorrowerDetailVO borrowerDetailVO = borrowerService.getBorrowerDetailVOById(borrower.getId());

        //封装集合结果
        Map<String, Object> result = new HashMap<>();
        result.put("borrowInfo", borrowInfoVo);
        result.put("borrower", borrowerDetailVO);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approval(BorrowInfoApprovalVO borrowInfoApprovalVO) {

        //修改借款审核状态 borrow_info
        Long borrowInfoId = borrowInfoApprovalVO.getId();
        this.update(Wrappers.<BorrowInfo>lambdaUpdate()
                .set(BorrowInfo::getStatus, borrowInfoApprovalVO.getStatus())
                .eq(BorrowInfo::getId, borrowInfoId));

        //审核通过产生新的标的记录 lend
        if (borrowInfoApprovalVO.getStatus().intValue() == BorrowInfoStatusEnum.CHECK_OK.getStatus().intValue()){
            //创建新标的
            lendService.createLend(borrowInfoApprovalVO);

        }
    }
}