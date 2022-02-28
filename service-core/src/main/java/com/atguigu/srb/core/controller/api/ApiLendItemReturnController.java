package com.atguigu.srb.core.controller.api;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.LendItemReturn;
import com.atguigu.srb.core.service.LendItemReturnService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 标的出借回款记录表 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Api(tags = "回款计划")
@RestController
@RequestMapping("/api/core/lendItemReturn")
@Slf4j
public class ApiLendItemReturnController {

    @Resource
    private LendItemReturnService lendItemReturnService;

    @ApiOperation("获取列表")
    @GetMapping("/list/{lendId}")
    public R list(
            @ApiParam(value = "标的id", required = true)
            @PathVariable Long lendId, HttpServletRequest request) {

        List<LendItemReturn> list = lendItemReturnService.selectByLendId(lendId, request);

        return R.ok().data("list", list);
    }
}