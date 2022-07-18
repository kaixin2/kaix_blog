package com.kaixin.copy_echo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        /*
         *   存入redis中的数据为什么要序列化
         *   详细知识: https://blog.csdn.net/weixin_43968372/article/details/106442009
         *
         * */

        //设置key的序列化的方式
        template.setKeySerializer(RedisSerializer.string());
        //设置value的序列化的方式
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的序列化的方式
        template.setHashKeySerializer(RedisSerializer.string());
        ///设置hash的value的序列化的方式
        template.setValueSerializer(RedisSerializer.json());
        return template;
    }
}
