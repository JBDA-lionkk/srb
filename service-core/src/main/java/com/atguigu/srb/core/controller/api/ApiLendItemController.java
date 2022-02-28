package com.atguigu.srb.core.controller.api;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.pojo.vo.InvestVO;
import com.atguigu.srb.core.service.LendItemService;
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
 * 标的出借记录表 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@RestController
@Slf4j
@RequestMapping("/api/core/lendItem")
@Api(tags = "标的的投资")
public class ApiLendItemController {

    @Resource
    private LendItemService lendItemService;

    @ApiOperation("会员投资提交数据")
    @PostMapping("/auth/commitInvest")
    public R commitInvest(@RequestBody InvestVO investVO, HttpServletRequest request) {

        String formStr = lendItemService.commitInvest(investVO, request);
        return R.ok().data("formStr", formStr);
    }

    @ApiOperation("用户投资异步回调")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {

        return lendItemService.notify(request);

    }

    @ApiOperation("获取列表")
    @GetMapping("/list/{lendId}")
    public R list(
            @ApiParam(value = "标的id", required = true)
            @PathVariable Long lendId) {

        List<LendItem> list = lendItemService.selectByLendId(lendId);

        return R.ok().data("list", list);

    }
}

