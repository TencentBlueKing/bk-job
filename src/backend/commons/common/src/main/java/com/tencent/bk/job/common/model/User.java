package com.tencent.bk.job.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户基本信息
 */
@Data
@NoArgsConstructor
public class User {

    /**
     * 租户 ID
     */
    private String tenantId;

    /**
     * 账号名（唯一 ID）
     */
    private String username;

    /**
     * 展示名
     */
    private String displayName;

    public User(String tenantId, String username, String displayName) {
        this.tenantId = tenantId;
        this.username = username;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName + "[" + username + "@" + tenantId + "]";
    }
}
