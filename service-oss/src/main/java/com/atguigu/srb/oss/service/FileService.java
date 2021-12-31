package com.atguigu.srb.oss.service;

import java.io.InputStream;

public interface FileService {

    /**
     * 文件上传至阿里云
     *
     * @param inputStream 以数据流上传
     * @param module      路径
     * @param fileName    上传名称
     * @return String
     */
    String upload(InputStream inputStream, String module, String fileName);

    void removeFile(String url);
}
