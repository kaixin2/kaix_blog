/*
	私信表
*/

DROP TABLE IF EXISTS `message`;
SET
character_SET_CLIENT = utf8mb4;
create table `message`
(
    `id`              int(11) NOT NULL auto_increment,
    `from_id`         int(11) DEFAULT NULL COMMENT '发送者编号',
    `to_id`           int(11) DEFAULT NULL comment '接受者编号',
    `conversation_id` varchar(45) NOT NULL comment '标识两个用户之间的对话,其实可以通过from_id 和to_id拼接得到',
    `content`         text comment '消息内容',
    `status`          int(11) DEFAULT NULL COMMENT '0-未读; 1-已读; 2-删除',
    `create_time`     timestamp NULL DEFAULT NULL,
    primary key (`id`),
    KEY               `index_from_id` (`from_id`),
    KEY               `index_to_id` (`to_id`),
    KEY               `index_conversation_id`(`conversation_id`)
)ENGINE = Innodb DEFAULT CHARSET = UTF8;

