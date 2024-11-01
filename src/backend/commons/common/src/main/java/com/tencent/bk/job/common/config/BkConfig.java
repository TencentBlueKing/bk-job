package com.tencent.bk.job.common.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 蓝鲸公共配置
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "bk")
@Configuration("bkCommonConfig")
public class BkConfig {
    /**
     * 蓝鲸根域名。指蓝鲸产品公共 cookies 写入的目录，同时也是各个系统的公共域名部分。
     */
    private String bkDomain;
    /**
     * BK助手链接。
     */
    private String bkHelperLink = "";
}
