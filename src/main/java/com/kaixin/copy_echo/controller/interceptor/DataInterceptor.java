package com.kaixin.copy_echo.controller.interceptor;

import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.service.DataService;
import com.kaixin.copy_echo.util.HostHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author KaiXin
 * @version 1.8
 * @since 1.5
 * 对所有请求ip进行统计
 */
@Component
public class DataInterceptor implements HandlerInterceptor {

    @Resource
    private DataService dataService;
    @Resource
    private HostHolder hostHolder;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //从request中获得请求的ip号
        String ip = request.getRemoteHost();
        dataService.recordUV(ip);


        User user = hostHolder.getUser();
        if (user != null) dataService.recordDAU(user.getId());
        return true;
    }
}
