package com.kaixin.copy_echo.event;

import com.alibaba.fastjson.JSONObject;
import com.kaixin.copy_echo.entity.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * 事件的生产者
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Component
public class EventProducer {

    @Resource
    private KafkaTemplate kafkaTemplate;

    /**
     * @Description: 处理事件
     * @Param: [event]
     * @return: void
     * @Date: 2022-02-09
     */
    public void fireEvent(Event event) {
        //将事件发布到指定的主题

        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }


}
