package com.kaixin.copy_echo.controller;

import com.kaixin.copy_echo.entity.Comment;
import com.kaixin.copy_echo.entity.DiscussPost;
import com.kaixin.copy_echo.entity.Page;
import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.service.*;
import com.kaixin.copy_echo.util.CommunityConstant;
import com.kaixin.copy_echo.util.CommunityUtil;
import com.kaixin.copy_echo.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户信息
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @Resource
    private UserService userService;
    @Resource
    private HostHolder hostHolder;
    @Resource
    private LikeService likeService;
    @Resource
    private FollowService followService;
    @Resource
    private DiscussPostService discussPostService;
    @Resource
    private CommentService commentService;


    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;


    /**
     * @Description: 跳转到用户设置界面, 会生成一个上传文件的七牛Token, 上传文件时携带, 文件在前端进行上传
     * @Param: [model]
     * @return: java.lang.String
     */
    @GetMapping("/setting")
    public String getSettingPage(Model model) {

        //生成上传文件的名称
        String fileName = CommunityUtil.generateUUID();
        model.addAttribute("fileName", fileName);

        //设置响应信息 ,七牛标准写法
        StringMap policy = new StringMap();
        //设置要返回的信息
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        //生成要上传到qiniu的凭证(qiniu的规定写法)
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken", uploadToken);

        System.err.println("生成文件名的方法被调用了");

        return "/site/setting";
    }

    /**
     * @Description: 更新图像的路径(将本地的图像路径更新为云服务器上的图像路径) setting.js中有上传方法
     * @Param: [fileName]
     * @return: java.lang.String
     * @Date: 2022-02-08
     */
    @PostMapping("/header/url")
    @ResponseBody
    public String updateHeaderUrl(String fileName) {


        if (StringUtils.isBlank(fileName))
            return CommunityUtil.getJSONString(1, "文件名不能为空");

//        System.err.println("改变头像路径的方法被调用了");
        System.out.println("文件名为: " + fileName);
        //文件在云服务器上的访问路径
        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJSONString(0);
    }

    /**
     * @Description: 修改用户密码
     * @Param: [oldPassword, newPassword, model]
     * @return: java.lang.String
     */
    @PostMapping("/password")
    public String updatePassword(String oldPassword, String newPassword, Model model) {

        //首先验证原密码是否正确
        User user = hostHolder.getUser();
        String md5PldPassword = CommunityUtil.md5(oldPassword + user.getSalt());

        if (!user.getPassword().equals(md5PldPassword)) {
            model.addAttribute("oldPasswordError", "原密码错误");
            return "/site/setting";
        }
        //判断新密码是否合法
        String md5NewPassword = CommunityUtil.md5(newPassword + user.getId());
        if (user.getPassword().equals(md5NewPassword)) {
            model.addAttribute("newPasswrodError", "新密码和原密码相同");
            return "/site/setting";
        }

        //修改用户密码,这里使用重定向,试试转发可以吗
        userService.updatePassword(user.getId(), newPassword);
   //     return "redirect:/index";
        return "redirect:/logout";

    }


    /**
     * @Description: 进入个人主页
     * @Param: []
     * @return: java.lang.String
     */
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) throw new RuntimeException("该用户不存在");

        //用户
        model.addAttribute("user", user);
        //获赞数量
        int userLikeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("userLikeCount", userLikeCount);
        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //当前登录用户是否已关注该用户(进了别人的主页)
        boolean hasFollowed = false;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }

        model.addAttribute("hasFollowed", hasFollowed);
        model.addAttribute("tab", "profile"); //该字段用于指示标签栏高亮
        return "/site/profile";
    }

    @GetMapping("/discuss/{userId}")
    public String getMyDiscussPosts(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) throw new RuntimeException("该用户不存在");

        model.addAttribute("user", user);

        //该用户的帖子总数
        int rows = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("rows", rows);

        page.setLimit(5);
        page.setPath("/user/discuss/" + userId);
        page.setRows(rows);

        //分页查询(按照最新查询)
        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        //封装帖子和该帖子对应的用户信息
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();

                map.put("post", post);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("tab", "mypost"); //用于指示标签栏高亮
        return "/site/my-post";
    }

    @GetMapping("/comment/{userId}")
    public String getMyComments(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) throw new RuntimeException("该用户不存在");
        model.addAttribute("user", user);

        //该用户的评论/回复
        int commentCounts = commentService.findCommentCountByUserId(userId);
        model.addAttribute("commentCounts", commentCounts);

        page.setLimit(5);
        page.setPath("/user/comment/" + userId);
        page.setRows(commentCounts);

        //分页查询
        List<Comment> list = commentService.findCommentByUserId(userId, page.getOffset(), page.getLimit());
        //封装评论和该评论对应的帖子信息
        List<Map<String, Object>> comments = new ArrayList<>();
        if (list != null) {
            for (Comment comment : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                //显示评论/回复对应的文章信息
                if (comment.getEntityType() == ENTITY_TYPE_POST) {
                    //如果是对帖子的评论,则直接查询target_id 即可
                    DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                    map.put("post", post);
                } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
                    //如果是对评论的回复,则先根据该回复的target_id 查询评论的id,再根据该评论的target_id,查询帖子的id
                    Comment targetComment = commentService.findCommentById(comment.getEntityId());
                    DiscussPost post = discussPostService.findDiscussPostById(targetComment.getEntityId());
                    map.put("post", post);
                }
                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);
        model.addAttribute("tab", "myreply");//用于指示标签栏高亮
        return "/site/my-reply";

    }
}
