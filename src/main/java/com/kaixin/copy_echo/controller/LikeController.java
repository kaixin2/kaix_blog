package com.kaixin.copy_echo.controller;

import com.kaixin.copy_echo.entity.Event;
import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.event.EventProducer;
import com.kaixin.copy_echo.service.LikeService;
import com.kaixin.copy_echo.util.CommunityConstant;
import com.kaixin.copy_echo.util.CommunityUtil;
import com.kaixin.copy_echo.util.HostHolder;
import com.kaixin.copy_echo.util.RedisKeyUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.net.ssl.HostnameVerifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Controller
public class LikeController implements CommunityConstant {

    @Resource
    private HostHolder hostHolder;

    @Resource
    private LikeService likeService;

    @Resource
    private EventProducer eventProducer;

    @Resource
    private RedisTemplate redisTemplate;


    /**
     * @Description:
     * @Param: [entityType, entityId, entityUserId,
     * postId: 帖子的id,点赞了哪个帖子,点赞的评论属于哪个帖子,点赞的回复属于哪个帖子
     * @return: java.lang.String
     */
    @ResponseBody
    @PostMapping("/like")
    public String like(int entityType, int entityId, int entityUserId, int postId) {

        User user = hostHolder.getUser();
        //点赞,先对mysql数据库操作
        likeService.like(user.getId(), entityType, entityId, entityUserId);
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        //点赞状态
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        //触发点赞事件(系统通知) - 取消点赞不通知
        if (likeStatus == 1) { //已经点赞
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityType == ENTITY_TYPE_POST) {
            //计算帖子分数键值  这是为什么呢
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }
        return CommunityUtil.getJSONString(0, null, map);
    }


}
