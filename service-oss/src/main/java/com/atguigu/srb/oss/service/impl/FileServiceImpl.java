package com.atguigu.srb.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.atguigu.srb.oss.service.FileService;
import com.atguigu.srb.oss.util.OssProperties;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String upload(InputStream inputStream, String module, String fileName) {

        // 1.创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);

        //2.判断Bucket仓库是否存在
        if (!ossClient.doesBucketExist(OssProperties.BUCKET_NAME)) {
            //不存在就创建
            ossClient.createBucket(OssProperties.BUCKET_NAME);
            ossClient.setBucketAcl(OssProperties.BUCKET_NAME, CannedAccessControlList.PublicRead);

        }


        //文件
        //文件目录 avatar/2021/12/31/uuid.jpg
        //构建日期路径
        String dateTime = new DateTime().toString("/yyyy/MM/dd/");

        //构建文件名
        fileName = UUID.randomUUID() + fileName.substring(fileName.lastIndexOf("."));

        //路径
        String path = module + dateTime + fileName;

        ossClient.putObject(OssProperties.BUCKET_NAME, path, inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        //文件的url地址
        //https://srb-file-ywh-200921.oss-cn-chengdu.aliyuncs.com/avatar/19136e521aead9fcfda2b7c62ee5da0e_1.jpg
        return "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/" + path;
    }


    @Override
    public void removeFile(String url) {


        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);

        //https://srb-file-ywh-200921.oss-cn-chengdu.aliyuncs.com/test/2021/12/31/3d6b982b-d5a7-47dd-a61d-79b0af6552ae.jpg
        //test/2021/12/31/3d6b982b-d5a7-47dd-a61d-79b0af6552ae.jpg
        String host = "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/";

        String objectName = url.substring(host.length());

        // 删除文件或目录。如果要删除目录，目录必须为空。
        ossClient.deleteObject(OssProperties.BUCKET_NAME, objectName);

        // 关闭OSSClient。
        ossClient.shutdown();
    }
}
