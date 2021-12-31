package com.atguigu.srb.core.mapper;

import com.atguigu.srb.core.pojo.dto.ExcelDictDto;
import com.atguigu.srb.core.pojo.entity.Dict;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 * 数据字典 Mapper 接口
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
public interface DictMapper extends BaseMapper<Dict> {

    void insertBatchs(List<ExcelDictDto> list);
}
