package com.kaixin.copy_echo.config;

import com.kaixin.copy_echo.controller.interceptor.DataInterceptor;
import com.kaixin.copy_echo.controller.interceptor.LoginTicketInterceptor;
import com.kaixin.copy_echo.controller.interceptor.MessageInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * 拦截器配置类
 * <p>
 * 1.将自定义的拦截器加入容器中,同时指定要拦截的对象
 * 2.路径映射,将某一较长的路径映射为较短的路径
 * 3.还可以通过实现WebMvcConfigurer中的各种方法实现各种配置,如跨域时的拦截,通过重写跨域配置方法
 * 指定可以进行跨域访问的对象以及时间,请求类型等
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginTicketInterceptor loginTicketInterceptor;
    @Resource
    private MessageInterceptor messageInterceptor;
    @Resource
    private DataInterceptor dataInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //对所有的静态资源都不做拦截,使用的参数是正则表达式拼成的资源路径
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/editor_md/**", "/editor-md-upload/**");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/editor_md/**", "/editor-md-upload/**");
        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/editor_md/**", "/editor-md-upload/**");
    }

    //配置虚拟路径映射访问
    //斜杠和反斜杠的区别 ： https://blog.csdn.net/weixin_43593330/article/details/89854744
    /*
     *  可以隐藏真正的路径名
     * */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = System.getProperty("user.dir") + "\\src\\main\\resources\\static\\editor-md-upload\\";
        registry.addResourceHandler("/editor-md-upload/**").addResourceLocations("file:" + path);
    }


}
