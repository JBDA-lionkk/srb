package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.UserBind;
import com.atguigu.srb.core.pojo.vo.UserBindVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户绑定表 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface UserBindService extends IService<UserBind> {

    String bind(UserBindVo userBindVo, HttpServletRequest request);

    String hfbNotify(HttpServletRequest request);

    String getBindCodeByUserId(Long userId);
}
