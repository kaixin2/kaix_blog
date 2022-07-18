package com.kaixin.copy_echo.quartz;

import com.kaixin.copy_echo.entity.DiscussPost;
import com.kaixin.copy_echo.service.DiscussPostService;
import com.kaixin.copy_echo.service.ElasticsearchService;
import com.kaixin.copy_echo.service.LikeService;
import com.kaixin.copy_echo.util.CommunityConstant;
import com.kaixin.copy_echo.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 帖子分数计算刷新
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DiscussPostService discussPostService;

    @Resource
    private LikeService likeService;

    @Resource
    private ElasticsearchService elasticsearchService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-01-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化Epoch级元失败");
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        String redisKey = RedisKeyUtil.getPostScoreKey();
        //获得key对应的集合对象
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数 " + operations.size());
        while (operations.size() > 0) this.refresh((Integer) operations.pop());
        logger.info("[任务结束] 帖子分数刷新完毕 ");

    }

    /**
     * @Description: 刷新帖子分数
     * @Param: []
     * @return: void
     * @Date: 2022-02-12
     */
    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post == null) {
            logger.error("该帖子不存在: id = " + postId);
            return;
        }
        //是否加精
        boolean wonderful = post.getStatus() == 1;
        //评论数量
        int commentCount = post.getCommentCount();
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        //计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        //分数 = 权重 + 发帖距离天数
        double score = Math.log10(Math.max(w, 1) + (
                post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 34)
        );
        //更新帖子分数
        discussPostService.updateScore(postId, score);
        //同步搜索数据
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);

    }
}
