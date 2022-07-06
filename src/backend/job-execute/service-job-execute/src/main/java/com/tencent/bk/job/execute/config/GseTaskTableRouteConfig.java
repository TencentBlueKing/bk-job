package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;

@ConfigurationProperties(prefix = "gse.task.table.route")
@Getter
@Setter
@ToString
@Slf4j
public class GseTaskTableRouteConfig {

    /**
     * 是否使用新表
     */
    private boolean newTableEnabled = true;

    /**
     * 根据stepInstanceId路由
     */
    private Long fromStepInstanceId;

    @PostConstruct
    public void print() {
        log.info("GseTaskTableRouteConfig: {}", JsonUtils.toJson(this));
    }

}
