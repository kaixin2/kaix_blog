package com.kaixin.copy_echo.controller;

import com.kaixin.copy_echo.entity.*;
import com.kaixin.copy_echo.event.EventProducer;
import com.kaixin.copy_echo.service.CommentService;
import com.kaixin.copy_echo.service.DiscussPostService;
import com.kaixin.copy_echo.service.LikeService;
import com.kaixin.copy_echo.service.UserService;
import com.kaixin.copy_echo.util.CommunityConstant;
import com.kaixin.copy_echo.util.CommunityUtil;
import com.kaixin.copy_echo.util.HostHolder;
import com.kaixin.copy_echo.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import javax.jws.Oneway;
import java.io.File;
import java.util.*;

/**
 * 帖子
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Resource
    private DiscussPostService discussPostService;

    @Resource
    private HostHolder hostHolder;

    @Resource
    private UserService userService;

    @Resource
    private CommentService commentService;

    @Resource
    private LikeService likeService;

    @Resource
    private EventProducer eventProducer;

    @Resource
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    //项目名(访问路径)
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${community.path.editormdUploadPath}")
    private String editormdUploadPath;

    /**
     * @Description: 进入发帖页面
     * @Param: []
     * @return: java.lang.String
     */
    @GetMapping("/publish")
    public String getPublish() {
        return "/site/discuss-publish";
    }

    //在帖子中上传文件
    @GetMapping("/uploadMdPic")
    @ResponseBody
    public String uploadMdPic(@RequestParam(value = "editormd-image-file", required = false) MultipartFile file) {
        String url = null;


        try {
            //获取上传文件的名称
            String trueFileName = file.getOriginalFilename();
            String suffix = trueFileName.substring(trueFileName.lastIndexOf("."));
            String fileName = CommunityUtil.generateUUID() + suffix;

            //图片存储路径
            File dest = new File(editormdUploadPath + "/" + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            //保存图片到存储路径
            file.transferTo(dest);
            url = domain + contextPath + "/editor-md-upload" + fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return CommunityUtil.getEditorMdJSONString(0, "上传失败", url);
        }

        return CommunityUtil.getEditorMdJSONString(1, "上传成功", url);
    }


    /**
     * 添加帖子（发帖）
     *
     * @param title
     * @param content
     * @return
     */
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "您还未登录");
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());

        discussPostService.addDiscussPost(discussPost);//帖子存入Mysql数据库

        // 触发发帖事件，通过消息队列将其存入 Elasticsearch 服务器
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH) //设置该事件存放的主题
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());
        eventProducer.fireEvent(event);  //生产者帖子相关信息放入消息队列中

        // 计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());//帖子存入redis缓存中

        return CommunityUtil.getJSONString(0, "发布成功");
    }


    /**
     * @Description: 进入帖子详情页
     * @Param: [discussPostId, model, page]
     * @return: java.lang.String
     * <p>
     * 1.没写PathVariable注解 就访问失败了
     */
    @GetMapping("/detail/{discussPostId}")
    private String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(discussPostId);

        //存入数据库中时,内容进行了转义,取出来要进行反转义
        String content = HtmlUtils.htmlUnescape(discussPost.getContent());
        discussPost.setContent(content);
        model.addAttribute("post", discussPost);

        //作者
        User user = userService.findUserById(discussPost.getUserId());
        model.addAttribute("user", user);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        //当前登录用户的点赞状态
        int likeStatus = hostHolder.getUser() == null ? 0 :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(discussPost.getCommentCount());

        //帖子的评论列表
        List<Comment> commentList = commentService.findCommentByEntity(
                ENTITY_TYPE_POST, discussPost.getId(), page.getOffset(), page.getLimit()
        );

        //封装评论及其相关信息
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> commentVo = new HashMap<>();

                commentVo.put("comment", comment);
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(
                        hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);
                //存储每个评论对应的回复(不做分页)

                List<Comment> replyList = commentService.findCommentByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //封装对评论的评论和评论的作者信息
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        replyVo.put("reply", reply); //回复
                        replyVo.put("user", userService.findUserById(reply.getUserId()));

                        User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                        replyVo.put("target", target); //该回复的目标用户
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        likeStatus = hostHolder.getUser() == null ? 0 : likeService.findEntityLikeStatus(
                                hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);//当前登录用户的点赞状态
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);
                //每个评论对应的回复数量
                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }

    /**
     * @Description: 置顶帖子
     * @Param: [id, type]
     * @return: java.lang.String
     */
    @PostMapping("/top")
    @ResponseBody
    public String updateTop(int id, int type) {//帖子id,type表示修改状态
        discussPostService.updateType(id, type);//具体的置顶在这里执行

        //触发发帖事件,通过消息队列将其存入ElasticSearch
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)//事件主题
                .setUserId(hostHolder.getUser().getId())//发送该事件的用户的id
                .setEntityType(ENTITY_TYPE_POST)//具体事件
                .setEntityId(id);//帖子id

        eventProducer.fireEvent(event);
        //发送成功
        return CommunityUtil.getJSONString(0);
    }

    /**
     * @Description: 加精帖子
     * @Param: [id]
     * @return: java.lang.String
     */
    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id) {
        discussPostService.updateStatus(id, 1);

        //触发发帖事件,通过消息队列将其存入ElasticSearch服务器
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        //用分数作键?
        redisTemplate.opsForSet().add(redisKey, id);
        return CommunityUtil.getJSONString(0);
    }

    @ResponseBody
    @PostMapping("/delete")
    public String setDelete(int id) {
        discussPostService.updateStatus(id, 2);
        //触发删帖
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        System.out.println("执行删帖操作");
        return CommunityUtil.getJSONString(0);
    }
}
