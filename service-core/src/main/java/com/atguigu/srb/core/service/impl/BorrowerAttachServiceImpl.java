package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.core.mapper.BorrowerAttachMapper;
import com.atguigu.srb.core.pojo.entity.BorrowerAttach;
import com.atguigu.srb.core.pojo.vo.BorrowerAttachVO;
import com.atguigu.srb.core.service.BorrowerAttachService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 借款人上传资源表 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class BorrowerAttachServiceImpl extends ServiceImpl<BorrowerAttachMapper, BorrowerAttach> implements BorrowerAttachService {

    @Override
    public List<BorrowerAttachVO> selectBorrowerAttachVOList(Long borrowerId) {
        //根据borrowerId 获取对应附件信息
        List<BorrowerAttach> borrowerAttaches = this.list(Wrappers.<BorrowerAttach>lambdaQuery().eq(BorrowerAttach::getBorrowerId, borrowerId));

        //封装VO数据
        List<BorrowerAttachVO> attachVOList = borrowerAttaches.stream().map(borrowerAttach -> {
            BorrowerAttachVO borrowerAttachVO = new BorrowerAttachVO();
            BeanUtils.copyProperties(borrowerAttach, borrowerAttachVO);
            return borrowerAttachVO;
        }).collect(Collectors.toList());
        return attachVOList;
    }
}
