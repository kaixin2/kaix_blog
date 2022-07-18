package com.kaixin.copy_echo.util_test;

import com.google.gson.Gson;
import com.kaixin.copy_echo.CopyEchoApplication;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 测试七牛云存储的使用
 *
 * 	    华东	Zone.zone0()
 * 	    华北	Zone.zone1()
 * 	    华南	Zone.zone2()
 * 	    北美	Zone.zoneNa0()
 *
 * @author KaiXin on 2021-09-19
 * @version 1.8
 * @since1.5
 */
//@SpringBootTest
//@RunWith(SpringRunner.class)
//@ContextConfiguration(classes = CopyEchoApplication.class)
public class TestQiNiu {

  //  @Value("${qiniu.key.access}")
    String accessKey = "ZTsFgzFiXd0J0Dvct2AFT9HOPVi6Oncchhw2s3OO";
  //  @Value("${qiniu.key.secret}")
    String secretKey = "slJUGusn9RYZeaon449OAFNNZ2mcCQpDkOXbOXSK";
 //   @Value("${qiniu.bucket.header.name}")
    String storeBucket = "echoblog-test";
 //   @Value("${qiniu.bucket.header.url}")
    String headerUrl = "qny.tv114.xyz";

    // 获取上传凭证
    public String getUpToken(){
        Auth auth = Auth.create(accessKey,secretKey);
        String uploadToken = auth.uploadToken(storeBucket);
        System.out.println(uploadToken);
        return uploadToken;
    }

        /**
        * 文件上传
        * @param zone
        *    华东	Zone.zone0()
        *    华北	Zone.zone1()
        *    华南	Zone.zone2()
        *    北美	Zone.zoneNa0()
        * @param upToken 上传凭证
        * @param localFilePath 需要上传的文件本地路径
        * @return
        **/
    public DefaultPutRet fileUploadToQiNiu(Zone zone, String upToken, String localFilePath){
        Configuration config = new Configuration(zone); //得到传送地区的操作配置
        UploadManager uploadManager = new UploadManager(config);//通过配置得到操作上传的对象
        String key = null;//传入null时,使用文件的哈希值作为文件名

        try {
            Response response = uploadManager.put(localFilePath, key, upToken); //上传文件
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(),DefaultPutRet.class);

            System.out.println(putRet.key);
            System.out.println(putRet.hash);
            return putRet;
        } catch (QiniuException e) {
            Response response = e.response;
            try {
                System.out.println("上传失败" + response.bodyString());
            } catch (QiniuException qiniuException) {
                qiniuException.printStackTrace();
            }
        }
        return null;
    }



    //公共空间文件访问路径
    /**
    * @Description:
    * @Param: [fileName 文件在七牛中的名字, domainOfBucket 存储空间对应的domain，可以在控制台中查看]
    * @return: java.lang.String
    * @Date: 2021-09-19
    */
    public String  publicFile(String fileName,String domainOfBucket ){
        String encodeFile = null;
        try {
            encodeFile = URLEncoder.encode(fileName,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String finalUrl = String.format("%s/%s",domainOfBucket,encodeFile);
        System.out.println(finalUrl);
        return finalUrl;
    }


    public Auth getAuth(){ return Auth.create(accessKey,secretKey); }
    //私有空间文件获取URL  expireInSeconds：私有空间文件的访问链接超时时间，单位（秒）
    public String privateFile(Auth auth,String fileName,String domainOfBucket,long expireInSeconds){
        String encodeFile = null;
        try {
            encodeFile = URLEncoder.encode(fileName,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String publicUrl = String.format("%s/%s",domainOfBucket,encodeFile);
        String finalUrl = auth.privateDownloadUrl(publicUrl, expireInSeconds);
        return finalUrl;
    }



    //获取文件详细信息
    String fileKey = "10.png";
    public FileInfo getFileInfo(Zone zone,Auth auth,String bucket){
        Configuration config = new Configuration(zone);
        BucketManager bucketManager = new BucketManager(auth,config);

        try {
            FileInfo fileInfo = bucketManager.stat(storeBucket, fileKey); //获取文件对象

            System.out.println(fileInfo.hash);
            System.out.println(fileInfo.fsize);
            System.out.println(fileInfo.mimeType);
            System.out.println(fileInfo.putTime);    //获取文件详细信息
            bucketManager.changeMime(storeBucket,fileKey,"png"); //修改文件类型

        } catch (QiniuException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    //测试读取文件信息
    public void test04(){
        getFileInfo(Zone.zone2(),getAuth(),storeBucket);
    }

    //测试路径获取情况
    @Test
    public void test02(){
        System.out.println(System.getProperty("user.dir") + path);
        //echoblog-test
    }

    @Test
    //测试公共文件路径解析情况
    public void test03(){
        publicFile(fileKey,headerUrl);
    }

    //测试上传文件
    String path = "D:\\IDEAProjectFiles\\copyProject\\copy_echo_blog\\src\\main\\resources\\static\\img\\404.png";
    @Test
    public void test01(){
        System.getProperty("user.dir");
        fileUploadToQiNiu(Zone.zone2(),getUpToken(), path);
    }
}

