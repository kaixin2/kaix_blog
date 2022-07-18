DROP TABLE IF EXISTS `user`;
SET
character_SET_CLIENT = utf8mb4;
CREATE TABLE `user`
(
    `id`              INT(11) NOT NULL auto_increment,
    `username`        VARCHAR(50)  DEFAULT NULL,
    `password`        VARCHAR(50)  DEFAULT NULL,
    `salt`            VARCHAR(50)  DEFAULT NULL,
    `email`           VARCHAR(100) DEFAULT NULL,
    `type`            INT(11) DEFAULT NULL COMMENT '0- 普通用户; 2-超级管理; 1-版主',
    `status`          int(11) DEFAULT NULL COMMENT '0-未激活 ; 1-已激活',
    `activation_code` varchar(100) DEFAULT NULL,
    `header_url`      varchar(200) DEFAULT NULL COMMENT '头像的的资源地址,默认使用牛客提供的头像地址',
    `create_time`     timestamp NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    #普通索引,
    加速该字段的查询
        KEY `index_username` (`username`(20)),
    KEY               `index_mail` (`email`(20))
)ENGINE = InnoDB auto_increment = 101 default CHARSET = utf8;

/*   
    1、在存储时间戳数据时，先将本地时区时间转换为UTC时区时间,
      再将UTC时区时间转换为INT格式的毫秒值(使用UNIX_TIMESTAMP函数)，然后存放到数据库中。
    2、在读取时间戳数据时，先将INT格式的毫秒值转换为UTC时区时间(使用FROM_UNIXTIME函数),
       然后再转换为本地时区时间，最后返回给客户端。
 */ 