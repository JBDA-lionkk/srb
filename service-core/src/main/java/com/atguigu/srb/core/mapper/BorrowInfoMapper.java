package com.atguigu.srb.core.mapper;

import com.atguigu.srb.core.pojo.entity.BorrowInfo;
import com.atguigu.srb.core.pojo.vo.BorrowInfoVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 借款信息表 Mapper 接口
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface BorrowInfoMapper extends BaseMapper<BorrowInfo> {

    List<BorrowInfoVO> selectBorrowInfoList();
}
