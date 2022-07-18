//package com.kaixin.copy_echo.dao_test;
//
//import com.kaixin.copy_echo.CopyEchoApplication;
//import com.kaixin.copy_echo.dao.CommentMapper;
//import com.kaixin.copy_echo.dao.DiscussPostMapper;
//import com.kaixin.copy_echo.dao.MessageMapper;
//import com.kaixin.copy_echo.dao.UserMapper;
//import com.kaixin.copy_echo.entity.Comment;
//import com.kaixin.copy_echo.entity.Message;
//import com.kaixin.copy_echo.entity.User;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.Resource;
//
///**
// * 测试mysql数据库的连接
// *
// * @author KaiXin on 2022-01-30
// * @version 1.8
// * @since1.5
// */
//@SpringBootTest
//@RunWith(SpringRunner.class)
//@ContextConfiguration(classes = CopyEchoApplication.class)
//public class MysqlTest {
//
//    @Autowired
//    CommentMapper commentMapper;
//
//    @Autowired
//    DiscussPostMapper discussPostMapper;
//
//    @Test
//    public void test_comment_ask(){
//        Comment comment = commentMapper.selectCommentById(1);
//        System.out.println(comment);
//    }
//
//
//    @Test
//    public void test_discussPost_ask(){
//        int cnt = discussPostMapper.selectDiscussPostRows(101);
//        System.out.println(cnt);
//    }
//
//    @Autowired
//    UserMapper userMapper;
//    @Test
//    public void test_user_ask(){
//        User user = userMapper.selectById(101);
//        System.out.println(user);
//    }
//
//    @Resource
//    MessageMapper messageMapper;
//    @Test
//    public void test_message_ask(){
//        int cnt = messageMapper.selectConversationCount(102);
//        System.out.println(cnt);
//    }
//}
