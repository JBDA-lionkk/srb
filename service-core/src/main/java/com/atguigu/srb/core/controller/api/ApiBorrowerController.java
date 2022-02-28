package com.atguigu.srb.core.controller.api;


import com.atguigu.common.result.R;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.pojo.vo.BorrowerVo;
import com.atguigu.srb.core.service.BorrowerService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 借款人 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Api(tags = "借款人")
@Slf4j
@RestController
@RequestMapping("/api/core/borrower")
public class ApiBorrowerController {

    @Resource
    private BorrowerService borrowerService;

    @ApiOperation("保存借款人信息")
    @PostMapping("/auth/save")
    public R save(@RequestBody BorrowerVo borrowerVo, HttpServletRequest request) {
        borrowerService.saveBorrowerVoByUserId(borrowerVo, request);

        return R.ok().message("信息提交成功");
    }

    @ApiOperation("查询借款人状态")
    @GetMapping("/auth/getBorrowerStatus")
    public R getBorrowerStatus(HttpServletRequest request) {

        Integer status = borrowerService.getStatusByUserId(request);
        return R.ok().data("borrowerStatus", status);
    }

}

