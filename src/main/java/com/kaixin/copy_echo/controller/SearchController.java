package com.kaixin.copy_echo.controller;

import com.kaixin.copy_echo.entity.DiscussPost;
import com.kaixin.copy_echo.entity.Page;
import com.kaixin.copy_echo.service.ElasticsearchService;
import com.kaixin.copy_echo.service.LikeService;
import com.kaixin.copy_echo.service.UserService;
import com.kaixin.copy_echo.util.CommunityConstant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索服务
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Controller
public class SearchController implements CommunityConstant {

    @Resource
    private ElasticsearchService elasticsearchService;

    @Resource
    private UserService userService;

    @Resource
    private LikeService likeService;

    @GetMapping("/search")
    public String search(String keyword, Page page, Model model) {
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());

        if(searchResult == null) System.out.println("searchResult == null");
        //聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResult != null) {
            for (DiscussPost post : searchResult) {
                Map<String, Object> map = new HashMap<>();
                //帖子
                map.put("post", post);
                //作者
                map.put("user", userService.findUserById(post.getUserId()));
                //点赞数量
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        //设置分页
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResult == null ? 0 : (int) searchResult.getTotalElements());
        return "/site/search";
    }
}
