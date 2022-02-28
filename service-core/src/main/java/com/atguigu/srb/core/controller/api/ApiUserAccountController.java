package com.atguigu.srb.core.controller.api;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.service.UserAccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;

/**
 * <p>
 * 用户账户 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Slf4j
@Api(tags = "会员账户")
@RestController
@RequestMapping("/api/core/userAccount")
public class ApiUserAccountController {

    @Resource
    private UserAccountService userAccountService;

    @ApiOperation("充值")
    @PostMapping("/auth/commitCharge/{chargeAmt}")
    public R commitCharge(
            @ApiParam(value = "充值金额", required = true)
            @PathVariable BigDecimal chargeAmt, HttpServletRequest request) {

        String formStr = userAccountService.commitCharge(chargeAmt, request);

        return R.ok().data("formStr", formStr);
    }

    @ApiOperation("用户充值异步回调")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {

        return userAccountService.notify(request);

    }

    @ApiOperation("查询账户余额")
    @GetMapping("/auth/getAccount")
    public R getAccount(HttpServletRequest request) {
        BigDecimal account = userAccountService.getAccount(request);
        return R.ok().data("account", account);
    }

    @ApiOperation("用户提现")
    @PostMapping("/auth/commitWithdraw/{fetchAmt}")
    public R commitWithdraw(@PathVariable BigDecimal fetchAmt, HttpServletRequest request) {
        String formStr = userAccountService.commitWithdraw(fetchAmt, request);
        return R.ok().data("formStr", formStr);
    }

    @ApiOperation("用户提现异步回调")
    @PostMapping("/notifyWithdraw")
    public String notifyWithdraw(HttpServletRequest request) {

        return userAccountService.notifyWithdraw(request);
    }

}

