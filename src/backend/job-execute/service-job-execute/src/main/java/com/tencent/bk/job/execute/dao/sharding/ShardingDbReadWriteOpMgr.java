package com.tencent.bk.job.execute.dao.sharding;

import com.tencent.bk.job.common.mysql.MySQLProperties;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.sharding.ReadModeEnum;
import com.tencent.bk.job.common.sharding.WriteModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 水平分库分表 db 读写状态管理
 */
@Component
@Slf4j
public class ShardingDbReadWriteOpMgr {

    private final MySQLProperties mySQLProperties;

    private final ShardingMigrationRwModeMgr shardingMigrationRwModeMgr;

    private final DataSourceMode dataSourceMode;


    @Autowired
    public ShardingDbReadWriteOpMgr(
        MySQLProperties mySQLProperties,
        ObjectProvider<ShardingMigrationRwModeMgr> shardingMigrationRwModeMgrObjectProvider) {
        this.mySQLProperties = mySQLProperties;
        this.dataSourceMode = DataSourceMode.valOf(mySQLProperties.getDataSourceMode());
        this.shardingMigrationRwModeMgr = shardingMigrationRwModeMgrObjectProvider.getIfAvailable();
    }

    /**
     * 判断是否支持读请求
     */
    public boolean isReadEnabled() {
        if (dataSourceMode == DataSourceMode.HORIZONTAL_SHARDING) {
            // 数据源模式为 HORIZONTAL_SHARDING，说明已完全迁移到分片集群
            return true;
        }
        if (!mySQLProperties.getMigration().isEnabled()) {
            return false;
        }
        // 如果处于分库分表 db 迁移状态，需要根据上下文判断是否是否已迁移到分片集群
        ReadModeEnum readMode = shardingMigrationRwModeMgr.evaluateReadMode();
        switch (readMode) {
            case READ_ORIGIN:
                // 读单库
                return false;
            case READ_SHARDING:
                // 读分库
                return true;
        }
        throw new IllegalStateException("Unexpected read mode");
    }


    /**
     * 判断是否支持写请求
     */
    public boolean isWriteEnabled() {
        if (dataSourceMode == DataSourceMode.HORIZONTAL_SHARDING) {
            // 数据源模式为 HORIZONTAL_SHARDING，说明已完全迁移到分片集群
            return true;
        }
        if (!mySQLProperties.getMigration().isEnabled()) {
            return false;
        }

        // 如果处于分库分表 db 迁移状态，需要根据上下文判断是否是否已迁移到分片集群
        WriteModeEnum writeMode = shardingMigrationRwModeMgr.evaluateWriteMode();
        switch (writeMode) {
            case WRITE_ORIGIN:
                // 读单库
                return false;
            case WRITE_SHARDING:
            case WRITE_BOTH:
                // 读分库
                return true;
        }
        throw new IllegalStateException("Unexpected write mode");
    }
}
