package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.pojo.vo.InvestVO;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 标的出借记录表 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface LendItemService extends IService<LendItem> {

    String commitInvest(InvestVO investVO, HttpServletRequest request);

    String notify(HttpServletRequest request);

    List<LendItem> selectByLendId(Long lendId, Integer status);

    List<LendItem> selectByLendId(Long lendId);
}
