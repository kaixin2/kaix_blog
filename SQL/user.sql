SELECT *
FROM copy_echo.user;
#
查看表格索引
SHOW INDEX FROM USER;

SELECT *
FROM USER;
#
清除表中所有的数据
DELETE
FROM user
where 1 = 1;

UPDATE USER
SET STATUS = 1
WHERE 1 = 1;

select count(*)
from user;

#去除查询行的限制
SELECT *
FROM USER LIMIT 0,3000;

#删除表
DROP TABLE IF exists `user`;
