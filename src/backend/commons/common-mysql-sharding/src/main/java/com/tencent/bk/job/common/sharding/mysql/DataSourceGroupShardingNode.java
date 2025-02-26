package com.tencent.bk.job.common.sharding.mysql;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 数据源组的分片节点元数据
 */
@Data
@NoArgsConstructor
@Slf4j
public class DataSourceGroupShardingNode {
    /**
     * 数据源分组名称
     */
    private String dataSourceGroupName;

    /**
     * db 节点数量
     */
    private int dbNodeCount;

    /**
     * 一个 db 节点上的表节点数量
     */
    private int tbNodeCount;

    /**
     * 总分片节点数量
     */
    private int sumSlot;

    private List<String> availableTargetDbs;

    public DataSourceGroupShardingNode(String dataSourceGroupName, int dbNodeCount, int tbNodeCount) {
        this.dataSourceGroupName = dataSourceGroupName;
        this.dbNodeCount = dbNodeCount;
        this.tbNodeCount = tbNodeCount;
        this.sumSlot = dbNodeCount * tbNodeCount;
        initAvailableTargetDbs();
    }

    private void initAvailableTargetDbs() {
        availableTargetDbs = new ArrayList<>(dbNodeCount);
        for (int suffix = 0; suffix < dbNodeCount; suffix++) {
            availableTargetDbs.add(dataSourceGroupName + "_" + suffix);
        }
    }

    public boolean validate() {
        if (StringUtils.isBlank(dataSourceGroupName)) {
            log.warn("Empty dataSourceGroupName");
            return false;
        }
        if (dbNodeCount <= 0) {
            log.warn("Invalid dbNodeCount : {}", dbNodeCount);
            return false;
        }
        if (tbNodeCount <= 0) {
            log.warn("Invalid tbNodeCount : {}", tbNodeCount);
            return false;
        }
        return true;
    }
}
