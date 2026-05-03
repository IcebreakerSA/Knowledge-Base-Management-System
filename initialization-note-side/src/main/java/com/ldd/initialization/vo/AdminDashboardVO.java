package com.ldd.initialization.vo;

import lombok.Data;

/**
 * 管理员仪表盘统计数据
 */
@Data
public class AdminDashboardVO {

    private long totalUsers;
    private long enabledUsers;
    private long disabledUsers;
    private long totalNotes;
    private long totalNotebooks;
    private long totalKnowledgeBases;
    private long recentRegistrations;
}
