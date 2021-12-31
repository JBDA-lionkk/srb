package com.atguigu.srb.core.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.atguigu.srb.core.mapper.DictMapper;
import com.atguigu.srb.core.pojo.dto.ExcelDictDto;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor
public class ExcelDictDtoListener extends AnalysisEventListener<ExcelDictDto> {


    DictMapper dictMapper;

    //数据列表
    private List<ExcelDictDto> list = new ArrayList<>();
    private static final Integer BATCH_COUNT = 5;


    //通过构造函数注入service
    public ExcelDictDtoListener(DictMapper dictMapper) {
        this.dictMapper = dictMapper;
    }

    @Override
    public void invoke(ExcelDictDto data, AnalysisContext analysisContext) {
        log.info("解析到一条记录:{}", data);

        //1.先往集合里添加数据
        list.add(data);

        //2.当数据大于等于5条时候 调用save方法进行保存,并清除list里的所有数据
        if (list.size() >= BATCH_COUNT) {
            //调用mapper层的save方法
            saveData();
            list.clear();
        }
    }

    private void saveData() {

        log.info("{} 条数据被存储导数据库....", list.size());

        //调用mapper层 save方法: save list对象
        dictMapper.insertBatchs(list);
        log.info("{} 条数据被存储导数据库成功!", list.size());

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        //当最后剩余的数据不足BATCH_COUNT时候,我们最终一次性存储剩余数据
        saveData();
        log.info("所有数据解析完成!");
    }
}
