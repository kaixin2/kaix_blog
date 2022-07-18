package com.kaixin.copy_echo.service;

import com.kaixin.copy_echo.dao.CommentMapper;
import com.kaixin.copy_echo.entity.Comment;
import com.kaixin.copy_echo.util.SensitiveFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import java.util.List;

import static com.kaixin.copy_echo.util.CommunityConstant.ENTITY_TYPE_POST;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Service
public class CommentService {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private SensitiveFilter sensitiveFilter;
    @Resource
    private DiscussPostService discussPostService;

    /**
     * 根据 id 查询评论
     *
     * @param id
     * @return
     */
    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }


    /**
     * 根据评论目标（类别、id）对评论进行分页查询
     *
     * @param entityType 实体类型  评论/点赞/回复
     * @param entityId
     * @param offset     要查询数据的首条在mysql中的位置 从offset处查询limit条数据
     * @param limit
     * @return
     */
    public List<Comment> findCommentByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentByEntity(entityType, entityId, offset, limit);
    }

    /**
     * 查询评论的数量
     *
     * @param entityType
     * @param entityId
     * @return
     */
    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    /**
     * 分页查询某个用户的评论/回复列表
     *
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Comment> findCommentByUserId(int userId, int offset, int limit) {
        return commentMapper.selectCommentByUserId(userId, offset, limit);
    }

    /**
     * 查询某个用户的评论/回复数量
     *
     * @param userId
     * @return
     */
    public int findCommentCountByUserId(int userId) {
        return commentMapper.selectCommentCountByUserId(userId);
    }

    /**
     * 添加评论（需要事务管理）
     *
     * @param comment
     * @return isolation 隔离级别 READ_COMMITTED读取已提交数据(会出现不可重复读和幻读)
     * propagation 传播行为
     * PROPAGATION_REQUIRED 果当前存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务。这是默认值
     * <p>
     * 详细知识网址 ： https://blog.csdn.net/jiangyu1013/article/details/84397366
     * <p>
     * <p>
     * 脏读: 脏读就是指当一个事务正在访问数据，并且对数据进行了修改
     * 而这种修改还没有提交到数据库中，这时，另外一个事务也访问这个数据，然后使用了这个数据。
     * 不可重复读: 是指在一个事务内，多次读同一数据。在这个事务还没有结束时，另外一个事务也访问该同一数据。
     * 那么，在第一个事务中的两次读数据之间，由于第二个事务的修改，那么第一个事务两次读到的的数据可能是不一样的。
     * 这样就发生了在一个事务内两次读到的数据是不一样的，因此称为是不可重复读。
     * 幻读: 是指当事务不是独立执行时发生的一种现象，例如第一个事务对一个表中的数据进行了修改，这种修改涉及到表中的全部数据行。
     * 同时，第二个事务也修改这个表中的数据，这种修改是向表中插入一行新数据。
     * 那么，以后就会发生操作第一个事务的用户发现表中还有没有修改的数据行，就好象发生了幻觉一样。
     * <p>
     * 详细知识网址: https://blog.csdn.net/qq_41776884/article/details/81608777
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        // Html 标签转义
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        // 敏感词过滤
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        // 添加评论
        int rows = commentMapper.insertComment(comment);
        // 更新帖子的评论数量
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }
        return rows;
    }
}
