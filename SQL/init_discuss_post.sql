DROP TABLE IF EXISTS `discuss_post`;
SET
character_SET_CLIENT = utf8mb4;
CREATE TABLE `discuss_post`
(
    `id`            int(11) NOT NULL auto_increment,
    `user_id`       int(11) NOT NULL,
    `title`         varchar(100) NOT NULL,
    `content`       text,
    `type`          int(11) DEFAULT NULL COMMENT '0-普通;1-置顶',
    `status`        int(11) DEFAULT NULL COMMENT '0-正常; 1-精华;2-拉黑',

    # NULL 表示 如果插入时间为NULL则使用当前时间自动填充
        `create_time` timestamp NULL DEFAULT NULL,
    `comment_count` int(11) DEFAULT NULL,
    `score`         double default null,
    primary key (`id`),
    key             `index_user_Id` (`user_id`)
)engine = innodb default charset = utf8;	
