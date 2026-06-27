package com.multiprofit.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户上下文 - 用于权限控制
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户角色
     */
    private Role role;

    /**
     * 可见机构列表
     */
    private List<String> visibleOrgs;

    /**
     * 可见产品列表
     */
    private List<String> visibleProducts;

    /**
     * 可见期间列表
     */
    private List<String> visiblePeriods;

    /**
     * 角色枚举
     */
    public enum Role {
        ADMIN,              // 管理员 - 全部权限
        BRANCH_MANAGER,     // 分行行长 - 本行数据
        CUSTOMER_MANAGER,   // 客户经理 - 本客户数据
        ANALYST             // 分析师 - 全部数据（只读）
    }
}
