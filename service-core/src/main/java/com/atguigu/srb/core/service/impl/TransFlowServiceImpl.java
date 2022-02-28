package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.TransFlow;
import com.atguigu.srb.core.mapper.TransFlowMapper;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.service.TransFlowService;
import com.atguigu.srb.core.service.UserInfoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 交易流水表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class TransFlowServiceImpl extends ServiceImpl<TransFlowMapper, TransFlow> implements TransFlowService {

    @Resource
    private UserInfoService userInfoService;


    @Override
    public void saveTransFlow(TransFlowBO transFlowBO) {

        //通过bindCode获取userId
        String bindCode = transFlowBO.getBindCode();
        UserInfo userInfo = userInfoService.getOne(Wrappers.<UserInfo>lambdaQuery().eq(UserInfo::getBindCode, bindCode));

        TransFlow transFlow = new TransFlow();
        transFlow.setTransAmount(transFlowBO.getAmount());//交易金额
        transFlow.setTransType(transFlowBO.getTransTypeEnum().getTransType());//交易类型（1：充值 2：提现 3：投标 4：投资回款 ...）
        transFlow.setTransTypeName(transFlowBO.getTransTypeEnum().getTransTypeName());//交易类型名称
        transFlow.setMemo(transFlowBO.getMemo());//备注
        transFlow.setTransNo(transFlowBO.getAgentBillNo());//流水号
        transFlow.setUserId(userInfo.getId());//用户id
        transFlow.setUserName(userInfo.getName());//用户名称

        this.save(transFlow);
    }

    @Override
    public Boolean isSaveTransFlow(String agentBillNo) {
        int count = this.count(Wrappers.<TransFlow>lambdaQuery().eq(TransFlow::getTransNo, agentBillNo));
        return count > 0;
    }

    @Override
    public List<TransFlow> selectByUserId(HttpServletRequest request) {
        //获取登录id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        return this.list(Wrappers.<TransFlow>lambdaQuery()
                .eq(TransFlow::getUserId, userId)
                .orderByDesc(TransFlow::getId));

    }
}
