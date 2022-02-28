package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.LendItemReturn;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 标的出借回款记录表 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface LendItemReturnService extends IService<LendItemReturn> {

    List<LendItemReturn> selectByLendId(Long lendId, HttpServletRequest request);

    List<Map<String, Object>> addReturnDetail(Long lendReturnId);

    /**
     * 根据还款记录的id查询对应的回款记录
     *
     * @param lendReturnId 还款记录的id
     * @return List<LendItemReturn>
     */
    List<LendItemReturn> selectLendItemReturnList(Long lendReturnId);

}
