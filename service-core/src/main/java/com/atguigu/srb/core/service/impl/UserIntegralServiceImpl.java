package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.core.enums.IntegralEnum;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.entity.UserIntegral;
import com.atguigu.srb.core.mapper.UserIntegralMapper;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.service.UserIntegralService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 用户积分记录表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class UserIntegralServiceImpl extends ServiceImpl<UserIntegralMapper, UserIntegral> implements UserIntegralService {

    @Resource
    private UserInfoService userInfoService;


    @Override
    public void saveIntegral(Long userId, BorrowerApprovalVO borrowerApprovalVO) {

        //获取用户对象
        UserInfo userInfo = userInfoService.getById(userId);

        //用户原始积分
        Integer integral = userInfo.getIntegral();

        //借款人基本信息
        UserIntegral userIntegral = new UserIntegral();
        userIntegral.setUserId(userId);
        userIntegral.setIntegral(borrowerApprovalVO.getInfoIntegral());
        userIntegral.setContent("借款人基本信息");
        this.save(userIntegral);

        //查询出来的积分和最新的积分相加
        int currentIntegral = integral + borrowerApprovalVO.getInfoIntegral();

        //身份证积分
        if (borrowerApprovalVO.getIsCarOk()) {
            userIntegral = getUserIntegral(userId, IntegralEnum.BORROWER_IDCARD.getIntegral(), IntegralEnum.BORROWER_IDCARD.getMsg());
            this.save(userIntegral);
            currentIntegral += IntegralEnum.BORROWER_IDCARD.getIntegral();
        }

        //房产信息积分
        if (borrowerApprovalVO.getIsHouseOk()) {
            userIntegral = getUserIntegral(userId, IntegralEnum.BORROWER_HOUSE.getIntegral(), IntegralEnum.BORROWER_HOUSE.getMsg());
            this.save(userIntegral);
            currentIntegral += IntegralEnum.BORROWER_HOUSE.getIntegral();
        }

        //车辆信息积分
        if (borrowerApprovalVO.getIsCarOk()) {
            userIntegral = getUserIntegral(userId, IntegralEnum.BORROWER_CAR.getIntegral(), IntegralEnum.BORROWER_CAR.getMsg());
            this.save(userIntegral);
            currentIntegral += IntegralEnum.BORROWER_CAR.getIntegral();
        }

        //设置用户总积分
        userInfo.setIntegral(currentIntegral);

        //修改审核状态
        userInfo.setBorrowAuthStatus(borrowerApprovalVO.getStatus());

        //更新userInfo
        userInfoService.updateById(userInfo);
    }

    private UserIntegral getUserIntegral(Long userId, Integer integer, String msg) {
        UserIntegral userIntegral = new UserIntegral();
        userIntegral.setUserId(userId);
        userIntegral.setIntegral(integer);
        userIntegral.setContent(msg);
        return userIntegral;
    }
}