package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.enums.BorrowerStatusEnum;
import com.atguigu.srb.core.pojo.entity.Borrower;
import com.atguigu.srb.core.mapper.BorrowerMapper;
import com.atguigu.srb.core.pojo.entity.BorrowerAttach;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.entity.UserIntegral;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerAttachVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.pojo.vo.BorrowerVo;
import com.atguigu.srb.core.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 借款人 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Service
public class BorrowerServiceImpl extends ServiceImpl<BorrowerMapper, Borrower> implements BorrowerService {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private BorrowerAttachService borrowerAttachService;

    @Resource
    private DictService dictService;

    @Resource
    private UserIntegralService userIntegralService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveBorrowerVoByUserId(BorrowerVo borrowerVo, HttpServletRequest request) {
        //从token中获取userId
        String token = request.getHeader("token");

        Long userId = JwtUtils.getUserId(token);

        //通过userId获取用户信息
        UserInfo userInfo = userInfoService.getById(userId);

        //封装数据
        Borrower borrower = new Borrower();
        BeanUtils.copyProperties(borrowerVo, borrower);
        borrower.setUserId(userId);
        borrower.setName(userInfo.getName());
        borrower.setIdCard(userInfo.getIdCard());
        borrower.setMobile(userInfo.getMobile());
        borrower.setStatus(BorrowerStatusEnum.AUTH_RUN.getStatus());//认证中

        //保存借款人信息
        this.save(borrower);


        List<BorrowerAttach> borrowerAttachList = borrowerVo.getBorrowerAttachList();

        //封装附件数据
        List<BorrowerAttach> attachList = borrowerAttachList.stream().map(borrowerAttach -> {
            BorrowerAttach attach = new BorrowerAttach();
            BeanUtils.copyProperties(borrowerAttach, attach);
            attach.setBorrowerId(borrower.getId());
            return attach;
        }).collect(Collectors.toList());

        //保存附件
        borrowerAttachService.saveBatch(attachList);

        //更新userInfo中借款人状态
        userInfoService.update(Wrappers.<UserInfo>lambdaUpdate()
                .eq(UserInfo::getId, userId)
                .set(UserInfo::getBorrowAuthStatus, BorrowerStatusEnum.AUTH_RUN.getStatus()));
    }

    @Override
    public Integer getStatusByUserId(HttpServletRequest request) {
        //请求头里获取登录人的信息
        String token = request.getHeader("token");
        Long userId = JwtUtils.getUserId(token);
        List<Borrower> borrowers = this.list(Wrappers.<Borrower>lambdaQuery().eq(Borrower::getUserId, userId));
        if (borrowers.size() == 0) {
            return BorrowerStatusEnum.NO_AUTH.getStatus();
        }

        Borrower borrower = borrowers.get(0);

        return borrower.getStatus();

    }

    @Override
    public IPage<Borrower> listPage(Page<Borrower> pageParam, String keyword) {

        //判断参数是否为空
        if (StringUtils.isBlank(keyword)) {
            return baseMapper.selectPage(pageParam, null);
        }

        //组装查询sql
        LambdaQueryWrapper<Borrower> wrapper = Wrappers.<Borrower>lambdaQuery()
                .like(Borrower::getName, keyword)
                .or().like(Borrower::getIdCard, keyword)
                .or().like(Borrower::getMobile, keyword)
                .orderByDesc(Borrower::getId);

        //执行分页
        IPage page = this.page(pageParam, wrapper);

        //获取查询结果
        List<Borrower> records = page.getRecords();

        if (CollectionUtils.isEmpty(records)) {
            return page;
        }

        //封装数据
        List<Borrower> list = records.stream().map(param -> {
            Borrower borrower = new Borrower();
            BeanUtils.copyProperties(param, borrower);
            return borrower;
        }).collect(Collectors.toList());


        //将数据设置并返回
        page.setRecords(list);
        return page;


    }

    @Override
    public BorrowerDetailVO getBorrowerDetailVOById(Long id) {
        //通过用户id获取借款信息
        Borrower borrower = this.getById(id);
        //封装查询的数据
        BorrowerDetailVO borrowerDetailVO = new BorrowerDetailVO();
        BeanUtils.copyProperties(borrower, borrowerDetailVO);

        //婚否
        borrowerDetailVO.setMarry(borrower.getMarry() ? "是" : "否");

        //性别
        borrowerDetailVO.setSex(borrower.getSex() == 1 ? "男" : "女");

        //下拉列表
        borrowerDetailVO.setEducation(dictService.getNameByParentDictCodeAndValue("education", borrower.getEducation()));//学历
        borrowerDetailVO.setIndustry(dictService.getNameByParentDictCodeAndValue("industry", borrower.getIndustry()));//行业
        borrowerDetailVO.setIncome(dictService.getNameByParentDictCodeAndValue("income", borrower.getIncome()));//月收入
        borrowerDetailVO.setReturnSource(dictService.getNameByParentDictCodeAndValue("returnSource", borrower.getReturnSource()));//还款来源
        borrowerDetailVO.setContactsRelation(dictService.getNameByParentDictCodeAndValue("relation", borrower.getContactsRelation()));//联系人关系

        //状态
        String status = BorrowerStatusEnum.getMsgByStatus(borrower.getStatus());
        borrowerDetailVO.setStatus(status);

        //附件列表
        //根据borrowerId获取附件信息
        List<BorrowerAttachVO> borrowerAttaches = borrowerAttachService.selectBorrowerAttachVOList(borrower.getId());

        borrowerDetailVO.setBorrowerAttachVOList(borrowerAttaches);


        return borrowerDetailVO;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approval(BorrowerApprovalVO borrowerApprovalVO) {
        //获取借款额度申请id
        Long borrowerId = borrowerApprovalVO.getBorrowerId();

        //获取借款额度申请对象
        Borrower borrower = this.getById(borrowerId);

        //设置审核状态
        borrower.setStatus(borrowerApprovalVO.getStatus());
        this.updateById(borrower);

        //获取用户id
        Long userId = borrower.getUserId();

        //计算积分
        userIntegralService.saveIntegral(userId, borrowerApprovalVO);

    }
}
