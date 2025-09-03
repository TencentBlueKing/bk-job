package com.tencent.bk.job.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集群相关配置
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "cluster")
public class ClusterProperties {
    /**
     * 所在集群名称
     */
    private String name = "default";
}
