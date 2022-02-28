package com.atguigu.srb.core.controller.api;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.TransFlow;
import com.atguigu.srb.core.service.TransFlowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 交易流水表 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Api(tags = "资金记录")
@Slf4j
@RestController
@RequestMapping("/api/core/transFlow")
public class ApiTransFlowController {

    @Resource
    private TransFlowService transFlowService;

    @ApiOperation("获取资金列表")
    @GetMapping("/list")
    public R list(HttpServletRequest request) {

        List<TransFlow> list = transFlowService.selectByUserId(request);
        return R.ok().data("transFlowList", list);
    }
}

