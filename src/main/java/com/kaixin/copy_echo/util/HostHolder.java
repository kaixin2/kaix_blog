package com.kaixin.copy_echo.util;

import com.kaixin.copy_echo.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息,用来代替Session对象
 *
 * @author KaiXin
 * @version 1.8
 * @since1.5
 */
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();

    //存储User
    public void setUser(User user) {
        users.set(user);
    }

    //获取 User
    public User getUser() {
        return users.get();
    }

    //清理
    public void clear() {
        users.remove();
    }
}
