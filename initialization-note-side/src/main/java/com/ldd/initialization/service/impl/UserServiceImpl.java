package com.ldd.initialization.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.Note;
import com.ldd.initialization.domain.Notebook;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.enums.RoleType;
import com.ldd.initialization.enums.StatusEnum;
import com.ldd.initialization.mapper.UserMapper;
import com.ldd.initialization.service.UserService;
import com.ldd.initialization.vo.CaptchaVO;
import com.ldd.initialization.vo.UserInfoVO;
import io.github.yindz.random.RandomSource;
import org.springframework.beans.BeanUtils;
import com.ldd.initialization.mapper.NoteMapper;
import com.ldd.initialization.mapper.NotebookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2025-02-21 13:43:03
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Autowired
    private StringRedisTemplate redisTemplate; // 用于存储验证码

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NotebookMapper notebookMapper;

    private static final String CAPTCHA_PREFIX = "captcha:";

    @Override
    public void register(String phone, String password, String captcha, String captchaId, int roleType) {
        // 校验验证码
        //todo
        String storedCaptcha = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaId);
//        if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
//            throw new BizException("验证码错误或已过期");
//        }
//        if (roleType == RoleType.ADMIN.getValue()) {
//            throw new BizException("管理员权限不允许注册");
//        }

        // 检查手机号是否重复
        if (count(new QueryWrapper<User>().eq("phone", phone).eq("role_type", roleType)) > 0) {
            throw new BizException("手机号已被注册");
        }


        // 创建用户
        User user = new User();
        user.setPhone(phone);
        user.setPassword(DigestUtil.sha256Hex(password)); // SHA256 加密
        user.setRoleType(roleType);
        user.setStatus(StatusEnum.ENABLED.getCode()); // 默认启用
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        String name = RandomSource.personInfoSource().randomChineseName();
        if (roleType == RoleType.ADMIN.getValue()) {
            user.setAvatarUrl("https://www.keaitupian.cn/cjpic/frombd/2/253/2990600787/3256164520.jpg");

            user.setUsername(name);
            user.setRealName(name);
        } else {
            user.setAvatarUrl("https://www.keaitupian.cn/cjpic/frombd/0/253/2221658670/3422894636.jpg");
            user.setUsername(name);
            user.setRealName(name);
        }
        // 保存用户
        save(user);

        // 删除验证码
        redisTemplate.delete(CAPTCHA_PREFIX + captchaId);

    }

    @Override
    public String login(String phone, String password, String captcha, String captchaId) {
        // 校验验证码
        String storedCaptcha = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaId);
//        if (storedCaptcha == null || !storedCaptcha.equalsIgnoreCase(captcha)) {
//            throw new BizException("验证码错误或已过期");
//        }

        // 查询用户
        User user = getOne(new QueryWrapper<User>().eq("phone", phone));
        if (user == null) {
            throw new BizException("手机号或密码错误");
        }

        // 校验密码（使用相同的 SHA256 加密方式）
        String encryptedPassword = DigestUtil.sha256Hex(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BizException("手机号或密码错误");
        }

        // 检查账号状态
        if (Objects.equals(user.getStatus(), StatusEnum.DISABLED.getCode())) {
            throw new BizException("账号已被禁用");
        }


        String roleTypeStr = RoleType.getDescByValue(user.getRoleType());


        // 执行登录
        StpUtil.login(user.getId());
        //保存用户权限
        StpUtil.getSession().set("role", roleTypeStr);

        // 设置角色
        StpUtil.getSession().set("user_info", user);
        // 更新登录信息
        user.setLastLoginTime(new Date());
        user.setLastLoginIp("");
        updateById(user);

        User userObj = (User) StpUtil.getSession().get("user_info");
        System.out.println("userObj = " + userObj);
        // 删除验证码
        redisTemplate.delete(CAPTCHA_PREFIX + captchaId);
        // 返回 Token
        return StpUtil.getTokenValue();
    }


    @Override
    public User getUserInfo(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateUser(User user) {
        user.setUpdateTime(new Date());
        updateById(user);
    }

    @Override
    public Page<User> listUsers(int page, int size, Integer roleType, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (roleType != null) {
            wrapper.eq("role_type", roleType);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(q -> q.like("username", keyword)
                    .or().like("phone", keyword));
        }
        page(userPage, wrapper);
        return userPage;
    }

    @Override
    public void batchDisableUsers(List<Long> userIds) {
        update(null, new LambdaUpdateWrapper<User>()
                .in(User::getId, userIds)
                .set(User::getStatus, StatusEnum.DISABLED.getCode())
                .set(User::getUpdateTime, new Date()));
    }

    @Override
    public CaptchaVO generateCaptcha() {
        // 使用 Hutool 生成图形验证码（宽120，高40，4位字符）
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = lineCaptcha.getCode(); // 获取验证码文本
        String imageBase64 = lineCaptcha.getImageBase64(); // 获取Base64编码图片

        // 生成唯一ID
        String captchaId = IdUtil.simpleUUID();

        // 存储验证码到 Redis，5分钟有效期
        redisTemplate.opsForValue().set(CAPTCHA_PREFIX + captchaId, code, 5, TimeUnit.MINUTES);

        // 返回验证码响应
        CaptchaVO response = new CaptchaVO();
        response.setCaptchaId(captchaId);
        response.setImageBase64(imageBase64);
        return response;
    }

    @Override
    public UserInfoVO getUserInfoById(Long userId) {
        User user = baseMapper.selectById(userId);
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }

    @Override
    public void batchEnableUsers(List<Long> userIds) {
        update(null, new LambdaUpdateWrapper<User>()
                .in(User::getId, userIds)
                .set(User::getStatus, StatusEnum.ENABLED.getCode()) // status=1 表示启用
                .set(User::getUpdateTime, new Date()));
    }

    @Override
    public void deleteUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        // 删除用户笔记
        noteMapper.delete(new QueryWrapper<Note>().eq("user_id", userId));
        // 删除用户笔记本
        notebookMapper.delete(new QueryWrapper<Notebook>().eq("user_id", userId));
        // 删除用户
        removeById(userId);
    }

    @Override
    public void updateUserRole(Long userId, Integer roleType) {
        User user = getById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setRoleType(roleType);
        user.setUpdateTime(new Date());
        updateById(user);
    }


}




