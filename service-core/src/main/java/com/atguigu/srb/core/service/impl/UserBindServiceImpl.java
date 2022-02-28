package com.atguigu.srb.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.enums.UserBindEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.pojo.entity.UserBind;
import com.atguigu.srb.core.mapper.UserBindMapper;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.vo.UserBindVo;
import com.atguigu.srb.core.service.UserBindService;
import com.atguigu.srb.core.service.UserInfoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户绑定表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
@Slf4j
public class UserBindServiceImpl extends ServiceImpl<UserBindMapper, UserBind> implements UserBindService {

    @Resource
    private UserInfoService userInfoService;

    @Override
    public String bind(UserBindVo userBindVo, HttpServletRequest request) {
        //从header中获取token ,并对token进行校验.确保用户登录,并从token中获取userId
        String token = request.getHeader("token");
        //获取用户id
        Long userId = JwtUtils.getUserId(token);


        //不同的user_id,相同的身份证,如果存在,不允许
        UserBind userBind = this.getOne(Wrappers.<UserBind>lambdaQuery()
                .eq(UserBind::getIdCard, userBindVo.getIdCard())
                .ne(UserBind::getUserId, userId));

        Assert.isNull(userBind, ResponseEnum.USER_BIND_IDCARD_EXIST_ERROR);

        //用户是否曾经填写过绑定表单
        userBind = this.getOne(Wrappers.<UserBind>lambdaQuery().eq(UserBind::getUserId, userId));
        if (userBind == null) {
            //创建用户绑定记录
            userBind = new UserBind();
            BeanUtils.copyProperties(userBindVo, userBind);
            userBind.setUserId(userId);
            userBind.setStatus(UserBindEnum.NO_BIND.getStatus());
            this.save(userBind);
        }

        //相同的user_id,如果存在,那么就取出数据做更新
        BeanUtils.copyProperties(userBindVo, userBind);
        this.updateById(userBind);

        //组装自动提交表单的参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("agentId", HfbConst.AGENT_ID);//给商户分配的唯一标识
        map.put("agentUserId", userId);//商户的个人会员ID
        map.put("idCard", userBindVo.getIdCard());//身份证号
        map.put("personalName", userBindVo.getName());//真实姓名
        map.put("bankType", userBindVo.getBankType());//银行卡类型
        map.put("bankNo", userBindVo.getBankNo());//银行卡
        map.put("mobile", userBindVo.getMobile());//银行卡预留手机
        map.put("returnUrl", HfbConst.USERBIND_RETURN_URL);//绑定完成后同步返回商户的完整地址
        map.put("notifyUrl", HfbConst.USERBIND_NOTIFY_URL);//绑定完成后异步通知商户的完整地址
        map.put("timeStamp", RequestHelper.getTimestamp());//时间戳。从1970-01-01 00:00:00算起的毫秒数
        map.put("sign", RequestHelper.getSign(map));//验签

        //根据userId对账户绑定,生成一个动态表单的字符串
        return FormHelper.buildForm(HfbConst.USERBIND_URL, map);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String hfbNotify(HttpServletRequest request) {

        //汇付宝向尚融宝发起回调请求时携带的参数
        Map<String, Object> paramMap = RequestHelper.switchMap(request.getParameterMap());
        log.info("账户绑定异步回调接收的参数如下:" + JSON.toJSONString(paramMap));

        //检验签名
        if (!RequestHelper.isSignEquals(paramMap)) {
            log.error("用户账号绑定异步回调签名验证错误:" + JSON.toJSONString(paramMap));
            return "fail";
        }

        log.info("验签成功!开始账户绑定");
        this.notify(paramMap);

        return "success";
    }

    @Override
    public String getBindCodeByUserId(Long userId) {
        UserBind userBind = this.getOne(Wrappers.<UserBind>lambdaQuery().eq(UserBind::getUserId, userId));
        return userBind.getBindCode();
    }

    private void notify(Map<String, Object> paramMap) {

        //获取绑定账户协议号
        String bindCode = (String) paramMap.get("bindCode");

        //用户id
        String userId = (String) paramMap.get("agentUserId");

        //根据用户id获取用户绑定信息
        UserBind userBind = this.getOne(Wrappers.<UserBind>lambdaQuery().eq(UserBind::getUserId, userId));

        //更新用户绑定表
        this.update(Wrappers.<UserBind>lambdaUpdate()
                .eq(UserBind::getUserId, userId)
                .set(UserBind::getBindCode, bindCode)
                .set(UserBind::getStatus, UserBindEnum.BIND_OK.getStatus()));

        //更新用户表
        userInfoService.update(Wrappers.<UserInfo>lambdaUpdate()
                .eq(UserInfo::getId, userId)
                .set(UserInfo::getBindCode, bindCode)
                .set(UserInfo::getIdCard, userBind.getIdCard())
                .set(UserInfo::getName, userBind.getName())
                .set(UserInfo::getBindStatus, UserBindEnum.BIND_OK.getStatus())
        );
    }
}
