package com.tencent.bk.job.common.model;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Data;

import java.util.StringJoiner;

/**
 * 业务基础信息
 */
@Data
public class BasicApp {
    /**
     * 业务ID(内部ID）
     */
    private Long id;

    /**
     * 资源管理空间
     */
    private ResourceScope scope;

    /**
     * 业务名称
     */
    private String name;

    /**
     * 业务所属租户 ID
     */
    private String tenantId;

    private AppResourceScope appResourceScope;

    public AppResourceScope getAppResourceScope() {
        if (appResourceScope == null) {
            appResourceScope = new AppResourceScope(id, scope);
        }
        return appResourceScope;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BasicApp.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("scope=" + scope)
            .add("name='" + name + "'")
            .add("tenantId='" + tenantId + "'")
            .toString();
    }
}
