package com.kaixin.copy_echo.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 统一日志记录
 * 作用: 对每个Service包里的方法都作了日志记录,打印某个ip何时调用了哪个方法
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */

@Component
@Aspect
/*
 *  详细介绍: https://blog.csdn.net/fz13768884254/article/details/83538709
 *
 *
 *   @Aspect:作用是把当前类标识为一个切面供容器读取
 *    @Pointcut：Pointcut是植入Advice的触发条件。每个Pointcut的定义包括2部分，
 *        一是表达式，二是方法签名。方法签名必须是 public及void型。
 *        可以将Pointcut中的方法看作是一个被Advice引用的助记符，因为表达式不直观，
 *        因此我们可以通过方法签名的方式为 此表达式命名。
 *        因此Pointcut中的方法只需要方法签名，而不需要在方法体内编写实际代码。
 *    @Around：环绕增强，相当于MethodInterceptor
 *    @AfterReturning：后置增强，相当于AfterReturningAdvice，方法正常退出时执行
 *    @Before：标识一个前置增强方法，相当于BeforeAdvice的功能，相似功能的还有
 *    @AfterThrowing：异常抛出增强，相当于ThrowsAdvice
 *    @After: final增强，不管是抛出异常或者正常退出都会执行
 */
public class ServiceLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    // 定义一个切入点表达式,用来确定哪些类需要代理
    // service包下的任何类的任何方法
    // 详细表达式知识了解 - https://blog.csdn.net/xiao190128/article/details/82181769
    @Pointcut("execution(* com.kaixin.copy_echo.service.*.*(..))")
    public void pointcut() {
    }

    @Before(value = "pointcut()")
    public void before(JoinPoint joinPoint) {
        /*
         *   框架应用,在springmvc中,为了方便能随时可以获取当前请求,使用ThreadLocal,
         *   将当前的Request放入了 RequestContextHolder中
         *   详细知识介绍 : https://blog.csdn.net/weixin_44251024/article/details/86544900
         * */

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getRemoteHost();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        /*
         *   getSignature 获取封装了署名信息的对象,在该对象中可以获取到目标方法名,所属类的Class等信息
         *   详细知识: https://blog.csdn.net/qq_15037231/article/details/80624064
         * */
        //类名
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." +
                joinPoint.getSignature().getName();//方法名
        logger.info(String.format("用户[%s] , 在[%s], 访问了[%s].", ip, time, target));
    }
}
