package com.kaixin.copy_echo.controller;

import com.kaixin.copy_echo.entity.Comment;
import com.kaixin.copy_echo.entity.DiscussPost;
import com.kaixin.copy_echo.entity.Event;
import com.kaixin.copy_echo.event.EventProducer;
import com.kaixin.copy_echo.service.CommentService;
import com.kaixin.copy_echo.service.DiscussPostService;
import com.kaixin.copy_echo.util.CommunityConstant;
import com.kaixin.copy_echo.util.HostHolder;
import com.kaixin.copy_echo.util.RedisKeyUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 评论/回复
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Resource
    private HostHolder hostHolder;

    @Resource
    private CommentService commentService;

    @Resource
    private DiscussPostService discussPostService;

    @Resource
    private EventProducer eventProducer;

    @Resource
    private RedisTemplate redisTemplate;


    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable int discussPostId, Comment comment) {

        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment); //加入数据库中

        /*
         *  触发评论事件,如果一个人评论了另一个的帖子或者评论,应当给被评论的人发送通知
         * */

        Event event = new Event()
                .setTopic(TOPIC_COMMNET)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId()) //被评论的id
                .setData("postId", discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            //触发发帖事件.通过消息队列将其存入,ElasticSearch 服务器
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);

            //计算帖子键值
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }
        /*
         * 做增、删、改的时候最好用重定向，因为如果不用重定向，每次刷新页面就相当于再请求一次，
         * 就可能会做额外的操作，导致数据不对。
         * */
        return "redirect:/discuss/detail/" + discussPostId;
    }

}
