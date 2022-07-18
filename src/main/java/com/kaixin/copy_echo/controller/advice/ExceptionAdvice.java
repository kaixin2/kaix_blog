package com.kaixin.copy_echo.controller.advice;

import com.kaixin.copy_echo.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author KaiXin
 * @version 1.8
 * @since1.5 处理服务器异常
 * 使用这个 Controller ，可以实现三个方面的功能：
 * 全局异常处理
 * 全局数据绑定
 * 全局数据预处理
 * <p>
 * 如果去掉这个异常处理器: 如果出现异常了,会怎么样？
 * 程序会打印异常信息,不会停止
 * <p>
 * 加上异常处理器,异常信息回以日志形式打印出来
 */

/*
 *   指定拦截所有带Controller注解的类,貌似和拦截器的功能可以整合在一起
 *   详细介绍: https://blog.csdn.net/qq_36829919/article/details/101210250
 * */
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    private Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    /*
     *   拦截所有的异常进行处理
     * */
    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常: " + e.getMessage());

        //打印异常的堆栈信息,一般都是从这里找到错误的地方
        for (StackTraceElement element : e.getStackTrace()) logger.error(element.toString());

        //区分异步请求和同步请求
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            //异步请求
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常"));
        } else {
            //普通请求                     可返回站点的根路径。
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }


}
