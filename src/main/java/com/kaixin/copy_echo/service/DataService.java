package com.kaixin.copy_echo.service;

import com.kaixin.copy_echo.util.RedisKeyUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 网站数据统计 (UV/DAU)
 * <p>
 * PV（Page View）访问量, 即页面浏览量或点击量
 * IP（Internet Protocol）独立IP数，是指1天内多少个独立的IP浏览了页面，即统计不同的IP浏览用户数量。
 * UV（Unique Visitor）独立访客
 * DAU(Daily Active User)，日活跃用户数量。
 * 一般用于反映网站、互联网应用等运营情况。结合MAU（月活跃用户数量）一起使用
 * 用来衡量服务的用户粘性以及服务的衰退周期。 [1]  。
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Service
public class DataService {

    @Resource
    private RedisTemplate redisTemplate;
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 将指定的 IP 计入当天的 UV
     *
     * @param ip
     */
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    /**
     * 统计指定日期范围内的 UV
     *
     * @param start
     * @param end
     * @return
     */
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null)
            throw new IllegalArgumentException("参数不能为空");

        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE, 1);//加一天
        }

        String redisKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        //将后面这个键值数组里所有键对应的ip集合合并在一起
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }


    /**
     * 将指定的 IP 计入当天的 DAU
     *
     * @param userId
     */
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }


    /**
     * 统计指定日期范围内的 DAU
     *
     * @param start
     * @param end
     * @return
     */
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) throw new IllegalArgumentException("参数不能为空");

        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);//加一天
        }

        /*
         *   这里好像是对键进行了位运算统计,不是应该对值进行统计吗
         *   RedisCallback通过它们可以在同一条连接下执行多个Redis命令。
         * */
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                //参数依次为: 进行的位运算操作,结果存储的地方,进行运算操作的对象
                //提供键,对对应的value作操作
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });

    }
}
