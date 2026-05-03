package com.ldd.initialization.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.vo.CaptchaVO;
import com.ldd.initialization.vo.UserInfoVO;

import java.util.List;

/**
* @author Administrator
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2025-02-21 13:43:03
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param phone 手机号
     * @param password 密码
     * @param captcha 验证码
     * @param captchaId 验证码ID
     * @param roleType 角色类型
     */
    void register(String phone, String password, String captcha, String captchaId, int roleType);

    /**
     * 用户登录
     * @param phone 手机号
     * @param password 密码
     * @param captcha 验证码
     * @param captchaId 验证码ID
     * @return Token
     */
    String login(String phone, String password, String captcha, String captchaId);

    User getUserInfo(Long userId);

    void updateUser(User user);

    Page<User> listUsers(int page, int size, Integer roleType, String keyword);

    void batchDisableUsers(List<Long> userIds);


    CaptchaVO generateCaptcha();

    UserInfoVO getUserInfoById(Long userId);

    void batchEnableUsers(List<Long> userIds);

    void deleteUser(Long userId);

    void updateUserRole(Long userId, Integer roleType);
}
