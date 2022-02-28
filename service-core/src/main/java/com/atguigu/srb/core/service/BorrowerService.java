package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.Borrower;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.pojo.vo.BorrowerVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 借款人 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface BorrowerService extends IService<Borrower> {

    void saveBorrowerVoByUserId(BorrowerVo borrowerVo, HttpServletRequest request);

    Integer getStatusByUserId(HttpServletRequest request);

    IPage<Borrower> listPage(Page<Borrower> pageParam, String keyword);

    BorrowerDetailVO getBorrowerDetailVOById(Long id);

    void approval(BorrowerApprovalVO borrowerApprovalVO);
}
