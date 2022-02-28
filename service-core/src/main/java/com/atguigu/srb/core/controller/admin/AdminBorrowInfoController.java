package com.atguigu.srb.core.controller.admin;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowInfoVO;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.atguigu.srb.core.service.BorrowInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 借款信息表 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Slf4j
@RestController
@RequestMapping("/admin/core/borrowInfo")
@Api(tags = "借款管理")
public class AdminBorrowInfoController {

    @Resource
    private BorrowInfoService borrowInfoService;

    @ApiOperation("借款信息列表")
    @GetMapping("/list")
    public R list() {
        List<BorrowInfoVO> borrowInfoList = borrowInfoService.selectList();
        return R.ok().data("borrowInfoList", borrowInfoList);
    }

    @ApiOperation("获取借款信息详情")
    @GetMapping("/show/{id}")
    public R show(
            @ApiParam(value = "借款信息id", required = true)
            @PathVariable Long id) {
        Map<String, Object> borrowInfoDetail = borrowInfoService.getBorrowInfoDetail(id);
        return R.ok().data("borrowInfoDetail", borrowInfoDetail);
    }

    @ApiOperation("审批借款信息")
    @PostMapping("/approval")
    public R approval(@RequestBody BorrowInfoApprovalVO borrowInfoApprovalVO) {

        borrowInfoService.approval(borrowInfoApprovalVO);
        return R.ok().message("审批完成");
    }

}