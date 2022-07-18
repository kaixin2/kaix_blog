package com.kaixin.copy_echo.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.kaixin.copy_echo.dao.DiscussPostMapper;
import com.kaixin.copy_echo.entity.DiscussPost;
import com.kaixin.copy_echo.util.SensitiveFilter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);
    @Resource
    private DiscussPostMapper discussPostMapper;
    @Resource
    private SensitiveFilter sensitiveFilter;
    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // 热帖列表的本地缓存
    // key - offset(每页的起始索引):limit(每页显示多少条数据)
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数的本地缓存
    // key - userId(其实就是0,表示查询的是所有用户. 对特定用户的查询不启用缓存）
    private LoadingCache<Integer, Integer> postRowsCache;

    /**
     * 初始化本地缓存 Caffeine
     *
     * @PostConstruct该注解被用来修饰一个非静态的void（）方法。被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行，
     * 并且只会被服务器执行一次。PostConstruct 在构造函数之后执行，init（）方法之前执行。
     * 通常我们会是在Spring框架中使用到@PostConstruct注解 该注解的方法在整个Bean初始化中的执行顺序：
     * Constructor(构造方法) -> @Autowired(依赖注入) -> @PostConstruct(注释的方法)
     */
    @PostConstruct
    public void init() {
        // 初始化热帖列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)//设置存储容量大小
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS) //指定本地缓存内容过期时间
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    // 如果缓存Caffeine中没有数据，告诉缓存如何去数据库中查数据，再装到缓存中
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误");
                        }

                        int offset = Integer.parseInt(params[0]);
                        int limit = Integer.parseInt(params[1]);
                        // 此处可以再访问二级缓存 Redis
                        logger.debug("load post list from DB");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                //设置创建后的过期时间
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB");
                        //初始化项目时,从数据库中,执行一次查询拿到帖子总数,并存在本地缓存中,key过期时会执行一次
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    /**
     * 分页查询讨论帖信息
     *
     * @param userId    当传入的 userId = 0 时查找所有用户的帖子
     *                  当传入的 userId != 0 时，查找该指定用户的帖子
     * @param offset    每页的起始索引
     * @param limit     每页显示多少条数据
     * @param orderMode 排行模式(若传入 1, 则按照热度来排序)
     * @return
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        // 查询本地缓存(当查询的是所有用户的帖子并且按照热度排序时)
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + ":" + limit);
        }
        // 否则查询数据库
        logger.debug("load post list from DB");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    /**
     * 查询讨论贴的个数
     *
     * @param userId 当传入的 userId = 0 时计算所有用户的帖子总数
     *               当传入的 userId ！= 0 时计算该指定用户的帖子总数
     * @return
     */
    public int findDiscussPostRows(int userId) {
        // 查询本地缓存(当查询的是所有用户的帖子总数时)
        if (userId == 0) {
            return postRowsCache.get(userId);
        }
        // 查询数据库
        logger.debug("load post rows from DB");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    /**
     * 添加帖子
     *
     * @param discussPost
     * @return
     */
    public int addDiscussPost(DiscussPost discussPost) {
        if (discussPost == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 转义 HTML 标记，防止在 HTML 标签中注入攻击语句
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        // 过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    /**
     * 根据 id 查询帖子
     *
     * @param id
     * @return
     */
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    /**
     * 修改帖子的评论数量
     *
     * @param id           帖子 id
     * @param commentCount
     * @return
     */
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    /**
     * 修改帖子类型：0-普通; 1-置顶;
     *
     * @param id
     * @param type
     * @return
     */
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    /**
     * 修改帖子状态：0-正常; 1-精华; 2-拉黑;
     *
     * @param id
     * @param status
     * @return
     */
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    /**
     * 修改帖子分数
     *
     * @param id
     * @param score
     * @return
     */
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
