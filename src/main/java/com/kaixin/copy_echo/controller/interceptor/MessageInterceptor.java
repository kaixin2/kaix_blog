package com.kaixin.copy_echo.controller.interceptor;

import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.service.MessageService;
import com.kaixin.copy_echo.util.HostHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5
 *
 */
@Component
public class MessageInterceptor implements HandlerInterceptor {

    @Resource
    private HostHolder hostHolder;

    @Resource
    private MessageService messageService;

    /**
     * @Description: Controller之后模板之前被调用, 获取未读私信/系统通知的数量
     * @Param: [request, response, handler]
     * @return: boolean
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int noticeUnreadCount = messageService.findNoticeUnReadCount(user.getId(), null);
            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
        }
    }
}
