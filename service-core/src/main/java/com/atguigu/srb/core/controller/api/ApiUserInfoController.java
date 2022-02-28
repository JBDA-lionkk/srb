package com.atguigu.srb.core.controller.api;


import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.RegexValidateUtils;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.pojo.vo.LoginVo;
import com.atguigu.srb.core.pojo.vo.RegisterVo;
import com.atguigu.srb.core.pojo.vo.UserIndexVo;
import com.atguigu.srb.core.pojo.vo.UserInfoVo;
import com.atguigu.srb.core.service.UserInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户基本信息 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@RestController
@RequestMapping("/api/core/userInfo")
@Api(tags = "会员接口")
@Slf4j
public class ApiUserInfoController {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private UserInfoService userInfoService;

    @ApiOperation("会员注册")
    @PostMapping("/register")
    public R register(@RequestBody RegisterVo registerVo) {

        String mobile = registerVo.getMobile();
        String password = registerVo.getPassword();
        String code = registerVo.getCode();

        //传入的数据进行校验
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.notEmpty(password, ResponseEnum.PASSWORD_NULL_ERROR);
        Assert.notEmpty(code, ResponseEnum.CODE_NULL_ERROR);
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);

        //校验验证码是否正确
        String codeGen = (String) redisTemplate.opsForValue().get("srb:sms:code:" + mobile);
        Assert.equals(code, codeGen, ResponseEnum.CODE_ERROR);

        //注册
        userInfoService.register(registerVo);

        return R.ok().message("注册成功");

    }

    @ApiOperation("会员登录")
    @PostMapping("/login")
    public R login(@RequestBody LoginVo loginVo, HttpServletRequest request) {
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();

        //非空判断
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.notEmpty(password, ResponseEnum.PASSWORD_NULL_ERROR);

        //获取登录客户端地址
        String ip = request.getRemoteAddr();

        UserInfoVo userInfoVo = userInfoService.login(loginVo, ip);

        return R.ok().data("userInfo", userInfoVo);

    }

    @ApiOperation("校验令牌")
    @GetMapping("/checkToken")
    public R checkToken(HttpServletRequest request) {

        //从请求头中获取token
        String token = request.getHeader("token");

        boolean result = JwtUtils.checkToken(token);

        if (result) {
            return R.ok();
        }
        return R.setResult(ResponseEnum.LOGIN_AUTH_ERROR);
    }


    @ApiOperation("校验手机号是否被注册")
    @GetMapping("/checkMobile/{mobile}")
    public boolean checkMobile(@PathVariable String mobile) {
        return userInfoService.checkMobile(mobile);
    }


    @ApiOperation("获取个人空间用户信息")
    @GetMapping("/auth/getIndexUserInfo")
    public R getIndexUserInfo(HttpServletRequest request) {

        UserIndexVo userIndexVo = userInfoService.getIndexUserInfo(request);

        return R.ok().data("userIndexVo", userIndexVo);
    }
}

