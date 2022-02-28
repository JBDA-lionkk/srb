package com.atguigu.srb.core.controller.admin;


import com.alibaba.excel.EasyExcel;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.core.pojo.dto.ExcelDictDto;
import com.atguigu.srb.core.pojo.entity.Dict;
import com.atguigu.srb.core.service.DictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * <p>
 * 数据字典 前端控制器
 * </p>
 *
 * @author 严文豪
 * @since 2021-12-26
 */
@Api(tags = "数据字典管理")
@RestController
@RequestMapping("/admin/core/dict")
@Slf4j
public class AdminDictController {

    @Resource
    private DictService dictService;

    @ApiOperation("Excel数据批量导入")
    @PostMapping("/import")
    public R batchImport(@RequestParam("file") MultipartFile file) {

        try {
            InputStream inputStream = file.getInputStream();
            dictService.importData(inputStream);

            return R.ok().message("数据批量导入成功");
        } catch (IOException e) {
            throw new BusinessException(ResponseEnum.UPLOAD_ERROR, e);
        }

    }

    /**
     * 文件下载（失败了会返回一个有部分数据的Excel）
     * <p>
     * 1. 创建excel对应的实体对象 参照{@link }
     * <p>
     * 2. 设置返回的 参数
     * <p>
     * 3. 直接写，这里注意，finish的时候会自动关闭OutputStream,当然你外面再关闭流问题不大
     */
    @ApiOperation("Excel数据的导出")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("myDict", "UTF-8").replaceAll("\\+", "%20");
        //以附件的形式下载到本地
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ExcelDictDto.class).sheet("数据字典").doWrite(dictService.listDictData());
    }

    /**
     * 树形数据的两种加载方案
     * 方案一:非延迟加载
     * 需要后端返回数据中心包含嵌套数据,并在嵌套数据放在children属性中
     * <p>
     * 方案二: 延迟加载
     * 不需要后端返回数据中包含嵌套数据,并且要定义布尔属性hasChildren,表示当前节点是否包含子节点
     * 如果hasChildren为true ,就表示当前节点包含子数据
     * 如果hasChildren为false, 就表示当前节点不包含子数据
     * 如果当前节点包含子数据 ,那么点击当前节点的时候,就需要通过load方法加载子数据
     */
    @ApiOperation("根据上级id获取节点数据列表")
    @GetMapping("/listByParentId/{parentId}")
    public R listByParentId(
            @ApiParam(value = "上级节点id", required = true)
            @PathVariable Long parentId) {
        List<Dict> dictList = dictService.listByParentId(parentId);
        return R.ok().data("dictList", dictList);
    }
}

