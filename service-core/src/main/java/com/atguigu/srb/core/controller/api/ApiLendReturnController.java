package com.atguigu.srb.core.controller.api;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.LendReturn;
import com.atguigu.srb.core.service.LendReturnService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 还款记录表 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Api(tags = "还款计划")
@RestController
@RequestMapping("/api/core/lendReturn")
@Slf4j
public class ApiLendReturnController {

    @Resource
    private LendReturnService lendReturnService;

    @ApiOperation("获取列表")
    @GetMapping("/list/{lendId}")
    public R list(
            @ApiParam(value = "标的id", required = true)
            @PathVariable Long lendId) {

        List<LendReturn> list = lendReturnService.selectByLendId(lendId);

        return R.ok().data("list", list);

    }

    @ApiOperation("用户还款")
    @PostMapping("/auth/commitReturn/{lendReturnId}")
    public R commitReturn(
            @ApiParam(value = "还款计划id", required = true)
            @PathVariable Long lendReturnId, HttpServletRequest request) {
        String formStr = lendReturnService.commitReturn(lendReturnId, request);
        return R.ok().data("formStr", formStr);
    }

    @ApiOperation("还款异步回调")
    @PostMapping("/notifyUrl")
    public String notify(HttpServletRequest request) {
        return lendReturnService.notify(request);
    }
}
