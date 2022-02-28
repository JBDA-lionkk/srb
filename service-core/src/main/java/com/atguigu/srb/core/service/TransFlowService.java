package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.TransFlow;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * <p>
 * 交易流水表 服务类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface TransFlowService extends IService<TransFlow> {

    void saveTransFlow(TransFlowBO transFlowBO);

    Boolean isSaveTransFlow(String agentBillNo);

    List<TransFlow> selectByUserId(HttpServletRequest request);
}
