package com.kaixin.copy_echo.config;

import com.kaixin.copy_echo.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5 一个Job可以对应多个Trigger，但一个Trigger只能对应一个Job
 */
@Configuration
public class QuartzConfig {

    /**
     * 刷新帖子分数任务
     *
     * @return
     */
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();

        factoryBean.setJobClass(PostScoreRefreshJob.class); //设置实际任务类,会执行该类重写的execute方法
        factoryBean.setName("postScoreRefreshJob");  //设置任务名
        factoryBean.setGroup("communityJobGroup");  //设置任务组名
        //指定job的持久性，也就是说，即使没有触发器指向它，它是否应该保留在存储中。
        factoryBean.setDurability(true);
        //设置该job的恢复标志，即，如果遇到“恢复”或“故障转移”情况，是否应该重新执行该job。
        factoryBean.setRequestsRecovery(true);

        return factoryBean;
    }

    /**
     * 刷新帖子分数触发器
     *
     * @return
     */
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();

        factoryBean.setJobDetail(postScoreRefreshJobDetail); //触发器绑定Job
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5); // 5分钟刷新一次,每5分钟执行一次PostScoreRefreshJob的方法
        factoryBean.setJobDataMap(new JobDataMap());//可携带参数去执行Job

        return factoryBean;
    }

}
