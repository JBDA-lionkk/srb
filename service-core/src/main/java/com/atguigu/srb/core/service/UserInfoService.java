package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.query.UserInfoQuery;
import com.atguigu.srb.core.pojo.vo.LoginVo;
import com.atguigu.srb.core.pojo.vo.RegisterVo;
import com.atguigu.srb.core.pojo.vo.UserIndexVo;
import com.atguigu.srb.core.pojo.vo.UserInfoVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 用户基本信息 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface UserInfoService extends IService<UserInfo> {

    void register(RegisterVo registerVo);

    UserInfoVo login(LoginVo loginVo, String ip);

    IPage<UserInfo> listPage(Page<UserInfo> page, UserInfoQuery userInfoQuery);

    void lock(Long id, Integer status);

    boolean checkMobile(String mobile);

    UserIndexVo getIndexUserInfo(HttpServletRequest request);

    String getMobileByBinCode(String bindCode);
}
