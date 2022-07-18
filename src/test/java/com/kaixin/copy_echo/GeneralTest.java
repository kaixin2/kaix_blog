package com.kaixin.copy_echo;

import com.kaixin.copy_echo.util.CommunityUtil;
import com.kaixin.copy_echo.util.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 测试一些杂项
 *
 * @author KaiXin on 2022-01-31
 * @version 1.8
 * @since1.5
 */
@SpringBootTest
public class GeneralTest {

    @Test
    //测试类路径的获取
    public void test01(){
        String filePath = System.getProperty("user.dir");
        System.err.println(filePath);
    }

    @Test
    public void test02(){
        String path = this.getClass().getClassLoader().getResource("sensitive-words.txt").toString();
        System.out.println(path);
    }

    /*
    * 测试敏感词的读取
    * */
    @Test
    public void test03(){
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ){
            String keyWord = null;
            while((keyWord = reader.readLine()) != null)
                System.out.println(keyWord);

        } catch (IOException exception) {
           // logger.error("加载敏感词文件失败" + exception.getMessage());
        }
    }


    /*
    *   测试前缀树的过滤功能
    * */
    @Test
    public void test04(){
        SensitiveFilter sensitiveFilter = new SensitiveFilter();
        sensitiveFilter.init();

        String temp = "我喜欢&赌&博&,更喜欢抽$毒$品的时候看$黄$色";
        String res = sensitiveFilter.filter(temp);
        System.out.println(res);
    }

    /**
     * 测试加密功能
     */
    @Test
    public void test05(){
        String password = "654321";
        String salt = "abc123";
        String s = CommunityUtil.md5("654321abc123");
        System.out.println(s);
    }





}
