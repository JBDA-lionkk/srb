package com.atguigu.srb.core.controller.admin;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.query.UserInfoQuery;
import com.atguigu.srb.core.service.UserInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 用户基本信息 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@RestController
@RequestMapping("/admin/core/userInfo")
@Api(tags = "会员管理")
@Slf4j
public class AdminUserInfoController {

    @Resource
    private UserInfoService userInfoService;

    @ApiOperation("获取会员分页列表")
    @GetMapping("/listPage/{page}/{limit}")
    public R listPage(@ApiParam(value = "当前页码", required = true) @PathVariable Long page,
                      @ApiParam(value = "每页记录数", required = true) @PathVariable Long limit,
                      @ApiParam(value = "查询对象", required = false) UserInfoQuery userInfoQuery) {

        Page<UserInfo> pageParam = new Page<UserInfo>(page, limit);
        IPage<UserInfo> pageModel = userInfoService.listPage(pageParam, userInfoQuery);
        return R.ok().data("pageModel", pageModel);
    }

    @ApiOperation("锁定和解锁")
    @PutMapping("/lock/{id}/{status}")
    public R lock(@ApiParam(value = "用户id", required = true) @PathVariable Long id,
                  @ApiParam(value = "状态(0:锁定 1:正常)", required = true) @PathVariable Integer status) {
        userInfoService.lock(id, status);
        return R.ok().message(status == 1 ? "解锁成功" : "锁定成功");
    }

}