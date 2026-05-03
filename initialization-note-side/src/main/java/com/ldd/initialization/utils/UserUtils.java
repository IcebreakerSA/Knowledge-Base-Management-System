package com.ldd.initialization.utils;

import cn.dev33.satoken.stp.StpUtil;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.enums.RoleType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UserUtils {

    /**
     * 获取当前登录用户信息
     * 从Session中获取用户信息并转换为UserVO
     */
    public static User getCurrentUser() {
        User userObj = (User) StpUtil.getSession().get("user_info");
        if (userObj == null) {
            throw new BizException("用户信息未找到，请重新登录");
        }

        return userObj;
    }


    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * 获取当前用户角色类型
     */
    public static Integer getCurrentUserRoleType() {
        return getCurrentUser().getRoleType();
    }


    /**
     * 是否是管理员
     */
    public static Boolean isAdmin() {
        return Objects.equals(getCurrentUserRoleType(), RoleType.ADMIN.getValue());
    }


}