package com.kaixin.copy_echo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CopyEchoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CopyEchoApplication.class, args);
    }

    /*
     *  解决ElasticSearch 和 Redis 底层的 Netty 启动冲突问题
     * */
    @PostConstruct
    public void init() {
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }

}
