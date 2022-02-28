package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.BorrowInfo;
import com.atguigu.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowInfoVO;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 借款信息表 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface BorrowInfoService extends IService<BorrowInfo> {

    BigDecimal getBorrowerAmount(Long userId);

    void saveBorrowInfo(BorrowInfo borrowInfo, HttpServletRequest request);

    Integer getStatusByUserId(HttpServletRequest request);

    List<BorrowInfoVO> selectList();

    Map<String, Object> getBorrowInfoDetail(Long id);

    void approval(BorrowInfoApprovalVO borrowInfoApprovalVO);
}
