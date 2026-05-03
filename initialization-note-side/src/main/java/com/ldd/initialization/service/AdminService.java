package com.ldd.initialization.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.SharedKnowledgeBase;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.vo.AdminDashboardVO;
import com.ldd.initialization.vo.KnowledgeBaseMemberVO;

import java.util.List;

/**
 * 管理员服务接口
 */
public interface AdminService {

    AdminDashboardVO getDashboard();

    Page<User> listUsers(int page, int size, Integer roleType, String keyword, Integer status);

    User getUserDetail(Long userId);

    void updateUserStatus(Long userId, Integer status);

    void updateUserRole(Long userId, Integer roleType);

    void deleteUser(Long userId);

    void updateUser(Long userId, User user);

    Page<SharedKnowledgeBase> listKnowledgeBases(int page, int size, String keyword, Integer status);

    void updateKnowledgeBaseStatus(Long kbId, Integer status);

    void deleteKnowledgeBase(Long kbId);

    List<KnowledgeBaseMemberVO> getKnowledgeBaseMembers(Long kbId);

    void removeKnowledgeBaseMember(Long kbId, Long userId);

    void addKnowledgeBaseMember(Long kbId, Long userId);
}
