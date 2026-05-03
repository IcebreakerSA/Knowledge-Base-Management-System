package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.config.ai.PgVectorConnectionPool;
import com.ldd.initialization.domain.FileUpInfo;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.dto.LoginDTO;
import com.ldd.initialization.dto.RegisterDTO;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.FileInfoService;
import com.ldd.initialization.service.UserService;
import com.ldd.initialization.vo.CaptchaVO;
import com.ldd.initialization.vo.UserInfoVO;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用接口
 *
 * @author lxt
 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private PgVectorConnectionPool connectionPool;

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 注册结果
     */
    @SaIgnore
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO request) {
        userService.register(request.getPhone(), request.getPassword(), request.getCaptcha(), request.getCaptchaId(), request.getRoleType());
        return Result.success(null);
    }

    /**
     * 获取图形验证码
     *
     * @return 验证码图片和ID
     */
    @SaIgnore
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        CaptchaVO captcha = userService.generateCaptcha();
        return Result.success(captcha);
    }

    /**
     * 用户登录
     *
     * @param request 登录请求
     * @return Token
     */
    @SaIgnore
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO request) {
        String token = userService.login(request.getPhone(), request.getPassword(), request.getCaptcha(), request.getCaptchaId(), request.getRoleType());
        return Result.success(token);
    }

    /**
     * 退出登录
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success(null);
    }

    /**
     * 获取当前用户信息
     */
    @SaCheckLogin
    @GetMapping("/user/info")
    public Result<User> getUserInfo() {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        User user = userService.getUserInfo(userId);
        return Result.success(user);
    }


    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info/{userId}")
    public Result<UserInfoVO> getUserInfoById(@PathVariable Long userId) {
        UserInfoVO user = userService.getUserInfoById(userId);
        return Result.success(user);
    }


    /**
     * 更新用户信息
     */
    @PutMapping("/user/update")
    public Result<Void> updateUser(@RequestBody User user) {
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        user.setId(userId);
        userService.updateUser(user);
        return Result.success(null);
    }


    /**
     * 文件上传（特殊情况使用MultipartFile）
     */
    @SaIgnore
    @PostMapping("/file/upload")
    public Result<FileUpInfo> uploadFile(@RequestParam("file") MultipartFile file) {

//        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        Long userId = 1L;
        FileUpInfo fileInfo = fileInfoService.uploadFile(file, userId);
        return Result.success(fileInfo);
    }

    /**
     * 分页查询用户列表
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) String keyword
    ) {
        Page<User> userPage = userService.listUsers(page, size, roleType, keyword);
        return Result.success(PageResult.convert(userPage));
    }

    /**
     * 测试PgVector连接池状态
     * 
     * @return 连接池状态信息
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/test/connection-pool")
    public Result<Map<String, Object>> testConnectionPool() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取连接池状态
            String poolStatus = connectionPool.getPoolStatus();
            result.put("poolStatus", poolStatus);
            
            // 测试连接是否正常
            boolean connectionTest = connectionPool.testConnection();
            result.put("connectionTest", connectionTest);
            
            result.put("success", true);
            result.put("message", "连接池状态检查完成");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "连接池状态检查失败");
        }
        
        return Result.success(result);
    }

}