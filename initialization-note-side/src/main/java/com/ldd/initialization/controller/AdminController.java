package com.ldd.initialization.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ldd.initialization.domain.SharedKnowledgeBase;
import com.ldd.initialization.domain.User;
import com.ldd.initialization.result.PageResult;
import com.ldd.initialization.result.Result;
import com.ldd.initialization.service.AdminService;
import com.ldd.initialization.vo.AdminDashboardVO;
import com.ldd.initialization.vo.KnowledgeBaseMemberVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员接口
 */
@RestController
@RequestMapping("/api/admin")
@SaCheckRole("ADMIN")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> getDashboard() {
        return Result.success(adminService.getDashboard());
    }

    /**
     * 分页查询用户列表（管理员视图，支持更多筛选）
     */
    @GetMapping("/users")
    public Result<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer roleType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        Page<User> userPage = adminService.listUsers(page, size, roleType, keyword, status);
        return Result.success(PageResult.convert(userPage));
    }

    /**
     * 获取单个用户详情
     */
    @GetMapping("/users/{id}")
    public Result<User> getUserDetail(@PathVariable Long id) {
        return Result.success(adminService.getUserDetail(id));
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestBody UserStatusRequest request) {
        adminService.updateUserStatus(id, request.getStatus());
        return Result.success(null);
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestBody UserRoleRequest request) {
        adminService.updateUserRole(id, request.getRoleType());
        return Result.success(null);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.success(null);
    }

    /**
     * 编辑用户信息
     */
    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        adminService.updateUser(id, user);
        return Result.success(null);
    }

    // ==================== 知识库管理 ====================

    /**
     * 分页查询知识库列表
     */
    @GetMapping("/knowledge-bases")
    public Result<PageResult<SharedKnowledgeBase>> listKnowledgeBases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        Page<SharedKnowledgeBase> kbPage = adminService.listKnowledgeBases(page, size, keyword, status);
        return Result.success(PageResult.convert(kbPage));
    }

    /**
     * 更新知识库状态（启用/禁用）
     */
    @PutMapping("/knowledge-bases/{id}/status")
    public Result<Void> updateKnowledgeBaseStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        adminService.updateKnowledgeBaseStatus(id, body.get("status"));
        return Result.success(null);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/knowledge-bases/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        adminService.deleteKnowledgeBase(id);
        return Result.success(null);
    }

    /**
     * 获取知识库成员列表
     */
    @GetMapping("/knowledge-bases/{id}/members")
    public Result<List<KnowledgeBaseMemberVO>> getKnowledgeBaseMembers(@PathVariable Long id) {
        return Result.success(adminService.getKnowledgeBaseMembers(id));
    }

    /**
     * 移除知识库成员
     */
    @DeleteMapping("/knowledge-bases/{kbId}/members/{userId}")
    public Result<Void> removeKnowledgeBaseMember(@PathVariable Long kbId, @PathVariable Long userId) {
        adminService.removeKnowledgeBaseMember(kbId, userId);
        return Result.success(null);
    }

    /**
     * 添加知识库成员
     */
    @PostMapping("/knowledge-bases/{id}/members")
    public Result<Void> addKnowledgeBaseMember(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        adminService.addKnowledgeBaseMember(id, body.get("userId"));
        return Result.success(null);
    }

    /**
     * 用户状态请求体
     */
    static class UserStatusRequest {
        private Integer status;

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    /**
     * 用户角色请求体
     */
    static class UserRoleRequest {
        private Integer roleType;

        public Integer getRoleType() {
            return roleType;
        }

        public void setRoleType(Integer roleType) {
            this.roleType = roleType;
        }
    }
}
