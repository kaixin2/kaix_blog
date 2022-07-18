package com.kaixin.copy_echo.controller;

import com.google.code.kaptcha.Producer;
import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.service.UserService;
import com.kaixin.copy_echo.util.CommunityConstant;
import com.kaixin.copy_echo.util.CommunityUtil;
import com.kaixin.copy_echo.util.HostHolder;
import com.kaixin.copy_echo.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 登录,登出,注册功能
 * @version 1.8
 * @since1.5
 */
@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    /*
    *  修复bug: 已经携带ticket的情况下访问登录或注册页面,上部分显示用户信息
    *           下部分仍然是注册或登录页面
               正常情况:已经有用户信息的情况下,必须退出登录才能使用注册或登录功能
       措施:跳转之前检查hostHolder是否持有user的信息

    * */

    @Resource
    private HostHolder hostHolder;

    @Resource
    private UserService userService;
    @Resource
    private Producer kaptchaProducer;
    @Resource
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * @Description: 进入注册页面, 因为springboot默认会访问resources目录下的
     * META-INF/resources > resources > static > public 的静态资源,需要设置其他默认访问目录需要设置
     * 另外html页面放在templates目录中,默认访问该目录下的页面,且不能直接通过url地址方法,需要
     * 通过controller层跳转
     * @Param: []
     * @return: java.lang.String
     */
    @GetMapping("/register")
    public String getRegisterPage() {

        //检查user是否为空,不为空,直接跳首页
        User user = hostHolder.getUser();
        if (user != null) return "forward:/index";

        return "site/register";
    }

    /**
     * @Description: 进入登录页面
     * @Param: []
     * @return: java.lang.String
     */
    @GetMapping("/login")
    public String getLoginPage() {

        //检查user是否为空,不为空,直接跳首页
        User user = hostHolder.getUser();
        if (user != null) return "forward:/index";
        return "site/login";
    }

    @GetMapping("/resetPwd")
    public String getResetPwdPage() {
        return "site/reset-pwd";
    }


    /*
     *  修正bug: 即使用户填写一个格式正确,其实不存在的邮箱,注册信息依然会被插入数据库中
     *          正确姿势:在客户提交正确的激活码之后再进行数据库插入
     * */

    @PostMapping("/register")
    public String register(Model model, User user){
        Map<String,Object> map = userService.register(user);

        /* 在上面的register方法中,作了各种合法检查,如果注册信息没有任何问题,将返回一个空的map,并且
            跳转到等待结果页面
        *
            如果map不为空,说明提交的用户信息有误,错误提示信息会封装在返回的map中,之后显示在提示框中
            然后重新跳转到注册页面
        */
        if(map == null || map.isEmpty()) {
            model.addAttribute("msg","注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活！");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }
        else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }
    }

    /**
    * @Description: 为用户激活账号,如果已经激活则在跳转操作结果页面后跳转首页
    * @Param: [model, userId, code]
    * @return: java.lang.String
    */
    @GetMapping("/activation/{userId}/{code}")
    public String activation (Model model, @PathVariable("userId")int userId,@PathVariable("code")String code){

        //在方法中,从数据库中拿到之前给用户的激活码,与提交的激活码进行比对,返回激活结果
        int result = userService.activation(userId,code);

        if(result == ACTIVATION_SUCCESS){
            model.addAttribute("msg","激活成功,您的账号已经可以正常使用!");
            model.addAttribute("target","/login");
        }
        else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg","无效的操作,您的账号已经激活过!");
            model.addAttribute("target","/index");
        }
        //先跳转到操作结果页面,再跳转到target中指定的页面
        return "/site/operate-result";
    }

//    @PostMapping("/register")
//    public String register(Model model, User user) {
//        int result = userService.activationRedis(user, user.getActivationCode());
//        System.out.println("打印一下注册结果" + result);
//        //注册成功,跳转到操作结果页面
//        if (result == ACTIVATION_SUCCESS) {
//            //执行用户数据插入数据库操作
//            userService.register(user);
//            model.addAttribute("msg", "您的账号已注册成功!");
//            model.addAttribute("target", "/index");
//            return "/site/operate-result";
//        } else {
//            model.addAttribute("activationMsg", "验证码错误,请检查激活码是否输入正确");
//        }
//
//        return "/site/register";
//    }

    /**
     * @Description: 点击发送邮件验证码后, 先校检其他数据, 再决定是否发送验证码, 同时如果有错误信息, 回传前端注册界面
     * @Param: [model, user]
     * @return: java.lang.String
     * @RequestParam 用在下面这种参数携带方式
     * http://localhost:8080/sendEmail?username=20201427&password=123&email=2159289430@qq.com
     * @PathVariable用在下面这种携带参数方式 http://localhost:8080/sendEmail/20201427/123/2159289430@qq.com
     */
    @GetMapping("/sendEmail")
    public String send_email(Model model, @RequestParam("username") String username,
                             @RequestParam("password") String password, @RequestParam("email") String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setUsername(username);

        //先检查用户的其他信息是否合法,合法之后再发送验证码
        Map<String, Object> map = userService.check_message(user);
        if (map == null || map.isEmpty()) {

            map = userService.sendEmail(user); //给用户发送邮件
            if (map != null || !map.isEmpty())
                model.addAttribute("activationMsg", map.get("activationMsg"));
            map.clear();
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
        }
        return "/site/register";
    }


    /**
     * @Description: 生成验证码图片, 并返回给浏览器
     * @Param: [response]
     * @return: void
     */
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response) {
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //此验证码需要存一份放在redis中,同时需要提供一个key给客户端,方便对方提供验证码时,知道对应的是哪一个验证码
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);//验证码只在60s内有效

        /*
         * 正常的cookie只能在一个应用中共享，即一个cookie只能由创建它的应用获得
         *  通过setPath,可以使得同一服务器下的path下其他应用可以使用该Cookie
         *   详细知识介绍: https://www.bbsmax.com/A/kmzLGE0EdG/
         *
         * */
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        //存入redis中 时间也是60s
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        //将生成的验证码图片输出到浏览器中
        response.setContentType("imag/png");
        try {
            //获取web响应的写入流
            ServletOutputStream os = response.getOutputStream();
            //使用Image的写入方法,将生成的图片放入web写入流中
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * @Description: 验证用户输入的图片验证码是否和redis存入的是否相等
     * @Param: [kaptchaOwner 服务器提供的客户端的键值(等于redis中获取验证码的键值), checkCode(用户提供的验证码)]
     * @return: java.lang.String
     */
    private String checkKaptchaCode(String kaptchaOwner, String checkCode) {
        if (StringUtils.isBlank(checkCode)) return "未发现输入的图片验证码";

        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        String kaptchaValue = (String) redisTemplate.opsForValue().get(redisKey);
        if (StringUtils.isBlank(kaptchaValue)) {
            return "图片验证码过期";
        } else if (!kaptchaValue.toLowerCase(Locale.ROOT).equals(checkCode.toLowerCase(Locale.ROOT))) {
            return "图片验证码错误";
        }
        return "";
    }


    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam("code") String code,
                        @RequestParam(value = "rememberMe", required = false) boolean rememberMe,
                        Model model, HttpServletResponse response,
                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        /*
         * 检查验证码
         * */
        String kaptcha = null;

        //如果提交了验证码对应的cookie,用该cookie生成键从redis中获取验证码进行比对
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        //如果没有从redis中获取到验证码,或提交的验证码为空,或两个验证码不相等,返回错误提示信息,跳转登录页面
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码错误");
            return "/site/login";
        }

        //根据用户勾选的是否记住我选项,设置过期时间 默认是12小时 ,记住我是100天
        int expireSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        //验证用户名和密码
        Map<String, Object> map = userService.login(username, password, expireSeconds);

        /*
         * ticket里只放了一个useId,没有放用户密码  根据ticket,如果有效,不需要输入密码
         * 在拦截器中发现ticket还有效时就会自动从数据库中查找用户数据,不需要用户数输入
         *
         * */
        if (map.containsKey("ticket")) {
            //账号和密码均正确 则服务端会生成ticket,浏览器通过cookie 存储ticket
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expireSeconds);
            response.addCookie(cookie);
            //不写重定向就错了,不能转发post请求
            return "redirect:/index";
        } else { //登录失败,用户名或者密码错误,返回的map中会包含提示信息
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    /**
     * @Description: 用户登出, 设置凭证状态为无效
     * @Param: []
     * @return: java.lang.String
     */
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket) {
        //在redis中设置凭证状态Status为失效
        userService.logout(ticket);

        SecurityContextHolder.clearContext();//拦截器的最后已经调用一次了
        return "redirect:/login";
    }

    @PostMapping("/resetPwd")
    @ResponseBody
    public Map<String, Object> resetPwd(@RequestParam("username") String username,
                                        @RequestParam("password") String password,
                                        @RequestParam("emailVerifyCode") String emailVerifyCode,
                                        @RequestParam("kaptchaCode") String kaptcha,
                                        Model model,
                                        @CookieValue("kaptchaOwner") String kaptchaOwner) {
        Map<String, Object> map = new HashMap<>(4);

        //检查图片验证码
        String kaptchaCheckRst = checkKaptchaCode(kaptchaOwner, kaptcha);
        if (StringUtils.isNotBlank(kaptchaCheckRst)) {
            map.put("status", 1);
            map.put("errMsg", kaptchaCheckRst);
        }
        //检查邮件验证码
        String emailVerifyCodeCheckRst = checkRedisResetPwdEmailCode(username, emailVerifyCode);
        if (StringUtils.isNotBlank(emailVerifyCodeCheckRst)) {
            map.put("status", 1);
            map.put("errMsg", emailVerifyCodeCheckRst);
        }
        //执行重置密码操作
        Map<String, Object> stringObjectMap = userService.doResetPwd(username, password);
        String usernameMsg = (String) stringObjectMap.get("errMsg");
        if (StringUtils.isBlank(usernameMsg)) {
            map.put("status", 0);
            map.put("msg", "重置密码成功");
            map.put("target", "/login");
        }
        return map;
    }

    /**
     * @Description: 检查邮件验证码
     * @Param: [username, checkCode 用户输入的图片验证码]
     * @return: java.lang.String
     */
    private String checkRedisResetPwdEmailCode(String username, String checkCode) {
        if (StringUtils.isBlank(checkCode)) return "未发现输入的邮件验证码";

        final String redisKey = "EmailCode4ResetPwd:" + username;
        String emailVerifyCodeInRedis = (String) redisTemplate.opsForValue().get(redisKey);

        if (StringUtils.isBlank(emailVerifyCodeInRedis))
            return "邮件验证码已过期";
        else if (!emailVerifyCodeInRedis.equals(checkCode))
            return "邮件验证码错误";
        return "";

    }

    /**
     * @Description: 发送邮件验证码, 用于重置密码
     * @Param: [model, kaptchaOwner, kaptcha, username]
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     */
    @ResponseBody
    @PostMapping("/sendEmailCodeForResetPwd")
    public Map<String, Object> sendEmailCodeForResetPwd(Model model,
                                                        @CookieValue("kaptchaOwner") String kaptchaOwner,
                                                        @RequestParam("kaptcha") String kaptcha,
                                                        @RequestParam(value = "username") String username) {
        Map<String, Object> map = new HashMap<>();
        //检查图片验证码
        String kaptchaCheckRst = checkKaptchaCode(kaptchaOwner, kaptcha);
        if (StringUtils.isNotBlank(kaptchaCheckRst)) {
            map.put("status", 1);
            map.put("errMsg", kaptchaCheckRst);
            return map; //验证码错误后直接返回提示信息
        }

        Map<String, Object> stringObjectMap = userService.doSendEmailCode4ResetPwd(username);
        String usernameMsg = (String) stringObjectMap.get("errMsg");
        if (StringUtils.isBlank(usernameMsg)) {
            map.put("status", 0);
            map.put("msg", "我们已经往您的邮箱发送了一封邮件验证码,请查收");
        }
        return map;
    }
}


