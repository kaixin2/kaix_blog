package com.kaixin.copy_echo.dao.elasticsearch;

import com.kaixin.copy_echo.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 在elasticSearch中查询帖子的实体类
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {

}
