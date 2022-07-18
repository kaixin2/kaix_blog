//package com.kaixin.copy_echo;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringRunner;
//
//@SpringBootTest
//@RunWith(SpringRunner.class)
//@ContextConfiguration(classes = CopyEchoApplication.class)
//class CopyEchoApplicationTests {
//
//    @Test
//    void contextLoads() { }
//
//    @Value("${qiniu.key.access}")
//    private String accessKey;
//
//    @Value("${qiniu.key.secret}")
//    private String secretKey;
//
//    @Value("${qiniu.bucket.header.name}")
//    private String headerBucketName;
//
//    @Value("${qiniu.bucket.header.url}")
//    private String headerBucketUrl;
//
//    @Test
//    public void test05(){
//        System.out.println(accessKey);
//        System.out.println(secretKey);
//    }
//
//}
