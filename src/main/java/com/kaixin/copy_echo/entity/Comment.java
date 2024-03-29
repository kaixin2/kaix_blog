package com.kaixin.copy_echo.entity;

import java.util.Date;

/**
 * 评论的实体类
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
public class Comment {

    private int id;
    //发布评论的作者
    private int userId;
    //评论目标的类型(是一篇帖子或是评论)

    private int entityType;

    private int entityId;
    //指明队哪个用户进行评论(用户,id)
    private int targetId;
    //内容
    private String content;
    //状态 : 0 正常,1 禁用
    private int status;
    //发布时间
    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public int getEntityType() {
        return entityType;
    }

    public void setEntityType(int entityType) {
        this.entityType = entityType;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", userId=" + userId +
                ", entityId=" + entityId +
                ", targetId=" + targetId +
                ", content='" + content + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
