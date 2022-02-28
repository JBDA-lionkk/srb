package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.mapper.LendItemReturnMapper;
import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.pojo.entity.LendItemReturn;
import com.atguigu.srb.core.pojo.entity.LendReturn;
import com.atguigu.srb.core.service.*;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 标的出借回款记录表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class LendItemReturnServiceImpl extends ServiceImpl<LendItemReturnMapper, LendItemReturn> implements LendItemReturnService {

    @Resource
    private LendService lendService;

    @Resource
    private LendReturnService lendReturnService;

    @Resource
    private LendItemService lendItemService;

    @Resource
    private UserBindService userBindService;

    @Override
    public List<LendItemReturn> selectByLendId(Long lendId, HttpServletRequest request) {

        //获取用户id
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);

        //根据投资人id获取他对应的回款计划列表
        return this.list(Wrappers.<LendItemReturn>lambdaQuery()
                .eq(LendItemReturn::getLendId, lendId)
                .eq(LendItemReturn::getInvestUserId, userId)
                .orderByAsc(LendItemReturn::getCurrentPeriod));
    }

    /**
     * 通过还款记录的 id  找到对应的回款计划数据,组装data参数中需要的List<Map>
     *
     * @param lendReturnId 还款记录的 id
     * @return List<Map < String, Object>>
     */
    @Override
    public List<Map<String, Object>> addReturnDetail(Long lendReturnId) {

        LendReturn lendReturn = lendReturnService.getById(lendReturnId);
        Long lendId = lendReturn.getLendId();

        //获取标的
        Lend lend = lendService.getById(lendId);

        List<LendItemReturn> lendItemReturnList = this.selectLendItemReturnList(lendReturnId);
        List<Map<String, Object>> lendItemReturnDetailList = new ArrayList<>();

        //组装数据
        lendItemReturnList.forEach(lendItemReturn -> {

            //获取出借记录表id
            Long lendItemId = lendItemReturn.getLendItemId();
            LendItem lendItem = lendItemService.getById(lendItemId);

            //获取投资人bindCode
            Long investUserId = lendItem.getInvestUserId();
            String bindCode = userBindService.getBindCodeByUserId(investUserId);

            Map<String, Object> map = new HashMap<>();
            map.put("agentProjectCode", lend.getLendNo());//还款项目编号
            map.put("voteBillNo", lendItem.getLendItemNo());//投资单号
            map.put("toBindCode", bindCode);//收款人（投资人）
            map.put("transitAmt", lendItemReturn.getTotal());//还款金额
            map.put("baseAmt", lendItemReturn.getPrincipal());//还款本金
            map.put("benifitAmt", lendItemReturn.getInterest());//还款利息
            map.put("feeAmt", new BigDecimal(0));//商户手续费

            lendItemReturnDetailList.add(map);

        });

        return lendItemReturnDetailList;
    }

    @Override
    public List<LendItemReturn> selectLendItemReturnList(Long lendReturnId) {
        //根据还款id找到回款列表
        return this.list(Wrappers.<LendItemReturn>lambdaQuery().eq(LendItemReturn::getLendReturnId, lendReturnId));
    }
}
