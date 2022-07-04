package com.tencent.bk.job.execute.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gse.task.table.route")
@Getter
@Setter
@ToString
public class GseTaskTableRouteConfig {

    /**
     * 是否使用新表
     */
    private boolean newTableEnabled = true;

    /**
     * 根据stepInstanceId路由
     */
    private Long fromStepInstanceId;

}
