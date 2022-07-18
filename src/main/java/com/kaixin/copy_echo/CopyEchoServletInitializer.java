package com.kaixin.copy_echo;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 封装入口
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
public class CopyEchoServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CopyEchoApplication.class);
    }

}
