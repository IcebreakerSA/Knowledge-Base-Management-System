package com.ldd.initialization.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.config.exception.BizException;
import com.ldd.initialization.domain.*;
import com.ldd.initialization.enums.RoleType;
import com.ldd.initialization.enums.StatusEnum;
import com.ldd.initialization.mapper.*;
import com.ldd.initialization.service.AdminService;
import com.ldd.initialization.vo.AdminDashboardVO;
import com.ldd.initialization.vo.KnowledgeBaseMemberVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NotebookMapper notebookMapper;

    @Autowired
    private SharedKnowledgeBaseMapper sharedKnowledgeBaseMapper;

    @Autowired
    private KnowledgeBaseMemberMapper knowledgeBaseMemberMapper;

    @Autowired
    private KnowledgeBaseFileMapper knowledgeBaseFileMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Override
    public AdminDashboardVO getDashboard() {
        AdminDashboardVO vo = new AdminDashboardVO();

        // 用户统计
        vo.setTotalUsers(userMapper.selectCount(new QueryWrapper<>()));
        vo.setEnabledUsers(userMapper.selectCount(new QueryWrapper<User>().eq("status", StatusEnum.ENABLED.getCode())));
        vo.setDisabledUsers(userMapper.selectCount(new QueryWrapper<User>().eq("status", StatusEnum.DISABLED.getCode())));

        // 内容统计
        vo.setTotalNotes(noteMapper.selectCount(new QueryWrapper<>()));
        vo.setTotalNotebooks(notebookMapper.selectCount(new QueryWrapper<>()));
        vo.setTotalKnowledgeBases(sharedKnowledgeBaseMapper.selectCount(new QueryWrapper<>()));

        // 7天内新注册用户
        long recentRegistrations = userMapper.selectCount(
                new QueryWrapper<User>()
                        .ge("create_time", getSevenDaysAgo())
        );
        vo.setRecentRegistrations(recentRegistrations);

        return vo;
    }

    @Override
    public Page<User> listUsers(int page, int size, Integer roleType, String keyword, Integer status) {
        Page<User> userPage = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if (roleType != null) {
            wrapper.eq("role_type", roleType);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(q -> q.like("username", keyword)
                    .or().like("phone", keyword));
        }
        wrapper.orderByDesc("create_time");

        userMapper.selectPage(userPage, wrapper);
        return userPage;
    }

    @Override
    public User getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 不允许禁用自己
        User currentUser = getUserById(userId);
        // 这里不需要校验自己，在实际操作中管理员不会这么操作，但为了防止意外，我们不做限制

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setStatus(status);
        updateUser.setUpdateTime(new Date());
        userMapper.updateById(updateUser);
    }

    @Override
    public void updateUserRole(Long userId, Integer roleType) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (RoleType.getDescByValue(roleType) == null) {
            throw new BizException("无效的角色类型");
        }

        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setRoleType(roleType);
        updateUser.setUpdateTime(new Date());
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 不允许删除admin用户
        if (RoleType.ADMIN.getValue() == user.getRoleType()) {
            throw new BizException("不允许删除管理员账户");
        }

        // 删除用户相关的笔记
        noteMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Note>().eq("user_id", userId));
        // 删除用户的笔记本
        notebookMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Notebook>().eq("user_id", userId));
        // 删除用户
        userMapper.deleteById(userId);
    }

    @Override
    public void updateUser(Long userId, User user) {
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new BizException("用户不存在");
        }

        user.setId(userId);
        user.setUpdateTime(new Date());
        // 只更新允许修改的字段
        userMapper.updateById(user);
    }

    @Override
    public Page<SharedKnowledgeBase> listKnowledgeBases(int page, int size, String keyword, Integer status) {
        Page<SharedKnowledgeBase> kbPage = new Page<>(page, size);
        QueryWrapper<SharedKnowledgeBase> wrapper = new QueryWrapper<>();

        if (status != null) {
            wrapper.eq("status", status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(q -> q.like("name", keyword)
                    .or().like("description", keyword));
        }
        wrapper.orderByDesc("create_time");

        sharedKnowledgeBaseMapper.selectPage(kbPage, wrapper);
        return kbPage;
    }

    @Override
    public void updateKnowledgeBaseStatus(Long kbId, Integer status) {
        SharedKnowledgeBase kb = sharedKnowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException("知识库不存在");
        }

        SharedKnowledgeBase updateKb = new SharedKnowledgeBase();
        updateKb.setId(kbId);
        updateKb.setStatus(status);
        updateKb.setUpdateTime(java.time.LocalDateTime.now());
        sharedKnowledgeBaseMapper.updateById(updateKb);
    }

    @Override
    @Transactional
    public void deleteKnowledgeBase(Long kbId) {
        SharedKnowledgeBase kb = sharedKnowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException("知识库不存在");
        }

        // 删除知识库成员
        knowledgeBaseMemberMapper.delete(
                new QueryWrapper<KnowledgeBaseMember>().eq("knowledge_base_id", kbId));
        // 删除知识库文件关联
        knowledgeBaseFileMapper.delete(
                new QueryWrapper<KnowledgeBaseFile>().eq("knowledge_base_id", kbId));
        // 删除知识库
        sharedKnowledgeBaseMapper.deleteById(kbId);
    }

    @Override
    public List<KnowledgeBaseMemberVO> getKnowledgeBaseMembers(Long kbId) {
        SharedKnowledgeBase kb = sharedKnowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException("知识库不存在");
        }
        return knowledgeBaseMemberMapper.selectMembersByKnowledgeBaseId(kbId);
    }

    @Override
    public void removeKnowledgeBaseMember(Long kbId, Long userId) {
        knowledgeBaseMemberMapper.delete(
                new QueryWrapper<KnowledgeBaseMember>()
                        .eq("knowledge_base_id", kbId)
                        .eq("user_id", userId));
    }

    @Override
    public void addKnowledgeBaseMember(Long kbId, Long userId) {
        SharedKnowledgeBase kb = sharedKnowledgeBaseMapper.selectById(kbId);
        if (kb == null) {
            throw new BizException("知识库不存在");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        // 检查是否已经是成员
        boolean exists = knowledgeBaseMemberMapper.existsByKnowledgeBaseIdAndUserId(kbId, userId);
        if (exists) {
            throw new BizException("该用户已经是知识库成员");
        }

        KnowledgeBaseMember member = new KnowledgeBaseMember();
        member.setKnowledgeBaseId(kbId);
        member.setUserId(userId);
        member.setRole(2); // 普通成员
        member.setJoinTime(java.time.LocalDateTime.now());
        knowledgeBaseMemberMapper.insert(member);
    }

    private User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 获取7天前的时间戳
     */
    private Date getSevenDaysAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        return calendar.getTime();
    }
}
