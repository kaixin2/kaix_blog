DROP TABLE IF EXISTS COMMENT;
SET
CHARACTER_SET_CLIENT = UTF8;
CREATE TABLE `comment`
(
    `id`          int(11) not null auto_increment,
    `user_id`     int(11) default null,
    `entity_type` int(11) default null,
    `entity_id`   int(11) default null,
    `target_id`   int(11) default null,
    `content`     text,
    `status`      int(11) default null,
    `create_time` timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    key           `index_user_id` (`user_id`),
    key           `index_entity_id` (`entity_id`)
)engine = innodb default charset = utf8;

/*
	user_Id 哪个用户发布了该条评论
	entity_type : 实体类型 如果是对帖子的评论该评论就是评论d,如果是对评论的评论,该评论就是回复
    entity_id : 实体的id 如果是对帖子的评论,就存储帖子的id, 如果是对评论的回复,就存储评论的id
    target_id :  回复的评论或者帖子的作者的id
    content: 评论/回复的内容 
    status : 评论/回复状态  0(正常) 1(被禁用)
    create_time : 评论/回复发布时间
*/