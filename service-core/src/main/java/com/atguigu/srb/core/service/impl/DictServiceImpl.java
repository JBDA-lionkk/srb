package com.atguigu.srb.core.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.srb.core.listener.ExcelDictDtoListener;
import com.atguigu.srb.core.mapper.DictMapper;
import com.atguigu.srb.core.pojo.dto.ExcelDictDto;
import com.atguigu.srb.core.pojo.entity.Dict;
import com.atguigu.srb.core.service.DictService;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 数据字典 服务实现类
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Slf4j
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    @Resource
    private RedisTemplate redisTemplate;


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void importData(InputStream inputStream) {
        EasyExcel.read(inputStream, ExcelDictDto.class, new ExcelDictDtoListener(baseMapper)).sheet().doRead();
        log.info("Excel导入成功");
    }

    @Override
    public List<ExcelDictDto> listDictData() {
        List<Dict> list = this.list(null);
        List<ExcelDictDto> collect = list.stream().map(dict -> {
            ExcelDictDto dictDto = new ExcelDictDto();
            BeanUtils.copyProperties(dict, dictDto);
            return dictDto;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Dict> listByParentId(Long parentId) {

        //不try的话,如果远程redis出现异常 就会导致服务终止 不执行下面的代码,
        try {
            //首先查询redis中是否存在数据
            List<Dict> list = (List<Dict>) redisTemplate.opsForValue().get("srb:core:dictList:" + parentId);

            //如果存在直接返回redis数据
            if (CollectionUtils.isNotEmpty(list)) {
                log.info("从Redis中获取数据");
                return list;
            }
        } catch (Exception e) {
            log.error("redis服务器异常:" + ExceptionUtils.getStackTrace(e));
        }

        //如果不存在则查询数据库
        //1.获取父节点数据
        List<Dict> dictList = this.list(Wrappers.<Dict>lambdaQuery().eq(Dict::getParentId, parentId));

        //2.填充hasChildren字段
        dictList.stream().map(dict -> {
            //3.判断当前节点是否有子节点,找到当前的dict下级有没有子节点
            boolean hasChildren = hasChildren(dict.getId());
            dict.setHasChildren(hasChildren);
            return dict;
        }).collect(Collectors.toList());

        try {
            //将数据存入redis中
            log.info("将数据存入Redis");
            redisTemplate.opsForValue().set("srb:core:dictList:" + parentId, dictList, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //返回数据
        return dictList;
    }

    /**
     * 判断当前id所在的节点下是否有子节点
     *
     * @param id 父id
     * @return boolean
     */
    private boolean hasChildren(Long id) {
        int count = this.count(Wrappers.<Dict>lambdaQuery().eq(Dict::getParentId, id));
        if (count > 0) {
            return true;
        }
        return false;
    }
}
