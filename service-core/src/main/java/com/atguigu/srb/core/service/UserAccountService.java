package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

/**
 * <p>
 * 用户账户 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface UserAccountService extends IService<UserAccount> {

    String commitCharge(BigDecimal chargeAmt, HttpServletRequest request);

    String notify(HttpServletRequest request);

    BigDecimal getAccount(HttpServletRequest request);

    String commitWithdraw(BigDecimal fetchAmt, HttpServletRequest request);

    String notifyWithdraw(HttpServletRequest request);

}
