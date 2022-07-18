package com.kaixin.copy_echo.controller.interceptor;

import com.kaixin.copy_echo.entity.LoginTicket;
import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.service.UserService;
import com.kaixin.copy_echo.util.CookieUtil;
import com.kaixin.copy_echo.util.HostHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;
    @Resource
    private HostHolder hostHolder;

    /**
     * 在每次访问Controller执行之前被调用
     * 检查凭证状态，若凭证有效则在本次请求中持有该用户信息
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从 cookie 中获取凭证
        /*
            如果之前登录过并且保存了相关信息在Cookie中时,就从这里取出相关信息
            "ticket就是cookie中键值,取出对应的值 再到我们的redis中进行查询"
        * */
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket != null) {
            // 查询凭证             从redis缓存中查询
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证状态（是否有效）以及是否过期
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {

                // 根据凭证从mysql中查询用户,不需要输入密码
                User user = userService.findUserById(loginTicket.getUserId());

                // 在本次请求中持有用户信息
                hostHolder.setUser(user);
                // 构建用户认证的结果，并存入 SecurityContext, 以便于 Spring Security 进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()) //权限级别
                );
                /*
                 *  获取存储在其中的信息方法
                 *  详细地址: https://blog.csdn.net/dnc8371/article/details/106811295
                 * */
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    /**
     * 在模板引擎之前被调用
     * 将用户信息存入 modelAndView, 便于模板引擎调用
     *
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            /*
             * 详细使用方法: https://blog.csdn.net/qq_40774175/article/details/87932825
             *  通过这里把用户信息回传给前端页面,页面就会变成有用户登录的状态
             * */
            modelAndView.addObject("loginUser", user);
        }
    }

    /**
     * 调用时间：DispatcherServlet进行视图的渲染之后
     * 清理本次请求持有的用户信息
     * 详细介绍 ： https://www.cnblogs.com/yanze/p/11057102.html
     * https://zhuanlan.zhihu.com/p/165386758
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        //每一次访问controller都会先加入,再清空
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
