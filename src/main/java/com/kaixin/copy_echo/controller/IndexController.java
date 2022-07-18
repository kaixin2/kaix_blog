package com.kaixin.copy_echo.controller;

import com.kaixin.copy_echo.entity.DiscussPost;
import com.kaixin.copy_echo.entity.Page;
import com.kaixin.copy_echo.entity.User;
import com.kaixin.copy_echo.service.DiscussPostService;
import com.kaixin.copy_echo.service.LikeService;
import com.kaixin.copy_echo.service.UserService;
import com.kaixin.copy_echo.util.CommunityConstant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Controller
public class IndexController implements CommunityConstant {

    @Resource
    private DiscussPostService discussPostService;
    @Resource
    private UserService userService;
    @Resource
    private LikeService likeService;

    @GetMapping("/")
    public String root() {
        //请求转发到 /index 请求,为什么不能直接转呢
        // 不用forward,直接跳转,页面就会变成500
        return "forward:/index";
    }

    /**
     * @Description: 未注册用户进入时, 展示首页,
     * @Param: [model
     * 用来存储向前端页面传递的数据,因为用的是thymleaf模板,如果是前后端分离的项目则只需要
     * 提供接口,返回JSON数据即可
     * page 前端访问时自动封装了一个page对象过来,包括分页相关的信息,如分多少页,展示第几页的数据以及排序的类型
     * orderMode 分页的类型
     * ]
     * @return: java.lang.String
     */

    @GetMapping("/index")
    public String getIndexPage(Model model, Page page,
                               @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {

        //从数据库中的discussPost表获取总页数
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index?orderMode=" + orderMode);

        //分页查询
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(),
                page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        //封装帖子
        if (list != null) {
            for (DiscussPost post : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount", likeCount);
                discussPosts.add(map);
            }
        }

        //把查询得到的数据封装到model里,这样前端就可以用了
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);

        //跳转到 index.html 页面,前面都是在获取首页需要展示的数据
        return "index";
    }

    /**
     * 进入 500 错误界面
     *
     * @return
     */
    @GetMapping("/error")
    public String getErrorPage() {
        return "/error/500";
    }

    /**
     * 没有权限访问时的错误界面（也是 404）
     *
     * @return
     */
    @GetMapping("/denied")
    public String getDeniedPage() {
        return "/error/404";
    }
}
