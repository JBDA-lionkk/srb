package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.UserIntegral;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户积分记录表 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface UserIntegralService extends IService<UserIntegral> {

    void saveIntegral(Long userId, BorrowerApprovalVO borrowerApprovalVO);
}
