package com.atguigu.srb.core.service.impl;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.MD5;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.mapper.UserInfoMapper;
import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.atguigu.srb.core.pojo.entity.UserBind;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.entity.UserLoginRecord;
import com.atguigu.srb.core.pojo.query.UserInfoQuery;
import com.atguigu.srb.core.pojo.vo.LoginVo;
import com.atguigu.srb.core.pojo.vo.RegisterVo;
import com.atguigu.srb.core.pojo.vo.UserIndexVo;
import com.atguigu.srb.core.pojo.vo.UserInfoVo;
import com.atguigu.srb.core.service.UserAccountService;
import com.atguigu.srb.core.service.UserBindService;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.service.UserLoginRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户基本信息 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private UserAccountService userAccountService;

    @Resource
    private UserLoginRecordService userLoginRecordService;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(RegisterVo registerVo) {
        //判断用户是否已被注册
        int count = this.count(Wrappers.<UserInfo>lambdaQuery().eq(UserInfo::getMobile, registerVo.getMobile()));

        Assert.isTrue(count == 0, ResponseEnum.MOBILE_EXIST_ERROR);

        //插入用户信息记录: user_info
        UserInfo userInfo = new UserInfo();
        userInfo.setUserType(registerVo.getUserType());
        userInfo.setNickName(registerVo.getMobile());
        userInfo.setName(registerVo.getMobile());
        userInfo.setMobile(registerVo.getMobile());
        userInfo.setPassword(MD5.encrypt(registerVo.getPassword()));

        //默认状态
        userInfo.setStatus(UserInfo.STATUS_NORMAL);

        //默认头像
        userInfo.setHeadImg(UserInfo.USER_AVATAR);
        this.save(userInfo);

        //插入用户账户记录: user_account
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userInfo.getId());
        userAccountService.save(userAccount);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfoVo login(LoginVo loginVo, String ip) {

        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();
        Integer userType = loginVo.getUserType();

        //用户是否存在
        UserInfo userInfo = this.getOne(Wrappers.<UserInfo>lambdaQuery()
                .eq(UserInfo::getMobile, mobile)
                .eq(UserInfo::getUserType, userType));

        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);

        //密码是否正确
        Assert.equals(MD5.encrypt(password), userInfo.getPassword(), ResponseEnum.LOGIN_PASSWORD_ERROR);

        //用户是否被禁用
        Assert.equals(userInfo.getStatus(), UserInfo.STATUS_NORMAL, ResponseEnum.LOGIN_LOKED_ERROR);

        //记录登录日志
        UserLoginRecord loginRecord = new UserLoginRecord();
        loginRecord.setUserId(userInfo.getId());
        loginRecord.setIp(ip);
        userLoginRecordService.save(loginRecord);


        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());

        //组装UserInfoVo
        UserInfoVo userInfoVo = new UserInfoVo();
        userInfoVo.setToken(token);
        userInfoVo.setName(userInfo.getName());
        userInfoVo.setMobile(userInfo.getMobile());
        userInfoVo.setUserType(userInfo.getUserType());
        userInfoVo.setNickName(userInfo.getNickName());
        userInfoVo.setHeadImg(userInfo.getHeadImg());

        //返回
        return userInfoVo;
    }

    @Override
    public IPage<UserInfo> listPage(Page<UserInfo> page, UserInfoQuery userInfoQuery) {

        String mobile = userInfoQuery.getMobile();
        Integer userType = userInfoQuery.getUserType();
        Integer status = userInfoQuery.getStatus();

        //组装sql查询语句
        LambdaQueryWrapper<UserInfo> wrapper = Wrappers.<UserInfo>lambdaQuery()
                .like(StringUtils.isNotBlank(userInfoQuery.getMobile()), UserInfo::getMobile, mobile)
                .eq(userInfoQuery.getUserType() != null, UserInfo::getUserType, userType)
                .eq(userInfoQuery.getStatus() != null, UserInfo::getStatus, status);

        //执行查询
        Page<UserInfo> entityPage = this.page(page, wrapper);

        //获取查询结果
        List<UserInfo> records = entityPage.getRecords();

        //判断
        if (CollectionUtils.isEmpty(records)) {
            return entityPage;
        }

        //将数据封装
        List<UserInfo> collect = records.stream().map(record -> {
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(record, userInfo);
            return userInfo;
        }).collect(Collectors.toList());

        return entityPage.setRecords(collect);
    }

    @Override
    public void lock(Long id, Integer status) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setStatus(status);

        this.updateById(userInfo);
    }

    @Override
    public boolean checkMobile(String mobile) {
        int count = this.count(Wrappers.<UserInfo>lambdaQuery().eq(UserInfo::getMobile, mobile));

        return count > 0;
    }

    @Override
    public UserIndexVo getIndexUserInfo(HttpServletRequest request) {
        //获取用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        //获取账户信息
        UserAccount userAccount = userAccountService.getOne(Wrappers.<UserAccount>lambdaQuery().eq(UserAccount::getUserId, userId));

        //获取用户最近登录时间
        UserLoginRecord userLoginRecord = userLoginRecordService.getOne(Wrappers.<UserLoginRecord>lambdaQuery()
                .eq(UserLoginRecord::getUserId, userId)
                .orderByDesc(UserLoginRecord::getId)
                .last("limit 1"));

        //获取用户基本信息
        UserInfo userInfo = this.getById(userId);

        //组装数据
        UserIndexVo userIndexVo = new UserIndexVo();
        BeanUtils.copyProperties(userInfo, userIndexVo);
        userIndexVo.setUserId(userInfo.getId());
        userIndexVo.setAmount(userAccount.getAmount());
        userIndexVo.setFreezeAmount(userAccount.getFreezeAmount());//冻结金额
        userIndexVo.setLastLoginTime(userLoginRecord.getCreateTime());

        return userIndexVo;
    }

    @Override
    public String getMobileByBinCode(String bindCode) {
        UserInfo userInfo = this.getOne(Wrappers.<UserInfo>lambdaQuery().eq(UserInfo::getBindCode, bindCode));

        return userInfo.getMobile();
    }
}
